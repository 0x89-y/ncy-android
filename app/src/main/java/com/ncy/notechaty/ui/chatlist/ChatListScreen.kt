package com.ncy.notechaty.ui.chatlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.ChatSortMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onStarredClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsState()
    val baseDir = viewModel.baseDir
    var showCreateDialog by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<Chat?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var longPressedChat by remember { mutableStateOf<Chat?>(null) }

    Scaffold(
        topBar = {
                TopAppBar(
                    title = { Text("Ncy") },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = onStarredClick) {
                            Icon(Icons.Default.Star, contentDescription = "Starred")
                        }
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                ChatSortMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.displayName) },
                                        onClick = {
                                            viewModel.setSortMode(mode)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        if (chats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No chats yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap + to create your first note chat",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(chats, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        baseDir = baseDir,
                        onClick = { onChatClick(chat.id) },
                        onLongClick = { longPressedChat = chat }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateChatDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                viewModel.createChat(name)
                showCreateDialog = false
            }
        )
    }

    // Long-press menu
    longPressedChat?.let { chat ->
        AlertDialog(
            onDismissRequest = { longPressedChat = null },
            title = { Text(chat.name) },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            viewModel.togglePin(chat)
                            longPressedChat = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            if (chat.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (chat.isPinned) "Unpin" else "Pin to top")
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(
                        onClick = {
                            longPressedChat = null
                            chatToDelete = chat
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { longPressedChat = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation
    chatToDelete?.let { chat ->
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Delete Chat") },
            text = { Text("Delete \"${chat.name}\" and all its messages? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteChat(chat.id)
                        chatToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListItem(
    chat: Chat,
    baseDir: File,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {

    ListItem(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = chat.name,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (chat.isPinned) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        supportingContent = {
            chat.lastMessagePreview?.let { preview ->
                Text(
                    text = preview,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            chat.lastMessageAt?.let { timestamp ->
                Text(
                    text = formatTimestamp(timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            val avatarFile = chat.avatarFile?.let {
                File(File(baseDir, "chat_${chat.id}"), it)
            }
            if (avatarFile != null && avatarFile.exists()) {
                AsyncImage(
                    model = avatarFile,
                    contentDescription = chat.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = chat.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    )
    Divider(modifier = Modifier.padding(start = 72.dp))
}

@Composable
private fun CreateChatDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Chat") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Chat name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(millis: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = millis }

    return when {
        now.get(Calendar.DATE) == time.get(Calendar.DATE) &&
                now.get(Calendar.YEAR) == time.get(Calendar.YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
        }
        now.get(Calendar.YEAR) == time.get(Calendar.YEAR) -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
        }
        else -> {
            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(millis))
        }
    }
}
