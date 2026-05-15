package com.ncy.notechaty.data.sync.protection

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Phase 3 — protected-chat encryption primitives. Pure crypto: no I/O, no state.
 *
 * **Algorithms.** AES-256-GCM for symmetric encryption, PBKDF2-HMAC-SHA256
 * ([KDF_ITERATIONS] iterations) for deriving the 32-byte key from the user's PIN
 * plus a per-chat salt. PBKDF2 is in `javax.crypto` so no new dependencies; the
 * iteration count is sized for casual-attacker resistance (a stolen device with the
 * encrypted log and access to a GPU can still brute-force a 4-digit PIN — the goal
 * here is "doesn't trivially leak in transit and at rest," not "secure against a
 * determined offline attacker"). Argon2id would be stronger; deferred to avoid the
 * JNI dep until we know we need it.
 *
 * **Sealed envelope.** `seal` returns a JSON-ready ciphertext object:
 *   `{ "v": 1, "nonce": "<base64-12>", "ct": "<base64>" }`
 * decoded by `open`. Encoding/decoding to/from the on-disk event payload happens at
 * the caller layer.
 */
object ProtectionCrypto {

    const val ENVELOPE_VERSION = 1
    const val KDF_ITERATIONS = 250_000
    private const val KEY_BITS = 256
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val SALT_BYTES = 16

    private val rng = SecureRandom()

    /** Generate a fresh 16-byte salt and return it as standard base64. */
    fun generateSalt(): String {
        val bytes = ByteArray(SALT_BYTES)
        rng.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Derive a 32-byte AES key from [pin] and a base64-encoded [saltB64]. Deterministic
     * — same inputs always yield the same key, which is what makes pairing-time key
     * exchange unnecessary: both devices type the same PIN and use the salt that
     * arrived via `ChatMetadataChanged`.
     */
    fun deriveKey(pin: String, saltB64: String): ByteArray {
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val spec = PBEKeySpec(pin.toCharArray(), salt, KDF_ITERATIONS, KEY_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    /** Encrypt [plaintext] under [key]; returns a `(nonce, ciphertext)` pair. */
    fun seal(key: ByteArray, plaintext: ByteArray): Sealed {
        val nonce = ByteArray(NONCE_BYTES).also { rng.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        }
        return Sealed(nonce = nonce, ciphertext = cipher.doFinal(plaintext))
    }

    /** Decrypt; returns null on tag mismatch or any other failure (wrong PIN). */
    fun open(key: ByteArray, sealed: Sealed): ByteArray? = try {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, sealed.nonce))
        }
        cipher.doFinal(sealed.ciphertext)
    } catch (_: Exception) {
        null
    }

    data class Sealed(val nonce: ByteArray, val ciphertext: ByteArray) {
        // ByteArrays don't structurally compare in data classes — override for tests.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Sealed) return false
            return nonce.contentEquals(other.nonce) && ciphertext.contentEquals(other.ciphertext)
        }
        override fun hashCode(): Int = 31 * nonce.contentHashCode() + ciphertext.contentHashCode()
    }
}
