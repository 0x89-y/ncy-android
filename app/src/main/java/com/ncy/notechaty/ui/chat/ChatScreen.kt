package com.ncy.notechaty.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.ui.richtext.RichTextInput
import com.ncy.notechaty.ui.voicemessage.VoiceRecorder
import java.io.File

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
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val view = LocalView.current
    val chatDir = viewModel.getChatDir()

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
    val seenMessageIds = remember { mutableSetOf<String>() }
    var isSearching by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var showChecklistDialog by remember { mutableStateOf(false) }

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

    val topBarColor = MaterialTheme.colorScheme.primaryContainer
    val gradientTopColor = lerp(topBarColor, Color.White, 0.3f)
    val gradientModifier = if (appSettings.topBarGradient) {
        Modifier.background(
            Brush.verticalGradient(
                colors = listOf(gradientTopColor, topBarColor)
            )
        )
    } else Modifier

    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    modifier = gradientModifier,
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
                        containerColor = if (appSettings.topBarGradient) Color.Transparent
                            else MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else if (selectionMode) {
                TopAppBar(
                    modifier = gradientModifier,
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
                        containerColor = if (appSettings.topBarGradient) Color.Transparent
                            else MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    modifier = gradientModifier,
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
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(36.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = (uiState.chat?.name ?: chatName.ifEmpty { "?" }).take(1).uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        containerColor = if (appSettings.topBarGradient) Color.Transparent
                            else MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Background image or subtle dot pattern
            val bgFile = uiState.chat?.backgroundFile?.let { File(chatDir, it) }
            if (bgFile != null && bgFile.exists()) {
                Image(
                    painter = rememberAsyncImagePainter(bgFile),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.3f
                )
            } else {
                val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val spacing = 24.dp.toPx()
                    val radius = 1.5.dp.toPx()
                    var x = spacing
                    while (x < size.width) {
                        var y = spacing
                        while (y < size.height) {
                            drawCircle(color = dotColor, radius = radius, center = Offset(x, y))
                            y += spacing
                        }
                        x += spacing
                    }
                }
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
                            MessageBubble(
                                message = message,
                                chatDir = chatDir,
                                onEdit = { viewModel.startEditing(message) },
                                onDelete = { showDeleteConfirm = message.id },
                                onToggleStar = { viewModel.toggleStar(message) },
                                onSetReminder = { reminderMessage = message },
                                onToggleChecklistItem = { itemId ->
                                    viewModel.toggleChecklistItem(message, itemId)
                                },
                                bubbleColor = parsedBubbleColor,
                                useGradient = uiState.chat?.bubbleGradient == true,
                                isSelectionMode = selectionMode,
                                isSelected = message.id in selectedIds,
                                isSearchResult = message.id in uiState.searchResultIds,
                                isCurrentSearchResult = message.id == currentSearchMessageId,
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

                // Voice recording UI
                if (isRecordingVoice) {
                    VoiceRecorder(
                        onRecorded = { file, durationMs ->
                            viewModel.sendVoiceMessage(file, durationMs)
                            isRecordingVoice = false
                        },
                        onCancel = { isRecordingVoice = false }
                    )
                } else {
                    // Input bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 2.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
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
                        onChecklistClick = { showChecklistDialog = true },
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

    // Delete confirmation
    showDeleteConfirm?.let { messageId ->
        AlertDialog(
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
        AlertDialog(
            onDismissRequest = { reminderMessage = null },
            title = { Text("Set Reminder") },
            text = {
                Column {
                    reminderOptions.forEach { (label, delay) ->
                        TextButton(
                            onClick = {
                                viewModel.setReminder(msg, System.currentTimeMillis() + delay)
                                reminderMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label)
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reminderMessage = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Checklist creation dialog
    if (showChecklistDialog) {
        ChecklistCreateDialog(
            onDismiss = { showChecklistDialog = false },
            onCreate = { items ->
                viewModel.sendChecklistMessage(items)
                showChecklistDialog = false
            }
        )
    }
}

@Composable
private fun ChecklistCreateDialog(
    onDismiss: () -> Unit,
    onCreate: (List<String>) -> Unit
) {
    var items by remember { mutableStateOf(listOf("", "")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Checklist") },
        text = {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = item,
                            onValueChange = { newText ->
                                items = items.toMutableList().also { it[index] = newText }
                            },
                            placeholder = { Text("Item ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                        if (items.size > 1) {
                            IconButton(
                                onClick = {
                                    items = items.toMutableList().also { it.removeAt(index) }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { items = items + "" }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add item")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(items.filter { it.isNotBlank() }) },
                enabled = items.any { it.isNotBlank() }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
