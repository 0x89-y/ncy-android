package com.ncy.notechaty.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * The protection PIN is the same secret that derives the AES key for protected
 * chats, so the verifier has to be as expensive as that KDF. These cover the
 * stretched format, the legacy-hash path that keeps existing users unlocked, and
 * the rehash signal that upgrades them.
 */
class PinHasherTest {

    @Test
    fun `new hashes round-trip`() {
        val hashed = PinHasher.newHash("1234")
        assertTrue(PinHasher.verify("1234", hashed.hash, hashed.saltB64))
        assertFalse(PinHasher.verify("1235", hashed.hash, hashed.saltB64))
    }

    @Test
    fun `new hashes are stretched and self-describing`() {
        val hashed = PinHasher.newHash("1234")
        val parts = hashed.hash.split('$')
        assertEquals(3, parts.size)
        assertEquals("pbkdf2-sha256", parts[0])
        assertEquals(PinHasher.ITERATIONS, parts[1].toInt())
        assertFalse(PinHasher.needsRehash(hashed.hash))
    }

    @Test
    fun `the verifier costs the same as the chat-key KDF`() {
        // Regression guard for the original defect: a cheap verifier next to an
        // expensive KDF lets an attacker crack the PIN on the cheap side and then
        // derive the chat key from it.
        assertEquals(
            com.ncy.notechaty.data.sync.protection.ProtectionCrypto.KDF_ITERATIONS,
            PinHasher.ITERATIONS,
        )
    }

    @Test
    fun `identical pins get different hashes`() {
        assertNotEquals(PinHasher.newHash("1234").hash, PinHasher.newHash("1234").hash)
    }

    @Test
    fun `legacy sha256 hashes still verify and are flagged for rehash`() {
        val salt = ByteArray(16) { it.toByte() }
        val legacyHash = java.util.Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-256").run {
                update(salt)
                update("4321".toByteArray(Charsets.UTF_8))
                digest()
            }
        )
        val legacySalt = java.util.Base64.getEncoder().encodeToString(salt)

        assertTrue(PinHasher.verify("4321", legacyHash, legacySalt))
        assertFalse(PinHasher.verify("0000", legacyHash, legacySalt))
        assertTrue(PinHasher.needsRehash(legacyHash))
    }

    @Test
    fun `malformed stored values are rejected rather than throwing`() {
        val salt = PinHasher.newHash("1234").saltB64
        assertFalse(PinHasher.verify("1234", "not base64 !!", salt))
        assertFalse(PinHasher.verify("1234", "pbkdf2-sha256\$0\$abcd", salt))
        assertFalse(PinHasher.verify("1234", "pbkdf2-sha256\$notanumber\$abcd", salt))
        assertFalse(PinHasher.verify("1234", "unknown-scheme\$1\$2\$3", salt))
        assertFalse(PinHasher.verify("1234", PinHasher.newHash("1234").hash, "not base64 !!"))
    }
}
