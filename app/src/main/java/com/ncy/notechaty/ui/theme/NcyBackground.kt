package com.ncy.notechaty.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/** The plain paper ground every screen sits on. */
@Composable
fun Modifier.ncyPaper(): Modifier = background(MaterialTheme.colorScheme.background)

/**
 * Paper with faint ruled lines, for the chat. Purely decorative: notes draw
 * their own paper behind their text, so the lines show only between them.
 */
@Composable
fun Modifier.ncyRuledPaper(): Modifier {
    val bg = MaterialTheme.colorScheme.background
    val rule = NcyTheme.colors.line.copy(alpha = 0.55f)
    return this
        .background(bg)
        .drawBehind {
            val step = 28.dp.toPx()
            val stroke = 1.dp.toPx()
            var y = step
            while (y < size.height) {
                drawLine(rule, Offset(0f, y), Offset(size.width, y), stroke)
                y += step
            }
        }
}
