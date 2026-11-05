-- Parent notes: free-form, family-shared notes per child. The body is plain
-- text with inline list markers ("• ", "1. ", "☐ " / "☑ ") so checklist state
-- lives in the text, same as appointment notes.

create table public.note (
    id                uuid primary key,
    family_id         uuid not null references public.family (id) on delete cascade,
    profile_id        uuid not null,
    title             text not null default '',
    body              text not null default '',
    created_at        timestamptz not null default now(),
    updated_at        timestamptz not null default now(),
    deleted_at        timestamptz,
    server_updated_at timestamptz not null default now(),
    version           int not null default 1,
    foreign key (profile_id, family_id) references public.profile (id, family_id) on delete cascade,
    check (length(trim(title)) > 0 or length(trim(body)) > 0)
);

create index note_profile_updated_idx on public.note (profile_id, updated_at desc);
create index note_family_sync_idx on public.note (family_id, server_updated_at);

create trigger set_server_updated_at before insert or update on public.note
    for each row execute function private.set_server_updated_at();

alter table public.note enable row level security;

revoke all on public.note from anon;
grant select, insert, update, delete on public.note to authenticated;

-- note: shared by every member of the family.
create policy note_select on public.note for select to authenticated
    using (family_id in (select private.my_family_ids()));
create policy note_insert on public.note for insert to authenticated
    with check (family_id in (select private.my_family_ids()));
create policy note_update on public.note for update to authenticated
    using (family_id in (select private.my_family_ids()))
    with check (family_id in (select private.my_family_ids()));
create policy note_delete on public.note for delete to authenticated
    using (family_id in (select private.my_family_ids()));

alter publication supabase_realtime add table public.note;
