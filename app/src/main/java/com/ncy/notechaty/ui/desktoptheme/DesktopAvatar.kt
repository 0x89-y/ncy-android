package com.ncy.notechaty.ui.desktoptheme

/*
 * Part of the deletable "Desktop" reskin — see DesktopTheme.kt.
 * Deterministic per-name avatar colour, ported verbatim from ncy-desktop
 * (src/lib/format.ts → avatarColor): same 8-colour palette, same hash, so a
 * given chat shows the same colour on phone and desktop.
 */

import androidx.compose.ui.graphics.Color

private val AvatarPalette = listOf(
    Color(0xFF6750A4), Color(0xFF7D5260), Color(0xFF1E88E5), Color(0xFF43A047),
    Color(0xFFFB8C00), Color(0xFF8E24AA), Color(0xFF00897B), Color(0xFF5E35B1),
)

/** Matches desktop: h = (h * 31 + charCode) >>> 0, then palette[h % len]. */
fun ncyAvatarColor(seed: String): Color {
    var h = 0L
    for (ch in seed) {
        h = (h * 31 + ch.code) and 0xFFFFFFFFL
    }
    return AvatarPalette[(h % AvatarPalette.size).toInt()]
}
