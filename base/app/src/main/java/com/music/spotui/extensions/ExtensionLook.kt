package com.music.spotui.extensions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import app.myspoti.extensions.Action
import app.myspoti.extensions.AppliedLook
import app.myspoti.extensions.Area
import app.myspoti.extensions.SurfaceGuard
import app.myspoti.extensions.UiPart
import app.myspoti.extensions.UiStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The look the enabled `ui.style` extensions give the app ([AppliedLook]), read by the theme
 * (colours, text size), the tab bar (order), the pages that can hide a part, and Home (blocks).
 * Compose state: what reads it follows an install, a switch or a reorder at once. While a
 * protected surface is on screen it is [AppliedLook.NONE]: nothing of theirs restyles those.
 */
object ExtensionLook {
    private var look by mutableStateOf(AppliedLook.NONE)
    private var shielded by mutableStateOf(false)

    /** What applies now. */
    val applied: AppliedLook get() = if (shielded) AppliedLook.NONE else look

    private var watching = false

    /** Follows the installed extensions and the protected surfaces (called by [Extensions.init]). */
    fun watch() {
        if (watching) return
        watching = true
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            combine(Extensions.registry, SurfaceGuard.app.state) { r, guard -> r to guard.isNotEmpty() }.collect { (r, guard) ->
                val looks = r.byPriority(Area.OTHER.key, r.extensions.filter { it.enabled && Action.UI_STYLE.key in it.granted }) { it.id }
                    .mapNotNull { e -> Extensions.manifest(e.id)?.look()?.let { e.id to it } }
                val next = runCatching { AppliedLook.of(looks) }.getOrDefault(AppliedLook.NONE)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    if (look != next) look = next
                    shielded = guard
                }
            }
        }
    }

    /** The colour a look sets for [key] ([UiStyle.ACCENT], [UiStyle.BACKGROUND], [UiStyle.SURFACE]), or null. */
    fun color(key: String): Color? = applied.colors[key]?.let { Color(it) } ?: themeColor(key)

    /**
     * The colour the picked theme's scheme sets for [key] (also `text` and `muted`), or null: under
     * an extension's look, and none while a protected surface is on screen.
     */
    fun themeColor(key: String): Color? = if (shielded) null else com.music.spotui.data.preferences.ThemePref.color(key)

    /** Whether a look hides [part]. */
    fun hides(part: UiPart): Boolean = part in applied.hidden || (!shielded && part in com.music.spotui.data.preferences.ThemePref.hidden)

    /** The text size a look asks for, 1 without one. */
    val textScale: Float get() = applied.textScale ?: (if (shielded) null else com.music.spotui.data.preferences.ThemePref.textScale) ?: 1f

    /** The wallpapers the enabled looks offer (`ui.wallpapers`), in the user's order; none while a protected surface is on screen. */
    val wallpapers: List<app.myspoti.extensions.PackWallpaper> get() = applied.wallpapers

    /** A protected surface is on screen: nothing of a look, the wallpaper included, shows. */
    val isShielded: Boolean get() = shielded

    /** The Classic look's bottom tabs as a look orders them (`home`, `search`, `library`), or null. */
    val navOrder: List<String>? get() = applied.navOrder
}
