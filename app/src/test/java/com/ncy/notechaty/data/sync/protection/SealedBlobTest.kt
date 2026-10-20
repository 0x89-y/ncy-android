package com.ncy.notechaty.data.sync.protection

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SealedBlobTest {
    private val key = ByteArray(32) { 7 }

    private fun pattern(length: Int) = ByteArray(length) { (it % 251).toByte() }

    private fun seal(plain: ByteArray): ByteArray =
        ByteArrayOutputStream().also { SealedBlob.seal(key, ByteArrayInputStream(plain), it) }.toByteArray()

    private fun open(sealed: ByteArray, with: ByteArray = key): ByteArray =
        ByteArrayOutputStream().also {
            SealedBlob.open(with, ByteArrayInputStream(sealed), sealed.size.toLong(), it)
        }.toByteArray()

    @Test
    fun `round-trips empty, small, boundary and multi-segment payloads`() {
        val segment = SealedBlob.SEGMENT_BYTES
        listOf(0, 1, 1000, segment - 1, segment, segment + 1, 3 * segment).forEach { length ->
            val plain = pattern(length)
            assertArrayEquals("length $length", plain, open(seal(plain)))
        }
    }

    @Test
    fun `a wrong key or a cut at a segment boundary does not open`() {
        val sealed = seal(pattern(2 * SealedBlob.SEGMENT_BYTES + 10))
        assertThrows(Exception::class.java) { open(sealed, ByteArray(32) { 8 }) }
        val cut = SealedBlob.HEADER_BYTES + 2 * (SealedBlob.SEGMENT_BYTES + 16)
        assertThrows(Exception::class.java) { open(sealed.copyOf(cut)) }
    }

    /** Shared with ncy-core's `sealed_blob` tests, so both ends seal identically. */
    @Test
    fun `matches the cross-platform vectors`() {
        val vectorKey = ByteArray(32) { (it + 1).toByte() }
        val prefix = ByteArray(8) { (0xA0 + it).toByte() }
        mapOf(
            0 to "d1f61cd54b9f71bc3d5884b398dc9b196e25a9ef7a0b7af50fd7f0d72a04d44a",
            15 to "2c1f13c3f85d4dd5f34397c5d3519935cc37de57e584fc086e773e7ec6c4f346",
            SealedBlob.SEGMENT_BYTES + 5 to "cb8da46d189541f008fa5f907dc560169a3bcae0e4d2ede1059dfbb3e3a66570",
        ).forEach { (length, expected) ->
            val out = ByteArrayOutputStream()
            SealedBlob.sealWithPrefix(vectorKey, prefix, ByteArrayInputStream(pattern(length)), out)
            val hex = MessageDigest.getInstance("SHA-256").digest(out.toByteArray())
                .joinToString("") { "%02x".format(it) }
            assertEquals("length $length", expected, hex)
        }
    }
}
