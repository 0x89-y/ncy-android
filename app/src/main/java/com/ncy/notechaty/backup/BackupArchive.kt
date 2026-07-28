package com.ncy.notechaty.backup

import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Guards for extracting an untrusted backup archive.
 *
 * A backup zip is attacker-reachable input: the user picks the file and it may
 * have come from anywhere. Extraction therefore has to bound both *where* entries
 * can land and *how much* they can expand to.
 */
internal object BackupArchive {

    /** Ceilings applied to a single archive. Generous — these catch abuse, not big backups. */
    const val MAX_ENTRIES = 100_000
    const val MAX_BYTES = 4L * 1024 * 1024 * 1024

    /**
     * Resolve [entryName] under [root], or `null` if it would escape.
     *
     * `File(root, entry.name)` on its own is zip-slip: an entry named
     * `../../shared_prefs/ncy_phone_pairing.xml` resolves outside the import
     * sandbox and into app-private storage, which holds the pairing secrets, the
     * device identity, and the PIN hash. Overwriting those would hand an attacker
     * sync access to every chat.
     *
     * Legitimate entries are relative, `/`-separated, and may nest
     * (`profile_x/chat_y/events.ndjson`), so nesting is allowed and only
     * containment is enforced.
     */
    fun safeTarget(root: File, entryName: String): File? {
        val normalized = entryName.replace('\\', '/')
        if (normalized.isEmpty() || normalized.startsWith("/") || normalized.contains(':')) return null
        if (normalized.split('/').any { it == ".." }) return null
        val rootPath = root.canonicalFile.toPath()
        val targetPath = File(root, normalized).canonicalFile.toPath()
        // `Path.startsWith` is segment-aware; a plain string prefix test would
        // accept a sibling directory whose name merely begins with the sandbox's.
        if (!targetPath.startsWith(rootPath) || targetPath == rootPath) return null
        return targetPath.toFile()
    }

    /**
     * Copy at most [limit] bytes, failing rather than truncating. A zip entry
     * declares its uncompressed size, but that declaration is attacker-controlled,
     * so the ceiling is enforced on what actually comes out of the stream.
     */
    fun copyAtMost(input: InputStream, out: OutputStream, limit: Long): Long {
        check(limit > 0) { "Backup archive expands beyond $MAX_BYTES bytes" }
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var written = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            written += read
            check(written <= limit) { "Backup archive expands beyond $MAX_BYTES bytes" }
            out.write(buffer, 0, read)
        }
        return written
    }
}
