// Central place to read env vars so a missing one fails fast and loud at request time,
// rather than as a confusing null deep in a pipeline stage.
function required(name: string): string {
  const value = Deno.env.get(name);
  if (!value) {
    throw new Error(`Missing required env var: ${name}`);
  }
  return value;
}

export const config = {
  supabaseUrl: required("SUPABASE_URL"),
  supabaseAnonKey: required("SUPABASE_ANON_KEY"),
  supabaseServiceRoleKey: required("SUPABASE_SERVICE_ROLE_KEY"),

  anthropicApiKey: required("ANTHROPIC_API_KEY"),
  // Claude Haiku class per PRD §7 — small/fast model for the extraction step.
  extractionModel: Deno.env.get("ANTHROPIC_EXTRACTION_MODEL") ?? "claude-haiku-4-5-20251001",

  // STT provider is abstracted behind transcribe.ts (PRD R5: swappable without client release).
  // Any Whisper-API-compatible endpoint works; default targets OpenAI's hosted transcription API.
  sttApiKey: required("STT_API_KEY"),
  sttApiUrl: Deno.env.get("STT_API_URL") ?? "https://api.openai.com/v1/audio/transcriptions",
  sttModel: Deno.env.get("STT_MODEL") ?? "whisper-1",

  // Voyage AI is Anthropic's recommended embeddings partner; voyage-3-lite is 512-dim, cheap,
  // and sized to match the `vector(512)` column in 0001_schema.sql.
  embeddingsApiKey: required("VOYAGE_API_KEY"),
  embeddingsModel: Deno.env.get("VOYAGE_EMBEDDINGS_MODEL") ?? "voyage-3-lite",

  freeDailyCaptureLimit: Number(Deno.env.get("FREE_DAILY_CAPTURE_LIMIT") ?? "5"),
  dedupSimilarityThreshold: Number(Deno.env.get("DEDUP_SIMILARITY_THRESHOLD") ?? "0.86"),
};
