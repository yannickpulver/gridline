-- Closes the grid to everyone but its own linked devices.
-- NOT applied yet: app versions before 1.2.12 have no sign-in and lose their grid once this runs.
-- Run it (SQL editor or `supabase db query --linked -f`) once 1.2.12 is live and most users updated.

drop policy if exists "Allow all access" on public.images;
drop policy if exists "Own grid" on public.images;
create policy "Own grid" on public.images for all to authenticated
    using (user_id = (select public.my_gridline_id()))
    with check (user_id = (select public.my_gridline_id()));

drop policy if exists "Give anon users access to JPG images in folder 1ffg0oo_0" on storage.objects;
drop policy if exists "Give anon users access to JPG images in folder 1ffg0oo_1" on storage.objects;
drop policy if exists "Give anon users access to JPG images in folder 1ffg0oo_2" on storage.objects;
drop policy if exists "Give anon users access to JPG images in folder 1ffg0oo_3" on storage.objects;
drop policy if exists "Own grid images" on storage.objects;
-- Images stay reachable by their (random) public url, listing and changing them needs the link
create policy "Own grid images" on storage.objects for all to authenticated
    using (bucket_id = 'images' and (storage.foldername(name))[1] = (select public.my_gridline_id()))
    with check (bucket_id = 'images' and (storage.foldername(name))[1] = (select public.my_gridline_id()));
