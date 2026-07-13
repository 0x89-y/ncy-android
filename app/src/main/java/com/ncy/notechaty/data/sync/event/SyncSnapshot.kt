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
 * one, so a paired desktop receives all profiles. Each event in a profile directory
 * is stamped with that physical directory's canonical id + display name, replacing
 * obsolete historical reinstall IDs. Pre-profile-migration chats that still sit
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
        ChatTombstoneGraveyard.events(dir)
            .filter { since == null || it.hlc > since }
            .forEach { ev ->
                out += if (profileId.isNotEmpty()) {
                    ev.copy(profileId = profileId, profileName = profileName)
                } else ev
            }
        dir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("chat_") }
            ?.forEach { chatDir ->
                val log = EventLog(File(chatDir, "events.ndjson"), codec)
                if (!log.exists()) return@forEach
                log.readSince(since).forEach { ev ->
                    // The physical directory is this installation's canonical profile.
                    // Restored historical events may carry obsolete profile UUIDs;
                    // restamp them on the wire so one chat cannot split across profiles.
                    out += if (profileId.isNotEmpty()) {
                        ev.copy(profileId = profileId, profileName = profileName)
                    } else {
                        ev
                    }
                }
            }
    }
}
