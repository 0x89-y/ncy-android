package com.ncy.notechaty.data.sync

/**
 * Chat and profile ids arrive from paired peers and from backup files, and both
 * end up as directory names (`chat_<id>`, `profile_<id>`). Restricting them to one
 * plain path segment stops an id like `/../../shared_prefs` from steering a write
 * outside chat storage. Android and desktop both mint UUIDs, which always pass.
 */
object SafeId {
    private const val MAX_LENGTH = 128

    fun isValid(id: String?): Boolean =
        id != null && id.isNotEmpty() && id.length <= MAX_LENGTH &&
            id.all { it.isLetterOrDigit() || it == '-' || it == '_' }
}
