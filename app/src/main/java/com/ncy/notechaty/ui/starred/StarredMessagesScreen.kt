package com.ncy.notechaty.ui.starred

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarredMessagesScreen(
    onNavigateBack: () -> Unit,
    onChatClick: (String) -> Unit,
    viewModel: StarredMessagesViewModel = hiltViewModel()
) {
    val starredMessages by viewModel.starredMessages.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        if (starredMessages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .ncyDottedGrid(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No favorites",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .ncyDottedGrid()
            ) {
                items(starredMessages, key = { it.message.id }) { starred ->
                    StarredMessageItem(
                        starred = starred,
                        onClick = { onChatClick(starred.chatId) },
                        onUnstar = { viewModel.toggleStar(starred) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StarredMessageItem(
    starred: StarredMessage,
    onClick: () -> Unit,
    onUnstar: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = {
            Text(
                text = starred.chatName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        },
        headlineContent = {
            val preview = when (starred.message.type) {
                MessageType.VOICE -> "\uD83C\uDFA4 Voice message"
                MessageType.IMAGE -> "\uD83D\uDDBC Image"
                MessageType.FILE -> "📎 ${starred.message.plainTextPreview()}"
                MessageType.CHECKLIST -> "☑ ${starred.message.plainTextPreview()}"
                MessageType.TEXT -> starred.message.plainTextPreview()
            }
            Text(
                text = preview,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                    .format(Date(starred.message.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            IconButton(onClick = onUnstar) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Unstar",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}
