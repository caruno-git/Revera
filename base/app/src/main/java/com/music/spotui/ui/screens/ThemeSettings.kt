package com.music.spotui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.myspoti.engine.R
import com.music.spotui.data.preferences.ThemePref
import com.music.spotui.ui.components.ThemeDots
import com.music.spotui.ui.components.ThemeLinkField
import com.music.spotui.ui.components.rememberThemeFilePicker
import com.music.spotui.ui.components.themeDetail
import com.music.spotui.ui.components.themeValue
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppDestructive
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone

/*
 * Themes in the Classic settings: a row in the Appearance tab (under Wallpaper) that opens a page of
 * its own, with the same rows, chips and section titles as the rest of Settings. The Immersive look
 * has its own page (ImmersiveThemePage).
 */

/** The row in the Appearance tab: the picked theme, one line. */
@Composable
internal fun ThemesLink(onOpen: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.themes_title), color = AppText, fontSize = 16.sp)
            Text(stringResource(R.string.themes_detail), color = AppMuted, fontSize = 12.sp)
        }
        Text(themeValue(), color = AppAccent, fontSize = 14.sp, maxLines = 1, modifier = Modifier.padding(start = 12.dp).weight(0.6f, fill = false))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppMuted)
    }
}

/** The page: the themes, the picked theme's colour schemes, adding a theme, removing one. */
@Composable
internal fun ThemesPage() {
    val ctx = LocalContext.current
    val pickFile = rememberThemeFilePicker()
    val current = ThemePref.current

    com.music.spotui.ui.screens.SwitchRow(
        stringResource(R.string.themes_match),
        stringResource(if (com.music.spotui.data.preferences.WallpaperPref.chosen == null) R.string.themes_match_none else R.string.themes_match_detail),
        com.music.spotui.data.preferences.WallpaperTheme.match,
    ) { com.music.spotui.data.preferences.WallpaperTheme.setMatch(ctx, it) }

    Section(R.string.themes_section_pick)
    ThemeRow(
        title = stringResource(R.string.themes_default), detail = stringResource(R.string.themes_default_detail),
        selected = current == null, dots = { ThemeDots(null, null) },
    ) { ThemePref.select(ctx, null) }
    for (e in ThemePref.entries) {
        ThemeRow(
            title = e.spec.name, detail = themeDetail(e), selected = current?.id == e.id,
            dots = { ThemeDots(e.spec, e.spec.schemeName(if (current?.id == e.id) ThemePref.schemeWanted else null)) },
        ) { ThemePref.select(ctx, e.id) }
    }

    if (current != null && current.spec.schemes.size > 1) {
        Section(R.string.themes_section_scheme)
        for (name in current.spec.schemes.keys) {
            ThemeRow(title = name, detail = "", selected = ThemePref.scheme == name, dots = { ThemeDots(current.spec, name) }) {
                ThemePref.selectScheme(ctx, name)
            }
        }
    }

    Section(R.string.themes_section_add)
    Column(Modifier.fillMaxWidth().clickable(onClick = pickFile).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(stringResource(R.string.themes_file), color = AppText, fontSize = 16.sp)
        Text(stringResource(R.string.themes_file_detail), color = AppMuted, fontSize = 12.sp)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(stringResource(R.string.themes_link), color = AppText, fontSize = 16.sp)
        Text(stringResource(R.string.themes_link_detail), color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        ThemeLinkField(text = AppText, muted = AppMuted, accent = AppAccent, border = tone(0xFF3E3E3E)) { label, onClick ->
            Text(
                label, color = AppText, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(tone(0xFF2A2A2A)).clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }

    if (current != null && !current.builtIn) {
        Text(
            stringResource(R.string.themes_remove), color = AppDestructive, fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().clickable { ThemePref.remove(ctx, current.id) }.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
    Text(stringResource(R.string.themes_note), color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
}

@Composable
private fun ThemeRow(title: String, detail: String, selected: Boolean, dots: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        dots()
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = AppText, fontSize = 16.sp)
            if (detail.isNotBlank()) Text(detail, color = AppMuted, fontSize = 12.sp)
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = AppAccent)
    }
}
