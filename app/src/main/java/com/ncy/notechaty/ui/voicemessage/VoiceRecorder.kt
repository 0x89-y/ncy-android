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
import com.ncy.notechaty.ui.theme.NcyTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.outlined.Delete
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

    // Timer, and the level meter: one bar per tick from the recorder's peak amplitude.
    val levels = remember { mutableStateListOf<Float>() }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                recordingDurationMs = System.currentTimeMillis() - startTime
                val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                levels.add((amp / 20000f).coerceIn(0.06f, 1f))
                if (levels.size > 48) levels.removeAt(0)
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

    val discard = {
        if (isRecording) {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (_: Exception) {}
            recorder = null
            tempFile.delete()
        }
        onCancel()
    }
    val start = {
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
    val stopAndSave = {
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
    }

    val c = NcyTheme.colors
    val rec = MaterialTheme.colorScheme.error
    // Recording takes over the composer: time and level on top, then labelled controls.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        HorizontalDivider(thickness = 1.dp, color = c.line)
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) rec else MaterialTheme.colorScheme.outline)
                )
                Spacer(Modifier.width(10.dp))
                Text(formatDuration(recordingDurationMs), style = NcyTheme.type.time.copy(fontSize = 14.sp))
                Spacer(Modifier.weight(1f))
                if (!isRecording) {
                    Text(
                        stringResource(R.string.recorder_tap_to_start),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Level meter, newest on the right; empty slots wait as short stubs.
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 6.dp),
            ) {
                val slots = 48
                repeat(slots) { i ->
                    val level = levels.getOrNull(i - (slots - levels.size))
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(level ?: 0.08f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (level != null) rec else c.lineStrong)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth(),
            ) {
                RecorderControl(stringResource(R.string.recorder_discard)) {
                    OutlinedIconButton(
                        onClick = discard,
                        modifier = Modifier.size(52.dp),
                        border = BorderStroke(1.dp, c.lineStrong),
                    ) { Icon(Icons.Outlined.Delete, contentDescription = null) }
                }
                RecorderControl(stringResource(if (isRecording) R.string.recorder_save else R.string.recorder_record)) {
                    FilledIconButton(
                        onClick = { if (isRecording) stopAndSave() else start() },
                        modifier = Modifier.size(68.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isRecording) c.ink else rec,
                            contentColor = if (isRecording) c.onInk else MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                // Balances the row so the main control stays centred.
                Spacer(Modifier.width(80.dp))
            }
        }
    }
}

@Composable
private fun RecorderControl(label: String, button: @Composable () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        button()
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
