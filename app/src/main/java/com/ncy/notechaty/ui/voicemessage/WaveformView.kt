package com.ncy.notechaty.ui.voicemessage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import kotlin.math.abs

@Composable
fun WaveformView(
    fileName: String,
    progress: Float,  // 0f to 1f
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
    modifier: Modifier = Modifier
) {
    val barCount = 32
    // Generate deterministic bar heights from filename hash
    val barHeights = remember(fileName) {
        val hash = fileName.hashCode().toLong()
        List(barCount) { i ->
            val seed = abs((hash * 31 + i * 17) % 100)
            0.2f + (seed / 100f) * 0.8f  // range 0.2 to 1.0
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val barWidth = size.width / (barCount * 2f - 1f)
        val maxBarHeight = size.height

        barHeights.forEachIndexed { index, heightFraction ->
            val barHeight = maxBarHeight * heightFraction
            val x = index * barWidth * 2f
            val y = (maxBarHeight - barHeight) / 2f
            val barProgress = index.toFloat() / barCount.toFloat()

            drawRoundRect(
                color = if (barProgress <= progress) activeColor else inactiveColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
