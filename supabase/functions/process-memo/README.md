# process-memo

The server-side AI pipeline from PRD §7: transcription → extraction → dedup → priority scoring.
Triggered by the Android client after it finishes uploading a memo's audio to the `audio` storage
bucket and inserting the corresponding `memos` row (see `android/.../CaptureRepository.kt`).

## Request

```
POST /functions/v1/process-memo
Authorization: Bearer <user's Supabase JWT>
Content-Type: application/json

{ "memo_id": "<uuid>" }
```

The function re-verifies the JWT and only operates on a memo owned by that user — it does not
trust a `user_id` from the request body.

## Local dev

```
cd supabase
cp .env.example .env   # fill in real API keys
supabase start
supabase functions serve process-memo --env-file .env
```

Then POST to `http://localhost:54321/functions/v1/process-memo` with a real user JWT (from
`supabase.auth`) and a `memo_id` that already exists with `audio_path` pointing at a real
uploaded file.

## Notes on this being a scaffold, not a validated pipeline

This code has not been run against real audio or a live Supabase project — there is no Deno
runtime or Supabase CLI in the environment this was written in. Per the PRD's own Week 1 exit
criteria ("extraction precision subjectively usable; per-memo cost measured"), do not build UI
on top of this until you've run it against ~15 real rambling memos (EN + mixed EN/ID) and
confirmed:

- Transcription quality on code-switched speech (§ Risk R3) — if it's bad, swap `STT_API_URL`
  before doing anything else.
- The extraction prompt in `extract.ts` actually avoids fabricating tasks on ambiguous/venting
  input (the whole point of M2's edge cases). Adjust the prompt and bump
  `EXTRACTION_PROMPT_VERSION` if not.
- `DEDUP_SIMILARITY_THRESHOLD` (0.86 default) merges genuine near-duplicates without
  over-merging — tune against real data, it's a guess.
- Per-memo cost against the $0.15/user/month COGS target. Fill in the `PRICE_*` vars in
  `.env` with current prices for your configured models, then read `memos.estimated_cost_usd`
  (and the raw `stt_seconds` / `llm_*_tokens` / `embedding_tokens` columns) per memo.
  Cross-check the totals against the providers' own billing dashboards once.
