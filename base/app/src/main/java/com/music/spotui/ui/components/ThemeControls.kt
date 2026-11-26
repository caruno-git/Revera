package com.music.spotui.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import app.myspoti.extensions.ThemeSpec
import com.music.spotui.data.preferences.ThemePref
import kotlinx.coroutines.launch

/*
 * The parts of the themes settings the Classic and the Immersive pages share: installing a theme
 * file, the link field, and the colour dots. Each look draws its own rows around them
 * (ui/screens/ThemeSettings.kt, ui/immersive/pages/ImmersiveThemePage.kt).
 */

/** Opens the system picker for a theme file (`theme.json`); a problem with it is told in a toast. */
@Composable
fun rememberThemeFilePicker(): () -> Unit {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            ThemePref.installFile(ctx, uri)?.let { Toast.makeText(ctx, ctx.getString(R.string.themes_install_failed, it), Toast.LENGTH_LONG).show() }
        }
    }
    return remember(launcher) { { launcher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) } }
}

/** The name of the theme on a row, and what the app says about the picked theme in one line. */
@Composable
fun themeValue(): String {
    val t = ThemePref.current ?: return stringResource(R.string.themes_default)
    val scheme = ThemePref.scheme
    return if (scheme != null && t.spec.schemes.size > 1) "${t.spec.name} · $scheme" else t.spec.name
}

/** Three dots: the page, the cards and the accent of one scheme of [spec]. */
@Composable
fun ThemeDots(spec: ThemeSpec?, scheme: String?, modifier: Modifier = Modifier) {
    val colors = spec?.colors(scheme) ?: emptyMap()
    val dots = listOf(
        colors[ThemeSpec.BACKGROUND]?.let { Color(it) } ?: Color(0xFF121212),
        colors[ThemeSpec.SURFACE]?.let { Color(it) } ?: Color(0xFF2A2A2A),
        colors[ThemeSpec.ACCENT]?.let { Color(it) } ?: Color(0xFFFF4F7B),
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
        for (d in dots) Box14(d)
    }
}

@Composable
private fun Box14(color: Color) {
    androidx.compose.foundation.layout.Box(Modifier.size(18.dp).clip(CircleShape).background(color).border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape))
}

/** A line saying who made [spec] and how many schemes it has. */
@Composable
fun themeDetail(entry: ThemePref.Entry): String {
    val s = entry.spec
    val schemes = if (s.schemes.size > 1) stringResource(R.string.themes_schemes_count, s.schemes.size) else null
    return listOfNotNull(s.author.takeIf { it.isNotBlank() }, schemes).joinToString(" · ")
}

/** The address field and its one button: the theme file at that https address is installed. [chip] draws the button in the look's style. */
@Composable
fun ThemeLinkField(
    text: Color,
    muted: Color,
    accent: Color,
    border: Color,
    modifier: Modifier = Modifier,
    chip: @Composable (label: String, onClick: () -> Unit) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var link by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = link,
            onValueChange = { link = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.wallpaper_link_hint), color = muted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = text, unfocusedTextColor = text, focusedBorderColor = accent,
                unfocusedBorderColor = border, cursorColor = accent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.padding(top = 10.dp)) {
            chip(stringResource(R.string.themes_link_add)) {
                if (!busy && link.isNotBlank()) {
                    busy = true
                    scope.launch {
                        val problem = ThemePref.installUrl(ctx, link)
                        busy = false
                        if (problem == null) link = "" else Toast.makeText(ctx, ctx.getString(R.string.themes_install_failed, problem), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
