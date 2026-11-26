package com.music.spotui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.music.spotui.data.preferences.WallpaperPref
import com.music.spotui.extensions.ExtensionLook
import com.music.spotui.ui.components.WallpaperDimSlider
import com.music.spotui.ui.components.WallpaperLinkField
import com.music.spotui.ui.components.rememberWallpaperPicker
import com.music.spotui.ui.components.wallpaperHiddenNote
import com.music.spotui.ui.components.wallpaperValue
import com.music.spotui.ui.theme.AppAccent
import com.music.spotui.ui.theme.AppDestructive
import com.music.spotui.ui.theme.AppMuted
import com.music.spotui.ui.theme.AppText
import com.music.spotui.ui.theme.tone

/*
 * Wallpaper in the Classic settings: a row in the Appearance tab (like Transitions in Playback)
 * that opens a page of its own, built from the same rows, chips and section titles as the rest of
 * Settings. The Immersive look has its own page (ImmersiveWallpaperPage).
 */

/** The row in the Appearance tab: what is set, one line; the choices on their own page. */
@Composable
internal fun WallpaperLink(onOpen: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.wallpaper_title), color = AppText, fontSize = 16.sp)
            Text(stringResource(R.string.wallpaper_detail), color = AppMuted, fontSize = 12.sp)
        }
        Text(wallpaperValue(), color = AppAccent, fontSize = 14.sp, maxLines = 1, modifier = Modifier.padding(start = 12.dp).weight(0.6f, fill = false))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AppMuted)
    }
}

/** The page: where the wallpaper comes from, the dimming, and removing it. */
@Composable
internal fun WallpaperPage() {
    val ctx = LocalContext.current
    val pick = rememberWallpaperPicker()
    val chosen = WallpaperPref.chosen
    val hidden = wallpaperHiddenNote()
    val packs = ExtensionLook.wallpapers
    if (hidden != null) Text(hidden, color = AppAccent, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))

    Section(R.string.wallpaper_section_source)
    Column(Modifier.fillMaxWidth().clickable(onClick = pick).padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(stringResource(R.string.wallpaper_phone), color = AppText, fontSize = 16.sp)
        Text(stringResource(R.string.wallpaper_phone_detail), color = AppMuted, fontSize = 12.sp)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Text(stringResource(R.string.wallpaper_link_title), color = AppText, fontSize = 16.sp)
        Text(stringResource(R.string.wallpaper_link_detail), color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        WallpaperLinkField(text = AppText, muted = AppMuted, accent = AppAccent, border = tone(0xFF3E3E3E)) { label, onClick ->
            Text(
                label, color = AppText, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(tone(0xFF2A2A2A)).clickable(onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }

    Section(R.string.wallpaper_section_packs)
    if (packs.isEmpty()) {
        Text(stringResource(R.string.wallpaper_packs_none), color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
    }
    for (w in packs) {
        val selected = chosen != null && chosen.from == w.extensionId && chosen.source == w.url
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { WallpaperPref.setPack(ctx, w) }.padding(horizontal = 20.dp, vertical = 10.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(w.title, color = AppText, fontSize = 16.sp)
                Text(stringResource(if (w.video) R.string.wallpaper_video_badge else R.string.wallpaper_picture_badge), color = AppMuted, fontSize = 12.sp)
            }
            if (selected) Icon(Icons.Default.Check, contentDescription = null, tint = AppAccent)
        }
    }

    if (chosen != null) {
        Section(R.string.wallpaper_section_dim)
        WallpaperDimSlider(AppAccent, AppText, AppMuted, Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.wallpaper_remove), color = AppDestructive, fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().clickable { WallpaperPref.clear(ctx) }.padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
    Text(stringResource(R.string.wallpaper_note), color = AppMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
}
