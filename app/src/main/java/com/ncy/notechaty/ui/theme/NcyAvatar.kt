package com.ncy.notechaty.ui.theme

/*
 * Chat and profile tiles. A chat's colour is a hue that is never used at full
 * strength: tiles take a paper tint of it with the glyph in a deeper shade, so
 * a list of chats reads as one calm page rather than a row of loud squares.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncy.notechaty.data.model.Chat

/** The rounded-square tile used for chats, profiles and settings glyphs. */
val NcyTileShape = RoundedCornerShape(percent = 31)

/** Kept as the name existing screens use for image avatars. */
val NcyAvatarShape = NcyTileShape

/** Chat hues: earthy enough to sit on paper, distinct enough to tell apart. */
val NcyChatHues = listOf(
    Color(0xFFB4471F), // terracotta
    Color(0xFFC69214), // ochre
    Color(0xFF2F855A), // green
    Color(0xFF0F766E), // teal
    Color(0xFF2B6CB0), // blue
    Color(0xFF805AD5), // violet
    Color(0xFFB83280), // plum
    Color(0xFF685F54), // stone
)

/**
 * A stable hue for [seed]. Same hash as the desktop (h = h * 31 + char, unsigned),
 * so a chat keeps one colour per name across devices.
 */
fun ncyAvatarColor(seed: String): Color {
    var h = 0L
    for (ch in seed) h = (h * 31 + ch.code) and 0xFFFFFFFFL
    return NcyChatHues[(h % NcyChatHues.size).toInt()]
}

/** A chat's hue: the colour picked for it, or one derived from its name. */
fun ncyChatHue(chat: Chat?, fallbackName: String = ""): Color {
    chat?.bubbleColor?.let { hex ->
        runCatching { return Color(android.graphics.Color.parseColor(hex)) }
    }
    return ncyAvatarColor(chat?.name ?: fallbackName)
}

/** Tile fill for [hue] on the current paper. */
@Composable
fun ncyTint(hue: Color): Color {
    val c = NcyTheme.colors
    return mix(hue, if (c.isDark) Color(0xFF1D1A15) else Color(0xFFFBF9F5), if (c.isDark) 0.24f else 0.17f)
}

/** Glyph colour on a [ncyTint] of [hue]. */
@Composable
fun ncyOnTint(hue: Color): Color {
    val text = MaterialTheme.colorScheme.onSurface
    return if (NcyTheme.colors.isDark) mix(hue, text, 0.55f) else mix(hue, text, 0.72f)
}

/** A tinted tile with any content (an icon, an initial). */
@Composable
fun NcyTile(
    hue: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.size(size).clip(NcyTileShape).background(ncyTint(hue)),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides ncyOnTint(hue)) { content() }
    }
}

/** A tile in ink, for locked things. */
@Composable
fun NcyInkTile(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    content: @Composable () -> Unit,
) {
    val c = NcyTheme.colors
    Box(
        modifier = modifier.size(size).clip(NcyTileShape).background(c.ink),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides c.onInk) { content() }
    }
}

/** A tinted tile showing the first letter of [name] in the serif. */
@Composable
fun NcyInitialTile(
    name: String,
    hue: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
) {
    NcyTile(hue = hue, modifier = modifier, size = size) {
        Text(
            text = name.trim().take(1).uppercase().ifEmpty { "·" },
            style = TextStyle(fontFamily = NcySerif, fontSize = (size.value * 0.5f).sp),
            color = LocalContentColor.current,
        )
    }
}
