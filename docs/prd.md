# Product Requirements Document: Nudge

**Version:** 1.0 (MVP Scope)
**Date:** July 2026
**Owner:** Haw (solo builder — product, architecture, implementation)
**Status:** Draft — contains 3 flagged OPEN DECISIONS requiring owner sign-off

---

## 1. Executive Summary & Product Overview

**Product Vision:** Nudge is a voice-first task copilot for adults with ADHD. Users ramble everything on their mind into a single widget tap; the AI extracts, deduplicates, and triages the tasks, then surfaces exactly **one** small next action back to the user. The user never manages a list.

**Core Problem:** People with ADHD experience capture overload and task paralysis. Traditional to-do apps demand the very executive function (organizing, prioritizing, maintaining) that the user lacks, producing the documented "app abandonment loop."

**Core Promise:** *"Speak it and forget it. Nudge triages for you and hands back one small step."*

**MVP Goal (what this document scopes):** Validate that zero-triage voice capture plus a single-next-action widget retains ADHD users past the week-2 abandonment cliff. The MVP is a retention experiment, not a feature showcase.

**What Nudge is NOT (positioning guardrails):**
- Not a to-do list manager. No manual sorting, tagging, or project hierarchies.
- Not a medical or therapeutic product. No treatment claims (Play Store and legal constraint).
- Not context-aware. It does not read the screen or observe user activity. **[OPEN DECISION 2 — conceded: all proactivity is session/schedule-based]**

---

## 2. Target Audience & User Personas

**Primary market:** English-speaking adults with ADHD (diagnosed or self-identified), 20–45, smartphone-primary. Secondary consideration: Bahasa Indonesia support is cheap via multilingual transcription and opens a home-market beta pool — flagged in roadmap, not a launch requirement.

### Persona A — "Maya," 34, serial app abandoner
- **Profile:** Diagnosed ADHD, office worker, has installed 15+ productivity apps. Technical proficiency: average consumer.
- **Pain points:** Every app works for one novelty week, then becomes another source of guilt. Notification blindness sets in by day 8. Overdue-task screens feel like being scolded.
- **What wins her:** An app that costs nothing to return to after a two-week absence. No streaks, no red badges, no backlog shame.

### Persona B — "Dan," 28, privacy-conscious developer
- **Profile:** Self-identified ADHD, technically sophisticated, reads permission manifests.
- **Pain points:** Rejects anything requesting accessibility services, screen capture, or always-on microphone. Wrong-context prompts ("still working on that email?" when he finished it) feel worse than silence.
- **What wins him:** Explicit push-to-talk capture, audio deleted after transcription, a readable privacy policy.

### Persona C — "Rin," 22, price-sensitive student
- **Profile:** Unmedicated (cost), heavy voice-note user, low tolerance for subscriptions.
- **Pain points:** Working memory — thoughts evaporate between classes. Paid $2 once for Goblin.tools; will not pay $10/month for anything.
- **What wins her:** A genuinely useful free tier; a paid tier priced as an impulse decision, not a commitment.

**Pain-point evidence basis:** Reddit/X community analysis (r/ADHD, r/adhdwomen). Confidence: medium — this is complaint-mining, evidence of pain but not willingness to pay. Willingness-to-pay is a beta-phase hypothesis to test, not an established fact.

---

## 3. User Stories & Use Cases

**Capture**
- As Rin, I want to dump every thought in one rambling voice memo from my lock screen so that nothing evaporates before I can write it down.
- As Rin, I want capture to work offline (queued upload) so that a dead spot never costs me a thought.

**Triage (invisible to user)**
- As Maya, I want the app to decide what matters without asking me so that I never face a triage screen.
- As Maya, I want duplicate or overlapping tasks from separate rambles merged automatically so that my list doesn't rot into noise.

**Next action**
- As Maya, I want my home-screen widget to show exactly one small next action so that I never see an overwhelming list.
- As Maya, I want a one-tap "not today" skip that carries no penalty so that saying no doesn't cost me guilt.
- As Maya, on a low-energy day I want one tap to switch the widget to only easy tasks so that I can still get a win.

**Return after absence**
- As Maya, I want to come back after two weeks and see a calm "welcome back — here's one thing" screen, not a backlog of 47 overdue items, so that returning is cheaper than staying away. **[OPEN DECISION 1 — this is the primary retention bet]**

**Trust**
- As Dan, I want my raw audio deleted immediately after transcription (default on) so that I control my data footprint.
- As Dan, I want to see and correct what the AI extracted so that a mis-heard task doesn't silently pollute my queue.

---

## 4. Features & Requirements (MoSCoW)

### MUST HAVE

**M1 — One-tap voice capture (widget + in-app)**
- Android home-screen widget: single tap starts recording; tap again to stop. Push-to-talk only; no wake word; no background listening.
- Acceptance criteria:
  - Recording starts < 1.5 s after widget tap (p90).
  - Works offline: audio stored locally, queued, uploaded on connectivity.
  - Max recording length 5 min (free) / 15 min (paid); graceful cutoff warning at limit.
  - Edge — mic permission denied: clear recovery screen, never a silent failure.
  - Edge — call interruption mid-recording: partial audio saved and processed.
  - Error — upload failure after 3 retries: item marked "stuck," user notified once, audio retained until processed.
  - Tests: instrumented tests for permission-denied, offline queue, interruption paths.

**M2 — AI task extraction & silent triage**
- Pipeline: transcription → LLM extraction of discrete tasks → deduplication against existing open tasks → effort/energy tag (low / medium / high) → priority score. No user-facing triage step.
- Acceptance criteria:
  - Round-trip (recording end → tasks visible) < 15 s p90 on a 60-second memo.
  - Handles mixed English/Bahasa Indonesia code-switching in one memo (multilingual transcription model requirement).
  - Edge — no actionable tasks detected (venting/journaling): stored as a note, response acknowledges without inventing tasks. The AI must never fabricate a task.
  - Edge — ambiguous item ("deal with the Sarah thing"): kept verbatim as a task, marked low-confidence, surfaced for one-tap confirmation in review — never silently rewritten.
  - Edge — near-duplicate ("email Sarah" vs "reply to Sarah's email"): merged, with merge visible in review history.
  - Error — LLM/API failure: raw transcript preserved, retry queued, user sees "captured, still thinking" state, never data loss.
  - Tests: golden-set eval of 50 real rambles (recruited from beta) scoring extraction precision/recall; regression suite run on prompt changes.

**M3 — Single Next Action widget**
- The widget's resting state shows exactly ONE task (the AI's top pick), with two actions: Done and Not Today (skip, no penalty). Never a list, never a count of remaining items.
- Acceptance criteria:
  - Skipped task is silently re-queued; no "skipped 3 times" messaging anywhere in the UI.
  - Widget updates < 5 s after Done/skip.
  - Edge — empty queue: widget shows a calm empty state + capture button, not a prompt to "add more tasks."
  - Edge — all remaining tasks are high-effort during Low Battery Mode: widget says "nothing light left — rest counts," never force-surfaces a heavy task.

**M4 — Shame-free system design (cross-cutting requirement)**
- No streaks, no overdue states, no red badges, no guilt copy anywhere. Max one push notification per day, off by default. Return-after-absence flow (see Story above) is a designed screen, not the default backlog. **[OPEN DECISION 1: this is an unvalidated design bet — primary beta hypothesis. Explicit risk: shame-free may underperform on re-engagement vs conventional retention mechanics. Measured, not assumed.]**
- Acceptance criteria: copy audit checklist applied to every screen before release; notification cap enforced at the system level, not per-feature.

**M5 — Review & correct screen**
- A minimal in-app list (the ONLY place a list exists) where users verify extraction, fix mis-transcriptions, delete tasks. Exists for trust (Dan) and eval data collection, not as a management surface.
- Acceptance criteria: every extracted task traceable to its source memo; one-tap edit; deletion is instant and unceremonious.

### SHOULD HAVE
- **S1 — Low Battery Mode:** one tap on widget filters next-action selection to low-effort tasks only. (Effort tags come free from M2; this is UI only.)
- **S2 — Micro-step hint:** when a surfaced task is tagged high-effort, show one AI-generated tiny first step beneath it ("Just open the email — that's it"). This is the paralysis hook, delivered as a property of M3 rather than a separate feature.

### COULD HAVE
- **C1 — Daily digest:** optional single morning notification, "one thing for today."
- **C2 — Task snooze horizons:** "not this week" as distinct from "not today."

### WON'T HAVE (this phase) — see Section 5.

---

## 5. Out of Scope (Hard Guardrails)

Explicitly NOT in the MVP. These are guardrails against scope creep and against AI-assisted code generation drifting into unrequested territory:

- **AI body doubling / PiP avatar / voice companion.** Highest build cost, weakest evidence (confidence that AI substitutes preserve body-doubling efficacy: low; mechanism is plausibly human social accountability). Phase 3 candidate only if beta users independently request it.
- **Context-aware check-ins of any kind.** No screen reading, no accessibility-service usage, no app-usage monitoring. Permanent constraint for Play Store compliance and Persona B trust, not just a phase deferral.
- **iOS, browser extension, smartwatch, tablet layouts.** Android phone only.
- **Calendar/email/third-party integrations.** No Google Calendar sync, no Gmail parsing.
- **Rigid scheduling and deadlines.** No due dates in MVP; the model is queue + energy, not calendar.
- **Collaboration, sharing, family/coach accounts.**
- **Gamification of any kind** (streaks, points, avatars, leaderboards) — conflicts with M4.
- **Medical/therapeutic claims.** No "treats," "manages," or "improves ADHD symptoms" language anywhere in product or store listing.
- **Web dashboard.**

---

## 6. Design & User Experience

**Design principles**
1. **One hand, one thumb, one action.** Every core flow (capture, done, skip) is a single tap reachable one-handed.
2. **Calm over engagement.** The app competes on being cheap to use, not sticky. No engagement-bait patterns.
3. **The widget IS the product.** The in-app experience is secondary; success means the user rarely opens the app.

**Visual & component standards**
- Material 3 (Material You) with dynamic color support; widget built on Jetpack Glance.
- Typography: high-legibility sans (Plus Jakarta Sans or system default); minimum 16 sp body text; optional dyslexia-friendly font toggle.
- Palette: low-saturation, calm neutrals; semantic color never used for guilt (no red for "overdue" — overdue does not exist).
- Dark mode at launch (heavily requested in ADHD communities for sensory reasons — confidence: medium, community-anecdote basis).

**Tone of voice**
- Warm, brief, never chirpy, never scolding. Copy rule: no exclamation marks in system messages; no "you haven't…" constructions.

**Accessibility**
- WCAG 2.1 AA contrast; TalkBack support for all core flows; touch targets ≥ 48 dp; reduced-motion setting honored.

**Interaction requirements**
- Capture reachable in ≤ 2 taps from any state (widget = 1 tap).
- Zero mandatory onboarding beyond mic permission + one sample capture; account creation deferrable (anonymous local start, sync on sign-up).

---

## 7. Technical Architecture & Constraints

**Client**
- Android native: Kotlin, Jetpack Compose (app), Jetpack Glance (widget), min SDK 26 (Android 8.0) — covers the vast majority of active devices; verify current distribution stats before locking.
- Offline-first capture: Room DB local queue for audio + pending tasks; WorkManager for retryable upload/processing jobs.
- Recording: standard MediaRecorder / AudioRecord push-to-talk. No foreground always-on service.

**Backend**
- Supabase: Auth (anonymous → email upgrade path), Postgres with RLS (per-user isolation), Storage for transient audio, Edge Functions for the processing pipeline.
- Rationale: owner already operates Supabase (existing project experience); RLS-first model matches privacy posture.

**AI pipeline (server-side Edge Function)**
1. Transcription: multilingual STT API (must handle EN/ID code-switching). Candidate: Whisper-class hosted API. On-device STT (Android SpeechRecognizer) as degraded fallback only — quality insufficient for rambling memos (confidence: medium; validate in Week 1 spike).
2. Extraction: small/fast LLM (Claude Haiku class) with a fixed, versioned prompt returning strict JSON: `{tasks: [{text, effort, confidence, source_span}], notes: [...]}`. Schema-validated; malformed output → one retry → fail-safe to raw transcript.
3. Dedup: embedding similarity against user's open tasks, threshold-merged, merge logged.
4. Priority scoring: deterministic heuristic (effort, recency, skip count) — NOT an LLM call. Keeps the widget's top pick cheap, fast, and explainable.

**Data flow**
Widget tap → local audio file → queued upload → Edge Function (STT → LLM → dedup → score) → Postgres → push sync → widget update. Raw audio deleted server-side immediately after successful transcription (default); transcript retained; user can disable transcript retention.

**Cost constraints (unit-economics guardrail)**
- Target COGS ≤ $0.15/user/month at free-tier limits. Enforced by design: free tier capped (see §9 monetization inputs), transcription is the dominant cost, priority scoring is non-LLM.
- Budget alert + kill switch on per-user daily pipeline spend to contain abuse.

**Hard constraints**
- No accessibility-service permission. No QUERY_ALL_PACKAGES. No background mic. (Play Store policy + product principle.)
- All AI processing server-side in MVP (no on-device LLM) to keep APK small and models swappable.

---

## 8. Non-Functional Requirements

**Performance**
- Widget tap → recording active: < 1.5 s p90.
- Recording end → tasks synced: < 15 s p90 (60 s memo), < 30 s p99.
- Widget action (Done/skip) → UI update: < 5 s.
- Cold app start: < 2 s on a mid-range device (reference: 3-year-old mid-tier Android).

**Reliability**
- Zero data loss: captured audio must survive app kill, reboot, and 72 h offline.
- Pipeline failure never blocks capture; degraded states are visible but calm.

**Scalability**
- MVP target: 1,000 MAU without architecture change. Supabase managed tier is sufficient; no premature optimization.

**Security & Privacy**
- TLS in transit; encryption at rest (Supabase default); RLS on every table; no third-party analytics SDK that receives content (analytics events are metadata-only, never transcript text).
- Raw audio TTL ≤ 24 h server-side even on pipeline failure.
- Compliance posture: GDPR-aligned data export + delete-account (full purge ≤ 30 days). If Indonesian users are targeted, UU PDP obligations apply — needs verification with current regulation text before ID launch.
- Sensitive-category caution: an "ADHD app" implies health data by inference. Store listing and data-safety form must be accurate; no health claims. Play Store health-app policy specifics need verification at submission time (policy moves; confidence in current details: medium).

---

## 9. Success Metrics & Analytics

**Northstar:** **Week-2 capture retention** — % of activated users who record ≥ 3 brain dumps during days 8–14. This directly measures survival past the documented abandonment cliff. Target: ≥ 25% at beta (baseline unknown; first cohort sets it).

**Supporting KPIs**
| Area | Metric | MVP target |
|---|---|---|
| Activation | First capture within 24 h of install | ≥ 60% |
| Trust | Extraction correction rate (edits per 10 tasks) | ≤ 2 |
| Core loop | Next-action completion rate (Done / surfaced) | ≥ 30% |
| Retention bet | Return-after-7-day-absence rate | tracked, no target — validates OPEN DECISION 1 |
| Monetization | Free→paid conversion at 60 days | 2–4% (industry-typical freemium band; confidence: medium, general benchmark not ADHD-specific) |
| Economics | AI COGS per weekly-active user | ≤ $0.15/mo |

**Monetization inputs (freemium + subscription)**
- Free: 5 captures/day, 5-min max memo, full widget functionality. The free tier must be genuinely usable (Persona C) — crippled free tiers reproduce the abandonment loop.
- Paid (~$4–5/mo or ~$25/yr; price-test in beta): unlimited captures, 15-min memos, Low Battery Mode, micro-step hints, daily digest.
- Deliberate: the widget core is never paywalled. The paywall sits on volume and comfort features, not the promise.

**Measurement plan**
- Event analytics: self-hosted or privacy-respecting provider; events carry IDs and durations only, never content.
- Extraction quality: golden-set eval (50 memos) re-run on every prompt/model change; correction-rate telemetry as live proxy.
- Qualitative: 10 beta interviews at week 3 focused on one question — "why did you stop / not stop using it?"

---

## 10. Risk Assessment & Mitigation

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | **Commoditization** — Gemini/OS assistants ship voice-to-task natively | High | High | Moat is post-capture behavior (silent triage, single-action surface, shame-free system), not capture itself. If the moat claim fails in beta interviews, reposition or kill. **[OPEN DECISION 3 — differentiation one-liner assumed, owner to confirm]** |
| R2 | Day-8 abandonment hits Nudge like every other app | High | Fatal | Shame-free return flow is the designed countermeasure; Northstar metric measures it directly; beta is explicitly a retention experiment with a kill/pivot threshold (Week-2 retention < 10% after iteration → pivot) |
| R3 | STT quality on rambling, code-switched speech | Medium | High | Week-1 technical spike with real memos before any UI is built; golden-set eval; fallback to English-only launch if ID code-switching underperforms |
| R4 | LLM/STT API cost outruns revenue | Medium | High | Free-tier caps, non-LLM priority scoring, per-user spend kill switch, COGS KPI reviewed weekly |
| R5 | Platform dependency — single STT/LLM vendor change | Medium | Medium | Server-side pipeline behind own API contract; models swappable without client release |
| R6 | Play Store health-policy friction on "ADHD" positioning | Low–Med | High | No medical claims; accurate data-safety form; verify current policy pre-submission (flagged: needs verification) |
| R7 | Solo-builder operational risk (support, outages, burnout) | High | Medium | Managed services only (Supabase, hosted APIs); scope discipline via §5; beta cohort capped at 50 |
| R8 | Community backlash — "another AI app monetizing ADHD" | Medium | Medium | Generous free tier, transparent privacy, recruit beta FROM the community with mod permission (subreddit self-promo rules are strict — verify per-subreddit before posting) |

---

## 11. Development Roadmap & Release Plan

Total MVP: **6 weeks** to closed beta, solo builder, AI-assisted development (Claude Code). Phases ordered by dependency and risk retirement.

**Week 1 — Risk-retirement spike (no UI)**
- STT + extraction pipeline as a bare Edge Function; test with 15 real self-recorded rambling memos (EN + mixed EN/ID).
- Exit criteria: extraction precision subjectively usable; per-memo cost measured. If this fails, the project stops here — cheapest possible kill point.

**Weeks 2–3 — Core loop**
- Supabase schema + RLS; Android capture (offline queue, WorkManager); Glance widget with capture + single next action; Done/skip flow.
- Milestone: end-to-end demo — speak into widget, see one next action, complete it.

**Week 4 — Trust & calm layer**
- Review/correct screen; return-after-absence flow; shame-free copy audit; Low Battery Mode (S1) if schedule allows.
- Milestone: internal alpha, self-use daily ("dogfood week").

**Week 5 — Closed beta**
- 20–50 users recruited from ADHD communities (with moderator permission) + personal network. Instrumented analytics live. Golden-set eval frozen as v1.
- Milestone: first Week-2 retention cohort begins.

**Week 6 — Iterate on beta signal**
- Fix top 3 friction points; price-test paywall screen (display only, no charge); micro-step hints (S2) if extraction quality permits.
- Milestone: go/no-go review against R2 kill threshold.

**Post-MVP (not scheduled):** Play Store open testing → public launch → C-tier features → Phase 3 evaluation of body doubling ONLY on pull from beta users.

**Release criteria for public launch:** Week-2 retention ≥ 20% across two cohorts; zero data-loss bugs in 30 days; COGS within target; privacy policy + data-safety form reviewed.

---

## 12. Stakeholder Management & Operations

**Decision authority:** Haw is sole decision-maker (product, technical, design). The three OPEN DECISIONS in this document require explicit sign-off before Week 2 begins; silence defaults to the assumptions as written.

**Advisory input:** Beta cohort functions as the user-research body; weekly synthesis of feedback → single prioritized change list (max 3 items/week to protect scope).

**Development handoff (AI-assisted workflow):**
- This PRD lives in `docs/prd.md` in the repo.
- A `CLAUDE.md` derived from §4–§8 governs implementation sessions: tech stack constraints, out-of-scope guardrails (§5 verbatim), acceptance criteria per feature, and the copy rules from §6.
- Phase gates: each roadmap week maps to a Claude Code working session set with its own exit criteria; no next-phase work before current exit criteria pass.

**Operations**
- Support channel: single email + in-app feedback form; 48 h response SLA during beta.
- Incident posture: Supabase status monitoring; pipeline failure alarms to owner; capture-path outages are P0 (data loss risk), everything else is P2.
- Documentation: schema migrations versioned via Supabase CLI; prompt versions tagged in repo alongside golden-set eval results.

---

## Appendix: Open Decisions Summary

| # | Decision | Assumption in this doc | Owner action |
|---|---|---|---|
| 1 | Day-8 retention mechanism | Shame-free system (no streaks, penalty-free return) as the bet; measured via Northstar | Confirm or propose alternative before Week 2 |
| 2 | Context awareness | Fully conceded — session/schedule-based only, permanent | Confirm (recommended: yes) |
| 3 | Differentiation claim | "Speak it and forget it — Nudge triages for you and hands back one small step" | Confirm wording; drives store listing + beta pitch |
