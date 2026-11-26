package com.music.spotui.data.preferences

/*
 * The app's wallpaper: an image or a video (looping, silent) behind the pages of both looks,
 * dimmed so the text stays readable. It comes from a file the user picks on the phone (copied into
 * the app's own storage), from a link the user types, or from the pack of an extension's look
 * (`ui.wallpapers`, see UiStyle). Compose state, so the app redraws when one changes.
 */

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.myspoti.extensions.PackWallpaper
import app.myspoti.extensions.WallpaperPick
import com.music.spotui.extensions.ExtensionLook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val PREF = "settings_prefs"

private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

object WallpaperPref {
    private const val K_SOURCE = "wallpaper_source"
    private const val K_VIDEO = "wallpaper_video"
    private const val K_FROM = "wallpaper_from"
    private const val K_TITLE = "wallpaper_title"
    private const val K_DIM = "wallpaper_dim"

    /** Dimming in percent: how much of the page colour covers the wallpaper. */
    const val DIM_DEFAULT = 55
    const val DIM_MAX = 90

    private const val MAX_IMAGE_BYTES = 25L * 1024 * 1024
    private const val MAX_VIDEO_BYTES = 250L * 1024 * 1024

    /**
     * What the user set. [source] is a file name in the app's wallpaper folder (a file from the
     * phone) or an https address (a link, or a pack's wallpaper, whose owner is [from]: an
     * extension id, empty for the user's own). [title] is shown in Settings.
     */
    data class Chosen(val video: Boolean, val source: String, val from: String, val title: String) {
        /** A file on the phone: it plays with no connection and uses no data. */
        val local: Boolean get() = !source.startsWith("https://")
    }

    /** How a file picked on the phone ended. */
    enum class Import { OK, UNSUPPORTED, TOO_BIG, FAILED }

    private fun dir(c: Context) = File(c.filesDir, "wallpaper")

    /** The file of a [Chosen.local] wallpaper. */
    fun fileOf(w: Chosen): File = File(dir(com.music.spotui.di.Graph.app), w.source)

    private fun load(): Chosen? {
        val c = com.music.spotui.di.Graph.app
        val p = prefs(c)
        val source = p.getString(K_SOURCE, null)?.takeIf { it.isNotBlank() } ?: return null
        if (!source.startsWith("https://") && !File(dir(c), source).isFile) return null
        return Chosen(p.getBoolean(K_VIDEO, false), source, p.getString(K_FROM, "") ?: "", p.getString(K_TITLE, "") ?: "")
    }

    /** What is set, whether or not it shows right now (see [shown]). */
    var chosen by mutableStateOf<Chosen?>(load())
        private set

    /** The app started: the colours that follow the wallpaper (see WallpaperTheme). */
    fun start(c: Context) = WallpaperTheme.refresh(c)

    /** Dimming, 0 to [DIM_MAX] percent. */
    var dim by mutableStateOf(prefs(com.music.spotui.di.Graph.app).getInt(K_DIM, DIM_DEFAULT).coerceIn(0, DIM_MAX))
        private set

    /** Why a wallpaper that is set does not show right now. */
    enum class Hidden {
        /** The black background setting is on (it is chosen to save power). */
        BLACK_BACKGROUND,

        /** It is a pack's wallpaper and the extension that offers it is off or gone. */
        EXTENSION_OFF,
    }

    /** Why [chosen] does not show, or null (it shows, or none is set). A protected surface on screen is not a reason: it is over in a moment. */
    val hidden: Hidden?
        get() {
            val c = chosen ?: return null
            if (app.myspoti.power.EnergySettings.oledBlack) return Hidden.BLACK_BACKGROUND
            if (c.from.isNotEmpty() && ExtensionLook.wallpapers.none { it.extensionId == c.from && it.url == c.source }) return Hidden.EXTENSION_OFF
            return null
        }

    /**
     * What the app draws now, or null: nothing is set, it is [hidden], or a protected surface is
     * on screen (the whole app is back to its own look there).
     */
    val shown: Chosen?
        get() {
            val c = chosen ?: return null
            if (hidden != null || ExtensionLook.isShielded) return null
            return c
        }

    /** A wallpaper shows: the pages let it through (see PageFill). */
    val active: Boolean get() = shown != null

    private fun save(c: Context, next: Chosen?) {
        val e = prefs(c).edit()
        if (next == null) {
            e.remove(K_SOURCE).remove(K_VIDEO).remove(K_FROM).remove(K_TITLE)
        } else {
            e.putString(K_SOURCE, next.source).putBoolean(K_VIDEO, next.video).putString(K_FROM, next.from).putString(K_TITLE, next.title)
        }
        e.apply()
        chosen = next
        prune(c)
        WallpaperTheme.refresh(c)
    }

    /** The files of wallpapers that are no longer the one set. */
    private fun prune(c: Context) {
        val keep = chosen?.takeIf { it.local }?.source
        dir(c).listFiles()?.forEach { f -> if (f.name != keep) runCatching { f.delete() } }
    }

    /** Back to no wallpaper. */
    fun clear(c: Context) = save(c, null)

    /** A wallpaper of an extension's pack. */
    fun setPack(c: Context, w: PackWallpaper) = save(c, Chosen(w.video, w.url, w.extensionId, w.title))

    /**
     * A link typed by the user: false when it is not an https address on a public host. [video]
     * null: decided by the address (`.mp4`, `.webm`, `.m3u8`...).
     */
    fun setLink(c: Context, url: String, video: Boolean? = null): Boolean {
        val u = url.trim()
        val host = runCatching { java.net.URI(u).host }.getOrNull().orEmpty().take(WallpaperPick.MAX_TITLE)
        val kind = when (video) { true -> WallpaperPick.VIDEO; false -> WallpaperPick.IMAGE; null -> WallpaperPick.AUTO }
        val pick = WallpaperPick(title = host, url = u, kind = kind)
        if (pick.problem() != null) return false
        save(c, Chosen(pick.video, u, "", host))
        return true
    }

    /**
     * An image or a video picked on the phone: copied into the app's own storage (so it keeps
     * working if the original moves, and plays with no connection), 25 MB at most for an image,
     * 250 MB for a video. Call it from the main thread: the copy runs on another.
     */
    suspend fun importFile(c: Context, uri: Uri): Import {
        val mime = c.contentResolver.getType(uri).orEmpty()
        val video = when {
            mime.startsWith("video/") -> true
            mime.startsWith("image/") -> false
            else -> return Import.UNSUPPORTED
        }
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: if (video) "mp4" else "jpg"
        val limit = if (video) MAX_VIDEO_BYTES else MAX_IMAGE_BYTES
        val folder = dir(c)
        val target = File(folder, "wp-${System.currentTimeMillis()}.$ext")
        val outcome = withContext(Dispatchers.IO) {
            var result = Import.OK
            try {
                folder.mkdirs()
                val input = c.contentResolver.openInputStream(uri)
                if (input == null) {
                    result = Import.FAILED
                } else {
                    input.use { ins ->
                        target.outputStream().use { out ->
                            val buffer = ByteArray(64 * 1024)
                            var total = 0L
                            while (true) {
                                val n = ins.read(buffer)
                                if (n < 0) break
                                total += n
                                if (total > limit) { result = Import.TOO_BIG; break }
                                out.write(buffer, 0, n)
                            }
                        }
                    }
                }
                if (result == Import.OK && !video) {
                    // A file that is not a picture would show as nothing.
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(target.path, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) result = Import.UNSUPPORTED
                }
            } catch (e: Exception) {
                result = Import.FAILED
            }
            if (result != Import.OK) runCatching { target.delete() }
            result
        }
        if (outcome == Import.OK) save(c, Chosen(video, target.name, "", ""))
        return outcome
    }

    /** The dimming, in percent. */
    fun setDim(c: Context, percent: Int) {
        val v = percent.coerceIn(0, DIM_MAX)
        dim = v
        prefs(c).edit().putInt(K_DIM, v).apply()
    }
}
