package app.myspoti.extensions

import kotlinx.serialization.Serializable

/**
 * A theme the user installs, as in Spicetify: a file (`theme.json`) with a name and one or more
 * colour schemes, the way a Spicetify theme has a `color.ini` with several sections. The user picks
 * the theme and its scheme in Settings > Wallpaper and themes; the app applies the scheme's colours
 * under whatever an extension's look sets (an extension is the layer on top, as in Spicetify).
 *
 * - [schemes]: scheme name -> colours: `accent`, `background`, `surface`, `text`, `muted`, as
 *   `#RRGGBB` (`#AARRGGBB` for the accent). Backgrounds stay dark: the app draws light text and
 *   icons on them, so a background or surface too bright, or a text too dim for it, is refused.
 * - [default]: the scheme used first (the first one when missing).
 * - [scale]: the text size, 0.85 to 1.3.
 * - [hide]: parts of the app to hide ([UiPart]).
 *
 * Nothing is drawn or run by the theme: it is values the app applies, like an extension's `ui`.
 */
@Serializable
data class ThemeSpec(
    val name: String = "",
    val author: String = "",
    val version: String = "",
    val description: String = "",
    val schemes: Map<String, Map<String, String>> = emptyMap(),
    val default: String? = null,
    val scale: Double? = null,
    val hide: List<String> = emptyList(),
) {
    /** Every reason this theme cannot be installed (empty: it can). */
    fun problems(): List<String> = buildList {
        if (name.isBlank()) add("name: required")
        if (name.length > MAX_NAME) add("name: $MAX_NAME characters at most")
        if (author.length > MAX_NAME) add("author: $MAX_NAME characters at most")
        if (version.length > 20) add("version: 20 characters at most")
        if (description.length > 300) add("description: 300 characters at most")
        if (schemes.isEmpty()) add("schemes: at least one colour scheme")
        if (schemes.size > MAX_SCHEMES) add("schemes: $MAX_SCHEMES at most")
        if (default != null && default !in schemes) add("default \"$default\": not one of the schemes")
        if (scale != null && (scale < UiStyle.MIN_SCALE || scale > UiStyle.MAX_SCALE)) add("scale: a number from ${UiStyle.MIN_SCALE} to ${UiStyle.MAX_SCALE}")
        if (hide.size > 20) add("hide: 20 parts at most")
        schemes.forEach { (scheme, colors) ->
            val at = "schemes.\"$scheme\""
            if (scheme.isBlank() || scheme.length > MAX_SCHEME_NAME) add("$at: a name of 1 to $MAX_SCHEME_NAME characters")
            if (colors.keys.none { it in COLORS }) add("$at: none of ${COLORS.joinToString()}")
            colors.forEach { (k, v) -> if (k in COLORS) colorProblem(k, v)?.let { add("$at.$k \"$v\": $it") } }
            val bg = colors[BACKGROUND]?.let { UiStyle.argb(it) }
            val text = colors[TEXT]?.let { UiStyle.argb(it) }
            if (bg != null && text != null && colorProblem(BACKGROUND, colors.getValue(BACKGROUND)) == null &&
                colorProblem(TEXT, colors.getValue(TEXT)) == null && contrast(text, bg) < MIN_CONTRAST
            ) add("$at.text: too little contrast with the background (at least $MIN_CONTRAST:1)")
        }
    }

    /** What this app does not know and leaves aside: told to the author, never a reason to refuse. */
    fun warnings(): List<String> = buildList {
        schemes.forEach { (scheme, colors) -> (colors.keys - COLORS).forEach { add("schemes.\"$scheme\".$it: unknown (known: ${COLORS.joinToString()}), ignored") } }
        hide.filter { UiPart.of(it) == null }.forEach { add("hide \"$it\": unknown (known: ${UiPart.entries.joinToString { p -> p.key }}), ignored") }
    }

    /** The scheme to use for [wanted]: it, else [default], else the first; null when there is none. */
    fun schemeName(wanted: String?): String? =
        wanted?.takeIf { it in schemes } ?: default?.takeIf { it in schemes } ?: schemes.keys.firstOrNull()

    /** The valid colours of scheme [wanted] (see [schemeName]), as ARGB ints by key; a refused value is left out. */
    fun colors(wanted: String?): Map<String, Int> {
        val scheme = schemes[schemeName(wanted)] ?: return emptyMap()
        val out = LinkedHashMap<String, Int>()
        for ((k, v) in scheme) {
            if (k !in COLORS || colorProblem(k, v) != null) continue
            out[k] = UiStyle.argb(v) ?: continue
        }
        return out
    }

    /** The parts to hide that this app knows. */
    fun hiddenParts(): Set<UiPart> = hide.mapNotNull { UiPart.of(it) }.toSet()

    /** The text size, or null. */
    fun textScale(): Float? = scale?.takeIf { it in UiStyle.MIN_SCALE..UiStyle.MAX_SCALE }?.toFloat()

    companion object {
        const val ACCENT = UiStyle.ACCENT
        const val BACKGROUND = UiStyle.BACKGROUND
        const val SURFACE = UiStyle.SURFACE
        const val TEXT = "text"
        const val MUTED = "muted"
        val COLORS = listOf(ACCENT, BACKGROUND, SURFACE, TEXT, MUTED)

        const val MAX_NAME = 40
        const val MAX_SCHEME_NAME = 30
        const val MAX_SCHEMES = 16
        /** A theme file is small: colours and names. */
        const val MAX_BYTES = 64 * 1024
        const val MIN_CONTRAST = 4.5

        /** The text colour must be light enough for dark pages: no dimmer than this (relative luminance). */
        private const val MIN_TEXT_LUMINANCE = 0.3
        private const val MIN_MUTED_LUMINANCE = 0.12

        private fun colorProblem(key: String, value: String): String? {
            val c = UiStyle.argb(value) ?: return "a colour like #1ED760"
            if (key == ACCENT) return null
            if ((c ushr 24) != 0xFF) return "opaque (#RRGGBB)"
            val l = UiStyle.luminance(c)
            return when (key) {
                BACKGROUND, SURFACE -> if (l > UiStyle.MAX_BACKGROUND_LUMINANCE) "too bright: the app draws light text on it" else null
                TEXT -> if (l < MIN_TEXT_LUMINANCE) "too dark for dark pages" else null
                MUTED -> if (l < MIN_MUTED_LUMINANCE) "too dark for dark pages" else null
                else -> null
            }
        }

        /** The WCAG contrast ratio of two opaque colours. */
        fun contrast(a: Int, b: Int): Double {
            val la = UiStyle.luminance(a)
            val lb = UiStyle.luminance(b)
            return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
        }
    }
}
