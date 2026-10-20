package com.ncy.notechaty.data.sync.protection

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Sealed media for protected chats (protocol §11.8).
 *
 * A protected chat's message text has always been sealed, but its images, voice
 * notes and files were stored and synced as plain bytes. Attachments are now sealed
 * with the chat key too, *before* they are hashed: the content address is the hash
 * of the sealed bytes, so transfer, verification and relaying by devices without
 * the key all work unchanged.
 *
 * The STREAM construction over AES-256-GCM, so a large file never has to fit in
 * memory and segments cannot be dropped, reordered or truncated:
 * ```
 * "NCYS" 0x01 | prefix (8 random bytes) | segment 0 | segment 1 | …
 * segment i = AES-256-GCM(key, nonce = prefix ‖ be32(i), aad = "NCYS" 0x01 ‖ be32(i) ‖ last?1:0)
 * ```
 * Every segment holds [SEGMENT_BYTES] of plaintext except the last. Byte-for-byte
 * identical to ncy-core's `sealed_blob.rs`.
 */
object SealedBlob {
    private val MAGIC = byteArrayOf('N'.code.toByte(), 'C'.code.toByte(), 'Y'.code.toByte(), 'S'.code.toByte(), 1)
    private const val PREFIX_BYTES = 8
    const val HEADER_BYTES = 5 + PREFIX_BYTES
    const val SEGMENT_BYTES = 64 * 1024
    private const val TAG_BYTES = 16
    private const val SEALED_SEGMENT_BYTES = SEGMENT_BYTES + TAG_BYTES
    private val rng = SecureRandom()

    /** Does [file] start like a sealed blob? A plain file that happens to fails to open. */
    fun isSealed(file: File): Boolean {
        if (!file.isFile || file.length() < HEADER_BYTES + TAG_BYTES) return false
        val head = ByteArray(MAGIC.size)
        val read = file.inputStream().use { readFull(it, head) }
        return read == MAGIC.size && head.contentEquals(MAGIC)
    }

    fun seal(key: ByteArray, input: InputStream, output: OutputStream) {
        val prefix = ByteArray(PREFIX_BYTES).also(rng::nextBytes)
        sealWithPrefix(key, prefix, input, output)
    }

    internal fun sealWithPrefix(key: ByteArray, prefix: ByteArray, input: InputStream, output: OutputStream) {
        output.write(MAGIC)
        output.write(prefix)
        // Read one segment ahead: a segment is last only if nothing follows it.
        var current = ByteArray(SEGMENT_BYTES)
        var next = ByteArray(SEGMENT_BYTES)
        var currentLen = readFull(input, current)
        var index = 0
        while (true) {
            val nextLen = if (currentLen == SEGMENT_BYTES) readFull(input, next) else 0
            val last = nextLen == 0
            output.write(cipher(Cipher.ENCRYPT_MODE, key, prefix, index, last).doFinal(current, 0, currentLen))
            if (last) break
            val swap = current
            current = next
            next = swap
            currentLen = nextLen
            index++
        }
        output.flush()
    }

    /**
     * Open a sealed blob of [totalLength] bytes. Throws on a wrong key or any
     * tampering, having written only authenticated segments.
     */
    fun open(key: ByteArray, input: InputStream, totalLength: Long, output: OutputStream) {
        val header = ByteArray(HEADER_BYTES)
        require(readFull(input, header) == HEADER_BYTES && header.copyOf(MAGIC.size).contentEquals(MAGIC)) {
            "not a sealed blob"
        }
        val prefix = header.copyOfRange(MAGIC.size, HEADER_BYTES)
        val body = totalLength - HEADER_BYTES
        require(body >= TAG_BYTES) { "sealed blob is truncated" }
        val segments = (body + SEALED_SEGMENT_BYTES - 1) / SEALED_SEGMENT_BYTES
        val lastLen = (body - (segments - 1) * SEALED_SEGMENT_BYTES).toInt()
        require(lastLen >= TAG_BYTES) { "sealed blob is truncated" }
        val buffer = ByteArray(SEALED_SEGMENT_BYTES)
        for (i in 0 until segments) {
            val last = i == segments - 1
            val len = if (last) lastLen else SEALED_SEGMENT_BYTES
            require(readFull(input, buffer, len) == len) { "sealed blob is truncated" }
            output.write(cipher(Cipher.DECRYPT_MODE, key, prefix, i.toInt(), last).doFinal(buffer, 0, len))
        }
        output.flush()
    }

    fun sealFile(key: ByteArray, source: File, target: File) {
        source.inputStream().buffered().use { input ->
            target.outputStream().buffered().use { output -> seal(key, input, output) }
        }
    }

    /** Open [source] into [target]; on failure nothing partial is left behind. */
    fun openFile(key: ByteArray, source: File, target: File) {
        try {
            source.inputStream().buffered().use { input ->
                target.outputStream().buffered().use { output -> open(key, input, source.length(), output) }
            }
        } catch (e: Exception) {
            target.delete()
            throw e
        }
    }

    private fun cipher(mode: Int, key: ByteArray, prefix: ByteArray, index: Int, last: Boolean): Cipher {
        val nonce = ByteBuffer.allocate(12).put(prefix).putInt(index).array()
        val aad = ByteBuffer.allocate(10).put(MAGIC).putInt(index).put(if (last) 1 else 0).array()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BYTES * 8, nonce))
            updateAAD(aad)
        }
    }

    private fun readFull(input: InputStream, buffer: ByteArray, length: Int = buffer.size): Int {
        var filled = 0
        while (filled < length) {
            val n = input.read(buffer, filled, length - filled)
            if (n <= 0) break
            filled += n
        }
        return filled
    }
}
