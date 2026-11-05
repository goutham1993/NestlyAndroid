-- Daily Vitamin D drops. Additive: only widens the allowed activity types.
alter table public.activity drop constraint if exists activity_type_check;
alter table public.activity add constraint activity_type_check check (type in (
    'BOTTLE_FEED', 'BREASTFEED', 'PUMP', 'DIAPER', 'GROWTH', 'MILESTONE',
    'SLEEP', 'SOLIDS', 'ROUTINE', 'BABY_FIRST', 'MEDICAL', 'VACCINE', 'VITAMIN_D'
)) not valid;
alter table public.activity validate constraint activity_type_check;
