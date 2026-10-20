package com.ncy.notechaty.ui.imageviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import coil.compose.AsyncImage
import java.io.File

@Composable
fun ImageViewerDialog(
    imageFile: File,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Blobs are stored under their SHA-256, which is no use as a filename the
    // user has to find again later.
    val suggestedName = remember(imageFile) {
        "Ncy-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
            .format(Date(imageFile.lastModified())) + ".jpg"
    }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg"),
    ) { destination ->
        if (destination != null && imageFile.isFile) {
            runCatching {
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    imageFile.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Cannot open destination")
            }.onSuccess {
                Toast.makeText(context, "Image saved", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Could not save image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hands the file to another app without copying it anywhere first. The
    // FileProvider authority already covers the media directory.
    val shareImage: () -> Unit = {
        runCatching {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile,
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share image"))
        }.onFailure {
            Toast.makeText(context, "Could not share image", Toast.LENGTH_SHORT).show()
        }
        Unit
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += panChange
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageFile,
                contentDescription = "Full screen image",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state = state),
                contentScale = ContentScale.Fit
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                IconButton(onClick = shareImage) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Share image",
                        tint = Color.White,
                    )
                }
                IconButton(onClick = { saveLauncher.launch(suggestedName) }) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Save image",
                        tint = Color.White,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
