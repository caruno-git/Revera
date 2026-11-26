package app.myspoti.power

import android.app.Application
import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application-owned preferences (the app's saver is independent of Android's); no polling or playback commands. */
object EnergySettings {
    private val mutableState = MutableStateFlow(EnergyState())
    val state = mutableState.asStateFlow()
    var oledBlack by mutableStateOf(false)
        private set
    private var app: Application? = null
    private var preferences: SharedPreferences? = null
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }

    fun init(context: Context) {
        if (app != null) return
        val application = context.applicationContext as Application
        app = application
        preferences = application.getSharedPreferences("energy_settings", Context.MODE_PRIVATE)
        preferences?.registerOnSharedPreferenceChangeListener(preferenceListener)
        watchHeat(application)
        refresh()
    }

    // The phone's own heat reading (API 29+): moderate or worse lightens the app, and it only lets go
    // a minute after the phone is back to light or cool, so the screen does not flip between 120 and 60.
    private var hot = false
    private var latestThermal = 0
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val release = Runnable {
        if (latestThermal < android.os.PowerManager.THERMAL_STATUS_MODERATE) { hot = false; refresh() }
    }

    private fun watchHeat(application: Application) {
        val pm = application.getSystemService(android.os.PowerManager::class.java) ?: return
        runCatching {
            pm.addThermalStatusListener(application.mainExecutor) { status ->
                latestThermal = status
                handler.removeCallbacks(release)
                if (status >= android.os.PowerManager.THERMAL_STATUS_MODERATE) {
                    if (!hot) { hot = true; refresh() }
                } else if (hot) {
                    handler.postDelayed(release, RELEASE_DELAY_MS)
                }
            }
        }
    }

    private const val RELEASE_DELAY_MS = 60_000L

    fun setSmoothDisplay(enabled: Boolean) {
        preferences?.edit { putBoolean("smooth_display", enabled) }
        refresh()
    }

    fun setThermalGuard(enabled: Boolean) {
        preferences?.edit { putBoolean("thermal_guard", enabled) }
        refresh()
    }

    fun setSaver(enabled: Boolean) {
        preferences?.edit { putBoolean("manual_saver", enabled) }
        refresh()
    }

    fun setBlackBackground(enabled: Boolean) {
        preferences?.edit { putBoolean("black_background", enabled) }
        refresh()
    }

    fun setCanvasEnabled(enabled: Boolean) {
        preferences?.edit { putBoolean("canvas", enabled) }
        refresh()
    }

    private fun refresh() {
        if (app == null) return
        val next = EnergyState(
            manualSaver = preferences?.getBoolean("manual_saver", false) == true,
            blackPreference = preferences?.getBoolean("black_background", false) == true,
            canvasEnabled = preferences?.getBoolean("canvas", true) != false,
            smoothDisplay = preferences?.getBoolean("smooth_display", true) != false,
            thermalGuard = preferences?.getBoolean("thermal_guard", true) != false,
            thermalHot = hot,
        )
        mutableState.value = next
        oledBlack = next.policy.oledBlack
    }
}
