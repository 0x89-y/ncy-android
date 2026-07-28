package com.ncy.notechaty.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ncy.notechaty.data.sync.media.MediaTransferState

/**
 * One presentation for media this device does not hold, whatever its type.
 *
 * Images, voice messages and file attachments each grew their own version of this
 * state: a grey card, a bare line of text with an oversized button, and a file row
 * carrying two competing download affordances. Same meaning, three looks. This is
 * the single one they all use.
 *
 * The container tints from [contentColor] rather than the theme surface, because
 * these render inside coloured bubbles — a fixed `surfaceVariant` reads as a dark
 * box dropped into a purple bubble.
 */
@Composable
fun MediaStatusCard(
    icon: ImageVector,
    title: String,
    contentColor: Color,
    progress: MediaTransferState.Progress?,
    failure: String?,
    /** Shown when nothing is happening and nothing has failed. */
    idleText: String,
    /** Extra detail under the title, e.g. a file size. */
    detail: String? = null,
    onDownload: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val muted = contentColor.copy(alpha = 0.72f)
    val transferring = progress != null
    // A cancel or a deliberate removal is a choice, not a fault; only a genuine
    // failure earns the error colour.
    val deliberate =
        failure == MediaTransferState.CANCELLED || failure == MediaTransferState.EVICTED
    val status = when {
        failure == MediaTransferState.CANCELLED -> "Download cancelled"
        failure == MediaTransferState.EVICTED -> "Removed from this device"
        failure != null -> "Transfer failed · $failure"
        progress != null -> {
            val fraction = progressFraction(progress)
            "${(fraction * 100).toInt()}% · " +
                "${formatTransferSize(progress.received)} of ${formatTransferSize(progress.total)}"
        }
        else -> idleText
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(contentColor.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 13.sp,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (detail != null && !transferring && failure == null) "$detail · $status" else status,
                fontSize = 11.sp,
                color = if (failure != null && !deliberate) {
                    MaterialTheme.colorScheme.error
                } else {
                    muted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (transferring) {
                LinearProgressIndicator(
                    progress = { progressFraction(progress) },
                    color = contentColor,
                    trackColor = contentColor.copy(alpha = 0.24f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                )
            }
        }
        when {
            transferring && onCancel != null -> {
                IconButton(onClick = onCancel, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Stop downloading",
                        modifier = Modifier.size(16.dp),
                        tint = muted,
                    )
                }
            }
            !transferring && onDownload != null -> {
                TextButton(
                    onClick = onDownload,
                    colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                ) { Text("Download", fontSize = 13.sp) }
            }
        }
    }
}

private fun progressFraction(progress: MediaTransferState.Progress?): Float {
    val p = progress ?: return 0f
    return if (p.total > 0) (p.received.toFloat() / p.total).coerceIn(0f, 1f) else 0f
}

internal fun formatTransferSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
