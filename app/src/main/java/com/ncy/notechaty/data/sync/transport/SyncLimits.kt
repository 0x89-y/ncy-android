package com.ncy.notechaty.data.sync.transport

import java.io.File

/** Resource ceilings shared by Android's sync client and phone-host server. */
object SyncLimits {
    const val MAX_TEXT_FRAME_CHARS = 1024 * 1024
    const val MAX_WIRE_FRAME_CHARS = 1400 * 1024
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
}
