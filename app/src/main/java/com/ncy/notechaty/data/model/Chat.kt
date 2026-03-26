package com.ncy.notechaty.data.model

data class Chat(
    val id: String,
    val name: String,
    val avatarFile: String? = null,
    val backgroundFile: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastMessageAt: Long? = null,
    val lastMessagePreview: String? = null,
    val isPinned: Boolean = false,
    val bubbleColor: String? = null,
    val bubbleGradient: Boolean = false,
    val isArchived: Boolean = false,
    val tags: List<String> = emptyList()
)
