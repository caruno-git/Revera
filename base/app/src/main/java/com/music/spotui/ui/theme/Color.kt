package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** The page: black with the AMOLED setting (an extension's look never undoes it), else a look's background or #121212. */
val AppBackground: Color get() =
    if (app.myspoti.power.EnergySettings.oledBlack) Color.Black
    else com.music.spotui.extensions.ExtensionLook.color(app.myspoti.extensions.UiStyle.BACKGROUND) ?: Color(0xFF121212)
/**
 * What a page paints behind itself: [AppBackground], or nothing while the wallpaper shows
 * (Settings > Display > Wallpaper) so it comes through. Only for the background of a whole page:
 * bars, sticky headers, fades and text colours keep [AppBackground], which stays solid.
 */
val PageFill: Color get() =
    if (com.music.spotui.data.preferences.WallpaperPref.active) Color.Transparent else AppBackground
/** The far end of a header's fade into the page: [AppBackground], or see-through (same colour, no alpha) while the wallpaper shows. */
val PageFade: Color get() =
    if (com.music.spotui.data.preferences.WallpaperPref.active) AppBackground.copy(alpha = 0f) else AppBackground
val GridBackground: Color get() = com.music.spotui.extensions.ExtensionLook.color(app.myspoti.extensions.UiStyle.SURFACE) ?: tone(0xFF2A2A2A)
/** The accent (Spotify green, the colour chosen in the settings, the wallpaper's: see DynamicColorPref, or an extension's look). */
val AppPalette: Color get() = AppAccent
val AppAccent: Color get() =
    com.music.spotui.extensions.ExtensionLook.color(app.myspoti.extensions.UiStyle.ACCENT)
        ?: com.music.spotui.data.preferences.DynamicColorPref.accent() ?: com.music.spotui.data.api.SupportReminder.accentColor

/** Primary text and icons. */
val AppText: Color get() = com.music.spotui.extensions.ExtensionLook.themeColor("text") ?: Color.White
/** Secondary text and icons. */
val AppMuted: Color get() = com.music.spotui.extensions.ExtensionLook.themeColor("muted") ?: Color(0xFFB3B3B3)
/** Destructive actions (delete, leave, block) and failures shown as text. */
val AppDestructive = Color(0xFFF15E6C)
/** What a tint is mixed with to make a header. */
val AppShade: Color = Color.Black

/** One of the palette's raised greys (#181818 ... #3E3E3E): "a bit raised from the page". */
fun tone(argb: Long): Color = Color(argb)

/** The cover's colour as a page header: darkened toward black, so the title stays readable. */
fun headerTint(tint: Color, amount: Float): Color = lerp(tint, Color.Black, amount)
