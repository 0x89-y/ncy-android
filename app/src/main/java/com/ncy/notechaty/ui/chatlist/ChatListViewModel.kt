package com.ncy.notechaty.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.ChatSortMode
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.security.ChatLockState
import com.ncy.notechaty.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    val pinManager: PinManager,
    val chatLockState: ChatLockState
) : ViewModel() {

    val baseDir: File = repository.getBaseDir()
    val settings = settingsRepository.settings

    fun verifyPin(pin: String): Boolean = pinManager.verifyPin(pin)
    fun markUnlocked(chatId: String) = chatLockState.unlock(chatId)

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
