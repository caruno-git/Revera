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
import com.music.spotui.data.preferences.WallpaperPref
import com.music.spotui.extensions.ExtensionLook
import com.music.spotui.ui.components.WallpaperDimSlider
import com.music.spotui.ui.components.WallpaperLinkField
import com.music.spotui.ui.components.rememberWallpaperPicker
import com.music.spotui.ui.components.wallpaperHiddenNote
import com.music.spotui.ui.immersive.Immersive

/**
 * Wallpaper in the Immersive settings: its own page in the Display category, in the same grouped
 * cards as the others (the Classic look has its own, ui/screens/WallpaperSettings.kt). A
 * picture or a video behind every page; the Immersive pages keep their glass bars over it.
 */
@Composable
internal fun WallpaperPage(back: () -> Unit) {
    val ctx = LocalContext.current
    val pick = rememberWallpaperPicker()
    val chosen = WallpaperPref.chosen
    val hidden = wallpaperHiddenNote()
    val packs = ExtensionLook.wallpapers
    ImmersivePage(title = stringResource(R.string.wallpaper_title), onBack = back) {
        immersiveGroup("source", headerRes = R.string.wallpaper_section_source, footer = hidden) {
            row {
                ImmersiveGroupRow(
                    stringResource(R.string.wallpaper_phone),
                    detail = stringResource(R.string.wallpaper_phone_detail),
                    chevron = true, onClick = pick,
                )
            }
            row {
                Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    WallpaperLinkField(
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
        immersiveGroup("packs", headerRes = R.string.wallpaper_section_packs, footerRes = if (chosen == null) R.string.wallpaper_note else null) {
            if (packs.isEmpty()) row { ImmersiveGroupRow(stringResource(R.string.wallpaper_packs_none)) }
            for (w in packs) row {
                ImmersiveChoiceRow(
                    w.title,
                    selected = chosen != null && chosen.from == w.extensionId && chosen.source == w.url,
                    detail = stringResource(if (w.video) R.string.wallpaper_video_badge else R.string.wallpaper_picture_badge),
                    onClick = { WallpaperPref.setPack(ctx, w) },
                )
            }
        }
        if (chosen != null) immersiveGroup("dim", headerRes = R.string.wallpaper_section_dim) {
            row {
                WallpaperDimSlider(Immersive.Accent, Immersive.Text, Immersive.TextSecondary, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            }
        }
        if (chosen != null) immersiveGroup("remove", footerRes = R.string.wallpaper_note) {
            row { ImmersiveGroupRow(stringResource(R.string.wallpaper_remove), destructive = true, onClick = { WallpaperPref.clear(ctx) }) }
        }
    }
}
