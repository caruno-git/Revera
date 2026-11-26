package com.music.spotui.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.data.preferences.WallpaperPref
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/*
 * The parts of the wallpaper settings the Classic and the Immersive pages share: picking a file,
 * the link field, the dimming slider and the short texts. Each look draws its own rows around them
 * (ui/screens/WallpaperSettings.kt, ui/immersive/pages/ImmersiveWallpaperPage.kt).
 */

/** Opens the system picker for a picture or a video; the file is imported and set, a failure is told in a toast. */
@Composable
fun rememberWallpaperPicker(): () -> Unit {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val message = when (WallpaperPref.importFile(ctx, uri)) {
                WallpaperPref.Import.OK -> null
                WallpaperPref.Import.TOO_BIG -> R.string.wallpaper_too_big
                WallpaperPref.Import.UNSUPPORTED -> R.string.wallpaper_unsupported
                WallpaperPref.Import.FAILED -> R.string.wallpaper_failed
            }
            if (message != null) Toast.makeText(ctx, message, Toast.LENGTH_LONG).show()
        }
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) }
    }
}

/** What the wallpaper is, in a word or a title: the value of the row that opens its page. */
@Composable
fun wallpaperValue(): String {
    val c = WallpaperPref.chosen ?: return stringResource(R.string.wallpaper_value_off)
    return c.title.ifBlank { stringResource(if (c.video) R.string.wallpaper_value_video else R.string.wallpaper_value_picture) }
}

/** The line that says why a wallpaper that is set does not show, or null. */
@Composable
fun wallpaperHiddenNote(): String? = when (WallpaperPref.hidden) {
    WallpaperPref.Hidden.BLACK_BACKGROUND -> stringResource(R.string.wallpaper_hidden_black)
    WallpaperPref.Hidden.EXTENSION_OFF -> stringResource(R.string.wallpaper_hidden_extension)
    null -> null
}

/**
 * The dimming, in percent, on a slider (saved when the finger lifts), drawn in the colours of the
 * look it is in: the same shape as [CrossfadeSlider].
 */
@Composable
fun WallpaperDimSlider(accent: Color, text: Color, muted: Color, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var value by remember(WallpaperPref.dim) { mutableFloatStateOf(WallpaperPref.dim.toFloat()) }
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.wallpaper_dim), color = text, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.wallpaper_dim_value, value.roundToInt()), color = accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { WallpaperPref.setDim(ctx, value.roundToInt()) },
            valueRange = 0f..WallpaperPref.DIM_MAX.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = accent, activeTrackColor = accent, inactiveTrackColor = muted.copy(alpha = 0.3f),
                activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(stringResource(R.string.wallpaper_dim_detail), color = muted, fontSize = 12.sp)
    }
}

/**
 * The address field with its two buttons: the link is set as a picture or as a video (the address
 * does not always say which). [chip] draws one button in the look's own style.
 */
@Composable
fun WallpaperLinkField(
    text: Color,
    muted: Color,
    accent: Color,
    border: Color,
    modifier: Modifier = Modifier,
    chip: @Composable (label: String, onClick: () -> Unit) -> Unit,
) {
    val ctx = LocalContext.current
    var link by remember { mutableStateOf("") }
    var bad by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = link,
            onValueChange = { link = it; bad = false },
            singleLine = true,
            isError = bad,
            placeholder = { Text(stringResource(R.string.wallpaper_link_hint), color = muted) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = text, unfocusedTextColor = text, focusedBorderColor = accent,
                unfocusedBorderColor = border, cursorColor = accent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (bad) Text(stringResource(R.string.wallpaper_link_bad), color = Color(0xFFF15E6C), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
            chip(stringResource(R.string.wallpaper_link_picture)) {
                if (WallpaperPref.setLink(ctx, link, video = false)) link = "" else bad = true
            }
            chip(stringResource(R.string.wallpaper_link_video)) {
                if (WallpaperPref.setLink(ctx, link, video = true)) link = "" else bad = true
            }
        }
    }
}
