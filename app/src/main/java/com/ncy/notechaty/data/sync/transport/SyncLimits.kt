package com.ncy.notechaty.data.sync.transport

import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** Resource ceilings shared by Android's sync client and phone-host server. */
object SyncLimits {
    const val MAX_TEXT_FRAME_CHARS = 1024 * 1024
    const val MAX_WIRE_FRAME_CHARS = 1400 * 1024
    /**
     * Byte ceiling the WebSocket library enforces *while* receiving, before any
     * text check can run. Sealed frames are pure ASCII, so bytes equal chars; the
     * headroom covers a non-ASCII device name in the plaintext Hello.
     */
    const val MAX_WIRE_FRAME_BYTES = MAX_WIRE_FRAME_CHARS + 64 * 1024
    /** Temporary universal safety ceiling. Automatic-download policy is configured separately. */
    const val MAX_BLOB_BYTES = 1024 * 1024 * 1024
    const val MAX_CHUNK_BYTES = 64 * 1024
    const val MAX_BASE64_CHARS = 90 * 1024
    const val MAX_PENDING_BLOBS = 8
    /** In step with ncy-core's `TRANSFER_TIMEOUT_SECS` — see the note there. */
    const val TRANSFER_TIMEOUT_MS = 180_000L
    const val TRANSFER_SWEEP_MS = 15_000L

    fun newBlobTempFile(baseDir: File): File {
        val dir = File(baseDir, ".sync_tmp").apply { mkdirs() }
        val staleBefore = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        dir.listFiles()?.filter { it.isFile && it.lastModified() < staleBefore }?.forEach { it.delete() }
        return File.createTempFile("blob_", ".part", dir)
    }

    /**
     * Where a partially received blob waits between attempts.
     *
     * Named by hash rather than randomly, so a later attempt can find what an
     * earlier one already wrote. A transfer that died at 90% used to start again
     * from zero, which on a link bad enough to break it once could mean it never
     * completed at all.
     */
    fun blobPartialFile(baseDir: File, hash: String, mediaType: String): File {
        val dir = File(baseDir, ".sync_tmp").apply { mkdirs() }
        return File(dir, "partial_$hash.$mediaType.part")
    }

    private val claimedPartials = ConcurrentHashMap.newKeySet<String>()

    /**
     * Take exclusive ownership of a partial before opening it for append.
     *
     * The name is shared by design, so the client role, the phone-host role, and
     * every peer session would otherwise all resolve the same file for one hash —
     * two appenders interleave their bytes and the result never verifies. Pair
     * each successful claim with [releasePartial] once the transfer stops.
     */
    fun claimPartial(file: File): Boolean = claimedPartials.add(file.absolutePath)

    fun releasePartial(file: File) {
        claimedPartials.remove(file.absolutePath)
    }

    /**
     * Delete transfer leftovers nothing has touched for a day.
     *
     * Partials are kept on purpose so a broken transfer can resume, which means
     * only age retires them — a download the user never returns to would otherwise
     * sit there forever. Every write refreshes the file's timestamp, so a transfer
     * being resumed periodically is never swept out from under itself.
     *
     * Called at startup rather than per transfer: it used to ride along inside
     * [newBlobTempFile], which the fetch paths no longer call.
     */
    fun sweepStaleBlobTemps(baseDir: File) {
        val dir = File(baseDir, ".sync_tmp")
        if (!dir.isDirectory) return
        val staleBefore = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        dir.listFiles()
            ?.filter { it.isFile && it.lastModified() < staleBefore }
            ?.forEach { it.delete() }
    }
}
