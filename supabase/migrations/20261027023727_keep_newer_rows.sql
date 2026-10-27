-- Reconciled from remote migration 20261027023727 (keep_newer_rows).
-- Last-write-wins guard: an UPDATE whose client updated_at is older than the
-- stored row is silently dropped. Note: for online-only optimistic locking,
-- activity/profile updates also check the version column (see online_only
-- migration); this trigger remains as a safety net for stale writes.
create function private.keep_newer_row()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
    if new.updated_at < old.updated_at then
        return null;
    end if;
    return new;
end;
$$;

create trigger keep_newer_row before update on public.family
    for each row execute function private.keep_newer_row();
create trigger keep_newer_row before update on public.caregiver
    for each row execute function private.keep_newer_row();
create trigger keep_newer_row before update on public.profile
    for each row execute function private.keep_newer_row();
create trigger keep_newer_row before update on public.activity
    for each row execute function private.keep_newer_row();
create trigger keep_newer_row before update on public.reminder
    for each row execute function private.keep_newer_row();
create trigger keep_newer_row before update on public.user_settings
    for each row execute function private.keep_newer_row();
