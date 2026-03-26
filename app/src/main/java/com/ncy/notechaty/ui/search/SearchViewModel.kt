package com.ncy.notechaty.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.model.MessageType
import com.ncy.notechaty.data.repository.ChatRepository
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
    private val repository: ChatRepository
) : ViewModel() {

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
            val chats = repository.observeChats().first()
            val searchResults = mutableListOf<SearchResult>()

            chats.forEach { chat ->
                val messages = repository.getMessages(chat.id)
                messages
                    .filter { it.type == MessageType.TEXT }
                    .filter { msg ->
                        msg.plainTextPreview().contains(query, ignoreCase = true)
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
