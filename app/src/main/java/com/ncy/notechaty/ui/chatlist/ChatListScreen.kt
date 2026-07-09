package com.ncy.notechaty.ui.chatlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.ncy.notechaty.ui.desktoptheme.NcyAlertDialog
import com.ncy.notechaty.ui.desktoptheme.NcyAvatarShape
import com.ncy.notechaty.ui.desktoptheme.NcyDividedCard
import com.ncy.notechaty.ui.desktoptheme.NcyInputSheet
import com.ncy.notechaty.ui.desktoptheme.NcyRowDivider
import com.ncy.notechaty.ui.desktoptheme.NcySectionLabel
import com.ncy.notechaty.ui.desktoptheme.NcySheetActionRow
import com.ncy.notechaty.ui.desktoptheme.ncyAvatarColor
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.security.BiometricHelper
import com.ncy.notechaty.util.gibberishFor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String, String) -> Unit,
    onSettingsClick: () -> Unit,
    onStarredClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onManageProfilesClick: () -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsState()
    val archivedChats by viewModel.archivedChats.collectAsState()
    val appSettings by viewModel.settings.collectAsState()
    val unlockedChatIds by viewModel.unlockedChatIds.collectAsState()
    val baseDir by viewModel.baseDir.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val view = LocalView.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<Chat?>(null) }
    var longPressedChat by remember { mutableStateOf<Chat?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    var chatToUnlock by remember { mutableStateOf<Chat?>(null) }
    var profileToUnlock by remember { mutableStateOf<Profile?>(null) }
    var showAddProfileDialog by remember { mutableStateOf(false) }

    val openChat: (Chat) -> Unit = { chat ->
        if (chat.isProtected && !viewModel.isChatUnlockedWithinGrace(chat.id)) {
            chatToUnlock = chat
        } else {
            onChatClick(chat.id, chat.name)
        }
    }

    val topBarColor = MaterialTheme.colorScheme.primaryContainer
    val gradientTopColor = lerp(topBarColor, Color.White, 0.3f)

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = if (appSettings.topBarGradient) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(gradientTopColor, topBarColor)
                        )
                    )
                } else Modifier,
                title = {
                    ProfileSwitcherDropdown(
                        profiles = profiles,
                        activeProfileId = activeProfileId,
                        onProfileSelected = { id ->
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            when (val result = viewModel.switchProfile(id)) {
                                is SwitchResult.RequiresAuth -> profileToUnlock = result.profile
                                else -> Unit
                            }
                        },
                        onAddProfile = { showAddProfileDialog = true },
                        onManageProfiles = onManageProfilesClick
                    )
                },
                actions = {
                    IconButton(onClick = onStarredClick) {
                        Icon(Icons.Default.Star, contentDescription = "Starred")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (appSettings.topBarGradient) Color.Transparent
                        else MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .ncyDottedGrid()
        ) {
            if (chats.isEmpty() && archivedChats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(128.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "No chats yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                val pinned = chats.filter { it.isPinned }
                val others = chats.filter { !it.isPinned }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item(key = "search") { SearchPill(onClick = onSearchClick) }

                    if (pinned.isNotEmpty()) {
                        item(key = "lbl_pinned") { NcySectionLabel("Pinned") }
                        item(key = "card_pinned") {
                            NcyDividedCard {
                                pinned.forEachIndexed { i, chat ->
                                    if (i > 0) NcyRowDivider()
                                    ChatListItem(
                                        chat = chat,
                                        baseDir = baseDir,
                                        isUnlocked = chat.id in unlockedChatIds,
                                        onClick = { openChat(chat) },
                                        onLongClick = { longPressedChat = chat }
                                    )
                                }
                            }
                        }
                    }

                    if (others.isNotEmpty()) {
                        item(key = "lbl_all") { NcySectionLabel(if (pinned.isEmpty()) "Chats" else "All") }
                        item(key = "card_all") {
                            NcyDividedCard {
                                others.forEachIndexed { i, chat ->
                                    if (i > 0) NcyRowDivider()
                                    ChatListItem(
                                        chat = chat,
                                        baseDir = baseDir,
                                        isUnlocked = chat.id in unlockedChatIds,
                                        onClick = { openChat(chat) },
                                        onLongClick = { longPressedChat = chat }
                                    )
                                }
                            }
                        }
                    }

                    // Archived section — its own card with an expand/collapse header row.
                    if (archivedChats.isNotEmpty()) {
                        item(key = "card_archived") {
                            NcyDividedCard {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            showArchived = !showArchived
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Archive,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Archived",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        "${archivedChats.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        if (showArchived) Icons.Default.KeyboardArrowUp
                                        else Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (showArchived) "Collapse" else "Expand",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (showArchived) {
                                    archivedChats.forEach { chat ->
                                        NcyRowDivider()
                                        ChatListItem(
                                            chat = chat,
                                            baseDir = baseDir,
                                            isUnlocked = chat.id in unlockedChatIds,
                                            onClick = { openChat(chat) },
                                            onLongClick = { longPressedChat = chat }
                                        )
                                    }
                                }
                            }
                        }
                    }
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
        ModalBottomSheet(
            onDismissRequest = { longPressedChat = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                // Sheet header — the chat's avatar + name.
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = NcyAvatarShape,
                        color = ncyAvatarColor(chat.name)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = chat.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        chat.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (!chat.isArchived) {
                    NcySheetActionRow(
                        Icons.Default.PushPin,
                        if (chat.isPinned) "Unpin" else "Pin to top"
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.togglePin(chat)
                        longPressedChat = null
                    }
                }
                NcySheetActionRow(
                    if (chat.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                    if (chat.isArchived) "Unarchive" else "Archive"
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.toggleArchive(chat)
                    longPressedChat = null
                }
                NcySheetActionRow(Icons.Default.Delete, "Delete", destructive = true) {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                    longPressedChat = null
                    chatToDelete = chat
                }
            }
        }
    }

    // Profile unlock dialog
    profileToUnlock?.let { profile ->
        ProfileUnlockDialog(
            profile = profile,
            useBiometric = appSettings.protectionUseBiometric,
            onDismiss = { profileToUnlock = null },
            onVerify = { pin -> viewModel.verifyProfilePin(profile.id, pin) },
            onUnlocked = {
                viewModel.confirmProfileUnlock(profile.id)
                profileToUnlock = null
            }
        )
    }

    if (showAddProfileDialog) {
        AddProfileDialog(
            onDismiss = { showAddProfileDialog = false },
            onCreate = { name ->
                showAddProfileDialog = false
                // Create + switch immediately so the user lands in the new profile.
                val created = viewModel.createProfileAndSwitch(name)
                if (created != null) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            }
        )
    }

    // Unlock dialog
    chatToUnlock?.let { chat ->
        ChatUnlockDialog(
            chat = chat,
            useBiometric = appSettings.protectionUseBiometric,
            onDismiss = { chatToUnlock = null },
            onVerify = { pin -> viewModel.verifyPin(pin) },
            onUnlocked = {
                viewModel.markUnlocked(chat.id)
                val unlocked = chat
                chatToUnlock = null
                onChatClick(unlocked.id, unlocked.name)
            }
        )
    }

    // Delete confirmation
    chatToDelete?.let { chat ->
        NcyAlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Delete Chat") },
            text = { Text("Delete \"${chat.name}\" and all its messages? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
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

/** Search field styled as a flat bordered pill; taps route to the search screen. */
@Composable
private fun SearchPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                "Search notes & chats",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListItem(
    chat: Chat,
    baseDir: File,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val showLocked = chat.isProtected && !isUnlocked
    val view = LocalView.current

    ListItem(
        // Transparent so the card surface (and its accent wash for pinned rows)
        // reads through — the enclosing divided-list card owns the background.
        colors = ListItemDefaults.colors(
            containerColor = if (chat.isPinned)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            else Color.Transparent
        ),
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onLongClick()
                }
            ),
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val displayName = if (showLocked && chat.hideName) {
                    gibberishFor(chat.id)
                } else {
                    chat.name
                }
                Text(
                    text = displayName,
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
                if (chat.isProtected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (showLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (showLocked) "Protected" else "Unlocked",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        supportingContent = {
            if (showLocked) {
                Text(
                    text = "Protected chat",
                    maxLines = 1,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                chat.lastMessagePreview?.let { preview ->
                    Text(
                        text = preview,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        .clip(NcyAvatarShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = NcyAvatarShape,
                    color = ncyAvatarColor(chat.name)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (showLocked && chat.hideName) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color.White
                            )
                        } else {
                            Text(
                                text = chat.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun CreateChatDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    NcyInputSheet(
        title = "New chat",
        label = "Chat name",
        confirmLabel = "Create",
        onDismiss = onDismiss,
        onConfirm = onCreate
    )
}

@Composable
private fun ChatUnlockDialog(
    chat: Chat,
    useBiometric: Boolean,
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canUseBio = useBiometric && activity != null && BiometricHelper.canUseBiometric(context)

    LaunchedEffect(canUseBio) {
        // canUseBio already includes `activity != null` — Kotlin's flow
        // analysis flags the second check as redundant. `!!` is safe.
        if (canUseBio) {
            BiometricHelper.authenticate(
                activity = activity!!,
                title = "Unlock chat",
                subtitle = "Verify to open this protected chat",
                onSuccess = onUnlocked
            )
        }
    }

    NcyAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text("Protected chat") },
        text = {
            Column {
                Text(
                    "Enter your PIN to open this chat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.take(64)
                        error = false
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    supportingText = if (error) {
                        { Text("Wrong PIN") }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (canUseBio) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            BiometricHelper.authenticate(
                                activity = activity!!,
                                title = "Unlock chat",
                                subtitle = "Verify to open this protected chat",
                                onSuccess = onUnlocked
                            )
                        }
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use fingerprint")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (onVerify(pin)) {
                        onUnlocked()
                    } else {
                        error = true
                        pin = ""
                    }
                },
                enabled = pin.isNotEmpty()
            ) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ProfileUnlockDialog(
    profile: Profile,
    useBiometric: Boolean,
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canUseBio = useBiometric && activity != null && BiometricHelper.canUseBiometric(context)

    LaunchedEffect(canUseBio) {
        // canUseBio already includes `activity != null` — Kotlin's flow
        // analysis flags the second check as redundant. `!!` is safe.
        if (canUseBio) {
            BiometricHelper.authenticate(
                activity = activity!!,
                title = "Switch to ${profile.name}",
                subtitle = "Verify to open this profile",
                onSuccess = onUnlocked
            )
        }
    }

    NcyAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text("Switch to ${profile.name}") },
        text = {
            Column {
                Text(
                    "Enter the PIN for this profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.take(64)
                        error = false
                    },
                    label = { Text("PIN") },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    supportingText = if (error) { { Text("Wrong PIN") } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (canUseBio) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            BiometricHelper.authenticate(
                                activity = activity!!,
                                title = "Switch to ${profile.name}",
                                subtitle = "Verify to open this profile",
                                onSuccess = onUnlocked
                            )
                        }
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Use fingerprint")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (onVerify(pin)) {
                        onUnlocked()
                    } else {
                        error = true
                        pin = ""
                    }
                },
                enabled = pin.isNotEmpty()
            ) { Text("Switch") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    NcyInputSheet(
        title = "New profile",
        label = "Profile name",
        confirmLabel = "Create",
        onDismiss = onDismiss,
        onConfirm = onCreate
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
