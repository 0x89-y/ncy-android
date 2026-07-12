package com.ncy.notechaty.ui.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import com.ncy.notechaty.data.model.*
import kotlinx.coroutines.launch
import com.ncy.notechaty.ui.desktoptheme.NcySheetActionRow
import com.ncy.notechaty.ui.imageviewer.ImageViewerDialog
import com.ncy.notechaty.ui.voicemessage.VoiceMessageBubble
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MessageBubble(
    message: Message,
    chatDir: File,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStar: () -> Unit = {},
    onSetReminder: () -> Unit = {},
    onToggleReaction: (String) -> Unit = {},
    onToggleChecklistItem: (String) -> Unit = {},
    onReply: () -> Unit = {},
    onJumpToReplyTarget: () -> Unit = {},
    /** False when the quoted message has since been deleted — the quote stays, the jump doesn't. */
    replyTargetExists: Boolean = false,
    /** Briefly tinted after a jump landed on this message. */
    isReplyHighlight: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onEnterSelectionMode: () -> Unit = {},
    bubbleColor: androidx.compose.ui.graphics.Color? = null,
    useGradient: Boolean = false,
    isSearchResult: Boolean = false,
    isCurrentSearchResult: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current

    // Swipe-to-reply. Bubbles are right-aligned, so the natural pull is rightward,
    // away from the screen edge. Past the trigger distance the gesture arms (with a
    // haptic tick) and fires on release; the bubble always springs back.
    val density = LocalDensity.current
    val triggerPx = with(density) { 56.dp.toPx() }
    val maxDragPx = with(density) { 72.dp.toPx() }
    val dragOffset = remember { Animatable(0f) }
    val dragScope = rememberCoroutineScope()
    var armed by remember { mutableStateOf(false) }
    val swipeProgress = (dragOffset.value / triggerPx).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when {
                    isSelected -> Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                    isReplyHighlight -> Modifier.background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)
                    )
                    isCurrentSearchResult -> Modifier.background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    )
                    isSearchResult -> Modifier.background(
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f)
                    )
                    else -> Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        if (swipeProgress > 0f) {
            Icon(
                Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = swipeProgress),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(20.dp)
            )
        }
        // Desktop identity: every note is "yours", so the default bubble is the
        // accent (--bubble-mine) with white-ish text. A per-chat custom colour
        // still wins when set.
        val resolvedBubbleColor = bubbleColor ?: MaterialTheme.colorScheme.primary
        val contentColor = if (bubbleColor != null) {
            if (resolvedBubbleColor.luminance() > 0.4f) Color.Black else Color.White
        } else {
            MaterialTheme.colorScheme.onPrimary
        }
        val subtleColor = contentColor.copy(alpha = 0.7f)

        // Desktop tail geometry: 18 18 4 18 (rounded except bottom-right).
        val bubbleShape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
        Surface(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .graphicsLayer { translationX = dragOffset.value }
                .pointerInput(message.id, isSelectionMode) {
                    if (isSelectionMode) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffset.value >= triggerPx) onReply()
                            armed = false
                            dragScope.launch { dragOffset.animateTo(0f) }
                        },
                        onDragCancel = {
                            armed = false
                            dragScope.launch { dragOffset.animateTo(0f) }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        val next = (dragOffset.value + dragAmount).coerceIn(0f, maxDragPx)
                        if (!armed && next >= triggerPx) {
                            armed = true
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        } else if (armed && next < triggerPx) {
                            armed = false
                        }
                        dragScope.launch { dragOffset.snapTo(next) }
                    }
                }
                .combinedClickable(
                    onClick = {
                        if (isSelectionMode) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onToggleSelect()
                        }
                    },
                    onLongClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        if (isSelectionMode) onToggleSelect()
                        else showMenu = true
                    }
                ),
            shape = bubbleShape,
            color = if (useGradient) Color.Transparent else resolvedBubbleColor,
            contentColor = contentColor,
            // Flat like the desktop — depth comes from the accent fill, not shadow.
            tonalElevation = 0.dp
        ) {
            val bgModifier = if (useGradient) {
                val gradientTop = lerp(resolvedBubbleColor, Color.White, 0.3f)
                Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(gradientTop, resolvedBubbleColor)
                    ),
                    shape = bubbleShape
                )
            } else Modifier
            Column(modifier = bgModifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (message.replyToId != null) {
                    ReplyQuote(
                        message = message,
                        contentColor = contentColor,
                        targetExists = replyTargetExists,
                        onJump = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onJumpToReplyTarget()
                        }
                    )
                }
                when (message.type) {
                    MessageType.TEXT -> RichTextContent(message.content, chatDir, contentColor)
                    MessageType.VOICE -> {
                        val voiceFile = message.voiceFile?.let { File(chatDir, it) }
                        if (voiceFile != null) {
                            VoiceMessageBubble(
                                voiceFile = voiceFile,
                                durationMs = message.voiceDurationMs ?: 0
                            )
                        }
                        // Transcript rides on the Message and is populated by
                        // whichever paired device ran whisper on the audio
                        // (currently desktop-only). Rendered inline under the
                        // audio player in muted italic text.
                        message.transcript?.takeIf { it.isNotBlank() }?.let { t ->
                            Text(
                                text = t,
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                color = subtleColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    MessageType.IMAGE -> RichTextContent(message.content, chatDir, contentColor)
                    MessageType.FILE -> {
                        val attachment = message.fileAttachments.firstOrNull()
                        if (attachment != null) {
                            FileAttachmentContent(attachment, chatDir, contentColor)
                        } else {
                            Text("File", color = contentColor)
                        }
                    }
                    MessageType.CHECKLIST -> {
                        ChecklistContent(
                            items = message.checklistItems,
                            contentColor = contentColor,
                            onToggleItem = { itemId ->
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                onToggleChecklistItem(itemId)
                            }
                        )
                    }
                }

                if (message.reactions.isNotEmpty()) {
                    ReactionChipRow(
                        reactions = message.reactions,
                        contentColor = contentColor,
                        onToggleReaction = { emoji ->
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onToggleReaction(emoji)
                        }
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (message.isStarred) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Starred",
                            modifier = Modifier.size(12.dp),
                            tint = subtleColor
                        )
                    }
                    if (message.editedAt != null) {
                        Text(
                            text = "edited",
                            style = MaterialTheme.typography.labelSmall,
                            color = subtleColor,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(message.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = subtleColor,
                        fontSize = 10.sp
                    )
                }
            }

            if (showMenu) {
                ModalBottomSheet(
                    onDismissRequest = { showMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        // Quick reactions across the top.
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            ReactionPickerStrip(
                                current = message.reactions,
                                onPick = { emoji ->
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    showMenu = false
                                    onToggleReaction(emoji)
                                }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        NcySheetActionRow(Icons.AutoMirrored.Filled.Reply, "Reply") {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showMenu = false
                            onReply()
                        }
                        if (message.type == MessageType.TEXT || message.type == MessageType.IMAGE || message.type == MessageType.CHECKLIST) {
                            NcySheetActionRow(Icons.Default.ContentCopy, "Copy") {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showMenu = false
                                val plainText = if (message.type == MessageType.CHECKLIST) {
                                    message.checklistItems.joinToString("\n") { item ->
                                        "${if (item.checked) "☑" else "☐"} ${item.text}"
                                    }
                                } else {
                                    message.content.joinToString("\n") { block ->
                                        block.spans.joinToString("") { it.text }
                                    }
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("message", plainText))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        }
                        NcySheetActionRow(
                            if (message.isStarred) Icons.Default.Star else Icons.Outlined.StarOutline,
                            if (message.isStarred) "Unstar" else "Star"
                        ) {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showMenu = false
                            onToggleStar()
                        }
                        NcySheetActionRow(Icons.Default.Alarm, "Set reminder") {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showMenu = false
                            onSetReminder()
                        }
                        NcySheetActionRow(Icons.Default.CheckBox, "Select") {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showMenu = false
                            onEnterSelectionMode()
                        }
                        if (message.type == MessageType.TEXT || message.type == MessageType.CHECKLIST) {
                            NcySheetActionRow(Icons.Default.Edit, "Edit") {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                showMenu = false
                                onEdit()
                            }
                        }
                        NcySheetActionRow(Icons.Default.Delete, "Delete", destructive = true) {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            showMenu = false
                            onDelete()
                        }
                    }
                }
            }
        }
    }
}

/**
 * One line of text standing in for a quoted message. [preview] is a frozen snapshot
 * (see `Message.replyingTo`) and is empty for voice-without-transcript and image
 * messages, which carry no text — those fall back to a label naming the type.
 */
internal fun replyPreviewText(preview: String?, type: MessageType?): String =
    preview?.takeIf { it.isNotBlank() }
        ?: when (type) {
            MessageType.VOICE -> "Voice message"
            MessageType.IMAGE -> "Photo"
            MessageType.FILE -> "File"
            MessageType.CHECKLIST -> "Checklist"
            else -> "Message"
        }

/**
 * The quoted message shown at the top of a reply bubble, rendered from the snapshot
 * frozen on [message] rather than from the live original. Tapping jumps to the
 * original when it still exists; a quote whose target was deleted stays readable but
 * is inert, since a tombstoned message is gone from the timeline entirely.
 */
@Composable
private fun ReplyQuote(
    message: Message,
    contentColor: Color,
    targetExists: Boolean,
    onJump: () -> Unit
) {
    val text = replyPreviewText(message.replyToPreview, message.replyToType)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = contentColor.copy(alpha = 0.14f),
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .then(if (targetExists) Modifier.clickable(onClick = onJump) else Modifier)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(contentColor.copy(alpha = 0.8f))
            )
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                message.replyToCreatedAt?.let { at ->
                    Text(
                        text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at)),
                        fontSize = 10.sp,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
                Text(
                    text = text,
                    fontSize = 13.sp,
                    color = contentColor.copy(alpha = 0.9f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FileAttachmentContent(
    attachment: FileAttachment,
    chatDir: File,
    contentColor: Color,
) {
    val context = LocalContext.current
    val localFile = remember(chatDir, attachment.file) { File(chatDir, attachment.file) }
    var available by remember(localFile) { mutableStateOf(localFile.isFile) }
    LaunchedEffect(localFile) {
        while (!available) {
            kotlinx.coroutines.delay(2_000)
            available = localFile.isFile
        }
    }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(attachment.mimeType ?: "application/octet-stream")
    ) { destination ->
        if (destination != null && localFile.isFile) {
            runCatching {
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    localFile.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Cannot open destination")
            }.onSuccess {
                Toast.makeText(context, "File saved", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, "Could not save file", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val openFile: () -> Unit = {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            localFile,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType ?: "application/octet-stream")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Open file")) }
            .onFailure {
                Toast.makeText(context, "No app can open this file", Toast.LENGTH_SHORT).show()
            }
        Unit
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = contentColor.copy(alpha = 0.14f),
        contentColor = contentColor,
        modifier = Modifier,
    ) {
        Row(
            modifier = Modifier
                .clickable(enabled = available, onClick = openFile)
                .padding(start = 10.dp, top = 8.dp, bottom = 8.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (available) Icons.Default.AttachFile else Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    attachment.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                )
                val detail = buildList {
                    attachment.sizeBytes?.let { add(formatFileSize(it)) }
                    if (!available) add("Downloading…")
                }.joinToString(" · ")
                if (detail.isNotEmpty()) {
                    Text(detail, fontSize = 11.sp, color = contentColor.copy(alpha = 0.72f))
                }
            }
            IconButton(
                enabled = available,
                onClick = { saveLauncher.launch(attachment.name) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(Icons.Default.Download, contentDescription = "Save file")
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

@Composable
fun RichTextContent(blocks: List<RichTextBlock>, chatDir: File, textColor: Color = Color.Unspecified) {
    Column {
        for (block in blocks) {
            when (block.type) {
                BlockType.HEADING1 -> RichTextSpans(block.spans, chatDir, fontSize = 24, fontWeight = FontWeight.Bold, textColor = textColor)
                BlockType.HEADING2 -> RichTextSpans(block.spans, chatDir, fontSize = 20, fontWeight = FontWeight.Bold, textColor = textColor)
                BlockType.HEADING3 -> RichTextSpans(block.spans, chatDir, fontSize = 18, fontWeight = FontWeight.SemiBold, textColor = textColor)
                BlockType.BULLET_LIST -> {
                    Row {
                        Text("  \u2022 ", style = MaterialTheme.typography.bodyLarge, color = textColor)
                        RichTextSpans(block.spans, chatDir, textColor = textColor)
                    }
                }
                BlockType.CODE_BLOCK -> {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = block.spans.joinToString("") { it.text },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                BlockType.PARAGRAPH -> RichTextSpans(block.spans, chatDir, textColor = textColor)
            }
        }
    }
}

@Composable
private fun RichTextSpans(
    spans: List<RichTextSpan>,
    chatDir: File,
    fontSize: Int = 16,
    fontWeight: FontWeight = FontWeight.Normal,
    textColor: Color = Color.Unspecified
) {
    val context = LocalContext.current
    var viewingImage by remember { mutableStateOf<File?>(null) }

    // Image viewer dialog
    viewingImage?.let { file ->
        ImageViewerDialog(imageFile = file, onDismiss = { viewingImage = null })
    }

    // Handle inline images
    val hasImages = spans.any { it.inlineImage != null }

    if (hasImages) {
        Column {
            for (span in spans) {
                if (span.inlineImage != null) {
                    val imageFile = File(chatDir, span.inlineImage)
                    if (imageFile.exists()) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = "Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewingImage = imageFile },
                            contentScale = ContentScale.FillWidth
                        )
                    }
                } else if (span.text.isNotEmpty()) {
                    LinkedText(
                        annotatedString = buildAnnotatedSpan(listOf(span), textColor),
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        textColor = textColor,
                        context = context
                    )
                }
            }
        }
    } else {
        val textSpans = spans.filter { it.text.isNotEmpty() }
        if (textSpans.isNotEmpty()) {
            LinkedText(
                annotatedString = buildAnnotatedSpan(textSpans, textColor),
                fontSize = fontSize,
                fontWeight = fontWeight,
                textColor = textColor,
                context = context
            )
        }
    }
}

@Composable
private fun LinkedText(
    annotatedString: androidx.compose.ui.text.AnnotatedString,
    fontSize: Int,
    fontWeight: FontWeight,
    textColor: Color = Color.Unspecified,
    context: Context
) {
    val hasLinks = annotatedString.getStringAnnotations("URL", 0, annotatedString.length).isNotEmpty()
    if (hasLinks) {
        ClickableText(
            text = annotatedString,
            style = androidx.compose.ui.text.TextStyle(
                fontSize = fontSize.sp,
                fontWeight = fontWeight,
                color = textColor
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()?.let { annotation ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                        context.startActivity(intent)
                    }
            }
        )
    } else {
        Text(
            text = annotatedString,
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            color = textColor
        )
    }
}

private val urlPattern = Regex(
    "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
    RegexOption.IGNORE_CASE
)

@Composable
private fun ChecklistContent(
    items: List<ChecklistItem>,
    contentColor: Color,
    onToggleItem: (String) -> Unit
) {
    val doneCount = items.count { it.checked }
    val total = items.size

    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        // Progress header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Icon(
                Icons.Default.Checklist,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$doneCount / $total",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
        if (total > 0) {
            LinearProgressIndicator(
                progress = { doneCount.toFloat() / total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .padding(bottom = 4.dp),
                color = contentColor.copy(alpha = 0.7f),
                trackColor = contentColor.copy(alpha = 0.15f)
            )
        }

        // Checklist items
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onToggleItem(item.id) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = { onToggleItem(item.id) },
                    modifier = Modifier.size(24.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = contentColor.copy(alpha = 0.7f),
                        uncheckedColor = contentColor.copy(alpha = 0.5f),
                        checkmarkColor = if (contentColor.luminance() > 0.5f) Color.Black else Color.White
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        textDecoration = if (item.checked) TextDecoration.LineThrough else null
                    ),
                    color = if (item.checked) contentColor.copy(alpha = 0.5f) else contentColor
                )
            }
        }
    }
}

private fun buildAnnotatedSpan(spans: List<RichTextSpan>, defaultColor: Color = Color.Unspecified) = buildAnnotatedString {
    for (span in spans) {
        val baseStyle = SpanStyle(
            fontWeight = if (span.bold) FontWeight.Bold else null,
            fontStyle = if (span.italic) FontStyle.Italic else null,
            textDecoration = buildList {
                if (span.underline) add(TextDecoration.Underline)
                if (span.strikethrough) add(TextDecoration.LineThrough)
            }.let {
                if (it.isEmpty()) null else TextDecoration.combine(it)
            },
            color = span.color?.let {
                try {
                    Color(android.graphics.Color.parseColor(it))
                } catch (e: Exception) {
                    defaultColor
                }
            } ?: defaultColor
        )

        // If span has an explicit link, render entire span as a link
        if (span.link != null) {
            pushStringAnnotation("URL", span.link)
            withStyle(baseStyle.copy(
                color = androidx.compose.ui.graphics.Color(0xFF64B5F6),
                textDecoration = TextDecoration.Underline
            )) {
                append(span.text)
            }
            pop()
        } else {
            // Auto-detect URLs in the text
            val text = span.text
            var lastEnd = 0
            for (match in urlPattern.findAll(text)) {
                // Text before the URL
                if (match.range.first > lastEnd) {
                    withStyle(baseStyle) {
                        append(text.substring(lastEnd, match.range.first))
                    }
                }
                // The URL itself
                pushStringAnnotation("URL", match.value)
                withStyle(baseStyle.copy(
                    color = androidx.compose.ui.graphics.Color(0xFF64B5F6),
                    textDecoration = TextDecoration.Underline
                )) {
                    append(match.value)
                }
                pop()
                lastEnd = match.range.last + 1
            }
            // Remaining text after last URL
            if (lastEnd < text.length) {
                withStyle(baseStyle) {
                    append(text.substring(lastEnd))
                }
            }
        }
    }
}

// Curated quick reactions (shown first) + an extended set behind a "more" toggle.
private val QUICK_REACTIONS = listOf("👍", "❤️", "😂", "🎉", "✅", "🔥")
private val EXTRA_REACTIONS = listOf(
    "🙏", "👀", "🤔", "😮", "😢", "😡",
    "💯", "⭐", "🚀", "💡", "👏", "🤝",
    "😍", "🥳", "🤯", "😅", "🙌", "✨"
)

@Composable
private fun ReactionPickerStrip(
    current: List<String>,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QUICK_REACTIONS.forEach { emoji ->
                ReactionPickerButton(
                    emoji = emoji,
                    active = current.contains(emoji),
                    onClick = { onPick(emoji) }
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            IconButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Fewer reactions" else "More reactions"
                )
            }
        }
        if (expanded) {
            // Two-row grid of extras; 9 per row keeps it comfortable on a phone dialog.
            EXTRA_REACTIONS.chunked(9).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { emoji ->
                        ReactionPickerButton(
                            emoji = emoji,
                            active = current.contains(emoji),
                            onClick = { onPick(emoji) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReactionPickerButton(
    emoji: String,
    active: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = emoji,
            fontSize = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ReactionChipRow(
    reactions: List<String>,
    contentColor: Color,
    onToggleReaction: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        reactions.forEach { emoji ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = contentColor.copy(alpha = 0.12f),
                modifier = Modifier.clickable { onToggleReaction(emoji) }
            ) {
                Text(
                    text = emoji,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
