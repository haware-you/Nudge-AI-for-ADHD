import { createClient } from "npm:@supabase/supabase-js@2";
import { corsHeaders } from "../_shared/cors.ts";

// PRD §8: "delete-account (full purge)". Removes the caller's stored audio, then the auth user;
// every table references auth.users with ON DELETE CASCADE (supabase/migrations/0001_schema.sql),
// so the rows go with it. Reads its own env vars instead of ../_shared/config.ts so it does not
// need the AI provider keys that process-memo requires.
function env(name: string): string {
  const value = Deno.env.get(name);
  if (!value) throw new Error(`Missing required env var: ${name}`);
  return value;
}

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

  // The user id always comes from the verified JWT, never from the request body.
  const authHeader = req.headers.get("Authorization");
  if (!authHeader) {
    return json({ error: "missing_authorization" }, 401);
  }
  const jwt = authHeader.replace(/^Bearer\s+/i, "");
  const authClient = createClient(env("SUPABASE_URL"), env("SUPABASE_ANON_KEY"), {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userError } = await authClient.auth.getUser(jwt);
  if (userError || !userData?.user) {
    return json({ error: "invalid_session" }, 401);
  }
  const userId = userData.user.id;

  const db = createClient(env("SUPABASE_URL"), env("SUPABASE_SERVICE_ROLE_KEY"));

  // Audio lives at `<user id>/<memo id>.m4a` in the `audio` bucket (0003_storage.sql).
  const bucket = db.storage.from("audio");
  for (;;) {
    const { data: files, error: listError } = await bucket.list(userId, { limit: 100 });
    if (listError) return json({ error: "storage_list_failed" }, 500);
    if (!files || files.length === 0) break;
    const { error: removeError } = await bucket.remove(files.map((f) => `${userId}/${f.name}`));
    if (removeError) return json({ error: "storage_remove_failed" }, 500);
  }

  const { error: deleteError } = await db.auth.admin.deleteUser(userId);
  if (deleteError) {
    return json({ error: "delete_failed" }, 500);
  }
  return json({ deleted: true });
});
