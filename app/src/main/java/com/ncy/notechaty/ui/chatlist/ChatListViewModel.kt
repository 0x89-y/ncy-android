package com.ncy.notechaty.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.ChatSortMode
import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.ProfileRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.security.ChatLockState
import com.ncy.notechaty.security.PinManager
import com.ncy.notechaty.security.ProfileLockState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed class SwitchResult {
    object Switched : SwitchResult()
    data class RequiresAuth(val profile: Profile) : SwitchResult()
    object NoChange : SwitchResult()
}

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val profileLockState: ProfileLockState,
    val pinManager: PinManager,
    val chatLockState: ChatLockState
) : ViewModel() {

    val baseDir: StateFlow<File> = profileRepository.activeProfileId
        .map { repository.getBaseDir() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repository.getBaseDir())

    val settings = settingsRepository.settings

    val profiles: StateFlow<List<Profile>> = profileRepository.profiles
    val activeProfileId: StateFlow<String> = profileRepository.activeProfileId
    val activeProfile: StateFlow<Profile?> = combine(profiles, activeProfileId) { all, id ->
        all.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun verifyPin(pin: String): Boolean = pinManager.verifyPin(pin)
    fun markUnlocked(chatId: String) = chatLockState.unlock(chatId)

    /** Returns true only when grace > 0 AND the chat is within its unlock window. */
    fun isChatUnlockedWithinGrace(chatId: String): Boolean {
        val grace = settingsRepository.settings.value.protectionGracePeriodMinutes
        if (grace <= 0) return false
        return chatLockState.isUnlocked(chatId, grace)
    }

    /**
     * Attempt a profile switch. If the target profile is protected and not within its
     * unlock grace, returns [SwitchResult.RequiresAuth] without switching — the screen
     * should then show a PIN/biometric prompt and call [confirmProfileUnlock] on success.
     */
    fun switchProfile(targetId: String): SwitchResult {
        if (targetId == profileRepository.activeProfileId.value) return SwitchResult.NoChange
        val target = profileRepository.profileById(targetId) ?: return SwitchResult.NoChange
        val grace = settingsRepository.settings.value.protectionGracePeriodMinutes
        if (target.isProtected && !profileLockState.isUnlocked(targetId, grace)) {
            return SwitchResult.RequiresAuth(target)
        }
        profileRepository.setActive(targetId)
        return SwitchResult.Switched
    }

    fun verifyProfilePin(profileId: String, pin: String): Boolean =
        profileRepository.verifyProfilePin(profileId, pin)

    fun confirmProfileUnlock(profileId: String) {
        profileLockState.unlock(profileId)
        profileRepository.setActive(profileId)
    }

    fun createProfileAndSwitch(name: String): Profile? {
        if (name.isBlank()) return null
        val created = profileRepository.createProfile(name)
        profileRepository.setActive(created.id)
        return created
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val chats: StateFlow<List<Chat>> = combine(
        repository.observeChats(),
        settingsRepository.settings,
        _searchQuery
    ) { chats, settings, query ->
        val active = chats.filter { !it.isArchived }
        val filtered = if (query.isBlank()) active
        else active.filter { it.name.contains(query, ignoreCase = true) }

        val sorted = when (settings.chatSortMode) {
            ChatSortMode.LAST_MESSAGE -> filtered.sortedByDescending { it.lastMessageAt ?: it.createdAt }
            ChatSortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
            ChatSortMode.CREATED -> filtered.sortedByDescending { it.createdAt }
        }

        // Pinned chats always on top
        sorted.sortedByDescending { it.isPinned }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedChats: StateFlow<List<Chat>> = combine(
        repository.observeChats(),
        _searchQuery
    ) { chats, query ->
        val archived = chats.filter { it.isArchived }
        if (query.isBlank()) archived
        else archived.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Chat ids that are currently inside the unlock grace window (or app-wide unlocked). Empty when grace = 0. */
    val unlockedChatIds: StateFlow<Set<String>> = combine(
        repository.observeChats(),
        settingsRepository.settings,
        chatLockState.timestamps,
        chatLockState.appUnlockedAt
    ) { chats, settings, timestamps, appUnlockedAt ->
        val grace = settings.protectionGracePeriodMinutes
        if (grace <= 0) return@combine emptySet()
        val now = System.currentTimeMillis()
        val perChat = timestamps
            .asSequence()
            .filter { (_, ts) -> now - ts < grace * 60_000L }
            .map { it.key }
            .toMutableSet()
        if (appUnlockedAt > 0L) {
            chats.asSequence().filter { it.isProtected }.forEach { perChat.add(it.id) }
        }
        perChat
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch {
            repository.scanForNewChats()
        }
    }

    fun createChat(name: String) {
        viewModelScope.launch {
            repository.createChat(name)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
        }
    }

    fun togglePin(chat: Chat) {
        viewModelScope.launch {
            repository.updateChat(chat.copy(isPinned = !chat.isPinned))
        }
    }

    fun toggleArchive(chat: Chat) {
        viewModelScope.launch {
            repository.updateChat(chat.copy(isArchived = !chat.isArchived, isPinned = false))
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortMode(mode: ChatSortMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                settingsRepository.settings.value.copy(chatSortMode = mode)
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.scanForNewChats()
        }
    }
}
