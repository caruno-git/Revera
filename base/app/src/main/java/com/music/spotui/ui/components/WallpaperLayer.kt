package com.music.spotui.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import app.myspoti.engine.R
import app.myspoti.power.EnergySettings
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.data.preferences.Look
import com.music.spotui.data.preferences.LookPref
import com.music.spotui.data.preferences.WallpaperPref
import com.music.spotui.ui.immersive.Immersive
import com.music.spotui.ui.immersive.shell.immersiveHazeSource
import com.music.spotui.ui.theme.AppBackground

/**
 * The app's wallpaper (Settings > Display): a picture or a silent looping video filling the window,
 * drawn first so every page is over it, in the Classic look and in the Immersive one. The pages'
 * whole-page fills are see-through while it shows (PageFill / Immersive.PageFill); one scrim of the
 * look's page colour over the wallpaper is the dimming. Draws nothing when no wallpaper shows
 * ([WallpaperPref.shown]): none set, the black background setting, a pack whose extension is off,
 * or a protected surface on screen.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun WallpaperLayer() {
    val w = WallpaperPref.shown ?: return
    val scrim = if (LookPref.look == Look.CLASSIC) AppBackground else Immersive.Page
    // A new wallpaper is a new player or picture, never the old one reused.
    key(w.source, w.video) {
        // Recorded for the Immersive look's glass and frost (a no-op in Classic): before the black, so it is in the recording.
        Box(Modifier.fillMaxSize().immersiveHazeSource().background(Color.Black)) {
            if (w.video) {
                WallpaperVideo(if (w.local) Uri.fromFile(WallpaperPref.fileOf(w)) else Uri.parse(w.source))
            } else {
                GlideImage(
                    model = if (w.local) WallpaperPref.fileOf(w) else w.source,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            // The dimming: the page colour over the wallpaper, so the text stays readable.
            Box(Modifier.fillMaxSize().background(scrim.copy(alpha = WallpaperPref.dim / 100f)))
        }
    }
}

/**
 * A looping video with no sound: the audio track is not even loaded, so it never takes the audio
 * from the music, and it is held to 720p (480p with the app's energy saver). It plays only while
 * the app is on screen and the app's energy saver is off and the phone is not hot (then it stays on its first frame).
 */
@Composable
private fun WallpaperVideo(uri: Uri) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val energy by EnergySettings.state.collectAsState()
    val policy = energy.policy
    val exo = remember(uri) {
        ExoPlayer.Builder(ctx).build().apply {
            volume = 0f
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }
    }
    val height = minOf(MAX_HEIGHT, policy.videoMaxHeight)
    DisposableEffect(exo, height) {
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, height)
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .build()
        onDispose { }
    }
    DisposableEffect(exo, lifecycle, policy.lowPower) {
        val sync = { exo.playWhenReady = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) && !policy.lowPower }
        val observer = LifecycleEventObserver { _, _ -> sync() }
        lifecycle.addObserver(observer)
        sync()
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
    DisposableEffect(exo) { onDispose { exo.release() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        update = { it.player = exo },
        factory = { c ->
            // Inflated: the surface type (texture view, so the pages can be over it) can only be set from XML.
            (android.view.LayoutInflater.from(c).inflate(R.layout.canvas_player_view, null) as androidx.media3.ui.PlayerView).apply {
                setArtworkDisplayMode(androidx.media3.ui.PlayerView.ARTWORK_DISPLAY_MODE_OFF)
                setDefaultArtwork(null)
                player = exo
            }
        },
    )
}

/** The wallpaper video's tallest picture. */
private const val MAX_HEIGHT = 720
