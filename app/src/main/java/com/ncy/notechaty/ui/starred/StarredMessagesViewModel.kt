package com.ncy.notechaty.ui.starred

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.ProfileRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.repository.VisibleChats
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
    private val settingsRepository: SettingsRepository,
    private val profileRepository: ProfileRepository,
    private val visibleChats: VisibleChats,
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
            visibleChats.chats.collect { chatList ->
                val visible = chatList.filter { !it.chat.isProtected }.map { (profileId, chat) ->
                    chat to repository.getMessagesIn(profileId, chat.id)
                }
                val starred = mutableListOf<StarredMessage>()
                visible.forEach { (chat, messages) ->
                    messages.filter { it.isStarred }.forEach { msg ->
                        starred.add(StarredMessage(msg, chat.name, chat.id))
                    }
                }
                _starredMessages.value = starred.sortedByDescending { it.message.createdAt }
                _reminderMessages.value = visible.flatMap { (chat, messages) ->
                    messages
                        .filter { it.reminderAt != null && it.reminderAt >= System.currentTimeMillis() }
                        .map { StarredMessage(it, chat.name, chat.id) }
                }.sortedBy { it.message.reminderAt }
            }
        }
    }

    fun toggleStar(starredMessage: StarredMessage) {
        // Under "All chats" the message may sit in another profile; writes go to the active one.
        profileRepository.findProfileForChat(starredMessage.chatId)
            ?.takeIf { profileRepository.showAllProfiles.value && it != profileRepository.activeProfileId.value }
            ?.let(profileRepository::setActive)
        viewModelScope.launch {
            val updated = starredMessage.message.copy(isStarred = false)
            repository.updateMessage(updated)
            _starredMessages.value = _starredMessages.value.filter {
                it.message.id != starredMessage.message.id
            }
        }
    }
}
