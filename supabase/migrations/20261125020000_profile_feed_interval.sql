-- One next-feed interval per child, shared by every caregiver and device.
-- Reminder rows stay per caregiver and only drive that caregiver's
-- notifications. Null means the app default (3 hours).
alter table public.profile
    add column if not exists feed_interval_minutes integer
        check (feed_interval_minutes > 0);

notify pgrst, 'reload schema';
