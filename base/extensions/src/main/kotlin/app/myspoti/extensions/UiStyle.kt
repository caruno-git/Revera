package app.myspoti.extensions

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * The manifest's `ui`: what a look-only extension ([Action.UI_STYLE]) changes in the app, all
 * declared, nothing drawn by the extension. The app applies it on its pages and never on a
 * [ProtectedSurface]: while one is on screen the whole app is back to its own look.
 *
 * - [colors]: `accent`, `background`, `surface`, as `#RRGGBB` (or `#AARRGGBB`, opaque only for the
 *   backgrounds). A background or surface too light for the app's white text is not applied.
 * - [hide]: parts of the app the user can also hide in Settings ([UiPart]).
 * - [order]: `nav`, the Classic look's bottom tabs (`home`, `search`, `library`).
 * - [styles]: CSS-like rules, a selector and its properties: `text` { `scale` 0.85 to 1.3 }.
 * - [blocks]: `home`, native blocks ([Blocks]) shown on Home under the extension's name.
 * - [wallpapers]: pictures and silent looping videos the user can pick as the app's wallpaper
 *   ([WallpaperPick]), fetched by the app from https addresses. The wallpaper itself is the user's
 *   choice, in Settings > Display.
 *
 * Unknown colours, parts, orders, selectors and properties are ignored (a newer extension on an
 * older app), `myspoti-ext check` tells the author ([warnings]).
 */
@Serializable
data class UiStyle(
    val colors: Map<String, String> = emptyMap(),
    val hide: List<String> = emptyList(),
    val order: Map<String, List<String>> = emptyMap(),
    val styles: Map<String, Map<String, JsonElement>> = emptyMap(),
    val blocks: Map<String, JsonElement> = emptyMap(),
    val wallpapers: List<WallpaperPick> = emptyList(),
) {
    /** Every reason this `ui` cannot be installed (empty: it can). */
    fun problems(): List<String> = buildList {
        if (colors.isEmpty() && hide.isEmpty() && order.isEmpty() && styles.isEmpty() && blocks.isEmpty() && wallpapers.isEmpty())
            add("ui: nothing to change (colors, hide, order, styles, blocks or wallpapers)")
        if (wallpapers.size > WallpaperPick.MAX) add("ui.wallpapers: ${WallpaperPick.MAX} at most")
        wallpapers.forEachIndexed { i, w -> w.problem()?.let { add("ui.wallpapers[$i]: $it") } }
        colors.forEach { (k, v) -> if (k in COLORS && argb(v) == null) add("ui.colors.$k \"$v\": a colour like #1ED760 or #CC1ED760") }
        if (hide.size > 20) add("ui.hide: 20 parts at most")
        order[NAV]?.let { tabs ->
            if (tabs.toSet() != NAV_TABS.toSet() || tabs.size != NAV_TABS.size) add("ui.order.nav: each of ${NAV_TABS.joinToString()} once")
        }
        styles[TEXT]?.get(SCALE)?.let { s ->
            val v = (s as? JsonPrimitive)?.doubleOrNull
            if (v == null || v < MIN_SCALE || v > MAX_SCALE) add("ui.styles.text.scale: a number from $MIN_SCALE to $MAX_SCALE")
        }
        blocks[HOME]?.let { b ->
            runCatching { Blocks.parse(b.toString(), Surface.HOME) }.exceptionOrNull()?.let { add("ui.blocks.home: ${it.message}") }
        }
    }

    /** What this Myspoti does not know and leaves aside: told to the author, never a reason to refuse. */
    fun warnings(): List<String> = buildList {
        (colors.keys - COLORS).forEach { add("ui.colors.$it: unknown (known: ${COLORS.joinToString()}), ignored") }
        hide.filter { UiPart.of(it) == null }.forEach { add("ui.hide \"$it\": unknown (known: ${UiPart.entries.joinToString { p -> p.key }}), ignored") }
        (order.keys - NAV).forEach { add("ui.order.$it: unknown (known: $NAV), ignored") }
        (styles.keys - TEXT).forEach { add("ui.styles.$it: unknown selector (known: $TEXT), ignored") }
        styles[TEXT]?.keys?.minus(SCALE)?.forEach { add("ui.styles.text.$it: unknown property (known: $SCALE), ignored") }
        (blocks.keys - HOME).forEach { add("ui.blocks.$it: unknown place (known: $HOME), ignored") }
    }

    companion object {
        const val ACCENT = "accent"
        const val BACKGROUND = "background"
        const val SURFACE = "surface"
        val COLORS = setOf(ACCENT, BACKGROUND, SURFACE)
        const val NAV = "nav"
        val NAV_TABS = listOf("home", "search", "library")
        const val TEXT = "text"
        const val SCALE = "scale"
        const val HOME = "home"
        const val MIN_SCALE = 0.85
        const val MAX_SCALE = 1.3

        /** Brighter than this (relative luminance), a background would hide the app's white text. */
        const val MAX_BACKGROUND_LUMINANCE = 0.2

        private val HEX = Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")

        /** `#RRGGBB` or `#AARRGGBB` as an ARGB int, or null. */
        fun argb(text: String): Int? {
            if (!HEX.matches(text)) return null
            val v = text.substring(1).toLong(16)
            return (if (text.length == 7) v or 0xFF000000 else v).toInt()
        }

        /** The relative luminance of an opaque [argb] colour, 0 (black) to 1 (white). */
        fun luminance(argb: Int): Double {
            fun ch(shift: Int): Double {
                val c = ((argb shr shift) and 0xFF) / 255.0
                return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * ch(16) + 0.7152 * ch(8) + 0.0722 * ch(0)
        }
    }
}

/**
 * One wallpaper an extension's look offers (`ui.wallpapers`): a picture or a silent looping video
 * the app fetches itself from an https [url] (the extension never draws or downloads anything).
 * [kind] is [IMAGE], [VIDEO] or [AUTO] (decided by the address: `.mp4`, `.webm`, `.m3u8`...);
 * [thumb] is an optional https preview picture, useful for a video.
 */
@Serializable
data class WallpaperPick(
    val title: String,
    val url: String,
    val kind: String = AUTO,
    val thumb: String? = null,
) {
    /** Whether this wallpaper plays as a video. */
    val video: Boolean
        get() = when (kind) {
            VIDEO -> true
            IMAGE -> false
            else -> path().substringAfterLast('.', "").let { it in VIDEO_EXTENSIONS }
        }

    private fun path(): String = runCatching { java.net.URI(url).path }.getOrNull().orEmpty().lowercase()

    /** Why this wallpaper cannot be used (null: it can). */
    fun problem(): String? {
        if (title.isBlank()) return "a title is needed"
        if (title.trim().length > MAX_TITLE) return "title: $MAX_TITLE characters at most"
        if (kind != AUTO && kind != IMAGE && kind != VIDEO) return "kind \"$kind\": $IMAGE, $VIDEO or $AUTO"
        publicHttps(url)?.let { return "url: $it" }
        thumb?.let { t -> publicHttps(t)?.let { return "thumb: $it" } }
        return null
    }

    companion object {
        const val AUTO = "auto"
        const val IMAGE = "image"
        const val VIDEO = "video"
        const val MAX_TITLE = 40
        const val MAX = 24
        private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "m3u8", "mkv", "mov", "m4v", "3gp")

        /** Null when [url] is an https address on a public host name (no login, no IP address, no local name), else why not. */
        fun publicHttps(url: String): String? {
            if (!url.startsWith("https://")) return "must be https"
            val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return "not a valid address"
            if (uri.rawUserInfo != null) return "no login in the address"
            val host = uri.host?.lowercase() ?: return "not a valid address"
            if (host.startsWith("[") || host.contains(':') || host.all { it.isDigit() || it == '.' }) return "a host name, not an IP address"
            if (!host.contains('.') || host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal"))
                return "a public host, not a local one"
            return null
        }
    }
}

/** A [WallpaperPick] of an enabled extension, as the app lists it: who offers it, and how it plays. */
data class PackWallpaper(
    val extensionId: String,
    val title: String,
    val url: String,
    val video: Boolean,
    val thumb: String? = null,
)

/** The parts a look may hide: each one the user can also turn off in Settings > Display. */
enum class UiPart(val key: String) {
    /** Spotify's DJ on Home. */
    HOME_DJ("home.dj"),
    /** The lyrics card under the Classic player. */
    PLAYER_LYRICS_CARD("player.lyrics-card"),
    /** The line under the player's title that says what plays and how. */
    PLAYER_SOURCE("player.source"),
    /** The capsule at the bottom of the Immersive player. */
    PLAYER_CAPSULE("player.capsule");

    companion object {
        fun of(key: String) = entries.firstOrNull { it.key == key }
    }
}

/**
 * The looks of the enabled extensions as the app applies them: for each colour, order and style,
 * the first extension in the user's order that sets it wins; hidden parts add up; Home blocks are
 * each extension's own ([homeBlocks], by id).
 */
data class AppliedLook(
    val colors: Map<String, Int> = emptyMap(),
    val hidden: Set<UiPart> = emptySet(),
    val navOrder: List<String>? = null,
    val textScale: Float? = null,
    val homeBlocks: List<Pair<String, String>> = emptyList(),
    /** The wallpapers the looks offer, in the user's order; the ones a manifest check would refuse are left out. */
    val wallpapers: List<PackWallpaper> = emptyList(),
) {
    companion object {
        val NONE = AppliedLook()

        /** [looks]: (extension id, its `ui`), in the user's order. Values a manifest check would refuse are skipped. */
        fun of(looks: List<Pair<String, UiStyle>>): AppliedLook {
            val colors = LinkedHashMap<String, Int>()
            val hidden = LinkedHashSet<UiPart>()
            var nav: List<String>? = null
            var scale: Float? = null
            val home = ArrayList<Pair<String, String>>()
            val walls = ArrayList<PackWallpaper>()
            for ((id, ui) in looks) {
                ui.wallpapers.take(WallpaperPick.MAX).filter { it.problem() == null }
                    .mapTo(walls) { PackWallpaper(id, it.title.trim(), it.url, it.video, it.thumb) }
                ui.colors.forEach { (k, v) ->
                    if (k !in UiStyle.COLORS || k in colors) return@forEach
                    val c = UiStyle.argb(v) ?: return@forEach
                    if (k != UiStyle.ACCENT) {
                        // A page must be opaque and dark enough for white text.
                        if ((c ushr 24) != 0xFF || UiStyle.luminance(c) > UiStyle.MAX_BACKGROUND_LUMINANCE) return@forEach
                    }
                    colors[k] = c
                }
                ui.hide.mapNotNullTo(hidden) { UiPart.of(it) }
                if (nav == null) ui.order[UiStyle.NAV]?.takeIf { it.size == UiStyle.NAV_TABS.size && it.toSet() == UiStyle.NAV_TABS.toSet() }?.let { nav = it }
                if (scale == null) ((ui.styles[UiStyle.TEXT]?.get(UiStyle.SCALE) as? JsonPrimitive)?.doubleOrNull)
                    ?.takeIf { it in UiStyle.MIN_SCALE..UiStyle.MAX_SCALE }?.let { scale = it.toFloat() }
                ui.blocks[UiStyle.HOME]?.let { home += id to it.toString() }
            }
            return AppliedLook(colors, hidden, nav, scale, home, walls)
        }
    }
}
