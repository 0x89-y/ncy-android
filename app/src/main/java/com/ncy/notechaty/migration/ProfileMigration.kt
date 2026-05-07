package com.ncy.notechaty.migration

import android.content.Context
import com.ncy.notechaty.data.model.Profile
import com.ncy.notechaty.data.repository.ProfileRepository
import java.io.File
import java.util.UUID

object ProfileMigration {

    private const val MARKER_NAME = ".migrated_v2"
    private const val DEFAULT_PROFILE_NAME = "Personal"

    /**
     * Migrate the legacy flat layout (`NoteChaty/chat_*`) into the profile-scoped layout
     * (`NoteChaty/profile_<id>/chat_*`). Idempotent — checks a marker file before running.
     * Must run BEFORE any ViewModel reads chats.
     */
    fun runIfNeeded(context: Context, profileRepository: ProfileRepository) {
        val rootDir = File(context.filesDir, "NoteChaty")
        if (!rootDir.exists()) rootDir.mkdirs()

        val marker = File(rootDir, MARKER_NAME)
        if (marker.exists()) {
            // Already migrated; just ensure a profile exists (covers fresh installs that hit
            // this path via the marker being created on a previous launch with no chats).
            profileRepository.ensureDefault(DEFAULT_PROFILE_NAME)
            return
        }

        val legacyChats = rootDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
            ?: emptyList()

        if (legacyChats.isEmpty()) {
            profileRepository.ensureDefault(DEFAULT_PROFILE_NAME)
            marker.createNewFile()
            return
        }

        val defaultId = UUID.randomUUID().toString()
        val profileDir = File(rootDir, "profile_$defaultId").apply { mkdirs() }
        legacyChats.forEach { chatDir ->
            val target = File(profileDir, chatDir.name)
            if (!chatDir.renameTo(target)) {
                // Fall back to copy + delete if rename across mount points fails.
                chatDir.copyRecursively(target, overwrite = true)
                chatDir.deleteRecursively()
            }
        }

        profileRepository.installInitial(
            Profile(id = defaultId, name = DEFAULT_PROFILE_NAME)
        )
        marker.createNewFile()
    }
}
