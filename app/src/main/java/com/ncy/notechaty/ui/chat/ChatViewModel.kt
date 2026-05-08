package com.ncy.notechaty.ui.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.*
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.ChatEventBus
import com.ncy.notechaty.data.sync.media.MediaHash
import com.ncy.notechaty.notifications.ReminderScheduler
import com.ncy.notechaty.security.ChatLockState
import com.ncy.notechaty.security.PinManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val editingMessage: Message? = null,
    val searchQuery: String = "",
    val searchResultIds: Set<String> = emptySet(),
    val searchResultIndex: Int = -1,
    val unlockRequired: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val chatLockState: ChatLockState,
    private val pinManager: PinManager,
    private val eventBus: ChatEventBus,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val chatId: String = savedStateHandle["chatId"] ?: ""

    private var hasUnlockToken: Boolean = chatLockState.isUnlocked(
        chatId,
        settingsRepository.settings.value.protectionGracePeriodMinutes
    )

    fun markUnlocked() {
        hasUnlockToken = true
        val chat = _uiState.value.chat
        if (chat != null) {
            _uiState.value = _uiState.value.copy(unlockRequired = false)
        }
    }

    val settings = settingsRepository.settings

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadChat()
        // Reload when an event for this chat lands — covers events received from peers
        // (sync inbound) without coupling the repository to the UI directly.
        viewModelScope.launch {
            eventBus.events
                .filter { it.chatId == chatId }
                .collect { loadChat() }
        }
        // Phase 2b: also reload when a media blob for this chat finishes downloading.
        // The materialized message list is unchanged, but bubbles need to re-evaluate
        // file existence so placeholders flip to actual audio/image renderers.
        viewModelScope.launch {
            eventBus.mediaArrived
                .filter { it == chatId }
                .collect { loadChat() }
        }
    }

    fun loadChat() {
        viewModelScope.launch {
            val chat = repository.getChat(chatId)
            val messages = repository.getMessages(chatId)
            val unlockRequired = chat?.isProtected == true && !hasUnlockToken
            _uiState.value = _uiState.value.copy(
                chat = chat,
                messages = messages,
                unlockRequired = unlockRequired
            )
        }
    }

    fun verifyPin(pin: String): Boolean = pinManager.verifyPin(pin)

    fun sendTextMessage(blocks: List<RichTextBlock>) {
        if (blocks.isEmpty() || blocks.all { it.spans.isEmpty() || it.spans.all { s -> s.text.isEmpty() } }) return

        viewModelScope.launch {
            val message = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                content = blocks,
                type = MessageType.TEXT
            )
            repository.addMessage(message)
            loadChat()
        }
    }

    fun sendPlainTextMessage(text: String) {
        if (text.isBlank()) return
        sendTextMessage(
            listOf(
                RichTextBlock(
                    type = BlockType.PARAGRAPH,
                    spans = listOf(RichTextSpan(text = text))
                )
            )
        )
    }

    fun sendVoiceMessage(voiceFile: File, durationMs: Long) {
        viewModelScope.launch {
            val voiceDir = repository.getVoiceDir(chatId)
            val targetFile = ingestMedia(voiceDir, voiceFile, "m4a")
            val message = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                voiceFile = "voice/${targetFile.name}",
                voiceDurationMs = durationMs,
                type = MessageType.VOICE
            )
            repository.addMessage(message)
            loadChat()
        }
    }

    fun sendImageMessage(imageFile: File) {
        viewModelScope.launch {
            val imagesDir = repository.getImagesDir(chatId)
            val targetFile = ingestMedia(imagesDir, imageFile, "jpg")
            val message = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                content = listOf(
                    RichTextBlock(
                        type = BlockType.PARAGRAPH,
                        spans = listOf(RichTextSpan(text = "", inlineImage = "images/${targetFile.name}"))
                    )
                ),
                type = MessageType.IMAGE
            )
            repository.addMessage(message)
            loadChat()
        }
    }

    /**
     * Copy [source] into [destDir] under a content-addressed name (`<sha256>.<ext>`).
     * Subsequent ingests of the same bytes reuse the existing file (dedup). The
     * SHA-256 in the filename is what the desktop later asks for via `BlobRequest`.
     */
    private fun ingestMedia(destDir: File, source: File, ext: String): File {
        if (!destDir.exists()) destDir.mkdirs()
        val temp = File(destDir, "incoming_${UUID.randomUUID()}.$ext")
        source.copyTo(temp, overwrite = true)
        val hash = MediaHash.ofFile(temp)
        val target = File(destDir, "$hash.$ext")
        if (target.exists()) {
            temp.delete()
        } else if (!temp.renameTo(target)) {
            temp.copyTo(target, overwrite = false)
            temp.delete()
        }
        return target
    }

    fun startEditing(message: Message) {
        _uiState.value = _uiState.value.copy(editingMessage = message)
    }

    fun cancelEditing() {
        _uiState.value = _uiState.value.copy(editingMessage = null)
    }

    fun saveEdit(blocks: List<RichTextBlock>) {
        val editing = _uiState.value.editingMessage ?: return
        viewModelScope.launch {
            val updated = editing.copy(
                content = blocks,
                editedAt = System.currentTimeMillis()
            )
            repository.updateMessage(updated)
            _uiState.value = _uiState.value.copy(editingMessage = null)
            loadChat()
        }
    }

    fun saveChecklistEdit(items: List<String>) {
        val editing = _uiState.value.editingMessage ?: return
        viewModelScope.launch {
            val oldByText = editing.checklistItems.associateBy { it.text }
            val updatedItems = items.filter { it.isNotBlank() }.map { text ->
                val trimmed = text.trim()
                oldByText[trimmed] ?: ChecklistItem(
                    id = UUID.randomUUID().toString(),
                    text = trimmed,
                    checked = false
                )
            }
            val updated = editing.copy(
                checklistItems = updatedItems,
                editedAt = System.currentTimeMillis()
            )
            repository.updateMessage(updated)
            _uiState.value = _uiState.value.copy(editingMessage = null)
            loadChat()
        }
    }

    fun saveEditPlainText(text: String) {
        saveEdit(
            listOf(
                RichTextBlock(
                    type = BlockType.PARAGRAPH,
                    spans = listOf(RichTextSpan(text = text))
                )
            )
        )
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(chatId, messageId)
            loadChat()
        }
    }

    fun deleteMessages(messageIds: Set<String>) {
        viewModelScope.launch {
            messageIds.forEach { id ->
                repository.deleteMessage(chatId, id)
            }
            loadChat()
        }
    }

    fun toggleStar(message: Message) {
        viewModelScope.launch {
            repository.updateMessage(message.copy(isStarred = !message.isStarred))
            loadChat()
        }
    }

    fun setReminder(message: Message, triggerAtMillis: Long) {
        viewModelScope.launch {
            val chat = _uiState.value.chat
            val chatName = chat?.name ?: "Ncy"
            val isProtected = chat?.isProtected == true
            repository.updateMessage(message.copy(reminderAt = triggerAtMillis))
            ReminderScheduler.schedule(appContext, message, chatName, triggerAtMillis, isProtected)
            loadChat()
        }
    }

    fun setSearchQuery(query: String) {
        val messages = _uiState.value.messages
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchQuery = "",
                searchResultIds = emptySet(),
                searchResultIndex = -1
            )
            return
        }
        val matchingIds = messages
            .filter { it.type == MessageType.TEXT || it.type == MessageType.IMAGE || it.type == MessageType.CHECKLIST }
            .filter {
                if (it.type == MessageType.CHECKLIST) {
                    it.checklistItems.any { item -> item.text.contains(query, ignoreCase = true) }
                } else {
                    it.plainTextPreview().contains(query, ignoreCase = true)
                }
            }
            .map { it.id }
            .toSet()
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            searchResultIds = matchingIds,
            searchResultIndex = if (matchingIds.isNotEmpty()) 0 else -1
        )
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            searchResultIds = emptySet(),
            searchResultIndex = -1
        )
    }

    fun nextSearchResult() {
        val state = _uiState.value
        if (state.searchResultIds.isEmpty()) return
        val nextIndex = (state.searchResultIndex + 1) % state.searchResultIds.size
        _uiState.value = state.copy(searchResultIndex = nextIndex)
    }

    fun previousSearchResult() {
        val state = _uiState.value
        if (state.searchResultIds.isEmpty()) return
        val prevIndex = if (state.searchResultIndex <= 0) state.searchResultIds.size - 1
            else state.searchResultIndex - 1
        _uiState.value = state.copy(searchResultIndex = prevIndex)
    }

    fun getSearchResultMessageId(): String? {
        val state = _uiState.value
        if (state.searchResultIndex < 0 || state.searchResultIds.isEmpty()) return null
        // Return IDs in message order (chronological)
        val orderedIds = state.messages
            .filter { it.id in state.searchResultIds }
            .map { it.id }
        return orderedIds.getOrNull(state.searchResultIndex)
    }

    fun sendChecklistMessage(items: List<String>) {
        if (items.isEmpty() || items.all { it.isBlank() }) return
        viewModelScope.launch {
            val checklistItems = items.filter { it.isNotBlank() }.map { text ->
                ChecklistItem(
                    id = UUID.randomUUID().toString(),
                    text = text.trim(),
                    checked = false
                )
            }
            val message = Message(
                id = UUID.randomUUID().toString(),
                chatId = chatId,
                type = MessageType.CHECKLIST,
                checklistItems = checklistItems
            )
            repository.addMessage(message)
            loadChat()
        }
    }

    fun toggleChecklistItem(message: Message, itemId: String) {
        viewModelScope.launch {
            val updatedItems = message.checklistItems.map {
                if (it.id == itemId) it.copy(checked = !it.checked) else it
            }
            val updated = message.copy(
                checklistItems = updatedItems,
                editedAt = System.currentTimeMillis()
            )
            repository.updateMessage(updated)
            loadChat()
        }
    }

    fun getChatDir(): File {
        return File(repository.getBaseDir(), "chat_$chatId")
    }
}
