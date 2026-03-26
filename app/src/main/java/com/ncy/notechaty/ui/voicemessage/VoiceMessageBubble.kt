package com.ncy.notechaty.ui.voicemessage

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VoiceMessageBubble(
    voiceFile: File,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var currentPosition by remember { mutableStateOf(0L) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    // Timer to update progress
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                player?.let { mp ->
                    try {
                        if (mp.isPlaying) {
                            currentPosition = mp.currentPosition.toLong()
                            progress = mp.currentPosition.toFloat() / mp.duration.toFloat()
                        }
                    } catch (_: Exception) {}
                }
                delay(100)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                player?.release()
            } catch (_: Exception) {}
        }
    }

    Row(
        modifier = modifier.widthIn(min = 200.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Play / Pause button
        FilledTonalIconButton(
            onClick = {
                if (isPlaying) {
                    player?.pause()
                    isPlaying = false
                } else {
                    if (player == null) {
                        player = MediaPlayer().apply {
                            setDataSource(voiceFile.absolutePath)
                            prepare()
                            setOnCompletionListener {
                                isPlaying = false
                                progress = 0f
                                currentPosition = 0
                            }
                        }
                    }
                    player?.start()
                    isPlaying = true
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(20.dp)
            )
        }

        // Waveform + duration
        Column(modifier = Modifier.weight(1f)) {
            WaveformView(
                fileName = voiceFile.name,
                progress = progress,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isPlaying) formatDuration(currentPosition) else formatDuration(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}
