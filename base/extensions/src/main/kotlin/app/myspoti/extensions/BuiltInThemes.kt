package app.myspoti.extensions

/**
 * The themes that come with the app, with the same format as one the user installs (a
 * [ThemeSpec]); they cannot be removed. Well-known dark palettes, each with its variants as
 * colour schemes.
 */
object BuiltInThemes {
    private fun scheme(accent: String, background: String, surface: String, text: String, muted: String) = mapOf(
        ThemeSpec.ACCENT to accent, ThemeSpec.BACKGROUND to background, ThemeSpec.SURFACE to surface,
        ThemeSpec.TEXT to text, ThemeSpec.MUTED to muted,
    )

    val all: List<Pair<String, ThemeSpec>> = listOf(
        "catppuccin" to ThemeSpec(
            name = "Catppuccin", author = "Catppuccin", description = "Soothing pastel colours on a dark base.",
            schemes = linkedMapOf(
                "Mocha" to scheme("#CBA6F7", "#1E1E2E", "#313244", "#CDD6F4", "#A6ADC8"),
                "Macchiato" to scheme("#C6A0F6", "#24273A", "#363A4F", "#CAD3F5", "#A5ADCB"),
                "Frappé" to scheme("#CA9EE6", "#303446", "#414559", "#C6D0F5", "#A5ADCE"),
            ),
            default = "Mocha",
        ),
        "rose-pine" to ThemeSpec(
            name = "Rosé Pine", author = "Rosé Pine", description = "All natural pine, faux fur and a bit of soho vibes.",
            schemes = linkedMapOf(
                "Main" to scheme("#EBBCBA", "#191724", "#1F1D2E", "#E0DEF4", "#908CAA"),
                "Moon" to scheme("#EA9A97", "#232136", "#2A273F", "#E0DEF4", "#908CAA"),
            ),
            default = "Main",
        ),
        "dracula" to ThemeSpec(
            name = "Dracula", author = "Dracula Theme", description = "A dark theme with vivid accents.",
            schemes = linkedMapOf(
                "Purple" to scheme("#BD93F9", "#282A36", "#343746", "#F8F8F2", "#8B9AC8"),
                "Pink" to scheme("#FF79C6", "#282A36", "#343746", "#F8F8F2", "#8B9AC8"),
                "Green" to scheme("#50FA7B", "#282A36", "#343746", "#F8F8F2", "#8B9AC8"),
            ),
            default = "Purple",
        ),
        "nord" to ThemeSpec(
            name = "Nord", author = "Arctic Ice Studio", description = "An arctic, north-bluish palette.",
            schemes = linkedMapOf(
                "Frost" to scheme("#88C0D0", "#2E3440", "#3B4252", "#ECEFF4", "#A3ADBF"),
                "Aurora green" to scheme("#A3BE8C", "#2E3440", "#3B4252", "#ECEFF4", "#A3ADBF"),
                "Aurora purple" to scheme("#B48EAD", "#2E3440", "#3B4252", "#ECEFF4", "#A3ADBF"),
            ),
            default = "Frost",
        ),
        "gruvbox" to ThemeSpec(
            name = "Gruvbox", author = "morhetz", description = "Retro groove colours, dark.",
            schemes = linkedMapOf(
                "Yellow" to scheme("#FABD2F", "#282828", "#3C3836", "#EBDBB2", "#A89984"),
                "Aqua" to scheme("#8EC07C", "#282828", "#3C3836", "#EBDBB2", "#A89984"),
                "Orange" to scheme("#FE8019", "#282828", "#3C3836", "#EBDBB2", "#A89984"),
            ),
            default = "Yellow",
        ),
        "tokyo-night" to ThemeSpec(
            name = "Tokyo Night", author = "enkia", description = "A clean dark theme inspired by the lights of Tokyo.",
            schemes = linkedMapOf(
                "Night" to scheme("#7AA2F7", "#1A1B26", "#24283B", "#C0CAF5", "#9AA5CE"),
                "Storm" to scheme("#7AA2F7", "#24283B", "#2F334D", "#C0CAF5", "#9AA5CE"),
            ),
            default = "Night",
        ),
    )
}
