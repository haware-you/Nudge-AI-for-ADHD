# Nudge

A voice-first task copilot for adults with ADHD, Android-only. Ramble into a home-screen widget; the app transcribes, extracts tasks, dedupes, and silently triages — surfacing exactly **one** next action at a time.

See [`docs/prd.md`](docs/prd.md) for the full product spec and [`CLAUDE.md`](CLAUDE.md) for the implementation-facing constraints (tech stack, out-of-scope guardrails, acceptance criteria, copy rules). CLAUDE.md is a distilled summary; the PRD is the source of truth if they ever disagree.

## Repository layout

```
android/            Native Android client (Kotlin, Jetpack Compose, Jetpack Glance widget)
supabase/           Backend: Postgres schema/RLS migrations + Edge Functions (AI pipeline)
docs/prd.md         Product requirements document (v1.0)
```

## Status

Early scaffold — see `CLAUDE.md` for exactly what exists vs. what's still a stub. This is not yet a working end-to-end product; it is the Week 1–3 structural groundwork described in the PRD's roadmap (§11).

## Android app

```
cd android
./gradlew assembleDebug   # requires Android SDK (compileSdk/targetSdk 34, minSdk 26)
./gradlew testDebugUnitTest
```

Building requires Android Studio or a configured Android SDK — this was scaffolded without one available, so it has not been compiled/verified locally. Review it in Android Studio before trusting it builds clean.

## Supabase backend

```
cd supabase
supabase start                       # local dev stack
supabase db reset                    # applies migrations/
supabase functions serve process-memo --env-file .env
```

Requires the Supabase CLI and a `.env` with `ANTHROPIC_API_KEY`, an STT provider key, and an embeddings provider key (see `supabase/functions/process-memo/README.md`). No secrets are committed — copy `supabase/.env.example` to `.env` and fill in your own keys.
