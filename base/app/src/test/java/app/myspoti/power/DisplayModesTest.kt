package app.myspoti.power

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayModesTest {
    private val modes = listOf(
        DisplayModeInfo(1, 1080, 2400, 60f), DisplayModeInfo(2, 1080, 2400, 120f),
        DisplayModeInfo(3, 720, 1600, 60f), DisplayModeInfo(4, 720, 1600, 120f),
    )
    private val now = modes[0]

    @Test fun highestRateOfTheCurrentResolution() = assertEquals(2, DisplayModes.pick(modes, now, 0f, true))
    @Test fun neverChangesTheResolution() = assertEquals(4, DisplayModes.pick(modes, modes[2], 0f, true))
    @Test fun aCapPicksTheFastestModeUnderIt() = assertEquals(1, DisplayModes.pick(modes, now, 60f, true))
    @Test fun noPreferenceLeavesItToTheSystem() = assertEquals(0, DisplayModes.pick(modes, now, 0f, false))
    @Test fun aScreenWithOneRateAsksForThat() = assertEquals(1, DisplayModes.pick(listOf(modes[0]), now, 0f, true))
    @Test fun ninetyHertzScreens() {
        val m = listOf(DisplayModeInfo(1, 1080, 2400, 60f), DisplayModeInfo(2, 1080, 2400, 90f))
        assertEquals(2, DisplayModes.pick(m, m[0], 0f, true))
        assertEquals(1, DisplayModes.pick(m, m[0], 60f, true))
    }
    @Test fun aCapNoModeMeetsIsNoPreference() = assertEquals(0, DisplayModes.pick(listOf(DisplayModeInfo(2, 1080, 2400, 120f)), now.copy(id = 2), 60f, false))
}
