package com.ncy.notechaty.ui.chatconfig

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.Chat
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.sync.event.ChatRetention
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ChatConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val pinManager: PinManager
) : ViewModel() {

    val settings = settingsRepository.settings

    fun hasAppPin(): Boolean = pinManager.hasPin()
    private val chatId: String = savedStateHandle["chatId"] ?: ""

    private val _chat = MutableStateFlow<Chat?>(null)
    val chat: StateFlow<Chat?> = _chat.asStateFlow()

    init {
        loadChat()
    }

    private fun loadChat() {
        viewModelScope.launch {
            _chat.value = repository.getChat(chatId)
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(name = name)
            repository.updateChat(updated)
            _chat.value = updated
        }
    }

    suspend fun setAvatar(file: File) {
        repository.setChatAvatar(chatId, file)
        _chat.value = repository.getChat(chatId)
    }

    suspend fun setBackground(file: File) {
        repository.setChatBackground(chatId, file)
        _chat.value = repository.getChat(chatId)
    }

    fun addTag(tag: String) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            if (tag !in current.tags) {
                val updated = current.copy(tags = current.tags + tag)
                repository.updateChat(updated)
                _chat.value = updated
            }
        }
    }

    fun removeTag(tag: String) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(tags = current.tags - tag)
            repository.updateChat(updated)
            _chat.value = updated
        }
    }

    fun setBubbleGradient(enabled: Boolean) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(bubbleGradient = enabled)
            repository.updateChat(updated)
            _chat.value = updated
        }
    }

    fun setProtected(enabled: Boolean) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(
                isProtected = enabled,
                hideName = if (!enabled) false else current.hideName
            )
            repository.updateChat(updated)
            _chat.value = updated
        }
    }

    fun setHideName(enabled: Boolean) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(hideName = enabled)
            repository.updateChat(updated)
            _chat.value = updated
        }
    }

    fun setBubbleColor(color: String?) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(bubbleColor = color)
            repository.updateChat(updated)
            _chat.value = updated
        }
    }

    fun setRemoteMessagesOnLeft(enabled: Boolean) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(remoteMessagesOnLeft = enabled)
            repository.updateChat(updated)
            _chat.value = updated
        }
    }

    /**
     * Set or clear this chat's retention window, then apply it immediately.
     *
     * Deletion is a normal tombstone, so it reaches every paired device. Zero clears
     * the rule. Applying straight away means the confirmation the user just accepted
     * is what actually happens, rather than waiting for the next periodic sweep.
     */
    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(retentionDays = days.takeIf { it > 0 })
            repository.updateChat(updated)
            _chat.value = updated
            repository.applyChatRetention()
        }
    }

    /** How many messages a window would delete right now — shown before turning it on. */
    suspend fun countExpiring(days: Int): Int {
        val current = _chat.value ?: return 0
        if (days <= 0) return 0
        val messages = runCatching { repository.getMessages(current.id) }.getOrNull() ?: return 0
        return ChatRetention.expiredMessageIds(
            current.copy(retentionDays = days),
            messages,
            System.currentTimeMillis(),
        ).size
    }

    fun setPinned(enabled: Boolean) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(isPinned = enabled, isArchived = if (enabled) false else current.isArchived)
            repository.updateChat(updated)
            _chat.value = updated
        }
    }

    fun toggleArchive(onDone: () -> Unit) {
        viewModelScope.launch {
            val current = _chat.value ?: return@launch
            val updated = current.copy(isArchived = !current.isArchived, isPinned = false)
            repository.updateChat(updated)
            _chat.value = updated
            onDone()
        }
    }

    fun deleteChat(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteChat(chatId)
            onDeleted()
        }
    }

    /**
     * Write the chat archive to [outputFile], then hand it to [onReady] so the
     * caller can copy it out. [onReady] runs only after the zip is complete —
     * previously this was fire-and-forget and the caller copied immediately,
     * which raced the writer and produced an empty file (or threw, because the
     * temp file did not exist yet).
     */
    fun exportChat(outputFile: File, onReady: (File) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.exportChat(chatId, outputFile) }
                .onSuccess { onReady(outputFile) }
                .onFailure(onError)
        }
    }

    fun getChatDir(): File = File(repository.getBaseDir(), "chat_$chatId")
}
