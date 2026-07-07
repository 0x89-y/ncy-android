package com.ncy.notechaty.data.model

enum class MessageType {
    TEXT, VOICE, IMAGE, CHECKLIST
}

data class ChecklistItem(
    val id: String,
    val text: String,
    val checked: Boolean = false
)

data class Message(
    val id: String,
    val chatId: String,
    val content: List<RichTextBlock> = emptyList(),
    val voiceFile: String? = null,
    val voiceDurationMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long? = null,
    val type: MessageType = MessageType.TEXT,
    val isStarred: Boolean = false,
    val imageFiles: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val reminderAt: Long? = null,
    val checklistItems: List<ChecklistItem> = emptyList(),
    /**
     * Reactions the user has attached to this message — an ordered set of emoji
     * strings (dedup on write, toggle-off on repeat). Sealed with the body in
     * protected chats since the whole [Message] rides inside `EncryptedMessage`.
     * Empty for legacy messages authored before this field existed.
     */
    val reactions: List<String> = emptyList(),
    /**
     * Whisper.cpp transcript for voice messages (null for other types, or when
     * transcription hasn't run yet). Symmetric sync: whichever device produces
     * a transcript republishes the [Message] via `MessageEdited`; peers see it
     * without re-transcribing. Sealed with the body in protected chats.
     */
    val transcript: String? = null
) {
    fun plainTextPreview(): String {
        if (type == MessageType.CHECKLIST) {
            val done = checklistItems.count { it.checked }
            val total = checklistItems.size
            val firstItem = checklistItems.firstOrNull()?.text ?: ""
            return "$done/$total $firstItem".take(100)
        }
        val fromContent = content.joinToString(" ") { block ->
            block.spans.joinToString("") { it.text }
        }
        // Voice messages carry no content blocks; fall back to the whisper
        // transcript so search results, notifications, and the chat-list
        // last-message preview all render legibly instead of an empty string.
        val text = if (fromContent.isBlank() && !transcript.isNullOrBlank()) {
            transcript
        } else {
            fromContent
        }
        return text.take(100)
    }
}
