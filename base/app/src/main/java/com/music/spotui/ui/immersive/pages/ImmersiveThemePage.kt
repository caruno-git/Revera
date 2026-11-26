package com.music.spotui.ui.immersive.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.data.preferences.ThemePref
import com.music.spotui.ui.components.ThemeLinkField
import com.music.spotui.ui.components.rememberThemeFilePicker
import com.music.spotui.ui.components.themeDetail
import com.music.spotui.ui.immersive.Immersive

/**
 * Themes in the Immersive settings: its own page in the Display category, in the same grouped
 * cards as the others (the Classic look has its own, ui/screens/ThemeSettings.kt). Themes are
 * installed files with colour schemes, as in Spicetify; a few come with the app.
 */
@Composable
internal fun ThemesPage(back: () -> Unit) {
    val ctx = LocalContext.current
    val pickFile = rememberThemeFilePicker()
    val current = ThemePref.current
    val entries = ThemePref.entries
    ImmersivePage(title = stringResource(R.string.themes_title), onBack = back) {
        immersiveGroup("match") {
            row {
                ImmersiveSwitchRow(
                    stringResource(R.string.themes_match),
                    checked = com.music.spotui.data.preferences.WallpaperTheme.match,
                    onChecked = { com.music.spotui.data.preferences.WallpaperTheme.setMatch(ctx, it) },
                    detail = stringResource(if (com.music.spotui.data.preferences.WallpaperPref.chosen == null) R.string.themes_match_none else R.string.themes_match_detail),
                )
            }
        }
        immersiveGroup("pick", headerRes = R.string.themes_section_pick) {
            row {
                ImmersiveChoiceRow(
                    stringResource(R.string.themes_default), selected = current == null,
                    detail = stringResource(R.string.themes_default_detail), onClick = { ThemePref.select(ctx, null) },
                )
            }
            for (e in entries) row {
                ImmersiveChoiceRow(e.spec.name, selected = current?.id == e.id, detail = themeDetail(e).ifBlank { null }, onClick = { ThemePref.select(ctx, e.id) })
            }
        }
        if (current != null && current.spec.schemes.size > 1) immersiveGroup("scheme", headerRes = R.string.themes_section_scheme) {
            for (name in current.spec.schemes.keys) row {
                ImmersiveChoiceRow(name, selected = ThemePref.scheme == name, onClick = { ThemePref.selectScheme(ctx, name) })
            }
        }
        immersiveGroup("add", headerRes = R.string.themes_section_add, footerRes = if (current == null || current.builtIn) R.string.themes_note else null) {
            row {
                ImmersiveGroupRow(
                    stringResource(R.string.themes_file), detail = stringResource(R.string.themes_file_detail),
                    chevron = true, onClick = pickFile,
                )
            }
            row {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    ThemeLinkField(
                        text = Immersive.Text, muted = Immersive.TextSecondary, accent = Immersive.Accent, border = Immersive.Separator,
                    ) { label, onClick ->
                        Text(
                            label, color = Immersive.Text, fontSize = 14.sp,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Immersive.Separator).clickable(onClick = onClick)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
        if (current != null && !current.builtIn) immersiveGroup("remove", footerRes = R.string.themes_note) {
            row { ImmersiveGroupRow(stringResource(R.string.themes_remove), destructive = true, onClick = { ThemePref.remove(ctx, current.id) }) }
        }
    }
}
