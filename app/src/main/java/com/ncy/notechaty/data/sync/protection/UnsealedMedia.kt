package com.ncy.notechaty.data.sync.protection

import java.io.File
import java.util.UUID

/**
 * Opened copies of a protected chat's sealed attachments (§11.8), for the views
 * that need a real file: the image loader, the media player, a share intent.
 *
 * They live in the app's cache, never next to the sealed originals, and are wiped
 * when the app locks and at every start — so a plain copy exists only while the
 * chat it belongs to is unlocked.
 */
object UnsealedMedia {
    @Volatile private var dir: File? = null

    /** Call from Application.onCreate. Clears anything a previous run left behind. */
    fun init(cacheDir: File) {
        dir = File(cacheDir, "unsealed").also { it.deleteRecursively() }
    }

    fun clear() {
        dir?.deleteRecursively()
    }

    /**
     * A readable version of [file]: itself when plain, otherwise an opened copy.
     * Null when it is sealed and [key] is missing or does not open it.
     */
    fun readable(file: File, key: ByteArray?): File? {
        if (!SealedBlob.isSealed(file)) return file
        key ?: return null
        val root = dir ?: return null
        root.mkdirs()
        // Named after the sealed file: its hash is unique, and the extension keeps
        // MIME detection working.
        val target = File(root, file.name)
        if (target.isFile) return target
        val partial = File(root, "${UUID.randomUUID()}.part")
        return runCatching {
            SealedBlob.openFile(key, file, partial)
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            target
        }.getOrElse {
            partial.delete()
            null
        }
    }
}
