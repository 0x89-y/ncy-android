package com.ncy.notechaty.ui.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StarredMessage(
    val message: Message,
    val chatName: String,
    val chatId: String
)

@HiltViewModel
class StarredMessagesViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings = settingsRepository.settings

    private val _starredMessages = MutableStateFlow<List<StarredMessage>>(emptyList())
    val starredMessages: StateFlow<List<StarredMessage>> = _starredMessages.asStateFlow()
    private val _reminderMessages = MutableStateFlow<List<StarredMessage>>(emptyList())
    val reminderMessages: StateFlow<List<StarredMessage>> = _reminderMessages.asStateFlow()

    init {
        loadStarredMessages()
    }

    private fun loadStarredMessages() {
        viewModelScope.launch {
            repository.observeChats().collect { chatList ->
                val visibleChats = chatList.filter { !it.isProtected }.map { chat ->
                    chat to repository.getMessages(chat.id)
                }
                val starred = mutableListOf<StarredMessage>()
                visibleChats.forEach { (chat, messages) ->
                    messages.filter { it.isStarred }.forEach { msg ->
                        starred.add(StarredMessage(msg, chat.name, chat.id))
                    }
                }
                _starredMessages.value = starred.sortedByDescending { it.message.createdAt }
                _reminderMessages.value = visibleChats.flatMap { (chat, messages) ->
                    messages
                        .filter { it.reminderAt != null && it.reminderAt >= System.currentTimeMillis() }
                        .map { StarredMessage(it, chat.name, chat.id) }
                }.sortedBy { it.message.reminderAt }
            }
        }
    }

    fun toggleStar(starredMessage: StarredMessage) {
        viewModelScope.launch {
            val updated = starredMessage.message.copy(isStarred = false)
            repository.updateMessage(updated)
            _starredMessages.value = _starredMessages.value.filter {
                it.message.id != starredMessage.message.id
            }
        }
    }
}
