-- Per-user isolation on every table (PRD §7). The Edge Function uses the service role key
-- and bypasses RLS by design (it writes on behalf of users during pipeline processing);
-- every client-facing read/write goes through these policies.

alter table profiles enable row level security;
alter table memos enable row level security;
alter table tasks enable row level security;
alter table notes enable row level security;
alter table dedup_merges enable row level security;
alter table usage_daily enable row level security;

create policy "profiles_select_own" on profiles for select using (auth.uid() = id);
create policy "profiles_update_own" on profiles for update using (auth.uid() = id);
-- No insert/delete policy for profiles: rows are created only by the handle_new_user trigger
-- (security definer) and removed only via auth.users cascade on account deletion.

create policy "memos_select_own" on memos for select using (auth.uid() = user_id);
create policy "memos_insert_own" on memos for insert with check (auth.uid() = user_id);
create policy "memos_update_own" on memos for update using (auth.uid() = user_id);
create policy "memos_delete_own" on memos for delete using (auth.uid() = user_id);

create policy "tasks_select_own" on tasks for select using (auth.uid() = user_id);
create policy "tasks_insert_own" on tasks for insert with check (auth.uid() = user_id);
create policy "tasks_update_own" on tasks for update using (auth.uid() = user_id);
create policy "tasks_delete_own" on tasks for delete using (auth.uid() = user_id);

create policy "notes_select_own" on notes for select using (auth.uid() = user_id);
create policy "notes_delete_own" on notes for delete using (auth.uid() = user_id);
-- Notes are written by the Edge Function (service role) only, never directly by the client.

create policy "dedup_merges_select_own" on dedup_merges for select using (auth.uid() = user_id);
-- Written by the Edge Function (service role) only — merges are a system decision, not user input.

create policy "usage_daily_select_own" on usage_daily for select using (auth.uid() = user_id);
-- Written by the Edge Function (service role) only.
