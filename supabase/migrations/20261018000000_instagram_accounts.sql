-- Links an Instagram account to its Gridline ID, so every device logged into the account shows the same grid.
-- RLS without policies: only the service role (the instagram-auth function) can read or write.
create table public.instagram_accounts (
    instagram_user_id text primary key,
    gridline_id uuid not null unique,
    created_at timestamptz not null default now()
);

alter table public.instagram_accounts enable row level security;
revoke all on public.instagram_accounts from anon, authenticated;

alter table public.instagram_logins add column instagram_user_id text;
