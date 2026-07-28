package com.ncy.notechaty.security

import com.ncy.notechaty.data.sync.protection.ProtectionCrypto
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Verifier for the app-wide protection PIN and per-profile lock PINs.
 *
 * **Why PBKDF2 and not a bare digest.** The protection PIN is the *same* secret
 * that [ProtectionCrypto.deriveKey] stretches into the AES key for protected
 * chats (see `ChatViewModel.verifyPin`). A cheap verifier next to an expensive
 * KDF is worthless: an attacker who can read the stored hash cracks the cheap
 * side — a numeric PIN is a keyspace of ~10^4..10^6, which an unsalted-iteration
 * digest falls to in milliseconds — and then simply derives the chat key from
 * the recovered PIN. The verifier therefore has to cost the same as the KDF it
 * protects, so [ITERATIONS] tracks [ProtectionCrypto.KDF_ITERATIONS].
 *
 * **Stored format.** New hashes are `pbkdf2-sha256$<iterations>$<base64>`. Hashes
 * written before this change are a bare base64 SHA-256 with no `$`; [verify]
 * still accepts those so existing users are not locked out, and [needsRehash]
 * reports them so callers can transparently upgrade on the next successful
 * unlock. Once [needsRehash] stops firing in the wild the legacy branch can go.
 *
 * This is phone-local: it is not part of the wire protocol or the sealed envelope,
 * so the format can change freely without breaking desktop parity. (The desktop
 * keeps no verifier at all — it trial-decrypts a real sealed payload instead.)
 */
internal object PinHasher {

    /** Kept equal to the KDF guarding protected chats — see the class doc. */
    const val ITERATIONS = ProtectionCrypto.KDF_ITERATIONS

    private const val SCHEME = "pbkdf2-sha256"
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    data class HashedPin(val hash: String, val saltB64: String)

    fun newHash(pin: String): HashedPin {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return HashedPin("$SCHEME$$ITERATIONS$${encode(pbkdf2(pin, salt, ITERATIONS))}", encode(salt))
    }

    fun verify(pin: String, savedHash: String, savedSaltB64: String): Boolean {
        val salt = decode(savedSaltB64) ?: return false
        val parts = savedHash.split('$')
        val (expected, actual) = when {
            // `pbkdf2-sha256$<iterations>$<base64>` — honour the *stored* iteration
            // count so raising ITERATIONS later doesn't invalidate existing PINs.
            parts.size == 3 && parts[0] == SCHEME -> {
                val iterations = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return false
                val stored = decode(parts[2]) ?: return false
                stored to pbkdf2(pin, salt, iterations)
            }
            // Legacy: bare base64 of SHA-256(salt || pin).
            parts.size == 1 -> {
                val stored = decode(savedHash) ?: return false
                stored to legacySha256(pin, salt)
            }
            else -> return false
        }
        return MessageDigest.isEqual(expected, actual)
    }

    /** True when [savedHash] predates the PBKDF2 format and should be re-written. */
    fun needsRehash(savedHash: String): Boolean = !savedHash.startsWith("$SCHEME$")

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun legacySha256(pin: String, salt: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").run {
            update(salt)
            update(pin.toByteArray(Charsets.UTF_8))
            digest()
        }

    // `java.util.Base64` (as in TransportCrypto) rather than `android.util.Base64`:
    // minSdk is 31 so it is always available, and it keeps this object testable
    // under plain JVM unit tests. Both emit standard padded base64 with no line
    // wrapping, so hashes and salts written by the previous implementation still
    // decode unchanged.
    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

    private fun decode(s: String): ByteArray? = try {
        Base64.getDecoder().decode(s)
    } catch (_: IllegalArgumentException) {
        null
    }
}
