package com.ncy.notechaty.data.sync.event

import android.content.Context
import android.util.Log
import com.ncy.notechaty.data.serialization.ChatSerializer
import java.io.File
import java.util.UUID

/**
 * Synthesizes an [EventLog] from each existing `chat.json`. Idempotent — skips any chat
 * folder that already has an `events.ndjson`. Safe — backs up `chat.json` before any
 * write, and only commits if a re-materialization round-trips to a structurally equal
 * `ChatData`. On mismatch the original file is restored and the chat is left in its
 * pre-migration state.
 *
 * Must run AFTER [com.ncy.notechaty.migration.ProfileMigration] so the layout is already
 * `NoteChaty/profile_<id>/chat_<id>/`.
 */
object EventLogMigrator {

    private const val TAG = "EventLogMigrator"
    private const val EVENTS_FILE = "events.ndjson"
    private const val CHAT_FILE = "chat.json"
    private const val BACKUP_SUFFIX = ".pre-sync-bak"

    fun runIfNeeded(context: Context, deviceId: String) {
        val rootDir = File(context.filesDir, "NoteChaty")
        if (!rootDir.exists()) return

        rootDir.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("profile_") }
            ?.forEach { profileDir ->
                profileDir.listFiles()
                    ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                    ?.forEach { chatDir -> migrateChat(chatDir, deviceId) }
            }
    }

    /** Public for callers (e.g. import) that produce a chat folder after main migration ran. */
    fun migrateChatIfNeeded(chatDir: File, deviceId: String) = migrateChat(chatDir, deviceId)

    private fun migrateChat(chatDir: File, deviceId: String) {
        val eventsFile = File(chatDir, EVENTS_FILE)
        val chatFile = File(chatDir, CHAT_FILE)

        if (eventsFile.exists()) {
            // Already migrated. Re-materialize chat.json from the log to repair any drift
            // a crash mid-mutation might have caused (event appended but chat.json not yet
            // overwritten). Cheap; runs once per launch.
            repairFromLog(eventsFile, chatFile)
            return
        }

        if (!chatFile.exists()) return // nothing to migrate

        val serializer = ChatSerializer()
        val original = try {
            serializer.readFromFile(chatFile)
        } catch (e: Exception) {
            Log.w(TAG, "Skipping ${chatDir.name}: chat.json unreadable", e)
            return
        }

        val backup = File(chatDir, CHAT_FILE + BACKUP_SUFFIX)
        chatFile.copyTo(backup, overwrite = true)

        try {
            val events = synthesizeEvents(original, deviceId)
            EventLog(eventsFile).replaceAll(events)

            val rebuilt = MaterializedView.fold(events)
            if (rebuilt == null || !rebuilt.equalsForMigration(original)) {
                Log.e(TAG, "Migration mismatch for ${chatDir.name}; rolling back")
                eventsFile.delete()
                backup.copyTo(chatFile, overwrite = true)
                return
            }
            backup.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed for ${chatDir.name}; rolling back", e)
            eventsFile.delete()
            if (backup.exists()) backup.copyTo(chatFile, overwrite = true)
        }
    }

    private fun synthesizeEvents(
        data: com.ncy.notechaty.data.model.ChatData,
        deviceId: String
    ): List<ChatEvent> {
        val events = ArrayList<ChatEvent>(data.messages.size + 1)

        events += ChatEvent(
            eventId = UUID.randomUUID().toString(),
            hlc = HlcTimestamp(physicalMs = data.chat.createdAt, logical = 0, nodeId = deviceId),
            originDevice = deviceId,
            chatId = data.chat.id,
            op = ChatEventOp.ChatMetadataChanged(data.chat)
        )

        data.messages.forEachIndexed { index, message ->
            events += ChatEvent(
                eventId = UUID.randomUUID().toString(),
                hlc = HlcTimestamp(physicalMs = message.createdAt, logical = index, nodeId = deviceId),
                originDevice = deviceId,
                chatId = data.chat.id,
                op = ChatEventOp.MessageCreated(message)
            )
            val editedAt = message.editedAt
            if (editedAt != null) {
                events += ChatEvent(
                    eventId = UUID.randomUUID().toString(),
                    hlc = HlcTimestamp(physicalMs = editedAt, logical = index, nodeId = deviceId),
                    originDevice = deviceId,
                    chatId = data.chat.id,
                    op = ChatEventOp.MessageEdited(message)
                )
            }
        }
        return events
    }

    private fun repairFromLog(eventsFile: File, chatFile: File) {
        val events = try {
            EventLog(eventsFile).readAll()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read event log at ${eventsFile.parentFile?.name}", e)
            return
        }
        if (events.isEmpty()) return
        val rebuilt = MaterializedView.fold(events) ?: return
        try {
            ChatSerializer().writeToFile(chatFile, rebuilt)
        } catch (e: Exception) {
            Log.w(TAG, "Could not repair chat.json for ${eventsFile.parentFile?.name}", e)
        }
    }

    /**
     * Equality check for migration: compares chat metadata and the message set (by id).
     * Message ordering after materialization is by createdAt, which matches the original
     * write order in practice — but we compare as sets so ties don't cause a false rollback.
     */
    private fun com.ncy.notechaty.data.model.ChatData.equalsForMigration(
        other: com.ncy.notechaty.data.model.ChatData
    ): Boolean {
        if (chat != other.chat) return false
        if (messages.size != other.messages.size) return false
        val a = messages.associateBy { it.id }
        val b = other.messages.associateBy { it.id }
        return a == b
    }
}
