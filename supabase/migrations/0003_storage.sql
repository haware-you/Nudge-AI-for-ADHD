-- Private bucket for transient recorded audio. Objects are stored at "{user_id}/{memo_id}.m4a"
-- so path-prefix checks double as per-user isolation. Audio is deleted by the Edge Function
-- immediately after successful transcription, and by the cleanup job below on the <=24h TTL
-- even when the pipeline fails (PRD §8 "Raw audio TTL <= 24h server-side even on pipeline failure").

insert into storage.buckets (id, name, public)
values ('audio', 'audio', false)
on conflict (id) do nothing;

create policy "audio_insert_own" on storage.objects
  for insert with check (
    bucket_id = 'audio' and (storage.foldername(name))[1] = auth.uid()::text
  );

create policy "audio_select_own" on storage.objects
  for select using (
    bucket_id = 'audio' and (storage.foldername(name))[1] = auth.uid()::text
  );

create policy "audio_delete_own" on storage.objects
  for delete using (
    bucket_id = 'audio' and (storage.foldername(name))[1] = auth.uid()::text
  );

-- Deletes audio for any memo older than 24h that still has audio_path set, and marks it.
-- Schedule with pg_cron (or an external scheduled Edge Function invocation) every 15-30 min:
--   select cron.schedule('purge-stale-audio', '*/15 * * * *', 'select purge_stale_audio()');
create or replace function purge_stale_audio()
returns void
language plpgsql
security definer set search_path = public
as $$
declare
  stale record;
begin
  for stale in
    select id, user_id, audio_path from memos
    where audio_path is not null
      and audio_deleted_at is null
      and created_at < now() - interval '24 hours'
  loop
    delete from storage.objects where bucket_id = 'audio' and name = stale.audio_path;
    update memos set audio_deleted_at = now() where id = stale.id;
  end loop;
end;
$$;
