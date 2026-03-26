package com.ncy.notechaty.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.model.ChatSortMode
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val baseDir: File = repository.getBaseDir()
    val settings = settingsRepository.settings

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val chats: StateFlow<List<Chat>> = combine(
        repository.observeChats(),
        settingsRepository.settings,
        _searchQuery
    ) { chats, settings, query ->
        val filtered = if (query.isBlank()) chats
        else chats.filter { it.name.contains(query, ignoreCase = true) }

        val sorted = when (settings.chatSortMode) {
            ChatSortMode.LAST_MESSAGE -> filtered.sortedByDescending { it.lastMessageAt ?: it.createdAt }
            ChatSortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
            ChatSortMode.CREATED -> filtered.sortedByDescending { it.createdAt }
        }

        // Pinned chats always on top
        sorted.sortedByDescending { it.isPinned }
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
