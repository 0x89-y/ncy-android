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
    val hideName: Boolean = false,
    /**
     * Phase 3: per-chat random salt used to derive the encryption key from the user's
     * PIN via PBKDF2. Generated the first time a chat is marked protected; carried in
     * `ChatMetadata` (plaintext, since salts don't need to be secret) so paired
     * desktops can derive the same key once the user enters the PIN. `null` for
     * pre-phase-3 chats and for non-protected chats.
     */
    val protectionSalt: String? = null,
    val remoteMessagesOnLeft: Boolean = false,
    /**
     * Automatic retention: delete messages older than this many days, on every
     * device. `null` (the default, and what every pre-existing chat decodes to)
     * keeps messages forever.
     *
     * Carried here rather than in local settings so the rule syncs with the chat.
     * The field must exist even before this platform enforces it: `ChatMetadataChanged`
     * is whole-record last-write-wins, so a phone that re-authored metadata without
     * it — renaming the chat, say — would silently clear a rule set elsewhere.
     */
    val retentionDays: Int? = null,
)
