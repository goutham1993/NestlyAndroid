-- Online-only migration: optimistic locking + Realtime.
--
-- version: clients update with WHERE id = ? AND version = ? and send
-- version = version + 1 (client-managed increment). Zero rows back means the
-- row changed underneath the client -> conflict, refetch and retry.
-- keep_newer_row (older migration) still guards against stale updated_at.

alter table public.family    add column version int not null default 1;
alter table public.caregiver add column version int not null default 1;
alter table public.profile   add column version int not null default 1;
alter table public.activity  add column version int not null default 1;
alter table public.reminder  add column version int not null default 1;

-- Realtime: clients treat events as refetch triggers, so default replica
-- identity (primary key) is enough; no payload diffing is done client-side.
alter publication supabase_realtime add table public.activity;
alter publication supabase_realtime add table public.profile;
alter publication supabase_realtime add table public.caregiver;
alter publication supabase_realtime add table public.family;
alter publication supabase_realtime add table public.reminder;
