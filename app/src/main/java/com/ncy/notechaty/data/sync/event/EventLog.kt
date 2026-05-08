package com.ncy.notechaty.data.sync.event

import java.io.File
import java.io.FileOutputStream

/**
 * Per-chat append-only event log stored as NDJSON. Writes are fsynced so a crash mid-mutation
 * never leaves the log inconsistent — a partial line at EOF is the worst case and is
 * filtered out on read.
 *
 * The log lives at `<chat dir>/events.ndjson`. Reads always go through here when the
 * materialized view needs to be rebuilt.
 */
class EventLog(
    private val file: File,
    private val codec: ChatEventCodec = ChatEventCodec()
) {

    fun exists(): Boolean = file.exists()

    /** Append a single event, fsync, return it. Caller is responsible for HLC ordering. */
    @Synchronized
    fun append(event: ChatEvent) {
        file.parentFile?.mkdirs()
        FileOutputStream(file, /* append = */ true).use { fos ->
            fos.write(codec.encode(event).toByteArray(Charsets.UTF_8))
            fos.write(NEWLINE)
            fos.flush()
            fos.fd.sync()
        }
    }

    /** Atomically write the full log (used by migration, snapshotting, import). */
    @Synchronized
    fun replaceAll(events: List<ChatEvent>) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(tmp).use { fos ->
            events.forEach {
                fos.write(codec.encode(it).toByteArray(Charsets.UTF_8))
                fos.write(NEWLINE)
            }
            fos.flush()
            fos.fd.sync()
        }
        if (!tmp.renameTo(file)) {
            // Fallback for cross-mount or stale target.
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
    }

    /** Read events with HLC strictly greater than [since]. Pass null for the entire log. */
    fun readSince(since: HlcTimestamp?): List<ChatEvent> {
        val all = readAll()
        return if (since == null) all else all.filter { it.hlc > since }
    }

    /** Read every event in append order. Skips malformed lines (e.g. a torn final line). */
    fun readAll(): List<ChatEvent> {
        if (!file.exists()) return emptyList()
        return file.useLines { lines ->
            lines.mapNotNull { line ->
                if (line.isBlank()) null else runCatching { codec.decode(line) }.getOrNull()
            }.toList()
        }
    }

    /** The largest HLC currently in the log, or null if empty. */
    fun lastHlc(): HlcTimestamp? = readAll().maxByOrNull { it.hlc }?.hlc

    private companion object {
        val NEWLINE = byteArrayOf('\n'.code.toByte())
    }
}
