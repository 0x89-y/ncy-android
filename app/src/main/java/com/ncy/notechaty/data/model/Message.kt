package com.ncy.notechaty.data.model

enum class MessageType {
    TEXT, VOICE, IMAGE
}

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
    val reminderAt: Long? = null
) {
    fun plainTextPreview(): String {
        return content.joinToString(" ") { block ->
            block.spans.joinToString("") { it.text }
        }.take(100)
    }
}
