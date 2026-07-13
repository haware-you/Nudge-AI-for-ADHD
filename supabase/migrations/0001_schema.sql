-- Nudge core schema.
-- Tables map to PRD §7 data flow: memo (raw capture) -> tasks/notes (extracted) -> dedup_merges (audit trail).
-- Every table carries user_id and is locked down in 0002_rls.sql — no table is readable cross-user.

create extension if not exists "uuid-ossp";
create extension if not exists vector; -- pgvector, for task dedup embeddings

create type memo_status as enum ('queued', 'processing', 'transcribed', 'done', 'failed', 'stuck');
create type task_effort as enum ('low', 'medium', 'high');
create type task_status as enum ('open', 'done', 'skipped');

-- One row per authenticated user (anonymous or upgraded). Created by a trigger on auth.users insert.
create table profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  plan text not null default 'free' check (plan in ('free', 'paid')),
  retain_transcript boolean not null default true,
  low_battery_mode boolean not null default false,
  notifications_enabled boolean not null default false,
  notification_hour smallint check (notification_hour between 0 and 23),
  created_at timestamptz not null default now()
);

-- One row per voice memo. audio_path points at Storage; raw audio is purged (see cleanup job)
-- regardless of pipeline outcome, per the <=24h TTL requirement in the PRD.
create table memos (
  id uuid primary key default uuid_generate_v4(),
  user_id uuid not null references auth.users (id) on delete cascade,
  status memo_status not null default 'queued',
  audio_path text,
  audio_deleted_at timestamptz,
  duration_seconds integer,
  transcript text,
  retry_count integer not null default 0,
  error text,
  created_at timestamptz not null default now(),
  processed_at timestamptz
);

create index memos_user_id_status_idx on memos (user_id, status);

-- Extracted, actionable items. One task can absorb several near-duplicate mentions (see dedup_merges).
create table tasks (
  id uuid primary key default uuid_generate_v4(),
  user_id uuid not null references auth.users (id) on delete cascade,
  source_memo_id uuid references memos (id) on delete set null,
  text text not null,
  source_span text,
  effort task_effort not null default 'medium',
  confidence real not null default 1.0 check (confidence between 0 and 1),
  low_confidence boolean not null default false,
  status task_status not null default 'open',
  skip_count integer not null default 0,
  priority_score double precision not null default 0,
  embedding vector(512), -- voyage-3-lite; see supabase/functions/process-memo
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  completed_at timestamptz
);

create index tasks_user_id_status_idx on tasks (user_id, status);
create index tasks_user_id_priority_idx on tasks (user_id, priority_score desc);
create index tasks_embedding_idx on tasks using ivfflat (embedding vector_cosine_ops) with (lists = 100);

-- Non-actionable extraction output: venting/journaling. Never promoted to a task automatically (M2 edge case).
create table notes (
  id uuid primary key default uuid_generate_v4(),
  user_id uuid not null references auth.users (id) on delete cascade,
  source_memo_id uuid references memos (id) on delete set null,
  text text not null,
  created_at timestamptz not null default now()
);

-- Audit trail for M2's "merge visible in review history" requirement — the Review & Correct
-- screen reads this to show why a task's mention count/source spans grew.
create table dedup_merges (
  id uuid primary key default uuid_generate_v4(),
  user_id uuid not null references auth.users (id) on delete cascade,
  surviving_task_id uuid not null references tasks (id) on delete cascade,
  merged_text text not null,
  merged_source_memo_id uuid references memos (id) on delete set null,
  similarity real not null,
  created_at timestamptz not null default now()
);

create index dedup_merges_surviving_task_idx on dedup_merges (surviving_task_id);

-- Per-user daily pipeline usage, backing the free-tier cap (5 captures/day) and the
-- per-user spend kill switch called out in PRD §7/§10 (R4).
create table usage_daily (
  user_id uuid not null references auth.users (id) on delete cascade,
  day date not null default current_date,
  capture_count integer not null default 0,
  estimated_cost_usd numeric(10, 4) not null default 0,
  primary key (user_id, day)
);

create or replace function handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles (id) values (new.id);
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function handle_new_user();

create or replace function set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger tasks_set_updated_at
  before update on tasks
  for each row execute function set_updated_at();
