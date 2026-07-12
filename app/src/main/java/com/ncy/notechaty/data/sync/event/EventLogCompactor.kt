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
                    ?.forEach { chatDir -> tryCompact(chatDir) }
            }
    }

    private fun tryCompact(chatDir: File) {
        val eventsFile = File(chatDir, "events.ndjson")
        if (!eventsFile.exists()) return
        val log = EventLog(eventsFile)
        val all = log.readAll()
        if (all.size < THRESHOLD) return
        val compacted = compact(all)
        if (compacted.size >= all.size) return // nothing to gain
        try {
            log.replaceAll(compacted)
            Log.i(TAG, "compacted ${chatDir.name}: ${all.size} → ${compacted.size} events")
        } catch (e: Exception) {
            Log.w(TAG, "compaction failed for ${chatDir.name}", e)
        }
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
                }
                is ChatEventOp.MessageCreated -> {
                    if (op.message.id in tombstones) continue
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
                        latestPerMessage[id] = event
                    }
                }
            }
        }

        val result = ArrayList<ChatEvent>(
            (if (latestMetadata != null) 1 else 0) + tombstones.size + latestPerMessage.size
        )
        latestMetadata?.let { result += it }
        result += latestPerMessage.values
        result += tombstones.values
        result.sortBy { it.hlc }
        return result
    }
}
