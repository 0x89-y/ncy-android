package com.ncy.notechaty.security

import java.security.MessageDigest
import java.security.SecureRandom

internal object PinHasher {

    data class HashedPin(val hash: String, val saltB64: String)

    fun newHash(pin: String): HashedPin {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return HashedPin(hash(pin, salt), encode(salt))
    }

    fun verify(pin: String, savedHash: String, savedSaltB64: String): Boolean {
        val salt = decode(savedSaltB64) ?: return false
        return hash(pin, salt) == savedHash
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        return encode(md.digest())
    }

    private fun encode(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    private fun decode(s: String): ByteArray? = try {
        android.util.Base64.decode(s, android.util.Base64.NO_WRAP)
    } catch (_: IllegalArgumentException) {
        null
    }
}
