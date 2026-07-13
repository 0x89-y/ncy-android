package com.ncy.notechaty.data.sync.event

import java.io.File

/** Exact, compact per-profile archive of whole-chat deletion events. */
object ChatTombstoneGraveyard {
    const val FILE_NAME = "chat-tombstones.ndjson"
    private data class CachedIds(val modified: Long, val size: Long, val ids: Set<String>)
    private val cache = HashMap<String, CachedIds>()

    @Synchronized
    fun archive(parentDir: File, event: ChatEvent): Boolean {
        require(event.op is ChatEventOp.ChatTombstoned)
        val log = EventLog(File(parentDir, FILE_NAME))
        val prior = log.readAll()
        if (prior.any { it.chatId == event.chatId }) return false
        log.replaceAll((prior + event).sortedBy { it.hlc })
        cache.remove(File(parentDir, FILE_NAME).absolutePath)
        return true
    }

    @Synchronized
    fun merge(parentDir: File, events: List<ChatEvent>): Int {
        val valid = events.filter { it.op is ChatEventOp.ChatTombstoned }
        val log = EventLog(File(parentDir, FILE_NAME))
        val prior = log.readAll()
        val known = prior.mapTo(HashSet()) { it.chatId }
        val added = valid.filter { known.add(it.chatId) }
        if (added.isNotEmpty()) log.replaceAll((prior + added).sortedBy { it.hlc })
        if (added.isNotEmpty()) cache.remove(File(parentDir, FILE_NAME).absolutePath)
        return added.size
    }

    fun events(parentDir: File): List<ChatEvent> =
        EventLog(File(parentDir, FILE_NAME)).readAll()
            .filter { it.op is ChatEventOp.ChatTombstoned }

    @Synchronized
    fun contains(rootDir: File, chatId: String): Boolean = graveyardParents(rootDir)
        .any { parent -> chatIds(parent).contains(chatId) }

    private fun chatIds(parentDir: File): Set<String> {
        val file = File(parentDir, FILE_NAME)
        if (!file.isFile) return emptySet()
        val key = file.absolutePath
        val cached = cache[key]
        if (cached != null && cached.modified == file.lastModified() && cached.size == file.length()) {
            return cached.ids
        }
        val fresh = events(parentDir).mapTo(HashSet()) { it.chatId }
        cache[key] = CachedIds(file.lastModified(), file.length(), fresh)
        return fresh
    }

    fun purge(rootDir: File): Int {
        val removedIds = HashSet<String>()
        graveyardParents(rootDir).forEach { parent ->
            val file = File(parent, FILE_NAME)
            if (file.isFile) {
                events(parent).mapTo(removedIds) { it.chatId }
                file.delete()
                cache.remove(file.absolutePath)
            }
            parent.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("chat_") }
                ?.forEach { chatDir ->
                    val events = EventLog(File(chatDir, "events.ndjson")).readAll()
                    if (events.any { it.op is ChatEventOp.ChatTombstoned }) {
                        events.mapNotNullTo(removedIds) {
                            (it.op as? ChatEventOp.ChatTombstoned)?.chatId
                        }
                        chatDir.deleteRecursively()
                    }
                }
        }
        return removedIds.size
    }

    fun graveyardParents(rootDir: File): List<File> = buildList {
        add(rootDir)
        rootDir.listFiles()?.filterTo(this) {
            it.isDirectory && it.name.startsWith("profile_")
        }
    }
}
