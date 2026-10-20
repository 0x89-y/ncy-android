package com.ncy.notechaty.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.data.sync.transport.SyncLimits
import com.ncy.notechaty.ui.desktoptheme.NcyAlertDialog
import com.ncy.notechaty.ui.desktoptheme.NcyAvatarShape
import com.ncy.notechaty.ui.desktoptheme.NcySheetActionRow
import com.ncy.notechaty.ui.desktoptheme.ncyAvatarColor
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid
import androidx.compose.ui.text.style.TextOverflow
import com.ncy.notechaty.ui.richtext.RichTextInput
import com.ncy.notechaty.ui.voicemessage.VoiceRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    chatName: String = "",
    onBack: () -> Unit,
    onConfigClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val appSettings by viewModel.settings.collectAsState()
    val mediaProgress by viewModel.mediaProgress.collectAsState()
    val mediaFailures by viewModel.mediaFailures.collectAsState()
    val mediaVersion by viewModel.mediaVersion.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val view = LocalView.current
    val chatDir = viewModel.getChatDir()
    // A protected chat's attachments are sealed on disk (§11.8); bubbles show opened copies.
    val chatProtected = uiState.chat?.isProtected == true
    val mediaReader = remember(chatProtected) {
        if (chatProtected) MediaReader { viewModel.readableMedia(it) } else PlainMediaReader
    }

    if (uiState.unlockRequired) {
        ChatLockOverlay(
            chatName = if (uiState.chat?.hideName == true) "Protected chat" else (uiState.chat?.name ?: chatName),
            useBiometric = appSettings.protectionUseBiometric,
            onBack = onBack,
            onVerify = { viewModel.verifyPin(it) },
            onUnlocked = { viewModel.markUnlocked() }
        )
        return
    }

    // Reload chat data when returning from config screen
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadChat()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var reminderMessage by remember { mutableStateOf<Message?>(null) }
    var pendingReminder by remember { mutableStateOf<Pair<Message, Long>?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val pending = pendingReminder
        pendingReminder = null
        if (granted && pending != null) {
            viewModel.setReminder(pending.first, pending.second)
        } else if (!granted) {
            Toast.makeText(context, "Notification permission is required for reminders", Toast.LENGTH_SHORT).show()
        }
    }
    val seenMessageIds = remember { mutableSetOf<String>() }
    var isSearching by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty() && !isSearching) {
            listState.animateScrollToItem(0)
        }
    }

    // Auto-focus search field
    LaunchedEffect(isSearching) {
        if (isSearching) {
            searchFocusRequester.requestFocus()
        }
    }

    // Jump-to-original, driven by tapping a reply quote. The target is looked up in
    // the live list, so a quote whose original was deleted simply has no index to
    // scroll to — the bubble disables the tap for that case anyway.
    val coroutineScope = rememberCoroutineScope()
    var replyHighlightId by remember { mutableStateOf<String?>(null) }
    val messageIds = remember(uiState.messages) {
        uiState.messages.mapTo(HashSet<String>()) { it.id }
    }
    val jumpToMessage: (String) -> Unit = { targetId ->
        coroutineScope.launch {
            val index = uiState.messages.reversed().indexOfFirst { it.id == targetId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
                replyHighlightId = targetId
                delay(1400)
                if (replyHighlightId == targetId) replyHighlightId = null
            }
        }
    }

    // Scroll to current search result
    val currentSearchMessageId = viewModel.getSearchResultMessageId()
    LaunchedEffect(uiState.searchResultIndex, currentSearchMessageId) {
        if (currentSearchMessageId != null) {
            val reversedMessages = uiState.messages.reversed()
            val index = reversedMessages.indexOfFirst { it.id == currentSearchMessageId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    // Image picker (multi-image)
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            inputStream?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            viewModel.sendImageMessage(tempFile)
            tempFile.delete()
        }
    }

    // Generic file picker. Files are copied into app storage immediately, so no
    // long-lived document-provider permission is required.
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        coroutineScope.launch {
            uris.forEach { uri ->
                val picked = withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    var displayName = "File"
                    var declaredSize: Long? = null
                    resolver.query(
                        uri,
                        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIndex >= 0) displayName = cursor.getString(nameIndex) ?: "File"
                            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                                declaredSize = cursor.getLong(sizeIndex)
                            }
                        }
                    }
                    if (declaredSize != null && declaredSize!! > SyncLimits.MAX_BLOB_BYTES) {
                        return@withContext null
                    }
                    val temp = File.createTempFile("picked_${UUID.randomUUID()}_", ".bin", context.cacheDir)
                    var copied = 0L
                    val successful = runCatching {
                        resolver.openInputStream(uri)?.use { input ->
                            temp.outputStream().buffered().use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    copied += count
                                    if (copied > SyncLimits.MAX_BLOB_BYTES) {
                                        throw IllegalArgumentException("File exceeds sync limit")
                                    }
                                    output.write(buffer, 0, count)
                                }
                            }
                        } ?: throw IllegalArgumentException("Cannot read selected file")
                    }.isSuccess
                    if (!successful) {
                        temp.delete()
                        null
                    } else {
                        val safeName = displayName
                            .substringAfterLast('/').substringAfterLast('\\')
                            .filterNot { it.isISOControl() }
                            .take(255)
                            .ifBlank { "File" }
                        PickedFile(temp, safeName, resolver.getType(uri), copied)
                    }
                }
                if (picked == null) {
                    Toast.makeText(
                        context,
                        "Could not attach file (maximum size is 1 GB)",
                        Toast.LENGTH_LONG,
                    ).show()
                } else {
                    viewModel.sendFileMessage(
                        picked.file,
                        picked.displayName,
                        picked.mimeType,
                        picked.sizeBytes,
                    )
                }
            }
        }
    }

    // Camera capture
    val cameraImageFile = remember {
        File(context.cacheDir, "camera_capture.jpg")
    }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageFile.exists()) {
            viewModel.sendImageMessage(cameraImageFile)
        }
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search in chat...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(searchFocusRequester)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            isSearching = false
                            viewModel.clearSearch()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    actions = {
                        if (uiState.searchResultIds.isNotEmpty()) {
                            Text(
                                "${uiState.searchResultIndex + 1}/${uiState.searchResultIds.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            IconButton(onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.previousSearchResult()
                            }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                            }
                            IconButton(onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                viewModel.nextSearchResult()
                            }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else if (selectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            selectionMode = false
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Cancel selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                viewModel.deleteMessages(selectedIds)
                                selectionMode = false
                                selectedIds = emptySet()
                            },
                            enabled = selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.clickable { onConfigClick() }
                        ) {
                            val avatarFile = uiState.chat?.avatarFile?.let { File(chatDir, it) }
                            if (avatarFile != null && avatarFile.exists()) {
                                AsyncImage(
                                    model = avatarFile,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(NcyAvatarShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = NcyAvatarShape,
                                    color = ncyAvatarColor(uiState.chat?.name ?: chatName)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (uiState.chat?.name ?: chatName.ifEmpty { "?" }).take(1).uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            Text(uiState.chat?.name ?: chatName.ifEmpty { "Chat" })
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            isSearching = true
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search in chat")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // targetSdk 35 forces edge-to-edge on API 35+, which means
                // Scaffold no longer resizes automatically for the IME even
                // with `windowSoftInputMode="adjustResize"`. Consume the IME
                // insets here so the input bar stays visible above the keyboard.
                .imePadding()
                // Desktop dotted-grid ground (shared modifier). A per-chat
                // background image, when set, draws on top of it.
                .ncyDottedGrid()
        ) {
            val bgFile = uiState.chat?.backgroundFile?.let { File(chatDir, it) }
            if (bgFile != null && bgFile.exists()) {
                Image(
                    painter = rememberAsyncImagePainter(bgFile),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Messages list — reversed so newest at bottom, anchored to bottom
                LazyColumn(
                    state = listState,
                    reverseLayout = true,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    val reversedMessages = uiState.messages.reversed()
                    itemsIndexed(
                        reversedMessages,
                        key = { _, msg -> msg.id }
                    ) { index, message ->
                        val isNew = message.id !in seenMessageIds
                        LaunchedEffect(message.id) { seenMessageIds.add(message.id) }
                        val parsedBubbleColor = uiState.chat?.bubbleColor?.let {
                            try { Color(android.graphics.Color.parseColor(it)) }
                            catch (_: Exception) { null }
                        }
                        AnimatedVisibility(
                            visible = true,
                            enter = if (isNew) fadeIn(tween(300)) + slideInVertically(
                                tween(300)
                            ) { it / 2 } else fadeIn(tween(0))
                        ) {
                            CompositionLocalProvider(LocalMediaReader provides mediaReader) {
                            MessageBubble(
                                message = message,
                                chatDir = chatDir,
                                onEdit = { viewModel.startEditing(message) },
                                onDelete = { showDeleteConfirm = message.id },
                                onToggleStar = { viewModel.toggleStar(message) },
                                onSetReminder = { reminderMessage = message },
                                onToggleReaction = { emoji ->
                                    viewModel.toggleReaction(message, emoji)
                                },
                                onToggleChecklistItem = { itemId ->
                                    viewModel.toggleChecklistItem(message, itemId)
                                },
                                onToggleTextChecklistBlock = { blockIndex ->
                                    viewModel.toggleTextChecklistBlock(message, blockIndex)
                                },
                                automaticFileDownloadLimitBytes =
                                    appSettings.automaticFileDownloadLimitBytes,
                                onDownloadFileAnyway = viewModel::downloadFileAnyway,
                                transferProgress = { hash -> mediaProgress[hash] },
                                transferFailure = { hash -> mediaFailures[hash] },
                                onCancelDownload = viewModel::cancelMediaDownload,
                                onRemoveDownload = viewModel::removeMediaDownload,
                                onRequestMedia = viewModel::requestMedia,
                                mediaVersion = mediaVersion,
                                onReply = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.startReply(message)
                                },
                                onJumpToReplyTarget = {
                                    message.replyToId?.let(jumpToMessage)
                                },
                                replyTargetExists = message.replyToId != null &&
                                    message.replyToId in messageIds,
                                isReplyHighlight = message.id == replyHighlightId,
                                bubbleColor = parsedBubbleColor,
                                useGradient = uiState.chat?.bubbleGradient == true,
                                isSelectionMode = selectionMode,
                                isSelected = message.id in selectedIds,
                                isSearchResult = message.id in uiState.searchResultIds,
                                isCurrentSearchResult = message.id == currentSearchMessageId,
                                isRemote = uiState.chat?.remoteMessagesOnLeft == true &&
                                    !message.authorDeviceId.isNullOrBlank() &&
                                    message.authorDeviceId != uiState.localDeviceId,
                                onToggleSelect = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    selectedIds = if (message.id in selectedIds) {
                                        selectedIds - message.id
                                    } else {
                                        selectedIds + message.id
                                    }
                                    if (selectedIds.isEmpty()) selectionMode = false
                                },
                                onEnterSelectionMode = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                    selectionMode = true
                                    selectedIds = setOf(message.id)
                                }
                            )
                            }
                        }
                        // Show date header below (visually above due to reverseLayout)
                        // when the next message in the reversed list is from a different day
                        val showHeader = if (index < reversedMessages.size - 1) {
                            getDateKey(message.createdAt) != getDateKey(reversedMessages[index + 1].createdAt)
                        } else {
                            true // always show header for the oldest message
                        }
                        if (showHeader) {
                            DateHeader(timestamp = message.createdAt)
                        }
                    }
                }

                // A pending reply attaches to whatever is sent next — text, checklist,
                // image, or voice — so the bar shows above the recorder too.
                val replyTarget = uiState.replyTarget.takeIf { uiState.editingMessage == null }

                // Voice recording UI
                if (isRecordingVoice) {
                    if (replyTarget != null) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ReplyComposerBar(replyTarget, onCancel = { viewModel.cancelReply() })
                    }
                    VoiceRecorder(
                        onRecorded = { file, durationMs ->
                            viewModel.sendVoiceMessage(file, durationMs)
                            isRecordingVoice = false
                        },
                        onCancel = { isRecordingVoice = false }
                    )
                } else {
                    // Input bar — flat with a hairline top border (no shadow).
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                    Column {
                    if (replyTarget != null) {
                        ReplyComposerBar(replyTarget, onCancel = { viewModel.cancelReply() })
                    }
                    RichTextInput(
                        editingMessage = uiState.editingMessage,
                        onSend = { blocks ->
                            if (uiState.editingMessage != null) {
                                viewModel.saveEdit(blocks)
                            } else {
                                viewModel.sendTextMessage(blocks)
                            }
                        },
                        onCancelEdit = { viewModel.cancelEditing() },
                        onVoiceClick = { isRecordingVoice = true },
                        onImageClick = { imagePickerLauncher.launch("image/*") },
                        onFileClick = { filePickerLauncher.launch("*/*") },
                        onSendChecklist = { items ->
                            if (uiState.editingMessage != null) viewModel.saveChecklistEdit(items)
                            else viewModel.sendChecklistMessage(items)
                        },
                        onCameraClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    cameraImageFile
                                )
                                cameraUri = uri
                                cameraLauncher.launch(uri)
                            }
                        }
                    )
                    }
                    }
                }
            }
        }
    }

    // Delete confirmation
    showDeleteConfirm?.let { messageId ->
        NcyAlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Message") },
            text = { Text("Delete this message?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.deleteMessage(messageId)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            }
        )
    }

    // Reminder picker
    reminderMessage?.let { msg ->
        val reminderOptions = listOf(
            "In 15 minutes" to 15L * 60 * 1000,
            "In 1 hour" to 60L * 60 * 1000,
            "In 3 hours" to 3L * 60 * 60 * 1000,
            "Tomorrow" to 24L * 60 * 60 * 1000
        )
        ModalBottomSheet(
            onDismissRequest = { reminderMessage = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                Text(
                    "Set reminder",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                reminderOptions.forEach { (label, delay) ->
                    NcySheetActionRow(Icons.Default.Alarm, label) {
                        val triggerAt = System.currentTimeMillis() + delay
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            pendingReminder = msg to triggerAt
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setReminder(msg, triggerAt)
                        }
                        reminderMessage = null
                    }
                }
            }
        }
    }

}

private data class PickedFile(
    val file: File,
    val displayName: String,
    val mimeType: String?,
    val sizeBytes: Long,
)

/**
 * Bar above the composer showing which message the next send will reply to. Reads the
 * live [target] rather than a snapshot — the snapshot is only frozen once the reply is
 * actually sent, so edits made while the bar is open are reflected here.
 */
@Composable
private fun ReplyComposerBar(target: Message, onCancel: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Replying to",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = replyPreviewText(target.plainTextPreview(), target.type),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onCancel) {
            Icon(Icons.Outlined.Close, contentDescription = "Cancel reply")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatLockOverlay(
    chatName: String,
    useBiometric: Boolean,
    onBack: () -> Unit,
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(chatName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        com.ncy.notechaty.security.PinPromptContent(
            title = "Protected chat",
            subtitle = "Enter your PIN to continue",
            biometricTitle = "Unlock chat",
            biometricSubtitle = "Verify to open this protected chat",
            useBiometric = useBiometric,
            onVerify = onVerify,
            onUnlocked = onUnlocked,
            modifier = Modifier.padding(padding)
        )
    }
}
