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

    /**
     * Receipt catch-up (§10.9): every event numbered after [after], in receipt
     * order, paired with its receipt. [hlcFloor] is set only for the one transition
     * session of a peer with no position in this epoch: it then gets what HLC
     * catch-up would have sent, but numbered. Events since dropped (compaction,
     * deletion scrubbing) are simply absent.
     */
    fun eventsAfterReceipt(after: Long, hlcFloor: HlcTimestamp? = null): List<Pair<Long, ChatEvent>> {
        val receipts = Receipts.log()?.after(after).orEmpty()
        if (receipts.isEmpty()) return emptyList()
        val wanted = receipts.mapTo(HashSet()) { it.chatId }
        val byId = HashMap<String, ChatEvent>()
        forEachLocation { dir, profileId, profileName ->
            fun stamp(ev: ChatEvent) =
                if (profileId.isNotEmpty()) ev.copy(profileId = profileId, profileName = profileName) else ev
            ChatTombstoneGraveyard.events(dir).forEach { byId[it.eventId] = stamp(it) }
            dir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("chat_") && it.name.removePrefix("chat_") in wanted }
                ?.forEach { chatDir ->
                    (EventLog(File(chatDir, "events.ndjson"), codec).readAll() +
                        MessageTombstoneGraveyard.events(chatDir))
                        .forEach { byId[it.eventId] = stamp(it) }
                }
        }
        return receipts.mapNotNull { receipt ->
            val event = byId.remove(receipt.eventId) ?: return@mapNotNull null
            if (hlcFloor != null && event.hlc <= hlcFloor) null else receipt.seq to event
        }
    }

    /**
     * Give a receipt to every stored event that lacks one, oldest HLC first. Covers
     * a new epoch (everything), a crash between an event write and its receipt, and
     * files restored by copying. An event can be numbered late, never left out.
     */
    fun numberUnreceiptedEvents(): Int {
        val log = Receipts.log() ?: return 0
        val missing = ArrayList<ChatEvent>()
        forEachLocation { dir, _, _ ->
            ChatTombstoneGraveyard.events(dir).filterTo(missing) { log.receiptOf(it.eventId) == null }
            dir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                ?.forEach { chatDir ->
                    (EventLog(File(chatDir, "events.ndjson"), codec).readAll() +
                        MessageTombstoneGraveyard.events(chatDir))
                        .filterTo(missing) { log.receiptOf(it.eventId) == null }
                }
        }
        missing.sortedBy { it.hlc }.forEach { log.record(it.chatId, it.eventId) }
        return missing.size
    }

    /** Every directory that holds chats: each profile, then the pre-profile root. */
    private fun forEachLocation(block: (dir: File, profileId: String, profileName: String) -> Unit) {
        val rootDir = profileRepository.rootDir()
        if (!rootDir.exists()) return
        rootDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("profile_") }
            ?.forEach { profileDir ->
                val profileId = profileDir.name.removePrefix("profile_")
                block(profileDir, profileId, profileRepository.profileById(profileId)?.name ?: "")
            }
        block(rootDir, "", "")
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
                val events = log.readSince(since) + MessageTombstoneGraveyard.events(chatDir)
                    .filter { since == null || it.hlc > since }
                events.distinctBy { it.eventId }.forEach { ev ->
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
