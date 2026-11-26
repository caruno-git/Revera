package app.myspoti.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ThemeSpecTest {
    private val rose = mapOf("accent" to "#ebbcba", "background" to "#191724", "surface" to "#1f1d2e", "text" to "#e0def4", "muted" to "#908caa")

    @Test fun aThemeFileIsRead() {
        val t = parseTheme(
            """{ "name": "Rosé", "author": "Me", "default": "Moon", "scale": 1.1, "hide": ["home.dj", "nope"],
                 "schemes": { "Main": { "accent": "#ebbcba", "background": "#191724", "text": "#e0def4" },
                              "Moon": { "background": "#232136" } }, "extra": 1 }""",
        )
        assertEquals("Moon", t.schemeName(null))
        assertEquals(setOf(UiPart.HOME_DJ), t.hiddenParts())
        assertEquals(1.1f, t.textScale())
        assertEquals(3, t.colors("Main").size)
        assertTrue(t.warnings().any { it.startsWith("hide") })
    }

    @Test fun aBadFileSaysWhy() {
        fun reason(text: String) = try { parseTheme(text); fail("accepted"); "" } catch (e: IllegalArgumentException) { e.message.orEmpty() }
        assertTrue(reason("nonsense").startsWith("not a theme file"))
        assertTrue(reason("""{ "name": "x", "schemes": {} }""").startsWith("schemes"))
        assertTrue(reason("x".repeat(ThemeSpec.MAX_BYTES + 1)).contains("larger"))
    }

    @Test fun darkPagesAndReadableText() {
        fun problems(scheme: Map<String, String>) = ThemeSpec(name = "x", schemes = mapOf("a" to scheme)).problems()
        assertTrue(problems(mapOf("background" to "#FFFFFF")).any { it.contains("too bright") })
        assertTrue(problems(mapOf("background" to "#80000000")).any { it.contains("opaque") })
        assertTrue(problems(mapOf("text" to "#222222")).any { it.contains("too dark") })
        assertTrue(problems(mapOf("background" to "#606060", "text" to "#9a9a9a")).any { it.contains("contrast") })
        assertTrue(problems(rose).isEmpty())
    }

    @Test fun builtInThemesAreValid() {
        for ((id, t) in BuiltInThemes.all) assertTrue("$id: ${t.problems()}", t.problems().isEmpty())
    }
}
