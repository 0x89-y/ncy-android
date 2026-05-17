package com.ncy.notechaty.data.sync.event

import com.ncy.notechaty.data.repository.ProfileRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads events for the catch-up stream a peer needs on (re)connect, sorted by HLC
 * across chats so the receiver can apply them in a single pass.
 *
 * Profile sync: this walks **every** `profile_<id>` directory, not just the active
 * one, so a paired desktop receives all profiles. Each event is stamped with its
 * profile (id + display name) — taken from the event itself when present (events
 * authored after profile-sync landed) and otherwise backfilled from the directory
 * the chat lives in (legacy events). Pre-profile-migration chats that still sit
 * directly under the root sync with an empty profile id (a single "default" bucket
 * on the receiver).
 */
@Singleton
class SyncSnapshot @Inject constructor(
    private val profileRepository: ProfileRepository
) {
    private val codec = ChatEventCodec()

    fun eventsSince(since: HlcTimestamp?): List<ChatEvent> {
        val rootDir = profileRepository.rootDir()
        if (!rootDir.exists()) return emptyList()

        val all = ArrayList<ChatEvent>()

        // Each profile_<id> directory is one profile.
        rootDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("profile_") }
            ?.forEach { profileDir ->
                val profileId = profileDir.name.removePrefix("profile_")
                val profileName = profileRepository.profileById(profileId)?.name ?: ""
                collectFrom(profileDir, profileId, profileName, since, all)
            }

        // Defensive: pre-ProfileMigration layout kept chats directly under the root.
        // Sync them under the empty/default profile bucket.
        collectFrom(rootDir, "", "", since, all)

        all.sortWith(compareBy { it.hlc })
        return all
    }

    private fun collectFrom(
        dir: File,
        profileId: String,
        profileName: String,
        since: HlcTimestamp?,
        out: MutableList<ChatEvent>
    ) {
        dir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
            ?.forEach { chatDir ->
                val log = EventLog(File(chatDir, "events.ndjson"), codec)
                if (!log.exists()) return@forEach
                log.readSince(since).forEach { ev ->
                    // Keep an event's own profile stamp when it has one (authoritative);
                    // otherwise backfill from the directory it physically lives in.
                    out += if (ev.profileId.isNotEmpty()) {
                        ev
                    } else {
                        ev.copy(profileId = profileId, profileName = profileName)
                    }
                }
            }
    }
}
