-- Short-lived hand-over of Instagram tokens from the instagram-auth function to the app.
-- RLS without policies: only the service role (the function) can read or write.
create table public.instagram_logins (
    state uuid primary key,
    access_token text not null,
    expires_in bigint,
    created_at timestamptz not null default now()
);

alter table public.instagram_logins enable row level security;
revoke all on public.instagram_logins from anon, authenticated;
