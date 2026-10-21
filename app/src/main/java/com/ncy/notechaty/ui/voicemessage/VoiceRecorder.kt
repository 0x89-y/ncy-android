package com.ncy.notechaty.ui.voicemessage

import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun VoiceRecorder(
    onRecorded: (File, Long) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingDurationMs by remember { mutableStateOf(0L) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val tempFile = remember {
        File(context.cacheDir, "temp_voice_${System.currentTimeMillis()}.m4a")
    }

    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var startTime by remember { mutableStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) onCancel()
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                recordingDurationMs = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {}
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button
            IconButton(onClick = {
                if (isRecording) {
                    try {
                        recorder?.stop()
                        recorder?.release()
                    } catch (_: Exception) {}
                    recorder = null
                    tempFile.delete()
                }
                onCancel()
            }) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.error)
            }

            // Duration display
            Text(
                text = formatDuration(recordingDurationMs),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            // Recording indicator
            if (isRecording) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.error
                ) {}
            }

            // Record / Stop / Send button
            if (!isRecording) {
                FilledIconButton(
                    onClick = {
                        if (hasPermission) {
                            try {
                                val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    MediaRecorder(context)
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaRecorder()
                                }
                                mr.apply {
                                    setAudioSource(MediaRecorder.AudioSource.MIC)
                                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                    setAudioEncodingBitRate(64000)
                                    setAudioSamplingRate(44100)
                                    setAudioChannels(1)
                                    setOutputFile(tempFile.absolutePath)
                                    prepare()
                                    start()
                                }
                                recorder = mr
                                startTime = System.currentTimeMillis()
                                isRecording = true
                            } catch (e: Exception) {
                                onCancel()
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.start_recording))
                }
            } else {
                FilledIconButton(
                    onClick = {
                        isRecording = false
                        val duration = System.currentTimeMillis() - startTime
                        try {
                            recorder?.stop()
                            recorder?.release()
                        } catch (_: Exception) {}
                        recorder = null

                        if (duration > 500 && tempFile.exists()) {
                            onRecorded(tempFile, duration)
                        } else {
                            tempFile.delete()
                            onCancel()
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.stop_recording))
                }
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
