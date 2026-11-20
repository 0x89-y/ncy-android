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
import com.ncy.notechaty.ui.theme.NcyAlertDialog
import com.ncy.notechaty.ui.theme.NcyAvatarShape
import com.ncy.notechaty.ui.theme.NcyFlushList
import com.ncy.notechaty.ui.theme.NcyFlushSectionLabel
import com.ncy.notechaty.ui.theme.NcyInputSheet
import com.ncy.notechaty.ui.theme.NcyRowDivider
import com.ncy.notechaty.ui.theme.NcySectionLabel
import com.ncy.notechaty.ui.theme.NcySheetActionRow
import com.ncy.notechaty.ui.theme.ncyAvatarColor
import com.ncy.notechaty.ui.theme.ncyPaper
import com.ncy.notechaty.ui.theme.NcyGutter
import com.ncy.notechaty.ui.theme.NcySheetShape
import com.ncy.notechaty.ui.theme.NcyTheme
import com.ncy.notechaty.ui.theme.ncyChatHue
import com.ncy.notechaty.data.model.MessageType
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
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
    onSyncClick: () -> Unit = {},
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
    val syncStatus by viewModel.syncStatus.collectAsState()
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


    val allChatsById = remember(chats, archivedChats) { (chats + archivedChats).associateBy { it.id } }
    val hueOf: (String) -> Color = { id -> ncyChatHue(allChatsById[id]) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (searchActive) {
                HomeSearchBar(
                    query = searchQuery,
                    onQuery = searchViewModel::setQuery,
                    onClose = {
                        searchViewModel.setQuery("")
                        searchActive = false
                    },
                    focusRequester = searchFocusRequester,
                )
            } else {
                Column {
                    HomeHeader(
                        title = stringResource(
                            when (homeTab) {
                                HomeTab.CHATS -> R.string.chats
                                HomeTab.STARRED -> R.string.home_starred
                                HomeTab.REMINDERS -> R.string.reminders
                                HomeTab.ARCHIVED -> R.string.home_archived
                            }
                        ),
                        activeProfile = profiles.firstOrNull { it.id == activeProfileId },
                        showAllProfiles = listsAllProfiles,
                        syncStatus = syncStatus,
                        onProfileClick = { showProfileSheet = true },
                        onSearchClick = { searchActive = true },
                        onSettingsClick = onSettingsClick,
                        onSyncClick = onSyncClick,
                    )
                    val select: (HomeTab) -> Unit = { tab ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        homeTab = tab
                    }
                    HomeChips(
                        listOf(
                            HomeChip(stringResource(R.string.all), null, chats.size, homeTab == HomeTab.CHATS) { select(HomeTab.CHATS) },
                            HomeChip(stringResource(R.string.home_starred), Icons.Outlined.StarOutline, starredMessages.size.takeIf { it > 0 }, homeTab == HomeTab.STARRED) { select(HomeTab.STARRED) },
                            HomeChip(stringResource(R.string.reminders), Icons.Outlined.NotificationsNone, reminderMessages.size.takeIf { it > 0 }, homeTab == HomeTab.REMINDERS) { select(HomeTab.REMINDERS) },
                            HomeChip(stringResource(R.string.home_archived), Icons.Outlined.Archive, archivedChats.size.takeIf { it > 0 }, homeTab == HomeTab.ARCHIVED) { select(HomeTab.ARCHIVED) },
                        )
                    )
                    NcyRowDivider()
                }
            }
        },
        floatingActionButton = {
            if (homeTab == HomeTab.CHATS && !searchActive) {
                ExtendedFloatingActionButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        showCreateDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.new_chat_2), style = MaterialTheme.typography.labelLarge) },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = NcyTheme.colors.ink,
                    contentColor = NcyTheme.colors.onInk,
                )
            }
        }
    ) { padding ->
        val listModifier = Modifier.fillMaxSize().padding(padding).ncyPaper()
        val listPadding = PaddingValues(bottom = 96.dp)
        when {
        searchActive -> HomeSearchResults(
            modifier = listModifier,
            query = searchQuery,
            chatResults = if (searchQuery.isBlank()) emptyList() else (chats + archivedChats).filter { it.name.contains(searchQuery, ignoreCase = true) },
            results = if (searchQuery.isBlank()) emptyList() else searchResults,
            searching = isSearching,
            hueOf = hueOf,
            chatRow = { chat ->
                HomeChatRow(
                    chat = chat,
                    baseDir = chatBaseDir(chat),
                    isUnlocked = chat.id in unlockedChatIds,
                    onClick = { openChat(chat) },
                    onLongClick = { longPressedChat = chat },
                )
            },
            onNoteClick = openChatById,
        )
        homeTab == HomeTab.STARRED -> if (starredMessages.isEmpty()) {
            Box(listModifier, contentAlignment = Alignment.Center) {
                HomeEmpty(Icons.Outlined.StarOutline, stringResource(R.string.no_favorites_yet), stringResource(R.string.no_favorites_hint))
            }
        } else LazyColumn(listModifier, contentPadding = listPadding) {
            items(starredMessages, key = { it.message.id }) { item ->
                HomeNoteRow(item.chatName, hueOf(item.chatId), item.message) { openChatById(item.chatId) }
                NcyRowDivider(inset = NcyGutter)
            }
        }
        homeTab == HomeTab.REMINDERS -> LazyColumn(listModifier, contentPadding = listPadding) {
            item(key = "exact") { HomeExactAlarmWarning() }
            if (reminderMessages.isEmpty()) {
                item(key = "empty") {
                    Box(Modifier.fillMaxWidth().padding(top = 96.dp), contentAlignment = Alignment.Center) {
                        HomeEmpty(Icons.Outlined.NotificationsNone, stringResource(R.string.no_upcoming_reminders), stringResource(R.string.no_reminders_hint))
                    }
                }
            }
            reminderMessages.groupBy { reminderDay(it.message.reminderAt ?: 0L) }.toSortedMap().forEach { (day, group) ->
                item(key = "day_$day") {
                    HomeLabel(
                        stringResource(
                            when (day) {
                                ReminderDay.TODAY -> R.string.today
                                ReminderDay.TOMORROW -> R.string.home_tomorrow
                                ReminderDay.LATER -> R.string.home_later
                            }
                        )
                    )
                }
                itemsIndexed(group, key = { _, it -> "rem_${it.message.id}" }) { i, item ->
                    if (i > 0) NcyRowDivider(inset = 76.dp)
                    HomeReminderRow(
                        item = item,
                        hue = hueOf(item.chatId),
                        showDay = day == ReminderDay.LATER,
                        onClick = { openChatById(item.chatId) },
                        onDone = { starredViewModel.clearReminder(item) },
                    )
                }
            }
        }
        homeTab == HomeTab.ARCHIVED -> if (archivedChats.isEmpty()) {
            Box(listModifier, contentAlignment = Alignment.Center) {
                HomeEmpty(Icons.Outlined.Archive, stringResource(R.string.no_archived_chats), stringResource(R.string.no_archived_chats_hint))
            }
        } else LazyColumn(listModifier, contentPadding = listPadding) {
            itemsIndexed(archivedChats, key = { _, it -> it.id }) { i, chat ->
                if (i > 0) HomeRowDivider()
                HomeChatRow(
                    chat = chat,
                    baseDir = chatBaseDir(chat),
                    profileTag = profileTag(chat),
                    isUnlocked = chat.id in unlockedChatIds,
                    onClick = { openChat(chat) },
                    onLongClick = { longPressedChat = chat },
                )
            }
        }
        chats.isEmpty() -> Box(listModifier, contentAlignment = Alignment.Center) {
            // "No chats yet" would be wrong when they are only archived.
            if (archivedChats.isNotEmpty()) {
                HomeEmpty(Icons.Outlined.Archive, stringResource(R.string.no_active_chats), stringResource(R.string.no_active_chats_hint))
            } else {
                HomeEmpty(Icons.Outlined.ChatBubbleOutline, stringResource(R.string.no_chats_yet), stringResource(R.string.no_chats_hint))
            }
        }
        else -> {
            val pinned = chats.filter { it.isPinned }
            val others = chats.filter { !it.isPinned }
            LazyColumn(listModifier, contentPadding = listPadding) {
                reminderMessages.firstOrNull()?.let { next ->
                    item(key = "up_next") {
                        HomeUpNextCard(next, reminderMessages.size - 1) { homeTab = HomeTab.REMINDERS }
                    }
                }
                if (pinned.isNotEmpty()) {
                    item(key = "lbl_pinned") { HomeLabel(stringResource(R.string.pinned)) }
                    itemsIndexed(pinned, key = { _, it -> it.id }) { i, chat ->
                        if (i > 0) HomeRowDivider()
                        HomeChatRow(
                            chat = chat,
                            baseDir = chatBaseDir(chat),
                            profileTag = profileTag(chat),
                            isUnlocked = chat.id in unlockedChatIds,
                            onClick = { openChat(chat) },
                            onLongClick = { longPressedChat = chat },
                        )
                    }
                }
                if (others.isNotEmpty()) {
                    if (pinned.isNotEmpty()) item(key = "lbl_all") { HomeLabel(stringResource(R.string.all_chats)) }
                    itemsIndexed(others, key = { _, it -> it.id }) { i, chat ->
                        if (i > 0) HomeRowDivider()
                        HomeChatRow(
                            chat = chat,
                            baseDir = chatBaseDir(chat),
                            profileTag = profileTag(chat),
                            isUnlocked = chat.id in unlockedChatIds,
                            onClick = { openChat(chat) },
                            onLongClick = { longPressedChat = chat },
                        )
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
            shape = NcySheetShape,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                // Sheet header: the chat's tile and name.
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeChatTile(chat, chatBaseDir(chat), locked = chat.isProtected && chat.id !in unlockedChatIds, size = 36.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(chat.name, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                NcyRowDivider()

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

@Composable
private fun HomeSearchResults(
    modifier: Modifier,
    query: String,
    chatResults: List<Chat>,
    results: List<SearchResult>,
    searching: Boolean,
    hueOf: (String) -> Color,
    chatRow: @Composable (Chat) -> Unit,
    onNoteClick: (String) -> Unit,
) {
    var filter by rememberSaveable { mutableStateOf(SearchFilter.ALL) }
    val shown = results.filter { r ->
        when (filter) {
            SearchFilter.ALL -> true
            SearchFilter.VOICE -> r.message.type == MessageType.VOICE
            SearchFilter.LISTS -> r.message.type == MessageType.CHECKLIST
            SearchFilter.STARRED -> r.message.isStarred
            SearchFilter.FILES -> r.message.type == MessageType.FILE || r.message.type == MessageType.IMAGE
        }
    }
    Column(modifier) {
        HomeChips(
            listOf(
                HomeChip(stringResource(R.string.all), null, results.size.takeIf { query.isNotBlank() }, filter == SearchFilter.ALL) { filter = SearchFilter.ALL },
                HomeChip(stringResource(R.string.home_filter_voice), Icons.Outlined.Mic, null, filter == SearchFilter.VOICE) { filter = SearchFilter.VOICE },
                HomeChip(stringResource(R.string.home_filter_lists), Icons.Outlined.FactCheck, null, filter == SearchFilter.LISTS) { filter = SearchFilter.LISTS },
                HomeChip(stringResource(R.string.home_starred), Icons.Outlined.StarOutline, null, filter == SearchFilter.STARRED) { filter = SearchFilter.STARRED },
                HomeChip(stringResource(R.string.home_filter_files), null, null, filter == SearchFilter.FILES) { filter = SearchFilter.FILES },
            )
        )
        NcyRowDivider()
        when {
            searching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            query.isBlank() -> Unit
            chatResults.isEmpty() && shown.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                HomeEmpty(Icons.Outlined.Search, stringResource(R.string.no_results_found), "")
            }
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                if (chatResults.isNotEmpty() && filter == SearchFilter.ALL) {
                    item(key = "lbl_chats") { HomeLabel(stringResource(R.string.chats)) }
                    itemsIndexed(chatResults, key = { _, it -> "chat-${it.id}" }) { i, chat ->
                        if (i > 0) HomeRowDivider()
                        chatRow(chat)
                    }
                }
                if (shown.isNotEmpty()) {
                    item(key = "lbl_notes") { HomeLabel(stringResource(R.string.home_notes)) }
                    itemsIndexed(shown, key = { _, it -> it.message.id }) { i, r ->
                        if (i > 0) NcyRowDivider(inset = NcyGutter)
                        HomeNoteRow(r.chatName, hueOf(r.chatId), r.message, query) { onNoteClick(r.chatId) }
                    }
                }
            }
        }
    }
}

private enum class SearchFilter { ALL, VOICE, LISTS, STARRED, FILES }

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
