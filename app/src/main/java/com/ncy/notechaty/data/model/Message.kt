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
    val checklistItems: List<ChecklistItem> = emptyList()
) {
    fun plainTextPreview(): String {
        if (type == MessageType.CHECKLIST) {
            val done = checklistItems.count { it.checked }
            val total = checklistItems.size
            val firstItem = checklistItems.firstOrNull()?.text ?: ""
            return "$done/$total $firstItem".take(100)
        }
        return content.joinToString(" ") { block ->
            block.spans.joinToString("") { it.text }
        }.take(100)
    }
}
