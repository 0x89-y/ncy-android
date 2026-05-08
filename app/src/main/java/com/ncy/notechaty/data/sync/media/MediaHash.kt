package com.ncy.notechaty.data.sync.media

import java.io.File
import java.security.MessageDigest

/**
 * SHA-256 helpers for content-addressing media files. Hashes are lowercase hex strings —
 * 64 chars long. Used to name files on disk so the same byte content always lands at the
 * same path on every device.
 */
object MediaHash {

    private val HEX = "0123456789abcdef".toCharArray()
    private val PATTERN = Regex("[0-9a-f]{64}")

    fun ofFile(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return toHex(md.digest())
    }

    fun ofBytes(bytes: ByteArray): String =
        toHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun isValidHash(s: String?): Boolean = s != null && PATTERN.matches(s)

    /**
     * Extract the SHA-256 hash from a content-addressed media reference like
     * `voice/<hash>.m4a` or `images/<hash>.jpg`. Returns null if the reference doesn't
     * follow the post-phase-2b naming convention (legacy random-named media).
     */
    fun extractHash(reference: String?): String? {
        if (reference == null) return null
        val name = reference.substringAfterLast('/')
        val stem = name.substringBeforeLast('.', missingDelimiterValue = name)
        return if (PATTERN.matches(stem)) stem else null
    }

    private fun toHex(bytes: ByteArray): String {
        val out = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val b = bytes[i].toInt() and 0xFF
            out[i * 2] = HEX[b shr 4]
            out[i * 2 + 1] = HEX[b and 0x0F]
        }
        return String(out)
    }
}
