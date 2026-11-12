-- 1. Recency signal fix.
--
-- Priority scoring used tasks.updated_at as "recency", but updated_at is bumped by the
-- set_updated_at trigger on *every* update — including the score recompute in process-memo
-- and the client's Not Today skip. Net effect: skipping a task reset its recency to "fresh",
-- which outweighs the skip penalty, so a skipped task usually came straight back as the top
-- pick (and every memo made every open task look fresh again).
--
-- last_mentioned_at only moves when the user actually *says* the task: on creation, and when
-- a later memo is dedup-merged into it. Nothing else should write it.
alter table tasks add column last_mentioned_at timestamptz not null default now();
update tasks set last_mentioned_at = created_at;

-- 2. Per-memo cost accounting (PRD §11 Week 1 exit criterion: "per-memo cost measured";
-- §7 COGS guardrail + per-user daily spend kill switch).
--
-- Raw usage units are stored alongside the dollar estimate on purpose: provider prices change
-- and the configured unit prices may be wrong, so cost must be recomputable from units later.
alter table memos
  add column stt_seconds integer,
  add column llm_input_tokens integer,
  add column llm_output_tokens integer,
  add column llm_calls smallint,
  add column embedding_tokens integer,
  -- null = unit prices not configured, not "free". See supabase/.env.example PRICE_* vars.
  add column estimated_cost_usd numeric(10, 6);

alter table usage_daily alter column estimated_cost_usd type numeric(10, 6);

-- Atomic increment so two memos finishing at once can't lose a count (the previous
-- read-then-upsert in process-memo could). Service role only.
create or replace function increment_usage_daily(
  p_user_id uuid,
  p_day date,
  p_cost_usd numeric
) returns void
language sql
security definer set search_path = public
as $$
  insert into usage_daily (user_id, day, capture_count, estimated_cost_usd)
  values (p_user_id, p_day, 1, coalesce(p_cost_usd, 0))
  on conflict (user_id, day) do update
    set capture_count = usage_daily.capture_count + 1,
        estimated_cost_usd = usage_daily.estimated_cost_usd + coalesce(p_cost_usd, 0);
$$;

revoke all on function increment_usage_daily(uuid, date, numeric) from public, anon, authenticated;
