package app.myspoti.extensions

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperPaletteTest {
    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun hex(c: Int) = "#%06X".format(c and 0xFFFFFF)
    private fun problems(m: Map<String, Int>) = ThemeSpec(name = "w", schemes = mapOf("w" to m.mapValues { hex(it.value) })).problems()

    @Test fun everyColourGivesAThemeTheAppAccepts() {
        for (r in 0..255 step 51) for (g in 0..255 step 51) for (b in 0..255 step 51) {
            val m = WallpaperPalette.derive(IntArray(100) { rgb(r, g, b) }) ?: continue
            assertTrue("rgb($r,$g,$b): ${problems(m)}", problems(m).isEmpty())
        }
    }

    @Test fun theAccentFollowsTheMainColour() {
        val red = WallpaperPalette.derive(IntArray(100) { rgb(220, 30, 30) })!!.getValue("accent")
        val blue = WallpaperPalette.derive(IntArray(100) { rgb(30, 60, 230) })!!.getValue("accent")
        assertTrue(((red shr 16) and 0xFF) > (red and 0xFF) + 60)
        assertTrue((blue and 0xFF) > ((blue shr 16) and 0xFF) + 60)
        val mostlyGreen = WallpaperPalette.derive(IntArray(100) { if (it < 70) rgb(30, 200, 60) else rgb(220, 30, 30) })!!.getValue("accent")
        assertTrue(((mostlyGreen shr 8) and 0xFF) > ((mostlyGreen shr 16) and 0xFF) + 40)
    }

    @Test fun aWallpaperWithoutColourKeepsTheAccent() {
        val grey = WallpaperPalette.derive(IntArray(100) { rgb(128, 128, 128) })!!
        assertNull(grey["accent"])
        assertNotNull(grey["background"])
        assertNull(WallpaperPalette.derive(IntArray(0)))
        assertNull(WallpaperPalette.derive(IntArray(10) { 0 }))
    }
}
