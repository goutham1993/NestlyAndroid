-- Reconciled from remote migration 20261027023806 (caregiver_upsert_policy).
-- Allows PostgREST upsert (insert ... on conflict) on caregiver: an insert
-- whose id already belongs to a caregiver in one of the caller's families is
-- permitted, so upserts pass the insert policy before hitting the update policy.
create function private.is_existing_caregiver_in_my_family(cid uuid)
returns boolean
language sql stable security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.caregiver c
        where c.id = cid and c.family_id in (select private.my_family_ids())
    )
$$;

revoke all on function private.is_existing_caregiver_in_my_family(uuid) from public, anon;
grant execute on function private.is_existing_caregiver_in_my_family(uuid) to authenticated;

drop policy caregiver_insert on public.caregiver;
create policy caregiver_insert on public.caregiver for insert to authenticated
    with check (
        (family_id in (select private.my_family_ids()) and user_id is null and role = 'MEMBER')
        or private.is_existing_caregiver_in_my_family(id)
        or (
            user_id = (select auth.uid())
            and role = 'OWNER'
            and exists (select 1 from public.family f where f.id = family_id and f.created_by = (select auth.uid()))
            and not private.family_has_members(family_id)
        )
    );
