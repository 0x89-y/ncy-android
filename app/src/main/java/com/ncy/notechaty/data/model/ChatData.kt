package com.ncy.notechaty.data.model

/**
 * Root object for chat.json serialization.
 */
data class ChatData(
    val version: Int = 2,
    val chat: ChatMetadata,
    val messages: List<Message> = emptyList()
)

data class ChatMetadata(
    val id: String,
    val name: String,
    val avatarFile: String? = null,
    val backgroundFile: String? = null,
    val createdAt: Long,
    val isPinned: Boolean = false,
    val bubbleColor: String? = null,
    val bubbleGradient: Boolean = false,
    val isArchived: Boolean = false,
    val tags: List<String> = emptyList(),
    val isProtected: Boolean = false,
    val hideName: Boolean = false
)
