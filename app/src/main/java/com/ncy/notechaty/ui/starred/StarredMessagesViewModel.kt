package com.ncy.notechaty.ui.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.Chat
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

    init {
        loadStarredMessages()
    }

    private fun loadStarredMessages() {
        viewModelScope.launch {
            val chats = mutableListOf<Pair<Chat, List<Message>>>()
            repository.observeChats().collect { chatList ->
                val starred = mutableListOf<StarredMessage>()
                chatList.forEach { chat ->
                    val messages = repository.getMessages(chat.id)
                    messages.filter { it.isStarred }.forEach { msg ->
                        starred.add(StarredMessage(msg, chat.name, chat.id))
                    }
                }
                _starredMessages.value = starred.sortedByDescending { it.message.createdAt }
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
