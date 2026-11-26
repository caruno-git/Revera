package app.myspoti.power

/** A screen mode as Android lists it: an id, a resolution and a refresh rate. */
data class DisplayModeInfo(val id: Int, val width: Int, val height: Int, val hz: Float)

/**
 * Which screen mode the window asks for (`preferredDisplayModeId`). Only modes of the current
 * resolution are considered, so asking for a rate never changes the sharpness.
 */
object DisplayModes {
    /**
     * The id of the mode to ask for, or 0 for no preference. [capHz] above 0: the fastest mode that
     * is not above it (the saver, or the heat: 60). Else [highest]: the fastest mode (120 Hz where
     * the screen has it). Else the system decides.
     */
    fun pick(modes: List<DisplayModeInfo>, current: DisplayModeInfo, capHz: Float, highest: Boolean): Int {
        val same = modes.filter { it.width == current.width && it.height == current.height }
        if (capHz > 0f) return same.filter { it.hz <= capHz + 0.5f }.maxByOrNull { it.hz }?.id ?: 0
        if (highest) return same.maxByOrNull { it.hz }?.id ?: 0
        return 0
    }
}
