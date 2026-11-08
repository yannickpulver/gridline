-- Each app install signs in anonymously. Once it shows a valid Instagram token, the instagram-auth
-- function links that sign-in to the account's Gridline ID. Access rules use this link.
-- RLS without policies: only the service role (the function) can read or write.
create table if not exists public.gridline_devices (
    auth_user_id uuid primary key references auth.users on delete cascade,
    gridline_id uuid not null,
    created_at timestamptz not null default now()
);

alter table public.gridline_devices enable row level security;
revoke all on public.gridline_devices from anon, authenticated;

-- The Gridline ID of the signed-in device, null when it isn't linked yet
create or replace function public.my_gridline_id() returns text
language sql stable security definer set search_path = '' as $$
    select gridline_id::text from public.gridline_devices where auth_user_id = auth.uid()
$$;

revoke execute on function public.my_gridline_id() from public, anon;
grant execute on function public.my_gridline_id() to authenticated;
