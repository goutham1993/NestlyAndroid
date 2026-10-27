drop index public.activity_created_by_idx;
drop index public.activity_updated_by_idx;
drop index public.reminder_profile_idx;

create index activity_created_by_idx on public.activity (created_by_id, family_id);
create index activity_updated_by_idx on public.activity (updated_by_id, family_id);
create index activity_profile_family_idx on public.activity (profile_id, family_id);
create index reminder_caregiver_family_idx on public.reminder (caregiver_id, family_id);
create index reminder_profile_idx on public.reminder (profile_id, family_id);
