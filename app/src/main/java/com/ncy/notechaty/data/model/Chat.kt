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
    val tags: List<String> = emptyList(),
    val isProtected: Boolean = false,
    val hideName: Boolean = false,
    /** Phase 3: per-chat PBKDF2 salt for protected-chat encryption. Mirrors the
     *  field on [ChatMetadata]; null for unprotected or pre-phase-3 chats. */
    val protectionSalt: String? = null,
    /** Display messages authored on other installations as incoming bubbles. */
    val remoteMessagesOnLeft: Boolean = false,
)
