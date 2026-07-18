package com.ncy.notechaty.data.sync.event

import androidx.annotation.Keep
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

/**
 * Compact per-chat archive of exact message deletions.
 *
 * The stored record omits the normal wire envelope/type/payload duplication. Catch-up
 * reconstructs the original [ChatEvent], preserving its event ID and HLC exactly.
 */
object MessageTombstoneGraveyard {
    const val FILE_NAME = "message-tombstones.ndjson"

    @Keep
    data class Record(
        val eventId: String = "",
        val hlc: HlcTimestamp = HlcTimestamp(0, 0, ""),
        val originDevice: String = "",
        val chatId: String = "",
        val profileId: String = "",
        val profileName: String = "",
        val messageId: String = "",
    )

    private data class CachedIds(val modified: Long, val size: Long, val ids: Set<String>)
    private val gson = Gson()
    private val cache = HashMap<String, CachedIds>()

    @Synchronized
    fun archive(chatDir: File, event: ChatEvent): Boolean {
        val op = event.op as? ChatEventOp.MessageTombstoned ?: return false
        require(chatDir.name == "chat_${event.chatId}")
        val prior = records(chatDir)
        if (prior.any { it.messageId == op.messageId }) return false
        writeRecords(chatDir, (prior + event.toRecord(op.messageId)).sortedBy { it.hlc })
        return true
    }

    @Synchronized
    fun merge(chatDir: File, incoming: List<ChatEvent>): Int {
        val prior = records(chatDir)
        val known = prior.mapTo(HashSet()) { it.messageId }
        val added = incoming.mapNotNull { event ->
            val op = event.op as? ChatEventOp.MessageTombstoned ?: return@mapNotNull null
            if (event.chatId != chatDir.name.removePrefix("chat_") || !known.add(op.messageId)) {
                null
            } else {
                event.toRecord(op.messageId)
            }
        }
        if (added.isNotEmpty()) writeRecords(chatDir, (prior + added).sortedBy { it.hlc })
        return added.size
    }

    fun events(chatDir: File): List<ChatEvent> = records(chatDir).map { record ->
        ChatEvent(
            eventId = record.eventId,
            hlc = record.hlc,
            originDevice = record.originDevice,
            chatId = record.chatId,
            op = ChatEventOp.MessageTombstoned(record.messageId),
            profileId = record.profileId,
            profileName = record.profileName,
        )
    }

    @Synchronized
    fun contains(chatDir: File, messageId: String): Boolean = messageIds(chatDir).contains(messageId)

    @Synchronized
    fun purge(rootDir: File): Int {
        val removed = HashSet<Pair<String, String>>()
        val parents = buildList {
            add(rootDir)
            rootDir.listFiles()?.filterTo(this) {
                it.isDirectory && it.name.startsWith("profile_")
            }
        }
        parents.forEach { parent ->
            parent.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                ?.forEach { chatDir ->
                    records(chatDir).mapTo(removed) { it.chatId to it.messageId }
                    val file = File(chatDir, FILE_NAME)
                    if (file.isFile) file.delete()
                    cache.remove(file.absolutePath)
                }
        }
        return removed.size
    }

    private fun records(chatDir: File): List<Record> {
        val file = File(chatDir, FILE_NAME)
        if (!file.isFile) return emptyList()
        val expectedChatId = chatDir.name.takeIf { it.startsWith("chat_") }
            ?.removePrefix("chat_")
        return file.useLines { lines ->
            lines.mapNotNull { line ->
                if (line.isBlank()) return@mapNotNull null
                runCatching {
                    gson.fromJson(line, Record::class.java).takeIf {
                        it.eventId.isNotBlank() && it.originDevice.isNotBlank() &&
                            (expectedChatId == null || it.chatId == expectedChatId) &&
                            it.messageId.isNotBlank() && it.hlc.nodeId.isNotBlank()
                    }
                }.getOrNull()
            }.toList()
        }
    }

    private fun messageIds(chatDir: File): Set<String> {
        val file = File(chatDir, FILE_NAME)
        if (!file.isFile) return emptySet()
        val key = file.absolutePath
        val cached = cache[key]
        if (cached != null && cached.modified == file.lastModified() && cached.size == file.length()) {
            return cached.ids
        }
        val fresh = records(chatDir).mapTo(HashSet()) { it.messageId }
        cache[key] = CachedIds(file.lastModified(), file.length(), fresh)
        return fresh
    }

    private fun writeRecords(chatDir: File, records: List<Record>) {
        chatDir.mkdirs()
        val file = File(chatDir, FILE_NAME)
        val tmp = File(chatDir, "$FILE_NAME.tmp")
        FileOutputStream(tmp).use { output ->
            records.forEach { record ->
                output.write(gson.toJson(record).toByteArray(Charsets.UTF_8))
                output.write('\n'.code)
            }
            output.flush()
            output.fd.sync()
        }
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
        cache.remove(file.absolutePath)
    }

    private fun ChatEvent.toRecord(messageId: String) = Record(
        eventId = eventId,
        hlc = hlc,
        originDevice = originDevice,
        chatId = chatId,
        profileId = profileId,
        profileName = profileName,
        messageId = messageId,
    )
}
