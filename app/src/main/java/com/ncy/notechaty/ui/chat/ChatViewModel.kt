package com.ncy.notechaty.ui.chat

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.*
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val editingMessage: Message? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val chatId: String = savedStateHandle["chatId"] ?: ""

    val settings = settingsRepository.settings

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadChat()
    }

    fun loadChat() {
        viewModelScope.launch {
            val chat = repository.getChat(chatId)
            val messages = repository.getMessages(chatId)
            _uiState.value = _uiState.value.copy(chat = chat, messages = messages)
        }
    }

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
            val targetFile = File(voiceDir, "voice_${UUID.randomUUID()}.m4a")
            voiceFile.copyTo(targetFile, overwrite = true)

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
            val targetFile = File(imagesDir, "img_${UUID.randomUUID()}.jpg")
            imageFile.copyTo(targetFile, overwrite = true)

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
            val chatName = _uiState.value.chat?.name ?: "Ncy"
            repository.updateMessage(message.copy(reminderAt = triggerAtMillis))
            ReminderScheduler.schedule(appContext, message, chatName, triggerAtMillis)
            loadChat()
        }
    }

    fun getChatDir(): File {
        return File(repository.getBaseDir(), "chat_$chatId")
    }
}
