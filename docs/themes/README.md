# Themes

Themes work like Spicetify's: a theme is a small file with one or more **colour schemes**. You install it from a file on your phone or from an https link (Settings > Appearance > Themes in the Classic look, Settings > Display > Themes in the Immersive one), pick the theme, then pick its colour scheme. Both looks use it, live. A few themes come with the app (Catppuccin, Rosé Pine, Dracula, Nord, Gruvbox, Tokyo Night); they cannot be removed.

A theme is values the app applies, like an extension's `ui`: nothing in it runs.

## The file (`theme.json`, 64 KB at most)

```json
{
  "name": "Midnight Ember",
  "author": "Flavio",
  "version": "1.0",
  "description": "A dark theme with warm accents.",
  "default": "Ember",
  "scale": 1.0,
  "hide": ["home.dj"],
  "schemes": {
    "Ember": { "accent": "#FF9E3D", "background": "#120B06", "surface": "#24170E", "text": "#F6E9DD", "muted": "#B79F8B" }
  }
}
```

- `schemes` (1 to 16): name -> colours. Colours are `#RRGGBB` (the accent may be `#AARRGGBB`): `accent`, `background` (the page), `surface` (cards and raised parts), `text`, `muted` (secondary text). A scheme sets any of them; what it leaves out stays the app's own.
- Pages stay dark: the app draws light text and icons on them. A `background` or `surface` that is too bright, a `text` or `muted` that is too dark, or a `text` with less than 4.5:1 contrast against the `background`, is refused with the reason.
- `default`: the scheme used first (the first one when missing). `scale`: the text size, 0.85 to 1.3. `hide`: parts of the app to hide (`home.dj`, `player.lyrics-card`, `player.source`, `player.capsule`).
- Unknown colours and parts are ignored (a newer theme on an older app).

## Match the wallpaper

The first switch on the Themes page (on by default) makes the colours follow the wallpaper, every time it changes: the accent from the wallpaper's main colour, the page, cards, text and muted text in the same hue, in the dark tones the app needs. It is made once when you set the wallpaper (from the picture, or from a few frames of a video; a link is read once to do it) and kept, so the app starts with it. A grey or black-and-white wallpaper gives neutral dark colours and leaves the accent as it was. Turn the switch off to use the picked theme as it is.

## How it combines

- Match the wallpaper (when on and a wallpaper shows) sets the accent, page, cards and text over the picked theme's colours; the theme still sets what the wallpaper doesn't (text size, hidden parts).

- An extension's look (`ui.style`) applies over the theme: where both set a colour, the extension's wins. The theme is the base layer.
- The black background setting still wins over the theme's background, and a wallpaper still shows through the pages.
- While a protected surface is on screen (sign-in, payment...), the app is back to its own look, the theme included.
- The theme's accent replaces your own accent colour while it is picked; pick "App default" to get yours back.

Sample: [midnight-ember.json](midnight-ember.json).
