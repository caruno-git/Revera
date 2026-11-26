package com.music.spotui.data.preferences

/*
 * Themes, as in Spicetify: a theme is a file with colour schemes (see ThemeSpec), installed from a
 * file on the phone or from a link, picked in Settings together with its colour scheme. A few
 * well-known ones come with the app (BuiltInThemes). The picked scheme's colours are applied under
 * an extension's look (ExtensionLook), in both looks. Compose state, so the app follows a change at once.
 */

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import app.myspoti.extensions.BuiltInThemes
import app.myspoti.extensions.ThemeSpec
import app.myspoti.extensions.UiPart
import app.myspoti.extensions.WallpaperPick
import app.myspoti.extensions.parseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val PREF = "settings_prefs"

private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

object ThemePref {
    private const val K_CURRENT = "theme_current"
    private const val K_SCHEME = "theme_scheme"
    private const val MAX_INSTALLED = 24

    /** A theme the app knows: one that comes with it ([builtIn]) or one the user installed. [id] is stable. */
    data class Entry(val id: String, val spec: ThemeSpec, val builtIn: Boolean)

    private fun dir(c: Context) = File(c.filesDir, "themes")

    private val builtIns: List<Entry> = BuiltInThemes.all.map { (id, spec) -> Entry("builtin:$id", spec, true) }

    private fun loadInstalled(): List<Entry> {
        val files = dir(com.music.spotui.di.Graph.app).listFiles { f -> f.isFile && f.name.endsWith(".json") }?.sortedBy { it.name } ?: return emptyList()
        return files.mapNotNull { f ->
            runCatching { Entry("user:" + f.nameWithoutExtension, parseTheme(f.readText()), false) }.getOrNull()
        }
    }

    private var installed by mutableStateOf(loadInstalled())

    /** Every theme: the ones that come with the app, then the installed ones. */
    val entries: List<Entry> get() = builtIns + installed

    /** The picked theme's id, or null: the app's own colours. */
    var currentId by mutableStateOf<String?>(prefs(com.music.spotui.di.Graph.app).getString(K_CURRENT, null)?.takeIf { it.isNotBlank() })
        private set

    /** The picked colour scheme's name ([ThemeSpec.schemeName] falls back to the theme's default). */
    var schemeWanted by mutableStateOf<String?>(prefs(com.music.spotui.di.Graph.app).getString(K_SCHEME, null)?.takeIf { it.isNotBlank() })
        private set

    /** The picked theme, or null when none is picked or the file is gone. */
    val current: Entry? get() = currentId?.let { id -> entries.firstOrNull { it.id == id } }

    /** The scheme the picked theme shows now. */
    val scheme: String? get() = current?.spec?.schemeName(schemeWanted)

    private val colors: Map<String, Int> get() = current?.spec?.colors(schemeWanted) ?: emptyMap()

    /** The colour the picked scheme sets for [key] (`accent`, `background`, `surface`, `text`, `muted`), or null. */
    fun color(key: String): Color? = WallpaperTheme.color(key) ?: colors[key]?.let { Color(it) }

    /** The parts the picked theme hides. */
    val hidden: Set<UiPart> get() = current?.spec?.hiddenParts() ?: emptySet()

    /** The text size the picked theme asks for, or null. */
    val textScale: Float? get() = current?.spec?.textScale()

    /** The colours of [spec]'s scheme [name], for a preview. */
    fun previewColors(spec: ThemeSpec, name: String?): Map<String, Int> = spec.colors(name)

    private fun save(c: Context, id: String?, scheme: String?) {
        prefs(c).edit().apply {
            if (id == null) remove(K_CURRENT) else putString(K_CURRENT, id)
            if (scheme == null) remove(K_SCHEME) else putString(K_SCHEME, scheme)
        }.apply()
        currentId = id
        schemeWanted = scheme
    }

    /** Picks [id] (null: the app's own colours) with its default scheme. */
    fun select(c: Context, id: String?) = save(c, id, null)

    /** Picks a colour scheme of the picked theme. */
    fun selectScheme(c: Context, name: String) {
        val id = currentId ?: return
        if (current?.spec?.schemes?.containsKey(name) == true) save(c, id, name)
    }

    /** Removes an installed theme; if it was the picked one the app is back to its own colours. */
    fun remove(c: Context, id: String) {
        if (!id.startsWith("user:")) return
        runCatching { File(dir(c), id.removePrefix("user:") + ".json").delete() }
        installed = loadInstalled()
        if (currentId == id) save(c, null, null)
    }

    /** At most [ThemeSpec.MAX_BYTES] + 1 bytes (one over means too big); InputStream.readNBytes is not on older Android. */
    private fun readCapped(ins: java.io.InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (out.size() <= ThemeSpec.MAX_BYTES) {
            val n = ins.read(buffer, 0, minOf(buffer.size, ThemeSpec.MAX_BYTES + 1 - out.size()))
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private fun slug(name: String): String =
        name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40).ifEmpty { "theme" }

    /** Installs the theme in [text] and picks it; null when it worked, else what is wrong with the file (a sentence). */
    fun installText(c: Context, text: String): String? {
        val spec = try { parseTheme(text) } catch (e: IllegalArgumentException) { return e.message ?: "not a theme file" }
        val id = slug(spec.name)
        if (installed.size >= MAX_INSTALLED && installed.none { it.id == "user:$id" }) return "$MAX_INSTALLED themes at most: remove one first"
        return try {
            val folder = dir(c)
            folder.mkdirs()
            File(folder, "$id.json").writeText(text)
            installed = loadInstalled()
            save(c, "user:$id", null)
            null
        } catch (e: Exception) {
            "the file could not be saved"
        }
    }

    /** A theme file picked on the phone: read (64 KB at most) and installed on another thread; null when it worked. */
    suspend fun installFile(c: Context, uri: Uri): String? {
        val text = withContext(Dispatchers.IO) {
            runCatching {
                c.contentResolver.openInputStream(uri)?.use { ins ->
                    val bytes = readCapped(ins)
                    if (bytes.size > ThemeSpec.MAX_BYTES) null else String(bytes, Charsets.UTF_8)
                }
            }.getOrNull()
        } ?: return "the file could not be read, or it is larger than ${ThemeSpec.MAX_BYTES / 1024} KB"
        return installText(c, text)
    }

    /** A theme file from an https link on a public host (no redirects); null when it worked. */
    suspend fun installUrl(c: Context, url: String): String? {
        val u = url.trim()
        if (WallpaperPick.publicHttps(u) != null) return "that is not an https address on a public site"
        val text = withContext(Dispatchers.IO) {
            runCatching {
                val conn = URL(u).openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 10_000
                conn.readTimeout = 10_000
                try {
                    if (conn.responseCode != 200) return@runCatching null
                    val bytes = conn.inputStream.use { readCapped(it) }
                    if (bytes.size > ThemeSpec.MAX_BYTES) null else String(bytes, Charsets.UTF_8)
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        } ?: return "the link did not give a theme file (an answer of 200, at most ${ThemeSpec.MAX_BYTES / 1024} KB, no redirect)"
        return installText(c, text)
    }
}
