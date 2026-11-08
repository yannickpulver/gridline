---
name: gridline
description: Add images to Yannick's Gridline grid (the Instagram feed planner) from the terminal, list or remove planned images. Use when the user says "add this image to Gridline", "put this on my grid", "plan this post in Gridline", "Bild in Gridline", or wants to see/remove what's planned — from any project.
---

# Gridline

`gridline` (in the gridline repo at `scripts/gridline`, linked to `~/.local/bin/gridline`) does what the app's + button does: shrinks the image, uploads it and puts it on top of the grid. Open Gridline apps (Mac, phone) show it right away.

```bash
gridline add ~/Desktop/photo.jpg            # one or more files
gridline add https://example.com/image.jpg  # URLs work too
gridline list                               # planned images, top of the grid first, with their #id
gridline remove 17029                       # takes it off the grid (the file stays in the app's image library)
```

- Several images: pass them in the order they should appear from bottom to top; the last one ends up at the top.
- Any format `sips` reads works (JPEG, PNG, HEIC, …).
- Run `gridline list` afterwards and tell the user the new `#id`.
- Like the app, it signs in to Supabase and links that sign-in to the grid with the Instagram token from the Mac app's settings (session in `~/.config/gridline/session.json`). "Not signed in, using the public key" is fine as long as the backend doesn't require sign-in yet.
- If it says no Gridline ID was found, the user isn't logged in on the Mac; ask them to log in to the Mac app.
- Reordering, placeholders and pinned posts happen in the app.
