package app.myspoti.extensions

/** Reads a theme file: the text of a `theme.json`. Throws [IllegalArgumentException] with a reason the user can read. */
fun parseTheme(text: String): ThemeSpec {
    require(text.length <= ThemeSpec.MAX_BYTES) { "theme file larger than ${ThemeSpec.MAX_BYTES / 1024} KB" }
    val spec = try {
        ExtJson.decodeFromString(ThemeSpec.serializer(), text)
    } catch (e: Exception) {
        throw IllegalArgumentException("not a theme file: ${e.message?.lineSequence()?.firstOrNull().orEmpty().take(120)}")
    }
    val problems = spec.problems()
    require(problems.isEmpty()) { problems.first() }
    return spec
}
