# Wallpapers

Pictures and videos behind Revera's pages (Classic and Immersive looks).

1. Install this extension (Settings, Extensions, Install from a file) and turn it on.
2. Settings, Display, **Wallpaper**:
   - **Choose a picture or video** from your phone,
   - or paste an https link to a picture or video (`.mp4`, `.webm`, `.m3u8` play as video),
   - or pick one of the wallpapers listed under "From your extensions".
3. Slide **Dimming** to taste. Remove the wallpaper any time.

Videos play without sound, and stop in battery saver or when the app is not on screen.

## Add your own to the list

Edit `ui.wallpapers` in `manifest.json` (24 at most):

    { "title": "Rain", "url": "https://example.com/rain.mp4", "kind": "video", "thumb": "https://example.com/rain.jpg" }

`kind` is `image`, `video` or `auto` (decided by the address). Addresses must be https on a public site.
The two bundled pictures are NASA images (public domain), served by Wikimedia Commons.
