# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

Scaffolded, not yet running end-to-end. `android/` contains a Kotlin/Compose/Glance Android
project and `supabase/` contains the Postgres schema, RLS policies, and the `process-memo` Edge
Function (STT → Claude Haiku extraction → dedup → priority score). None of this has been
compiled, deployed, or run against real audio — it was written in a sandbox with no Android SDK,
no Supabase CLI, and no network access to Google's Maven repository (only Maven Central and the
Gradle Plugin Portal were reachable), so there was no way to verify a Gradle sync or an Edge
Function invocation. Treat this as a structurally-complete first draft that needs to survive
contact with Android Studio and a real Supabase project, not as tested code.

**What's actually been verified:** the deterministic priority-scoring logic
(`android/app/src/main/kotlin/com/nudge/app/data/PriorityScore.kt`) is pure Kotlin with no
Android dependency, so it was compiled and unit-tested for real in a throwaway JVM sandbox
before being committed (test lives at
`android/app/src/test/kotlin/com/nudge/app/data/PriorityScoreTest.kt`). Everything else —
Gradle config, Room, Glance, WorkManager, the supabase-kt client calls, the Edge Function — is
unverified by a compiler.

**Known gaps to close before this is a real MVP, roughly in order:**
1. Open `android/` in Android Studio and fix whatever the first Gradle sync surfaces —
   supabase-kt's exact API shape (`SupabaseClientProvider.kt`, `TaskRepository.kt`,
   `CaptureRepository.kt`, `NudgePreferences.kt` all carry a note about this) is the most likely
   source of compile errors, since its DSL has shifted across major versions.
2. Stand up a real Supabase project, run the `supabase/migrations/`, deploy `process-memo` with
   real STT/Anthropic/Voyage API keys, and run the PRD's own Week 1 exit criteria: process ~15
   real rambling memos (EN + mixed EN/ID) and subjectively check extraction quality and
   per-memo cost before building anything further on top of it.
3. Dedup-merge visibility in the Review screen (M2: "merge visible in review history") isn't
   wired up yet — `dedup_merges` exists server-side but nothing on the client reads it.
4. Instrumented tests for the M1 edge cases (permission-denied, offline queue, call
   interruption) don't exist yet — only the one pure-logic unit test above does.
5. The daily notification (M4: "max one push/day, off by default") has a toggle in Settings but
   no actual scheduling behind it yet.
6. `usage_daily.estimated_cost_usd` is never populated — the COGS KPI has no real numbers behind
   it until token/second-based cost accounting is added to `process-memo`.
7. No real app icon/brand palette — placeholder shapes and a neutral color only.

This document is derived from §4–§8 of the PRD per its own instruction (§12, "Development
handoff"). Treat the PRD (`docs/prd.md`) as the source of truth; this file is a distilled,
implementation-facing summary of its constraints. If the two ever disagree, defer to the PRD
and flag the conflict.

### Commands

```
cd android && ./gradlew assembleDebug   # requires Android Studio / a configured Android SDK
cd android && ./gradlew testDebugUnitTest
cd supabase && supabase db reset        # requires the Supabase CLI + `supabase start`
cd supabase && supabase functions serve process-memo --env-file .env
```

No Gradle wrapper jar is committed (generating one needs network access this sandbox didn't
have) — Android Studio will offer to create one on first open, or run `gradle wrapper` yourself.

## What Nudge is

A voice-first task copilot for adults with ADHD, Android-only. The user rambles into a widget; the app transcribes, extracts tasks, dedupes, and silently triages — surfacing exactly **one** next action at a time. There is no user-facing to-do list (except the review/correct screen). The retention bet being tested is a shame-free design: no streaks, no overdue states, no backlog on return after absence.

**Northstar metric:** Week-2 capture retention (≥3 brain dumps during days 8–14). Every feature decision should be evaluated against whether it helps or risks this metric.

## Hard guardrails — out of scope for this phase

These are permanent or phase-long constraints, not just unbuilt features. Do not add any of the following without explicit user sign-off, even if it seems like a natural extension:

- No AI body doubling / PiP avatar / voice companion.
- No context-awareness of any kind: no screen reading, no accessibility-service usage, no app-usage monitoring, no `QUERY_ALL_PACKAGES`, no background/always-on mic.
- Android phone only — no iOS, browser extension, smartwatch, or tablet layouts.
- No calendar/email/third-party integrations (no Google Calendar sync, no Gmail parsing).
- No due dates or rigid scheduling — the model is queue + energy tag, not calendar.
- No collaboration, sharing, or family/coach accounts.
- No gamification of any kind (streaks, points, avatars, leaderboards, badges) — directly conflicts with the shame-free design requirement (M4).
- No medical/therapeutic claims anywhere in product copy or store listing ("treats," "manages," "improves ADHD symptoms" are all forbidden).
- No web dashboard.

## Core architecture (target, per §7)

**Client** — Android native, Kotlin, Jetpack Compose for the app, Jetpack Glance for the home-screen widget (the widget is the primary product surface; the in-app experience is secondary). Min SDK 26. Offline-first: Room DB as local queue for audio + pending tasks, WorkManager for retryable upload/processing. Recording via standard MediaRecorder/AudioRecord, push-to-talk only — no foreground always-on service.

**Backend** — Supabase: Auth (anonymous → email upgrade), Postgres with RLS (per-user isolation on every table), Storage for transient audio, Edge Functions for the processing pipeline.

**AI pipeline** (server-side Edge Function only — no on-device LLM in MVP):
1. Transcription: multilingual STT (must handle EN/Bahasa Indonesia code-switching). On-device Android SpeechRecognizer is a degraded fallback only.
2. Extraction: small/fast LLM (Claude Haiku class) with a fixed, versioned prompt returning strict JSON `{tasks: [{text, effort, confidence, source_span}], notes: [...]}`. Schema-validated; malformed output → one retry → fail safe to raw transcript. Never fabricate a task from ambiguous/venting input — store as a note instead.
3. Dedup: embedding similarity against the user's open tasks, threshold-merged, merge visible in review history.
4. Priority scoring: deterministic heuristic (effort, recency, skip count) — **not** an LLM call. This keeps the widget's top-pick selection cheap, fast, and explainable.

**Data flow:** widget tap → local audio file → queued upload → Edge Function (STT → LLM → dedup → score) → Postgres → push sync → widget update. Raw audio is deleted server-side immediately after successful transcription by default (TTL ≤ 24h even on pipeline failure); transcript retained unless the user disables retention.

## Feature acceptance criteria (§4 — treat as binding for implementation)

- **Capture (M1):** widget tap → recording active in <1.5s p90. Works fully offline with queued upload. Max recording 5 min free / 15 min paid. Mic-permission-denied must show a clear recovery screen, never a silent failure. Call interruption mid-recording must save partial audio.
- **Extraction & triage (M2):** recording end → tasks visible in <15s p90 for a 60s memo. Ambiguous items are kept verbatim and marked low-confidence for one-tap confirmation — never silently rewritten. Near-duplicates are merged with the merge logged/visible.
- **Next Action widget (M3):** resting state shows exactly one task, two actions (Done / Not Today). Never a list, never a remaining-count. Skips are silent — no "skipped N times" messaging anywhere.
- **Shame-free system (M4, cross-cutting):** no streaks, no overdue states, no red/guilt badges, no guilt copy. Max one push notification/day, off by default. Apply this as a constraint check to every new screen, not just onboarding.
- **Review & correct (M5):** the only screen where a list exists. Every extracted task traceable to its source memo.

## Tone and copy rules (§6)

- No exclamation marks in system messages.
- No "you haven't…" constructions or any guilt/scolding phrasing.
- No red used for "overdue" — overdue does not exist as a state in this product.
- Warm, brief tone; never chirpy.
- All user-facing copy lives in `android/app/src/main/res/values/strings.xml` so the M4 "copy
  audit checklist" has one place to check.

## Non-functional targets (§8, worth checking against when implementing the pipeline or widget)

- Widget tap → recording active: <1.5s p90.
- Recording end → tasks synced: <15s p90 (60s memo), <30s p99.
- Widget Done/skip → UI update: <5s.
- Cold app start: <2s on a mid-range device.
- Zero data loss: captured audio must survive app kill, reboot, and 72h offline.
- AI COGS target ≤ $0.15/user/month — priority scoring must stay non-LLM to hold this line; be deliberate before adding any new LLM call to a hot path.

## Open decisions (§ Appendix — unresolved as of PRD v1.0)

If work touches these areas, flag rather than silently assuming:
1. Day-8 retention mechanism — shame-free design is the current bet, unvalidated.
2. Context awareness — conceded off, but confirm before building anything that could be read as context-aware.
3. Differentiation one-liner ("Speak it and forget it") — drives store listing and beta pitch copy; confirm wording before using it in user-facing text.
