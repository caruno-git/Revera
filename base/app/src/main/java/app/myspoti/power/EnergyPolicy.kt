package app.myspoti.power

/** Playback transport and listening events are independent of this visual policy. */
data class EnergyPolicy(
    val saverEnabled: Boolean = false,
    val oledBlack: Boolean = false,
    val allowCanvas: Boolean = true,
    val videoMaxHeight: Int = 1080,
    val allowBackgroundLookups: Boolean = true,
    val progressFps: Int = 30,
    /** The word-by-word sweep of synced lyrics: 60 frames a second (the screen ran it at 120), 30 with the saver. */
    val lyricsFps: Int = 60,
    /** 0 = no preference: the LTPO screen already drops its rate when nothing moves. */
    val displayRefreshCap: Float = 0f,
    /** Ask for the screen's highest rate (120 Hz) while the app is on screen: the user's switch, off when the saver or the heat limits it. */
    val preferHighRefresh: Boolean = true,
    /** The phone is hot: the same limits as the saver for the screen and the animations (not for lookups), until it cools. */
    val thermalLimited: Boolean = false,
) {
    /** Decorative motion (a wallpaper video, looping artwork) stands still: the saver is on or the phone is hot. */
    val lowPower: Boolean get() = saverEnabled || thermalLimited


    companion object {
        /**
         * Only the app's own switch turns the saver on: Android's battery saver is not
         * followed (it already lowers the screen's refresh rate by itself).
         */
        fun resolve(
            manualSaver: Boolean,
            blackPreference: Boolean,
            canvasEnabled: Boolean = true,
            smoothDisplay: Boolean = true,
            thermalLimited: Boolean = false,
        ): EnergyPolicy {
            val saver = manualSaver
            // The saver and the heat limit the screen and the animations the same way.
            val limited = saver || thermalLimited
            return EnergyPolicy(
                saverEnabled = saver,
                oledBlack = blackPreference,
                allowCanvas = !limited && canvasEnabled,
                videoMaxHeight = if (limited) 480 else 1080,
                allowBackgroundLookups = !saver,
                progressFps = if (limited) 20 else 30,
                lyricsFps = if (limited) 30 else 60,
                displayRefreshCap = if (limited) 60f else 0f,
                preferHighRefresh = smoothDisplay && !limited,
                thermalLimited = thermalLimited,
            )
        }
    }
}

data class EnergyState(
    val manualSaver: Boolean = false,
    /** AMOLED appearance stays user-controlled while saver limits visual work. */
    val blackPreference: Boolean = false,
    /** The user's Canvas switch (Settings), independent of the saver. */
    val canvasEnabled: Boolean = true,
    /** The user's switch: ask for the screen's highest refresh rate (120 Hz). */
    val smoothDisplay: Boolean = true,
    /** The user's switch: lighten the app by itself while the phone is hot. */
    val thermalGuard: Boolean = true,
    /** The phone reports it is hot (not saved: it is read from Android as it changes). */
    val thermalHot: Boolean = false,
) {
    val policy: EnergyPolicy
        get() = EnergyPolicy.resolve(manualSaver, blackPreference, canvasEnabled, smoothDisplay, thermalLimited = thermalGuard && thermalHot)
}
