package com.ncy.notechaty.ui.chat

import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
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
import androidx.compose.material.icons.filled.FileDownloadOff
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
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
import com.ncy.notechaty.data.sync.media.MediaHash
import com.ncy.notechaty.data.sync.media.MediaTransferState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.ncy.notechaty.ui.theme.NcySheetActionRow
import com.ncy.notechaty.ui.theme.withReadableWhiteText
import com.ncy.notechaty.ui.imageviewer.ImageViewerDialog
import com.ncy.notechaty.ui.voicemessage.VoiceMessageBubble
import com.ncy.notechaty.ui.theme.NcyTheme
import com.ncy.notechaty.ui.theme.NcyRowDivider
import com.ncy.notechaty.ui.theme.NcySheetShape
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.drawBehind
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileDownloadOff
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.filled.Check
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
    onToggleTextChecklistBlock: (Int) -> Unit = {},
    automaticFileDownloadLimitBytes: Long = 100L * 1024 * 1024,
    onDownloadFileAnyway: (FileAttachment) -> Unit = {},
    /** Live progress for a blob hash, or null when nothing is in flight. */
    transferProgress: (String) -> MediaTransferState.Progress? = { null },
    /** Why a transfer stopped, or null. Distinguishes dead from merely slow. */
    transferFailure: (String) -> String? = { null },
    onCancelDownload: (String) -> Unit = {},
    /** Drop this device's copy while keeping the message: (hash, mediaType). */
    onRemoveDownload: (String, String) -> Unit = { _, _ -> },
    /** Fetch a blob this device does not hold: (hash, mediaType). */
    onRequestMedia: (String, String) -> Unit = { _, _ -> },
    /** Bumped when media presence changes, so disk-backed state re-evaluates. */
    mediaVersion: Int = 0,
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
    isRemote: Boolean = false,
    /** Name of the installation that wrote a note from another device. */
    remoteDeviceName: String? = null,
    /** The chat's "Other devices on the left" option: set such notes apart with a rule. */
    remoteOnLeft: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Only offer removal for media this device actually holds. A message carries one
    // media kind, so the first hit is the answer.
    // Every blob this message points at, with where it would live on disk. Images
    // arrive as rich-text spans rather than `imageFiles`, so both are walked.
    val mediaRefs: List<Triple<String, String, File>> = remember(message, chatDir) {
        fun ref(r: String?, type: String, dir: String, ext: String): Triple<String, String, File>? {
            val hash = MediaHash.extractHash(r) ?: return null
            return Triple(hash, type, File(File(chatDir, dir), hash + "." + ext))
        }
        buildList {
            ref(message.voiceFile, "voice", "voice", "m4a")?.let(::add)
            message.imageFiles.forEach { img -> ref(img, "image", "images", "jpg")?.let(::add) }
            message.content.forEach { block ->
                block.spans.forEach { span ->
                    ref(span.inlineImage, "image", "images", "jpg")?.let(::add)
                }
            }
            message.fileAttachments.forEach { a -> ref(a.file, "file", "files", "bin")?.let(::add) }
        }.distinctBy { it.first }
    }
    // Only offer removal for media actually held here.
    val localMedia: Pair<String, String>? = remember(mediaRefs, mediaVersion) {
        mediaRefs.firstOrNull { it.third.isFile }?.let { it.first to it.second }
    }
    // Durable, unlike the in-memory transfer state: without it a removed file reads
    // as "Downloading…" again after a restart.
    val evictedMedia: Boolean = remember(mediaRefs, mediaVersion) {
        mediaRefs.any { !it.third.isFile && File(it.third.path + ".evicted").isFile }
    }
    // An image this message holds locally, if any — what save and share act on.
    val localImage: File? = remember(mediaRefs, mediaVersion) {
        mediaRefs.firstOrNull { it.second == "image" && it.third.isFile }?.third
    }
    val imageSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/jpeg"),
    ) { destination ->
        val source = localImage
        if (destination != null && source != null && source.isFile) {
            runCatching {
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: error("Cannot open destination")
            }.onSuccess {
                Toast.makeText(context, context.getString(R.string.image_saved), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.could_not_save_image), Toast.LENGTH_SHORT).show()
            }
        }
    }
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

    val c = NcyTheme.colors
    val contentColor = MaterialTheme.colorScheme.onSurface
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    // A note is a bubble: yours on the right on the accent tint; with the chat's
    // "Other devices on the left" on, notes from other devices sit left on plain paper.
    val mine = !(isRemote && remoteOnLeft)
    val bubbleShape = if (mine) RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp)
        else RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
    val maxBubble = (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * 0.84f).dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when {
                    isSelected -> Modifier.background(c.accentSoft.copy(alpha = 0.6f))
                    isReplyHighlight -> Modifier.background(c.accentSoft.copy(alpha = 0.45f))
                    isCurrentSearchResult -> Modifier.background(c.accentSoft.copy(alpha = 0.4f))
                    isSearchResult -> Modifier.background(c.accentSoft.copy(alpha = 0.18f))
                    else -> Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 3.dp),
        contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        if (swipeProgress > 0f) {
            Icon(
                Icons.AutoMirrored.Filled.Reply,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = swipeProgress),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .size(20.dp)
            )
        }
        Column(
            modifier = Modifier
                .widthIn(max = maxBubble)
                .graphicsLayer { translationX = dragOffset.value }
                .clip(bubbleShape)
                .background(if (mine) c.accentSoft else c.raised)
                .border(1.dp, if (mine) c.accentEdge else c.line, bubbleShape)
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
                )
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                if (isRemote) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
                        Icon(Icons.Outlined.Laptop, null, Modifier.size(13.dp), tint = subtleColor)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            remoteDeviceName ?: stringResource(R.string.chat_another_device),
                            style = NcyTheme.type.time,
                            color = subtleColor,
                        )
                    }
                }
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
                    MessageType.TEXT -> CompositionLocalProvider(
                        LocalMediaRequest provides onRequestMedia,
                        LocalMediaVersion provides mediaVersion,
                    ) { RichTextContent(
                        message.content,
                        chatDir,
                        contentColor,
                        onChecklistToggle = { blockIndex ->
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onToggleTextChecklistBlock(blockIndex)
                        }
                    ) }
                    MessageType.VOICE -> {
                        val voiceFile = message.voiceFile?.let { File(chatDir, it) }
                        if (voiceFile != null && !voiceFile.isFile) {
                            // The player would hand a missing path to MediaPlayer and
                            // take the app down with it.
                            val voiceHash = MediaHash.extractHash(message.voiceFile)
                            MediaStatusCard(
                                icon = Icons.Default.Mic,
                                title = stringResource(R.string.voice_message),
                                contentColor = contentColor,
                                progress = voiceHash?.let(transferProgress),
                                failure = voiceHash?.let(transferFailure)
                                    ?: if (evictedMedia) MediaTransferState.EVICTED else null,
                                idleText = stringResource(R.string.not_on_this_device),
                                onDownload = voiceHash?.let { h -> { onRequestMedia(h, "voice") } },
                                onCancel = voiceHash?.let { h -> { onCancelDownload(h) } },
                            )
                        } else if (voiceFile != null) {
                            // Sealed in a protected chat: played from an opened copy,
                            // which takes a moment the first time.
                            rememberReadable(voiceFile)?.let { playable ->
                                VoiceMessageBubble(
                                    voiceFile = playable,
                                    durationMs = message.voiceDurationMs ?: 0
                                )
                            }
                        }
                        // The transcript reads as a quote under the recording.
                        message.transcript?.takeIf { it.isNotBlank() }?.let { t ->
                            Text(
                                text = "“$t”",
                                style = NcyTheme.type.quote,
                                color = subtleColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    MessageType.IMAGE -> CompositionLocalProvider(
                        LocalMediaRequest provides onRequestMedia,
                        LocalMediaVersion provides mediaVersion,
                    ) { RichTextContent(message.content, chatDir, contentColor) }
                    MessageType.FILE -> {
                        val attachment = message.fileAttachments.firstOrNull()
                        if (attachment != null) {
                            FileAttachmentContent(
                                attachment,
                                chatDir,
                                contentColor,
                                automaticFileDownloadLimitBytes,
                                onDownloadFileAnyway,
                                transferProgress,
                                transferFailure,
                                onCancelDownload,
                                mediaVersion,
                            )
                        } else {
                            Text(stringResource(R.string.file), color = contentColor)
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
                message.reminderAt?.takeIf { it > System.currentTimeMillis() }?.let { at ->
                    ReminderChip(at, onBubble = mine, onClick = onSetReminder)
                }
                // Star, "edited" and the time, small in the bubble's corner.
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (message.isStarred) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = stringResource(R.string.favorite),
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (message.editedAt != null) {
                        Text(stringResource(R.string.edited), style = NcyTheme.type.time, color = subtleColor)
                    }
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAt)),
                        style = NcyTheme.type.time,
                        color = subtleColor,
                    )
                }
            }
        }
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = NcySheetShape,
        ) {
            val tap = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
            val copyable = message.type == MessageType.TEXT || message.type == MessageType.IMAGE || message.type == MessageType.CHECKLIST ||
                (message.type == MessageType.VOICE && !message.transcript.isNullOrBlank())
            val editable = message.type == MessageType.TEXT || message.type == MessageType.CHECKLIST
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                // The sheet covers the note it was opened from, so the note is shown
                // here, lifted, to say what these actions apply to.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(c.raised)
                        .border(1.dp, c.line, RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        (if (isRemote) (remoteDeviceName ?: stringResource(R.string.chat_another_device)) + " · " else "") +
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.createdAt)),
                        style = NcyTheme.type.time,
                        color = subtleColor,
                    )
                    Text(
                        replyPreviewText(message.plainTextPreview(), message.type),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Reactions on top, as one row of large round targets.
                Box(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 6.dp)) {
                    ReactionPickerStrip(
                        current = message.reactions,
                        onPick = { emoji ->
                            tap()
                            showMenu = false
                            onToggleReaction(emoji)
                        }
                    )
                }
                NcyRowDivider()
                NcySheetActionRow(Icons.AutoMirrored.Outlined.Reply, stringResource(R.string.reply)) {
                    tap()
                    showMenu = false
                    onReply()
                }
                if (copyable) {
                    NcySheetActionRow(
                        Icons.Outlined.ContentCopy,
                        stringResource(if (message.type == MessageType.VOICE) R.string.chat_copy_transcript else R.string.chat_copy_text),
                    ) {
                        tap()
                        showMenu = false
                        val plainText = when (message.type) {
                            MessageType.CHECKLIST -> message.checklistItems.joinToString("\n") { item ->
                                "${if (item.checked) "☑" else "☐"} ${item.text}"
                            }
                            MessageType.VOICE -> message.transcript.orEmpty()
                            else -> message.content.joinToString("\n") { block ->
                                block.spans.joinToString("") { it.text }
                            }
                        }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("message", plainText))
                        Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
                    }
                }
                if (editable) {
                    NcySheetActionRow(Icons.Outlined.Edit, stringResource(R.string.edit)) {
                        tap()
                        showMenu = false
                        onEdit()
                    }
                }
                NcySheetActionRow(
                    if (message.isStarred) Icons.Default.Star else Icons.Outlined.StarOutline,
                    if (message.isStarred) stringResource(R.string.chat_unstar) else stringResource(R.string.chat_star),
                    iconTint = if (message.isStarred) MaterialTheme.colorScheme.primary else null,
                ) {
                    tap()
                    showMenu = false
                    onToggleStar()
                }
                NcySheetActionRow(
                    Icons.Outlined.Alarm,
                    if (message.reminderAt != null) stringResource(R.string.edit_reminder) else stringResource(R.string.chat_remind_me),
                ) {
                    tap()
                    showMenu = false
                    onSetReminder()
                }
                localImage?.let { source ->
                    NcySheetActionRow(Icons.Outlined.Download, stringResource(R.string.save_image)) {
                        tap()
                        showMenu = false
                        // The blob is named by hash, which is no use to
                        // anyone looking for it later.
                        imageSaveLauncher.launch(
                            "Ncy-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                                .format(Date(source.lastModified())) + ".jpg",
                        )
                    }
                    NcySheetActionRow(Icons.Outlined.Share, stringResource(R.string.share_image)) {
                        tap()
                        showMenu = false
                        runCatching {
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                source,
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(intent, context.getString(R.string.share_image)),
                            )
                        }.onFailure {
                            Toast.makeText(
                                context,
                                context.getString(R.string.could_not_share_image),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
                localMedia?.let { (hash, mediaType) ->
                    // Not the bin: that icon belongs to Delete, and this keeps
                    // the message.
                    NcySheetActionRow(Icons.Outlined.FileDownloadOff, stringResource(R.string.remove_download)) {
                        tap()
                        showMenu = false
                        onRemoveDownload(hash, mediaType)
                    }
                }
                NcySheetActionRow(Icons.Outlined.CheckCircle, stringResource(R.string.chat_select_more)) {
                    tap()
                    showMenu = false
                    onEnterSelectionMode()
                }
                // Set apart from everything else, so it is not hit by a slip.
                Spacer(Modifier.height(4.dp))
                NcyRowDivider()
                Spacer(Modifier.height(4.dp))
                NcySheetActionRow(Icons.Outlined.Delete, stringResource(R.string.delete), destructive = true) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    showMenu = false
                    onDelete()
                }
            }
        }
    }
}

/** "Today 16:00" under a note with a reminder still to come; opens the reminder sheet. */
@Composable
private fun ReminderChip(at: Long, onBubble: Boolean = false, onClick: () -> Unit) {
    val c = NcyTheme.colors
    val now = Calendar.getInstance()
    val t = Calendar.getInstance().apply { timeInMillis = at }
    val sameDay = now.get(Calendar.YEAR) == t.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == t.get(Calendar.DAY_OF_YEAR)
    val label = if (sameDay) {
        stringResource(R.string.today) + " " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at))
    } else SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(at))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(999.dp))
            // On a tinted bubble the tint would vanish, so the chip is outlined there.
            .then(if (onBubble) Modifier.border(1.dp, c.accentEdge, RoundedCornerShape(999.dp)) else Modifier.background(c.accentSoft))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Icon(Icons.Outlined.NotificationsNone, null, Modifier.size(13.dp), tint = c.onAccentSoft)
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.onAccentSoft)
    }
}

/**
 * One line of text standing in for a quoted message. [preview] is a frozen snapshot
 * (see `Message.replyingTo`) and is empty for voice-without-transcript and image
 * messages, which carry no text — those fall back to a label naming the type.
 */
@Composable
internal fun replyPreviewText(preview: String?, type: MessageType?): String =
    preview?.takeIf { it.isNotBlank() }
        ?: when (type) {
            MessageType.VOICE -> stringResource(R.string.voice_message)
            MessageType.IMAGE -> stringResource(R.string.photo)
            MessageType.FILE -> stringResource(R.string.file)
            MessageType.CHECKLIST -> stringResource(R.string.checklist)
            else -> stringResource(R.string.message)
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
    val accent = MaterialTheme.colorScheme.primary
    Column(
        modifier = Modifier
            .padding(bottom = 5.dp)
            .then(if (targetExists) Modifier.clickable(onClick = onJump) else Modifier)
            .drawBehind { drawRect(accent, size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height)) }
            .padding(start = 10.dp, top = 1.dp, bottom = 1.dp)
    ) {
        message.replyToCreatedAt?.let { at ->
            Text(
                text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at)),
                style = NcyTheme.type.time,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FileAttachmentContent(
    attachment: FileAttachment,
    chatDir: File,
    contentColor: Color,
    automaticFileDownloadLimitBytes: Long,
    onDownloadAnyway: (FileAttachment) -> Unit,
    transferProgress: (String) -> MediaTransferState.Progress? = { null },
    transferFailure: (String) -> String? = { null },
    onCancelDownload: (String) -> Unit = {},
    mediaVersion: Int = 0,
) {
    val context = LocalContext.current
    val localFile = remember(chatDir, attachment.file) { File(chatDir, attachment.file) }
    val hash = remember(attachment.file) { MediaHash.extractHash(attachment.file) }
    var available by remember(localFile, mediaVersion) { mutableStateOf(localFile.isFile) }
    // The marker on disk survives a restart; the in-memory transfer state does not, so
    // without consulting it a removed file starts claiming it is downloading again.
    val evicted = remember(localFile, mediaVersion) {
        !localFile.isFile && File(localFile.path + ".evicted").isFile
    }
    var manuallyRequested by remember(localFile) { mutableStateOf(false) }
    val deferred = !available && !manuallyRequested &&
        attachment.sizeBytes?.let { it > automaticFileDownloadLimitBytes } == true
    LaunchedEffect(localFile) {
        while (!available) {
            kotlinx.coroutines.delay(2_000)
            available = localFile.isFile
        }
    }
    // Android works out where the extension starts from the MIME type, not the
    // filename:  asks MimeTypeMap what extension the type implies,
    // and only splits the name there. Handed "application/octet-stream" it cannot
    // derive ".apk", treats "app-release.apk" as one indivisible name, and on a
    // collision produces "app-release.apk (1)" — which the system will then refuse
    // to open. Deriving the type from the extension keeps the suffix in the right
    // place, and describes the file more honestly besides.
    val saveMimeType = remember(attachment.name, attachment.mimeType) {
        val declared = attachment.mimeType?.takeIf {
            it.isNotBlank() && it != "application/octet-stream"
        }
        val fromExtension = attachment.name
            .substringAfterLast('.', "")
            .lowercase(Locale.US)
            .takeIf { it.isNotEmpty() }
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
        declared ?: fromExtension ?: "application/octet-stream"
    }
    // In a protected chat the stored file is sealed; open and save hand out its
    // readable contents, never the ciphertext.
    val reader = LocalMediaReader.current
    val scope = rememberCoroutineScope()
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(saveMimeType)
    ) { destination ->
        if (destination != null && localFile.isFile) scope.launch {
            val readable = reader.readable(localFile)
            runCatching {
                checkNotNull(readable) { "Cannot open the attachment" }
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destination)?.use { output ->
                        readable.inputStream().use { input -> input.copyTo(output) }
                    } ?: error("Cannot open destination")
                }
            }.onSuccess {
                Toast.makeText(context, context.getString(R.string.file_saved), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(context, context.getString(R.string.could_not_save_file), Toast.LENGTH_SHORT).show()
            }
        }
    }
    val openFile: () -> Unit = {
        scope.launch {
            val readable = reader.readable(localFile)
            if (readable == null) {
                Toast.makeText(context, context.getString(R.string.could_not_open_this_file), Toast.LENGTH_SHORT).show()
                return@launch
            }
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                readable,
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, attachment.mimeType ?: "application/octet-stream")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching { context.startActivity(Intent.createChooser(intent, context.getString(R.string.open_file))) }
                .onFailure {
                    Toast.makeText(context, context.getString(R.string.no_app_can_open_this_file), Toast.LENGTH_SHORT).show()
                }
        }
        Unit
    }
    // Absent media reads the same wherever it appears. The rich card below is for a
    // file that is actually here (or deliberately deferred) — it offers Open and
    // Save, neither of which means anything without the bytes, and pairing it with a
    // second Download control was how one row ended up with two of them.
    if (!available && !deferred) {
        MediaStatusCard(
            icon = Icons.Default.AttachFile,
            title = attachment.name,
            contentColor = contentColor,
            progress = hash?.let(transferProgress),
            // The marker on disk outlives the process; the transfer map does not, so
            // a removed file must not revert to claiming it is downloading.
            failure = hash?.let(transferFailure)
                ?: if (evicted) MediaTransferState.EVICTED else null,
            // Not "Downloading…": nothing is in flight in this state, and since the
            // backfill cutoff leaves older media deliberately unfetched, it is now
            // the common one. A live transfer shows its progress instead.
            idleText = stringResource(R.string.not_on_this_device),
            detail = attachment.sizeBytes?.let { formatFileSize(it) },
            onDownload = hash?.let { {
                manuallyRequested = true
                onDownloadAnyway(attachment)
            } },
            onCancel = hash?.let { h -> { onCancelDownload(h) } },
        )
        return
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
                    if (deferred) {
                        add(
                            if (automaticFileDownloadLimitBytes == 0L) stringResource(R.string.automatic_downloads_are_set_to_always)
                            else stringResource(R.string.above_your_automatic_download_limit, formatFileSize(automaticFileDownloadLimitBytes)),
                        )
                    }
                }.joinToString(" · ")
                if (detail.isNotEmpty()) {
                    Text(detail, fontSize = 11.sp, color = contentColor.copy(alpha = 0.72f))
                }
            }
            if (deferred) {
                TextButton(
                    onClick = {
                        manuallyRequested = true
                        onDownloadAnyway(attachment)
                    },
                ) {
                    Text(stringResource(R.string.download_anyway))
                }
            } else {
                IconButton(
                    onClick = { saveLauncher.launch(attachment.name) },
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(Icons.Default.Download, contentDescription = stringResource(R.string.save_file))
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}

private fun checklistState(block: RichTextBlock): Boolean? {
    val text = block.spans.joinToString("") { it.text }
    return when {
        text.startsWith("[ ] ") -> false
        text.startsWith("[x] ") || text.startsWith("[X] ") -> true
        else -> null
    }
}

private fun withoutChecklistMarker(spans: List<RichTextSpan>): List<RichTextSpan> {
    var remaining = 4
    return spans.mapNotNull { span ->
        if (remaining == 0) return@mapNotNull span
        if (span.text.length <= remaining) {
            remaining -= span.text.length
            null
        } else {
            span.copy(text = span.text.drop(remaining)).also { remaining = 0 }
        }
    }
}

/**
 * Lets rich-text content several levels down ask for a blob it does not hold.
 *
 * Inline images sit inside spans inside blocks, so passing a callback down would
 * mean adding a parameter to every block type on the path for the benefit of one
 * leaf.
 */
val LocalMediaRequest = staticCompositionLocalOf<((String, String) -> Unit)?> { null }

/** Bumped when media presence changes, so cached disk lookups re-evaluate. */
val LocalMediaVersion = compositionLocalOf { 0 }

@Composable
fun RichTextContent(
    blocks: List<RichTextBlock>,
    chatDir: File,
    textColor: Color = Color.Unspecified,
    onChecklistToggle: ((Int) -> Unit)? = null
) {
    Column {
        blocks.forEachIndexed { blockIndex, block ->
            val checked = checklistState(block)
            if (checked != null) {
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = if (onChecklistToggle == null) null else {
                            { onChecklistToggle(blockIndex) }
                        },
                        modifier = Modifier.size(24.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = textColor,
                            checkmarkColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    RichTextSpans(
                        withoutChecklistMarker(block.spans),
                        chatDir,
                        textColor = textColor
                    )
                }
                return@forEachIndexed
            }
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
                    val mediaVersion = LocalMediaVersion.current
                    val imageFile = File(chatDir, span.inlineImage)
                    val present = remember(imageFile, mediaVersion) { imageFile.isFile }
                    if (present) {
                        // A sealed image is shown, and opened full-size, from its
                        // opened copy; the stored file is ciphertext.
                        val shown = rememberReadable(imageFile, present)
                        AsyncImage(
                            model = shown,
                            contentDescription = stringResource(R.string.image),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { if (shown != null) viewingImage = shown },
                            contentScale = ContentScale.FillWidth
                        )
                    } else {
                        MissingImagePlaceholder(
                            imageFile,
                            span.inlineImage,
                            if (textColor == Color.Unspecified) LocalContentColor.current else textColor,
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$doneCount / ${items.size}",
            style = NcyTheme.type.time,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .toggleable(value = item.checked, role = Role.Checkbox) { onToggleItem(item.id) }
            ) {
                NcyCheckBox(item.checked)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (item.checked) TextDecoration.LineThrough else null
                    ),
                    color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else contentColor
                )
            }
        }
    }
}

/** Square Notebook checkbox: a hairline box, or the accent with a tick. */
@Composable
internal fun NcyCheckBox(checked: Boolean) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(19.dp)
            .clip(shape)
            .then(
                if (checked) Modifier.background(MaterialTheme.colorScheme.primary)
                else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, shape)
            ),
    ) {
        if (checked) Icon(Icons.Default.Check, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimary)
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

        // Links keep the text colour and are told apart by the underline. A fixed
        // light blue read at under 3:1 on the accent bubble, and worse on pastel ones.
        // If span has an explicit link, render entire span as a link
        if (span.link != null) {
            pushStringAnnotation("URL", span.link)
            withStyle(baseStyle.copy(textDecoration = TextDecoration.Underline)) {
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
                withStyle(baseStyle.copy(textDecoration = TextDecoration.Underline)) {
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
                    contentDescription = if (expanded) stringResource(R.string.fewer_reactions) else stringResource(R.string.more_reactions)
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(3.dp)
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .then(if (active) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
            .clickable(onClick = onClick)
    ) {
        Text(text = emoji, fontSize = 21.sp)
    }
}

@Composable
private fun ReactionChipRow(
    reactions: List<String>,
    contentColor: Color,
    onToggleReaction: (String) -> Unit
) {
    val c = NcyTheme.colors
    Row(
        modifier = Modifier
            .padding(top = 6.dp)
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        reactions.forEach { emoji ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .heightIn(min = 30.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(c.raised)
                    .border(1.dp, c.lineStrong, RoundedCornerShape(999.dp))
                    .clickable { onToggleReaction(emoji) }
                    .padding(horizontal = 10.dp)
            ) {
                Text(text = emoji, fontSize = 14.sp)
            }
        }
    }
}

/**
 * Stands in for an inline image this device does not hold.
 *
 * Previously the render was guarded by an existence check with no else branch, so
 * a removed or not-yet-fetched image left a blank gap that read as a broken
 * message rather than as absent media.
 */
@Composable
private fun MissingImagePlaceholder(imageFile: File, reference: String, contentColor: Color) {
    val request = LocalMediaRequest.current
    val hash = remember(reference) { MediaHash.extractHash(reference) }
    val mediaVersion = LocalMediaVersion.current
    val evicted = remember(imageFile, mediaVersion) {
        File(imageFile.path + ".evicted").isFile
    }
    MediaStatusCard(
        icon = Icons.Default.Image,
        title = stringResource(R.string.image),
        contentColor = contentColor,
        progress = null,
        failure = if (evicted) MediaTransferState.EVICTED else null,
        idleText = stringResource(R.string.not_on_this_device),
        onDownload = if (hash != null && request != null) {
            { request(hash, "image") }
        } else null,
    )
}
