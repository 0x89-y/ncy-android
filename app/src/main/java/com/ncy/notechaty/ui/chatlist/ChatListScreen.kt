package com.ncy.notechaty.ui.chatlist

import androidx.compose.ui.res.stringResource
import com.ncy.notechaty.R
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.ncy.notechaty.ui.desktoptheme.NcyAlertDialog
import com.ncy.notechaty.ui.desktoptheme.NcyAvatarShape
import com.ncy.notechaty.ui.desktoptheme.NcyFlushList
import com.ncy.notechaty.ui.desktoptheme.NcyFlushSectionLabel
import com.ncy.notechaty.ui.desktoptheme.NcyInputSheet
import com.ncy.notechaty.ui.desktoptheme.NcyRowDivider
import com.ncy.notechaty.ui.desktoptheme.NcySectionLabel
import com.ncy.notechaty.ui.desktoptheme.NcySheetActionRow
import com.ncy.notechaty.ui.desktoptheme.ncyAvatarColor
import com.ncy.notechaty.ui.desktoptheme.ncyDottedGrid
import androidx.compose.ui.graphics.Color
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
import com.ncy.notechaty.ui.search.SearchResult
import com.ncy.notechaty.ui.search.SearchViewModel
import com.ncy.notechaty.ui.starred.StarredMessage
import com.ncy.notechaty.ui.starred.StarredMessagesViewModel
import com.ncy.notechaty.security.BiometricHelper
import com.ncy.notechaty.security.pinSupportingText
import com.ncy.notechaty.security.rememberPinLockoutSeconds
import com.ncy.notechaty.util.gibberishFor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private enum class HomeTab { CHATS, STARRED, REMINDERS, ARCHIVED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onChatClick: (String, String) -> Unit,
    onSettingsClick: () -> Unit,
    onManageProfilesClick: () -> Unit = {},
    viewModel: ChatListViewModel = hiltViewModel(),
    searchViewModel: SearchViewModel = hiltViewModel(),
    starredViewModel: StarredMessagesViewModel = hiltViewModel()
) {
    val chats by viewModel.chats.collectAsState()
    val archivedChats by viewModel.archivedChats.collectAsState()
    val appSettings by viewModel.settings.collectAsState()
    val unlockedChatIds by viewModel.unlockedChatIds.collectAsState()
    val baseDir by viewModel.baseDir.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val activeProfileId by viewModel.activeProfileId.collectAsState()
    val showAllProfiles by viewModel.showAllProfiles.collectAsState()
    val chatProfiles by viewModel.chatProfiles.collectAsState()
    var showProfileSheet by remember { mutableStateOf(false) }
    val searchQuery by searchViewModel.query.collectAsState()
    val searchResults by searchViewModel.results.collectAsState()
    val isSearching by searchViewModel.isSearching.collectAsState()
    val starredMessages by starredViewModel.starredMessages.collectAsState()
    val reminderMessages by starredViewModel.reminderMessages.collectAsState()
    val view = LocalView.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var chatToDelete by remember { mutableStateOf<Chat?>(null) }
    var longPressedChat by remember { mutableStateOf<Chat?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }
    var chatToUnlock by remember { mutableStateOf<Chat?>(null) }
    var profileToUnlock by remember { mutableStateOf<Profile?>(null) }
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var homeTab by rememberSaveable { mutableStateOf(HomeTab.CHATS) }
    var searchActive by rememberSaveable { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = searchActive) {
        searchViewModel.setQuery("")
        searchActive = false
    }
    // Favorites and Reminders open over the list now that there is no tab bar.
    BackHandler(enabled = !searchActive && homeTab != HomeTab.CHATS) {
        homeTab = HomeTab.CHATS
    }
    LaunchedEffect(searchActive) {
        if (searchActive) searchFocusRequester.requestFocus()
    }

    val openChat: (Chat) -> Unit = { chat ->
        // Under "All chats" the chat's own profile becomes active first: the chat
        // screen, and the unlock below, work in the active profile.
        viewModel.useProfileOf(chat.id)
        if (chat.isProtected && !viewModel.isChatUnlockedWithinGrace(chat.id)) {
            chatToUnlock = chat
        } else {
            onChatClick(chat.id, chat.name)
        }
    }
    val openChatById: (String) -> Unit = { id ->
        viewModel.useProfileOf(id)
        onChatClick(id, "")
    }
    val multipleProfiles = profiles.size > 1
    val listsAllProfiles = showAllProfiles && multipleProfiles
    val rootDir = baseDir.parentFile ?: baseDir
    val profileNames = remember(profiles) { profiles.associate { it.id to it.name } }
    /** Where a listed chat's files live, which under "All chats" need not be the active profile. */
    val chatBaseDir: (Chat) -> File = { chat ->
        chatProfiles[chat.id]?.let { File(rootDir, "profile_$it") } ?: baseDir
    }
    val profileTag: (Chat) -> String? = { chat ->
        if (listsAllProfiles) chatProfiles[chat.id]?.let(profileNames::get) else null
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = searchViewModel::setQuery,
                            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                            placeholder = { Text(stringResource(R.string.search_chats_messages)) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else if (homeTab != HomeTab.CHATS) {
                        Text(
                            stringResource(
                                when (homeTab) {
                                    HomeTab.STARRED -> R.string.favorites
                                    HomeTab.ARCHIVED -> R.string.archived_chats
                                    else -> R.string.reminders
                                }
                            )
                        )
                    } else ProfileSwitcherButton(
                        activeProfile = profiles.firstOrNull { it.id == activeProfileId },
                        showAll = showAllProfiles && profiles.size > 1,
                        onClick = { showProfileSheet = true },
                    )
                },
                navigationIcon = {
                    if (searchActive) {
                        IconButton(onClick = {
                            searchViewModel.setQuery("")
                            searchActive = false
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close_search))
                        }
                    } else if (homeTab != HomeTab.CHATS) {
                        IconButton(onClick = { homeTab = HomeTab.CHATS }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                actions = {
                    if (!searchActive && homeTab == HomeTab.CHATS) {
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        }
                        Box {
                            IconButton(onClick = { filtersOpen = true }) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = stringResource(R.string.filter_chats),
                                )
                            }
                            DropdownMenu(expanded = filtersOpen, onDismissRequest = { filtersOpen = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.favorites)) },
                                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                    trailingIcon = {
                                        if (starredMessages.isNotEmpty()) {
                                            Text(
                                                starredMessages.size.toString(),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        filtersOpen = false
                                        homeTab = HomeTab.STARRED
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.archived_chats)) },
                                    leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                                    trailingIcon = {
                                        if (archivedChats.isNotEmpty()) {
                                            Text(
                                                archivedChats.size.toString(),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = {
                                        filtersOpen = false
                                        homeTab = HomeTab.ARCHIVED
                                    }
                                )
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        floatingActionButton = {
            if (homeTab == HomeTab.CHATS && searchQuery.isBlank()) {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    showCreateDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.new_chat))
            }
            }
        }
    ) { padding ->
        when {
        searchQuery.isNotBlank() -> HomeSearchResults(
            Modifier.padding(padding),
            chats.filter { it.name.contains(searchQuery, ignoreCase = true) },
            searchResults,
            isSearching
        ) { openChatById(it) }
        homeTab == HomeTab.STARRED -> HomeSavedMessages(
            Modifier.padding(padding),
            starredMessages,
            EmptyState(
                Icons.Outlined.StarOutline,
                stringResource(R.string.no_favorites_yet),
                stringResource(R.string.no_favorites_hint),
            ),
        ) { openChatById(it) }
        homeTab == HomeTab.REMINDERS -> HomeSavedMessages(
            Modifier.padding(padding),
            reminderMessages,
            EmptyState(
                Icons.Outlined.NotificationsNone,
                stringResource(R.string.no_upcoming_reminders),
                stringResource(R.string.no_reminders_hint),
            ),
        ) { openChatById(it) }
        homeTab == HomeTab.ARCHIVED -> Box(
            modifier = Modifier.fillMaxSize().padding(padding).ncyDottedGrid()
        ) {
            if (archivedChats.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyStateMessage(
                        EmptyState(
                            Icons.Outlined.Archive,
                            stringResource(R.string.no_archived_chats),
                            stringResource(R.string.no_archived_chats_hint),
                        )
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    item(key = "card_archived") {
                        NcyFlushList {
                            archivedChats.forEachIndexed { i, chat ->
                                if (i > 0) NcyRowDivider()
                                ChatListItem(
                                    chat = chat,
                                    baseDir = chatBaseDir(chat),
                                    profileTag = profileTag(chat),
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
        else ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .ncyDottedGrid()
        ) {
            if (chats.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyStateMessage(
                        // "No chats yet" would be wrong when they are only archived.
                        if (archivedChats.isNotEmpty()) EmptyState(
                            Icons.Outlined.Archive,
                            stringResource(R.string.no_active_chats),
                            stringResource(R.string.no_active_chats_hint),
                        ) else EmptyState(
                            Icons.Outlined.ChatBubbleOutline,
                            stringResource(R.string.no_chats_yet),
                            stringResource(R.string.no_chats_hint),
                        )
                    )
                }
            } else {
                val pinned = chats.filter { it.isPinned }
                val others = chats.filter { !it.isPinned }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    // Upcoming reminders are worth seeing at a glance; favorites are
                    // looked up, so they sit in the filter menu instead.
                    if (reminderMessages.isNotEmpty()) {
                        item(key = "shortcuts") {
                            NcyFlushList {
                                HomeShortcutRow(
                                    Icons.Default.Notifications,
                                    stringResource(R.string.reminders),
                                    reminderMessages.size,
                                ) { homeTab = HomeTab.REMINDERS }
                            }
                        }
                    }
                    if (pinned.isNotEmpty()) {
                        item(key = "lbl_pinned") { NcyFlushSectionLabel(stringResource(R.string.pinned)) }
                        item(key = "card_pinned") {
                            NcyFlushList {
                                pinned.forEachIndexed { i, chat ->
                                    if (i > 0) NcyRowDivider()
                                    ChatListItem(
                                        chat = chat,
                                        baseDir = chatBaseDir(chat),
                                        profileTag = profileTag(chat),
                                        isUnlocked = chat.id in unlockedChatIds,
                                        onClick = { openChat(chat) },
                                        onLongClick = { longPressedChat = chat }
                                    )
                                }
                            }
                        }
                    }

                    if (others.isNotEmpty()) {
                        // A heading only when another section needs telling apart:
                        // "All" under Pinned, "Chats" below the Reminders row.
                        when {
                            pinned.isNotEmpty() -> item(key = "lbl_all") { NcyFlushSectionLabel(stringResource(R.string.all)) }
                            reminderMessages.isNotEmpty() -> item(key = "lbl_all") { NcyFlushSectionLabel(stringResource(R.string.chats)) }
                        }
                        item(key = "card_all") {
                            NcyFlushList {
                                others.forEachIndexed { i, chat ->
                                    if (i > 0) NcyRowDivider()
                                    ChatListItem(
                                        chat = chat,
                                        baseDir = chatBaseDir(chat),
                                        profileTag = profileTag(chat),
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
        // Under "All chats" a new chat needs a profile; it defaults to the last used.
        var targetProfileId by remember { mutableStateOf(activeProfileId) }
        CreateChatDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                // Straight into the new chat: creating one is how you start a note.
                viewModel.createChat(name, targetProfileId) { created -> onChatClick(created.id, created.name) }
            },
            profilePicker = if (listsAllProfiles) {
                {
                    val open = profiles.filter { it.id == activeProfileId || !it.isProtected || it.id in chatProfiles.values }
                    ProfileDropdownField(
                        profiles = open,
                        selectedId = targetProfileId,
                        onSelected = { targetProfileId = it },
                    )
                }
            } else null,
        )
    }

    if (showProfileSheet) {
        var counts by remember { mutableStateOf<Map<String, Int>?>(null) }
        LaunchedEffect(profiles) {
            counts = withContext(Dispatchers.IO) { viewModel.chatCounts() }
        }
        ProfileSwitcherSheet(
            profiles = profiles,
            activeProfileId = activeProfileId,
            showAll = listsAllProfiles,
            chatCounts = counts,
            onDismiss = { showProfileSheet = false },
            onShowAll = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                viewModel.showAllProfiles()
                showProfileSheet = false
            },
            onProfileSelected = { id ->
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                showProfileSheet = false
                when (val result = viewModel.selectProfile(id)) {
                    is SwitchResult.RequiresAuth -> profileToUnlock = result.profile
                    else -> Unit
                }
            },
            onAddProfile = {
                showProfileSheet = false
                showAddProfileDialog = true
            },
            onManageProfiles = {
                showProfileSheet = false
                onManageProfilesClick()
            },
        )
    }

    // Long-press menu
    longPressedChat?.let { chat ->
        ModalBottomSheet(
            onDismissRequest = { longPressedChat = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                        if (chat.isPinned) stringResource(R.string.unpin) else stringResource(R.string.pin_to_top)
                    ) {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        viewModel.togglePin(chat)
                        longPressedChat = null
                    }
                }
                NcySheetActionRow(
                    if (chat.isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                    if (chat.isArchived) stringResource(R.string.unarchive) else stringResource(R.string.archive)
                ) {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    viewModel.toggleArchive(chat)
                    longPressedChat = null
                }
                NcySheetActionRow(Icons.Default.Delete, stringResource(R.string.delete), destructive = true) {
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
            title = { Text(stringResource(R.string.delete_chat)) },
            text = { Text(stringResource(R.string.delete_chat_confirm, chat.name)) },
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
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

/** A Favorites or Reminders entry above the chats, with how many it holds. */
@Composable
private fun HomeShortcutRow(icon: ImageVector, label: String, count: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HomeSearchResults(modifier: Modifier, chatResults: List<Chat>, results: List<SearchResult>, searching: Boolean, onChatClick: (String) -> Unit) {
    if (searching) {
        Box(modifier.fillMaxSize().ncyDottedGrid(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (chatResults.isEmpty() && results.isEmpty()) {
        Box(modifier.fillMaxSize().ncyDottedGrid(), contentAlignment = Alignment.Center) { Text(stringResource(R.string.no_results_found), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    } else {
        LazyColumn(modifier.fillMaxSize().ncyDottedGrid()) {
            items(chatResults, key = { "chat-${it.id}" }) { chat ->
                HomeMessageRow(stringResource(R.string.chat), chat.name, chat.lastMessageAt ?: chat.createdAt) { onChatClick(chat.id) }
            }
            items(results.size, key = { results[it].message.id }) { index ->
                val result = results[index]
                HomeMessageRow(result.chatName, result.message.plainTextPreview(), result.message.createdAt) { onChatClick(result.chatId) }
            }
        }
    }
}

@Composable
private fun HomeSavedMessages(
    modifier: Modifier,
    messages: List<StarredMessage>,
    empty: EmptyState,
    onChatClick: (String) -> Unit,
) {
    if (messages.isEmpty()) {
        Box(modifier.fillMaxSize().ncyDottedGrid(), contentAlignment = Alignment.Center) {
            EmptyStateMessage(empty)
        }
    } else {
        LazyColumn(modifier.fillMaxSize().ncyDottedGrid()) {
            items(messages.size, key = { messages[it].message.id }) { index ->
                val item = messages[index]
                HomeMessageRow(item.chatName, item.message.plainTextPreview(), item.message.reminderAt ?: item.message.createdAt) { onChatClick(item.chatId) }
            }
        }
    }
}

/** What an empty tab shows: an icon, what it is, and how to fill it. */
private data class EmptyState(val icon: ImageVector, val title: String, val hint: String)

/**
 * One empty-state layout for every tab, so Favorites and Reminders explain
 * themselves like Chats does. Readable colours: the hint used to be 40% alpha.
 */
@Composable
private fun EmptyStateMessage(state: EmptyState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 40.dp),
    ) {
        Surface(
            modifier = Modifier.size(112.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    state.icon,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            state.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            state.hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HomeMessageRow(chatName: String, preview: String, timestamp: Long, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        overlineContent = { Text(chatName, color = MaterialTheme.colorScheme.primary) },
        headlineContent = { Text(preview, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestamp))) }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatListItem(
    chat: Chat,
    baseDir: File,
    isUnlocked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    /** The chat's profile, named under "All chats" so equal chat names can be told apart. */
    profileTag: String? = null,
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
                        contentDescription = stringResource(R.string.pinned),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (chat.isProtected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (showLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = if (showLocked) stringResource(R.string.protected_label) else stringResource(R.string.unlocked),
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        supportingContent = {
            if (showLocked) {
                Text(
                    text = stringResource(R.string.protected_chat),
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                chat.lastMessageAt?.let { timestamp ->
                    Text(
                        text = formatTimestamp(timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (profileTag != null) {
                    // The profile's own avatar colour: tells profiles apart at a
                    // glance, matches the switcher, and is made for white text.
                    Text(
                        text = profileTag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(max = 96.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ncyAvatarColor(profileTag))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
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
    onCreate: (String) -> Unit,
    profilePicker: (@Composable () -> Unit)? = null,
) {
    NcyInputSheet(
        title = stringResource(R.string.new_chat_2),
        label = stringResource(R.string.chat_name),
        confirmLabel = stringResource(R.string.create),
        onDismiss = onDismiss,
        onConfirm = onCreate,
        extra = profilePicker,
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
    var failedAttempts by remember { mutableIntStateOf(0) }
    val lockoutSeconds = rememberPinLockoutSeconds(failedAttempts)
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canUseBio = useBiometric && activity != null && BiometricHelper.canUseBiometric(context)

    LaunchedEffect(canUseBio) {
        // canUseBio already includes `activity != null` — Kotlin's flow
        // analysis flags the second check as redundant. `!!` is safe.
        if (canUseBio) {
            BiometricHelper.authenticate(
                activity = activity!!,
                title = context.getString(R.string.unlock_chat),
                subtitle = context.getString(R.string.verify_to_open_this_protected_chat),
                onSuccess = onUnlocked
            )
        }
    }

    NcyAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.protected_chat)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.enter_your_pin_to_open_this_chat),
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
                    label = { Text(stringResource(R.string.pin)) },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    supportingText = pinSupportingText(error, lockoutSeconds),
                    modifier = Modifier.fillMaxWidth()
                )
                if (canUseBio) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            BiometricHelper.authenticate(
                                activity = activity!!,
                                title = context.getString(R.string.unlock_chat),
                                subtitle = context.getString(R.string.verify_to_open_this_protected_chat),
                                onSuccess = onUnlocked
                            )
                        }
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.use_fingerprint))
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
                        failedAttempts++
                    }
                },
                enabled = pin.isNotEmpty() && lockoutSeconds == 0
            ) {
                Text(stringResource(R.string.unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
    var failedAttempts by remember { mutableIntStateOf(0) }
    val lockoutSeconds = rememberPinLockoutSeconds(failedAttempts)
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val canUseBio = useBiometric && activity != null && BiometricHelper.canUseBiometric(context)

    LaunchedEffect(canUseBio) {
        // canUseBio already includes `activity != null` — Kotlin's flow
        // analysis flags the second check as redundant. `!!` is safe.
        if (canUseBio) {
            BiometricHelper.authenticate(
                activity = activity!!,
                title = context.getString(R.string.switch_to, profile.name),
                subtitle = context.getString(R.string.verify_to_open_this_profile),
                onSuccess = onUnlocked
            )
        }
    }

    NcyAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text(stringResource(R.string.switch_to, profile.name)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.enter_the_pin_for_this_profile),
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
                    label = { Text(stringResource(R.string.pin)) },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    supportingText = pinSupportingText(error, lockoutSeconds),
                    modifier = Modifier.fillMaxWidth()
                )
                if (canUseBio) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            BiometricHelper.authenticate(
                                activity = activity!!,
                                title = context.getString(R.string.switch_to, profile.name),
                                subtitle = context.getString(R.string.verify_to_open_this_profile),
                                onSuccess = onUnlocked
                            )
                        }
                    ) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.use_fingerprint))
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
                        failedAttempts++
                    }
                },
                enabled = pin.isNotEmpty() && lockoutSeconds == 0
            ) { Text(stringResource(R.string.switch_label)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun AddProfileDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    NcyInputSheet(
        title = stringResource(R.string.new_profile),
        label = stringResource(R.string.profile_name),
        confirmLabel = stringResource(R.string.create),
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
