package com.ncy.notechaty.data.serialization

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Something worked out from a chat's `chat.json`, kept until that file changes.
 *
 * The chat list, Favorites, Reminders and the reminder alarms each used to read
 * every chat's whole file — every message — after every change anywhere. With a long
 * history that is most of the work the app does. A chat that did not change does not
 * need reading again, so each reader keeps the little it needs (a summary, the
 * starred messages, the reminder times) next to how the file looked when read.
 *
 * "Changed" is any of: [ChatFileWrites] counted a write through [ChatSerializer], the
 * modification time moved, or the size changed. The write count catches two writes
 * within the file system's timestamp resolution that happen to leave the size equal;
 * the time and size catch writes from elsewhere, such as a restored backup.
 */
class ChatFileCache<T : Any> {
    private data class Stamp(val writes: Long, val modified: Long, val length: Long)
    private class Entry<T>(val stamp: Stamp, val value: T)

    private val entries = ConcurrentHashMap<String, Entry<T>>()

    /**
     * The cached value for [file], or [compute]'s result when the file changed or was
     * never read. A missing file is not cached: [compute] may rebuild it, and the next
     * call then reads what it wrote.
     */
    fun get(file: File, compute: () -> T?): T? {
        val key = file.path
        val before = stampOf(file)
        if (before == null) {
            entries.remove(key)
            return compute()
        }
        entries[key]?.takeIf { it.stamp == before }?.let { return it.value }
        val value = compute() ?: run {
            entries.remove(key)
            return null
        }
        // Stamped as it was before reading: a write during the read leaves a stamp
        // that no longer matches, so the next call reads again rather than keeping
        // something older than the file.
        entries[key] = Entry(before, value)
        return value
    }

    fun clear() = entries.clear()

    private fun stampOf(file: File): Stamp? {
        if (!file.isFile) return null
        return Stamp(ChatFileWrites.of(file), file.lastModified(), file.length())
    }
}

/** Counts writes to each chat file made through [ChatSerializer], for [ChatFileCache]. */
object ChatFileWrites {
    private val counts = ConcurrentHashMap<String, Long>()

    fun record(file: File) {
        counts.merge(file.path, 1L, Long::plus)
    }

    fun of(file: File): Long = counts[file.path] ?: 0L
}
