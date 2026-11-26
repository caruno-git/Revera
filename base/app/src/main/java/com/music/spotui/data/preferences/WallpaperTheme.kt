package com.music.spotui.data.preferences

/*
 * "Match the wallpaper": the app's colours (accent, page, cards, text) follow the wallpaper, every
 * time it changes. The palette is made once when a wallpaper is set (from the picture, or from
 * frames of the video, see WallpaperPalette), kept with it, and applied by ThemePref above the picked
 * theme's colours (an extension's look still goes over both). On by default; it does nothing
 * while no wallpaper shows.
 */

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import app.myspoti.extensions.ThemeSpec
import app.myspoti.extensions.WallpaperPalette
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

object WallpaperTheme {
    private const val PREF = "settings_prefs"
    private const val K_MATCH = "theme_match_wallpaper"
    private const val K_PALETTE = "wallpaper_palette"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val keys = ThemeSpec.COLORS

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    /** Whether the colours follow the wallpaper. */
    var match by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getBoolean(K_MATCH, true))
        private set

    /** The colours made from the wallpaper that is set (by [ThemeSpec.COLORS] key), or null: none yet, or it has none. */
    var palette by mutableStateOf<Map<String, Int>?>(
        // What was kept for the wallpaper set now, at once: the first frame already has its colours.
        WallpaperPref.chosen?.let { decode(prefs(com.music.spotui.di.Graph.app).getString(K_PALETTE, null), stamp(it)) },
    )
        private set

    private fun stamp(w: WallpaperPref.Chosen) = "${w.source}|${w.video}"

    /** Reads what was kept for the wallpaper set now, and makes it again if it is another one (the app starting, or a new wallpaper). */
    fun refresh(c: Context) {
        val w = WallpaperPref.chosen
        if (w == null) {
            palette = null
            prefs(c).edit().remove(K_PALETTE).apply()
            return
        }
        val kept = prefs(c).getString(K_PALETTE, null)
        decode(kept, stamp(w))?.let { palette = it; return }
        palette = null
        scope.launch {
            val made = runCatching { sample(c, w) }.getOrNull()?.let { WallpaperPalette.derive(it) }
            // Another wallpaper was set meanwhile: that one has its own run.
            if (WallpaperPref.chosen?.let { stamp(it) } != stamp(w)) return@launch
            palette = made
            prefs(c).edit().putString(K_PALETTE, encode(stamp(w), made)).apply()
        }
    }

    fun setMatch(c: Context, on: Boolean) {
        match = on
        prefs(c).edit().putBoolean(K_MATCH, on).apply()
    }

    /** The wallpaper's colour for [key], or null: matching is off, no wallpaper shows, or it has none. */
    fun color(key: String): Color? {
        if (!match || WallpaperPref.chosen == null || WallpaperPref.hidden != null) return null
        return palette?.get(key)?.let { Color(it) }
    }

    private fun encode(stamp: String, p: Map<String, Int>?): String =
        stamp + "\n" + keys.joinToString(",") { k -> p?.get(k)?.let { Integer.toHexString(it) } ?: "-" }

    private fun decode(text: String?, stamp: String): Map<String, Int>? {
        val lines = text?.split("\n") ?: return null
        if (lines.size != 2 || lines[0] != stamp) return null
        val parts = lines[1].split(",")
        if (parts.size != keys.size) return null
        val out = LinkedHashMap<String, Int>()
        keys.forEachIndexed { i, k -> parts[i].takeIf { it != "-" }?.toLongOrNull(16)?.let { out[k] = it.toInt() } }
        return out.takeIf { it.isNotEmpty() }
    }

    /** A small sample of the wallpaper's pixels: the picture itself, or a few frames of the video. Null when it cannot be read. */
    private fun sample(c: Context, w: WallpaperPref.Chosen): IntArray? {
        val frames = if (w.video) videoFrames(c, w) else listOfNotNull(picture(c, w))
        if (frames.isEmpty()) return null
        val out = ArrayList<Int>()
        for (bmp in frames) {
            val small = Bitmap.createScaledBitmap(bmp, 48, 48, true)
            val px = IntArray(48 * 48)
            small.getPixels(px, 0, 48, 0, 0, 48, 48)
            px.forEach { out.add(it) }
        }
        return out.toIntArray()
    }

    private fun picture(c: Context, w: WallpaperPref.Chosen): Bitmap? {
        val glide = Glide.with(c.applicationContext)
        val future = glide.asBitmap().load(if (w.local) WallpaperPref.fileOf(w) else w.source).override(96, 96).submit()
        return try {
            future.get(20, TimeUnit.SECONDS)?.copy(Bitmap.Config.ARGB_8888, false)
        } finally {
            glide.clear(future)
        }
    }

    private fun videoFrames(c: Context, w: WallpaperPref.Chosen): List<Bitmap> {
        val r = MediaMetadataRetriever()
        return try {
            if (w.local) r.setDataSource(WallpaperPref.fileOf(w).path) else r.setDataSource(w.source, HashMap<String, String>())
            val ms = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            listOf(0.1, 0.4, 0.7).mapNotNull { f ->
                r.getFrameAtTime((ms * f * 1000).toLong(), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            runCatching { r.release() }
        }
    }
}
