-- Solids can keep the milk clock or restart it. Existing rows restart it,
-- which is how every solids entry behaved before this choice existed.
alter table public.activity
    add column if not exists solids_resets_feed_schedule boolean not null default true;

notify pgrst, 'reload schema';
