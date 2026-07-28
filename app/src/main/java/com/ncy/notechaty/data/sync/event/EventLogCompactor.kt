package com.ncy.notechaty.data.sync.event

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Cuts every chat's `events.ndjson` down to the minimum events required to
 * materialize its current state. Drops superseded edits and the create/edit history of
 * tombstoned messages. Keeps tombstones themselves so a future offline peer's reply
 * about a tombstoned message id is still suppressed correctly.
 *
 * Runs on app launch (after [EventLogMigrator]) and only acts on chats whose log is
 * over [THRESHOLD] events — small chats are left alone since the savings aren't worth
 * the rewrite I/O.
 */
object EventLogCompactor {

    private const val TAG = "EventLogCompactor"
    /** Minimum log size before compaction kicks in. */
    private const val THRESHOLD = 2_000

    fun runIfNeeded(context: Context) {
        val rootDir = File(context.filesDir, "NoteChaty")
        if (!rootDir.exists()) return
        rootDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("profile_") }
            ?.forEach { profileDir ->
                profileDir.listFiles()
                    ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                    ?.forEach { chatDir -> compactChatDir(chatDir) }
            }
    }

    internal fun compactChatDir(chatDir: File) {
        val eventsFile = File(chatDir, "events.ndjson")
        if (!eventsFile.exists()) return
        val log = EventLog(eventsFile)
        val active = log.readAll()
        val all = (active + MessageTombstoneGraveyard.events(chatDir))
            .distinctBy { it.eventId }
        all.filter { it.op is ChatEventOp.ChatTombstoned }.maxByOrNull { it.hlc }?.let { tombstone ->
            try {
                ChatTombstoneGraveyard.archive(requireNotNull(chatDir.parentFile), tombstone)
                chatDir.deleteRecursively()
                logInfo("archived ${chatDir.name} in compact graveyard")
            } catch (e: Exception) {
                logWarning("graveyard archive failed for ${chatDir.name}", e)
            }
            return
        }
        // A deleted message keeps its text in the `MessageCreated` that survives it,
        // so a log holding one is fully recoverable by anyone who reads the file.
        // The size threshold exists to avoid pointless rewrite I/O — but a rewrite
        // that removes deleted notes from disk is not pointless, however small the
        // chat, and gating it on 2,000 events left ordinary chats readable forever.
        val tombstonedIds = all
            .mapNotNull { (it.op as? ChatEventOp.MessageTombstoned)?.messageId }
            .toSet()
        val holdsDeletedContent = tombstonedIds.isNotEmpty() && active.any { event ->
            when (val op = event.op) {
                is ChatEventOp.MessageCreated -> op.message.id in tombstonedIds
                is ChatEventOp.MessageEdited -> op.message.id in tombstonedIds
                else -> false
            }
        }
        if (all.size < THRESHOLD && !holdsDeletedContent) return
        val compacted = compact(all)
        val messageTombstones = compacted.filter { it.op is ChatEventOp.MessageTombstoned }
        val compactedActive = compacted.filterNot { it.op is ChatEventOp.MessageTombstoned }
        if (!holdsDeletedContent &&
            compactedActive.size >= active.size &&
            active.none { it.op is ChatEventOp.MessageTombstoned }
        ) return // nothing to gain
        try {
            messageTombstones.forEach { MessageTombstoneGraveyard.archive(chatDir, it) }
            if (compacted.size == 1 && compacted.single().op is ChatEventOp.ChatTombstoned) {
                ChatTombstoneGraveyard.archive(requireNotNull(chatDir.parentFile), compacted.single())
                chatDir.deleteRecursively()
                logInfo("archived ${chatDir.name} in compact graveyard")
                return
            }
            log.replaceAll(compactedActive)
            logInfo(
                "compacted ${chatDir.name}: ${active.size} → ${compactedActive.size} active events " +
                    "+ ${messageTombstones.size} exact message tombstones"
            )
        } catch (e: Exception) {
            logWarning("compaction failed for ${chatDir.name}", e)
        }
    }

    private fun logInfo(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    private fun logWarning(message: String, error: Exception) {
        runCatching { Log.w(TAG, message, error) }
    }

    /**
     * Pure: returns the smallest subset of [events] that materializes identically and
     * preserves tombstone semantics. All returned events come from the input verbatim
     * (HLCs unchanged) so peers can still verify any individual event's authenticity.
     */
    fun compact(events: List<ChatEvent>): List<ChatEvent> {
        val sorted = events.sortedBy { it.hlc }
        var latestMetadata: ChatEvent? = null
        val tombstones = LinkedHashMap<String, ChatEvent>()
        val latestPerMessage = LinkedHashMap<String, ChatEvent>()
        val authorSeeds = LinkedHashMap<String, ChatEvent>()

        for (event in sorted) {
            when (val op = event.op) {
                is ChatEventOp.ChatMetadataChanged -> latestMetadata = event
                is ChatEventOp.ChatTombstoned -> {
                    // Whole chat is dead — log collapses to a single tombstone.
                    return listOf(event)
                }
                is ChatEventOp.MessageTombstoned -> {
                    tombstones[op.messageId] = event
                    latestPerMessage.remove(op.messageId)
                    authorSeeds.remove(op.messageId)
                }
                is ChatEventOp.MessageCreated -> {
                    if (op.message.id in tombstones) continue
                    authorSeeds.putIfAbsent(op.message.id, event)
                    latestPerMessage[op.message.id] = event
                }
                is ChatEventOp.MessageEdited -> {
                    if (op.message.id in tombstones) continue
                    latestPerMessage[op.message.id] = event
                }
                is ChatEventOp.EncryptedMessage -> {
                    val id = op.messageId
                    if (id == null) {
                        // Legacy envelopes did not expose the UUID, so retain each one.
                        latestPerMessage["enc_${event.eventId}"] = event
                    } else if (id !in tombstones) {
                        if (op.innerType == ChatEventCodec.TYPE_MESSAGE_CREATED) {
                            authorSeeds.putIfAbsent(id, event)
                        }
                        latestPerMessage[id] = event
                    }
                }
            }
        }

        val result = ArrayList<ChatEvent>(
            (if (latestMetadata != null) 1 else 0) + tombstones.size + latestPerMessage.size
        )
        latestMetadata?.let { result += it }
        latestPerMessage.forEach { (id, latest) ->
            val message = when (val op = latest.op) {
                is ChatEventOp.MessageCreated -> op.message
                is ChatEventOp.MessageEdited -> op.message
                else -> null
            }
            if (message?.authorDeviceId.isNullOrBlank()) {
                authorSeeds[id]?.takeIf { it.eventId != latest.eventId }?.let { result += it }
            }
            result += latest
        }
        result += tombstones.values
        result.sortBy { it.hlc }
        return result
    }
}
