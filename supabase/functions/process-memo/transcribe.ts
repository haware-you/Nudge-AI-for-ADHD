import { config } from "../_shared/config.ts";

// Isolated behind this single function so the STT vendor can be swapped without touching the
// rest of the pipeline (PRD R5). Default targets any Whisper-API-shaped multipart endpoint.
// Must handle EN/Bahasa Indonesia code-switching in one memo (PRD §4 M2) — validate against
// real rambling memos in the Week 1 spike per the roadmap; if quality is insufficient, swap
// STT_API_URL/STT_MODEL to a different multilingual provider without touching callers.
export async function transcribeAudio(
  audioBytes: Uint8Array,
  mimeType: string,
): Promise<string> {
  const form = new FormData();
  form.append("model", config.sttModel);
  form.append(
    "file",
    new Blob([audioBytes], { type: mimeType }),
    `memo.${mimeType.includes("mp4") ? "m4a" : "wav"}`,
  );

  const response = await fetch(config.sttApiUrl, {
    method: "POST",
    headers: { Authorization: `Bearer ${config.sttApiKey}` },
    body: form,
  });

  if (!response.ok) {
    const body = await response.text().catch(() => "");
    throw new Error(`STT request failed (${response.status}): ${body}`);
  }

  const data = await response.json();
  const transcript = typeof data.text === "string" ? data.text.trim() : "";
  if (!transcript) {
    throw new Error("STT returned an empty transcript");
  }
  return transcript;
}
