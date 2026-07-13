import { createClient } from "npm:@supabase/supabase-js@2";
import { corsHeaders } from "../_shared/cors.ts";
import { config } from "../_shared/config.ts";
import { transcribeAudio } from "./transcribe.ts";
import { extractTasks } from "./extract.ts";
import { embedText, findDuplicate } from "./dedup.ts";
import { computePriorityScore } from "./score.ts";
import { LOW_CONFIDENCE_THRESHOLD } from "./types.ts";

const MAX_RETRIES = 3;

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "content-type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "POST") {
    return json({ error: "method_not_allowed" }, 405);
  }

  // Verify the caller's identity with the anon client + their JWT (respects RLS), then do all
  // subsequent work with the service-role client — notes/dedup_merges/usage_daily have no
  // client-facing insert policy by design (see supabase/migrations/0002_rls.sql).
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    return json({ error: "missing_authorization" }, 401);
  }
  const jwt = authHeader.replace(/^Bearer\s+/i, "");

  const authClient = createClient(config.supabaseUrl, config.supabaseAnonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userError } = await authClient.auth.getUser(jwt);
  if (userError || !userData?.user) {
    return json({ error: "invalid_session" }, 401);
  }
  const userId = userData.user.id;

  let memoId: string | undefined;
  try {
    const body = await req.json();
    memoId = body.memo_id;
  } catch {
    // memoId stays undefined, handled below
  }
  if (!memoId) {
    return json({ error: "missing_memo_id" }, 400);
  }

  const db = createClient(config.supabaseUrl, config.supabaseServiceRoleKey);

  const { data: memo, error: memoError } = await db
    .from("memos")
    .select("*")
    .eq("id", memoId)
    .eq("user_id", userId)
    .single();

  if (memoError || !memo) {
    return json({ error: "memo_not_found" }, 404);
  }

  // Idempotent: a WorkManager retry that lands after a previous call already succeeded should
  // not reprocess or double-charge usage.
  if (memo.status === "done" || memo.status === "processing") {
    return json({ memo_id: memoId, status: memo.status });
  }

  const { data: profile } = await db
    .from("profiles")
    .select("plan, retain_transcript")
    .eq("id", userId)
    .single();

  const today = new Date().toISOString().slice(0, 10);
  const { data: usage } = await db
    .from("usage_daily")
    .select("capture_count")
    .eq("user_id", userId)
    .eq("day", today)
    .maybeSingle();

  if (profile?.plan === "free" && (usage?.capture_count ?? 0) >= config.freeDailyCaptureLimit) {
    return json({ error: "daily_capture_limit_reached" }, 402);
  }

  await db.from("memos").update({ status: "processing" }).eq("id", memoId);

  try {
    if (!memo.audio_path) {
      throw new Error("memo has no audio_path");
    }
    const { data: audioBlob, error: downloadError } = await db.storage
      .from("audio")
      .download(memo.audio_path);
    if (downloadError || !audioBlob) {
      throw new Error(`audio download failed: ${downloadError?.message}`);
    }
    const audioBytes = new Uint8Array(await audioBlob.arrayBuffer());

    const transcript = await transcribeAudio(audioBytes, audioBlob.type || "audio/mp4");

    // Transcript is durable the moment we have it — an extraction failure below must never
    // lose it (PRD M2 error handling: "raw transcript preserved... never data loss").
    await db
      .from("memos")
      .update({ status: "transcribed", transcript })
      .eq("id", memoId);

    const extraction = await extractTasks(transcript);

    if (!extraction) {
      // Fail-safe: never fabricate a task from output we couldn't validate. Preserve the
      // transcript as a note so nothing is lost, per CLAUDE.md's extraction fail-safe rule.
      await db.from("notes").insert({
        user_id: userId,
        source_memo_id: memoId,
        text: transcript,
      });
    } else {
      const { data: openTasks } = await db
        .from("tasks")
        .select("id, embedding")
        .eq("user_id", userId)
        .eq("status", "open")
        .not("embedding", "is", null);

      const candidateEmbeddings: { id: string; embedding: number[] }[] = (openTasks ?? []).map(
        (t) => ({ id: t.id, embedding: t.embedding }),
      );

      for (const task of extraction.tasks) {
        const embedding = await embedText(task.text);
        const duplicate = findDuplicate(embedding, candidateEmbeddings);

        if (duplicate) {
          await db.from("dedup_merges").insert({
            user_id: userId,
            surviving_task_id: duplicate.taskId,
            merged_text: task.text,
            merged_source_memo_id: memoId,
            similarity: duplicate.similarity,
          });
          // Touch the surviving task so recency-based scoring reflects the fresh mention.
          await db.from("tasks").update({ updated_at: new Date().toISOString() }).eq(
            "id",
            duplicate.taskId,
          );
        } else {
          const { data: inserted } = await db
            .from("tasks")
            .insert({
              user_id: userId,
              source_memo_id: memoId,
              text: task.text,
              source_span: task.source_span,
              effort: task.effort,
              confidence: task.confidence,
              low_confidence: task.confidence < LOW_CONFIDENCE_THRESHOLD,
              embedding,
            })
            .select("id")
            .single();
          if (inserted) {
            candidateEmbeddings.push({ id: inserted.id, embedding });
          }
        }
      }

      for (const note of extraction.notes) {
        await db.from("notes").insert({ user_id: userId, source_memo_id: memoId, text: note });
      }
    }

    // Recompute priority for every open task — cheap deterministic pass, not an LLM call,
    // so it can run on every memo without threatening the COGS target.
    const { data: openTasksForScoring } = await db
      .from("tasks")
      .select("id, effort, updated_at, skip_count")
      .eq("user_id", userId)
      .eq("status", "open");

    for (const t of openTasksForScoring ?? []) {
      const priority_score = computePriorityScore({
        effort: t.effort,
        updatedAt: new Date(t.updated_at),
        skipCount: t.skip_count,
      });
      await db.from("tasks").update({ priority_score }).eq("id", t.id);
    }

    const finalTranscript = profile?.retain_transcript === false ? null : transcript;

    await db
      .from("memos")
      .update({
        status: "done",
        processed_at: new Date().toISOString(),
        transcript: finalTranscript,
        audio_deleted_at: new Date().toISOString(),
      })
      .eq("id", memoId);

    await db.storage.from("audio").remove([memo.audio_path]);

    await db.from("usage_daily").upsert(
      {
        user_id: userId,
        day: today,
        capture_count: (usage?.capture_count ?? 0) + 1,
      },
      { onConflict: "user_id,day" },
    );

    return json({ memo_id: memoId, status: "done" });
  } catch (err) {
    const retryCount = (memo.retry_count ?? 0) + 1;
    const nextStatus = retryCount >= MAX_RETRIES ? "stuck" : "failed";
    await db
      .from("memos")
      .update({
        status: nextStatus,
        retry_count: retryCount,
        error: err instanceof Error ? err.message : String(err),
      })
      .eq("id", memoId);
    return json({ memo_id: memoId, status: nextStatus, error: "processing_failed" }, 500);
  }
});
