package com.ncy.notechaty.ui.desktoptheme

/*
 * Part of the deletable "Desktop" reskin — see DesktopTheme.kt.
 * The signature dotted-grid ground from ncy-desktop (app.css):
 *   background-image: radial-gradient(var(--grid-dot) 1px, transparent 1px);
 *   background-size: 22px 22px;
 * Applied as a Modifier so any screen can sit its (transparent) content on top.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

/**
 * Paints the desktop's `--bg` fill plus the 22px dotted grid behind whatever
 * content is drawn on top. Content surfaces should be opaque so the dots read
 * only in the gutters, exactly like the desktop.
 */
@Composable
fun Modifier.ncyDottedGrid(): Modifier {
    val dark = isSystemInDarkTheme()
    val bg = MaterialTheme.colorScheme.background
    // --grid-dot: rgba(0,0,0,.06) light / rgba(255,255,255,.04) dark.
    val dot = MaterialTheme.colorScheme.onSurface.copy(alpha = if (dark) 0.04f else 0.06f)
    return this
        .background(bg)
        .drawBehind {
            val spacing = 22.dp.toPx()
            val radius = 1.dp.toPx()
            var x = spacing / 2f
            while (x < size.width) {
                var y = spacing / 2f
                while (y < size.height) {
                    drawCircle(color = dot, radius = radius, center = Offset(x, y))
                    y += spacing
                }
                x += spacing
            }
        }
}
