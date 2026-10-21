package com.ncy.notechaty.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.repository.VisibleChats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchResult(
    val message: Message,
    val chatName: String,
    val chatId: String
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val visibleChats: VisibleChats,
) : ViewModel() {

    val settings = settingsRepository.settings

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    fun setQuery(query: String) {
        _query.value = query
        searchJob?.cancel()
        if (query.isBlank()) {
            _results.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // debounce
            _isSearching.value = true
            val chats = visibleChats.chats.first()
            val searchResults = mutableListOf<SearchResult>()

            chats.filter { !it.chat.isProtected }.forEach { (profileId, chat) ->
                val messages = repository.getMessagesIn(profileId, chat.id)
                messages
                    .filter { it.type == MessageType.TEXT || !it.transcript.isNullOrBlank() }
                    .filter { msg ->
                        // Match against text preview OR the voice transcript so
                        // spoken-word content in voice messages is findable
                        // once whisper has run over them.
                        msg.plainTextPreview().contains(query, ignoreCase = true) ||
                            (msg.transcript?.contains(query, ignoreCase = true) == true)
                    }
                    .forEach { msg ->
                        searchResults.add(SearchResult(msg, chat.name, chat.id))
                    }
            }

            _results.value = searchResults.sortedByDescending { it.message.createdAt }
            _isSearching.value = false
        }
    }
}
