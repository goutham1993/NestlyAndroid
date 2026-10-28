-- Doctor appointments: family-shared future/past appointments per child,
-- each with a checkable list of questions to ask and free-text notes.
-- Reminder timing is a local-device concern (AlarmManager); the lead time
-- is stored here so every caregiver's device can schedule its own alarm.

create table public.appointment (
    id                    uuid primary key,
    family_id             uuid not null references public.family (id) on delete cascade,
    profile_id            uuid not null,
    title                 text not null check (length(trim(title)) > 0),
    doctor_name           text,
    location              text,
    starts_at             timestamptz not null,
    time_zone             text not null,
    notes                 text,
    -- Null means no reminder; 0 = at appointment time.
    reminder_lead_minutes integer check (reminder_lead_minutes in (0, 60, 1440)),
    created_at            timestamptz not null default now(),
    updated_at            timestamptz not null default now(),
    deleted_at            timestamptz,
    server_updated_at     timestamptz not null default now(),
    version               int not null default 1,
    foreign key (profile_id, family_id) references public.profile (id, family_id) on delete cascade
);

create table public.appointment_question (
    id             uuid primary key,
    appointment_id uuid not null references public.appointment (id) on delete cascade,
    -- Denormalized so Realtime subscriptions can filter by family_id.
    family_id      uuid not null references public.family (id) on delete cascade,
    text           text not null check (length(trim(text)) > 0),
    is_done        boolean not null default false,
    sort_order     int not null default 0,
    created_at     timestamptz not null default now(),
    updated_at     timestamptz not null default now(),
    deleted_at     timestamptz,
    server_updated_at timestamptz not null default now(),
    version        int not null default 1
);

create index appointment_profile_starts_idx on public.appointment (profile_id, starts_at);
create index appointment_family_sync_idx on public.appointment (family_id, server_updated_at);
create index appointment_question_appointment_idx on public.appointment_question (appointment_id, sort_order);
create index appointment_question_family_sync_idx on public.appointment_question (family_id, server_updated_at);

create trigger set_server_updated_at before insert or update on public.appointment
    for each row execute function private.set_server_updated_at();
create trigger set_server_updated_at before insert or update on public.appointment_question
    for each row execute function private.set_server_updated_at();

alter table public.appointment enable row level security;
alter table public.appointment_question enable row level security;

revoke all on public.appointment, public.appointment_question from anon;
grant select, insert, update, delete on public.appointment, public.appointment_question to authenticated;

-- appointment and appointment_question: shared by every member of the family.
create policy appointment_select on public.appointment for select to authenticated
    using (family_id in (select private.my_family_ids()));
create policy appointment_insert on public.appointment for insert to authenticated
    with check (family_id in (select private.my_family_ids()));
create policy appointment_update on public.appointment for update to authenticated
    using (family_id in (select private.my_family_ids()))
    with check (family_id in (select private.my_family_ids()));
create policy appointment_delete on public.appointment for delete to authenticated
    using (family_id in (select private.my_family_ids()));

create policy appointment_question_select on public.appointment_question for select to authenticated
    using (family_id in (select private.my_family_ids()));
create policy appointment_question_insert on public.appointment_question for insert to authenticated
    with check (family_id in (select private.my_family_ids()));
create policy appointment_question_update on public.appointment_question for update to authenticated
    using (family_id in (select private.my_family_ids()))
    with check (family_id in (select private.my_family_ids()));
create policy appointment_question_delete on public.appointment_question for delete to authenticated
    using (family_id in (select private.my_family_ids()));

alter publication supabase_realtime add table public.appointment;
alter publication supabase_realtime add table public.appointment_question;
