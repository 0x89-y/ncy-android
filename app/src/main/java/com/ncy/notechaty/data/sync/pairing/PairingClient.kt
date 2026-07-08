package com.ncy.notechaty.data.sync.pairing

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot WebSocket handshake against a mDNS-discovered desktop. Performs a
 * P-256 ECDH key agreement in-band so the pairing JSON (containing the
 * long-term PSK) is AES-256-GCM sealed on the wire — passive LAN sniffers
 * see only ciphertext.
 *
 * The handshake also derives a 6-digit safety code from the ECDH shared
 * secret. That code is surfaced back to the UI via the `SafetyCodeReady`
 * intermediate callback so the user can eyeball it against the same code
 * shown in the desktop's confirm modal (defeats active MITM: an attacker
 * doing two separate ECDHs produces different codes on the two sides).
 *
 * See `ncy-core/src/sync/{wire.rs, pair_crypto.rs, server.rs}` for the
 * matching server code.
 */
@Singleton
class PairingClient @Inject constructor() {

    private val tag = "PairingClient"
    private val gson = Gson()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(70, TimeUnit.SECONDS) // > server's 60s decision window
        .build()

    sealed class Result {
        /** Handshake complete; pairing JSON is plaintext, ready for import. */
        data class Success(val pairingJson: String) : Result()
        data class Rejected(val reason: String) : Result()
        data class Failed(val message: String) : Result()
    }

    /**
     * Open the pair WebSocket. Runs ECDH → hands the derived safety code
     * back through `onSafetyCode` (so the UI can display it) → awaits the
     * desktop's decision → decrypts the pairing JSON on accept.
     *
     * Blocks the calling coroutine; call from a background dispatcher.
     * Caps total wait at 70s (5s connect + 60s server decision + slack).
     */
    suspend fun request(
        host: String,
        port: Int,
        phoneId: String,
        phoneName: String,
        onSafetyCode: (String) -> Unit,
    ): Result {
        // 1. Generate ephemeral P-256 keypair.
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        val phoneKeyPair = kpg.generateKeyPair()
        val phonePub = phoneKeyPair.public as ECPublicKey
        val phonePubBytes = encodeSec1Uncompressed(phonePub)
        val phonePubB64 = Base64.encodeToString(phonePubBytes, Base64.NO_WRAP)

        val request = Request.Builder()
            .url("ws://$host:$port/pair")
            .build()

        val outcome = CompletableDeferred<Result>()
        var socket: WebSocket? = null
        // Stashed between PairChallenge and PairAccept — the desktop's pubkey
        // arrives in the challenge (immediately), the ciphertext arrives in
        // the accept (post-user-confirm), and both share the same AES key.
        var storedAesKey: ByteArray? = null

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i(tag, "connected to $host:$port")
                socket = webSocket
                val frame = JsonObject().apply {
                    addProperty("type", "PairRequest")
                    addProperty("phoneId", phoneId)
                    addProperty("phoneName", phoneName)
                    addProperty("phonePubKey", phonePubB64)
                }
                webSocket.send(gson.toJson(frame))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val obj = try {
                    JsonParser.parseString(text).asJsonObject
                } catch (e: Exception) {
                    return
                }
                when (obj["type"]?.asString) {
                    "Challenge" -> Unit // sync-protocol Challenge, irrelevant to us
                    "PairChallenge" -> {
                        // Immediate: server sends this before its user confirms,
                        // so we can derive + display the safety code *in
                        // parallel* with the desktop's modal — that's the whole
                        // point of the 4-message flow.
                        val desktopPubB64 = obj["desktopPubKey"]?.asString
                        if (desktopPubB64.isNullOrBlank()) {
                            outcome.complete(Result.Failed("PairChallenge missing desktopPubKey"))
                            webSocket.close(1000, "done")
                            return
                        }
                        try {
                            val desktopPubBytes = Base64.decode(desktopPubB64, Base64.NO_WRAP)
                            val desktopPub = decodeSec1Uncompressed(desktopPubBytes, phonePub.params)
                            val shared = ecdhSharedSecret(phoneKeyPair.private, desktopPub)
                            val (aesKey, safetyCode) = hkdfKeyAndCode(shared)
                            storedAesKey = aesKey
                            onSafetyCode(safetyCode)
                        } catch (e: Exception) {
                            Log.w(tag, "ECDH failed", e)
                            outcome.complete(Result.Failed("handshake failed: ${e.message}"))
                            webSocket.close(1000, "done")
                        }
                    }
                    "PairAccept" -> {
                        val key = storedAesKey
                        if (key == null) {
                            outcome.complete(Result.Failed("PairAccept before PairChallenge"))
                            webSocket.close(1000, "done")
                            return
                        }
                        val nonceB64 = obj["nonce"]?.asString
                        val ciphertextB64 = obj["ciphertext"]?.asString
                        if (nonceB64.isNullOrBlank() || ciphertextB64.isNullOrBlank()) {
                            outcome.complete(Result.Failed("PairAccept missing crypto fields"))
                            webSocket.close(1000, "done")
                            return
                        }
                        try {
                            val nonce = Base64.decode(nonceB64, Base64.NO_WRAP)
                            val ciphertext = Base64.decode(ciphertextB64, Base64.NO_WRAP)
                            val plaintext = aesGcmOpen(key, nonce, ciphertext)
                            outcome.complete(Result.Success(String(plaintext, Charsets.UTF_8)))
                        } catch (e: Exception) {
                            Log.w(tag, "decrypt failed", e)
                            outcome.complete(Result.Failed("decrypt failed: ${e.message}"))
                        }
                        webSocket.close(1000, "done")
                    }
                    "PairReject" -> {
                        val reason = obj["reason"]?.asString ?: "rejected"
                        outcome.complete(Result.Rejected(reason))
                        webSocket.close(1000, "done")
                    }
                    "Error" -> {
                        val msg = obj["message"]?.asString ?: "server error"
                        outcome.complete(Result.Failed(msg))
                        webSocket.close(1000, "done")
                    }
                    else -> Unit
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w(tag, "connection failed", t)
                if (!outcome.isCompleted) {
                    outcome.complete(Result.Failed(t.message ?: "connection failed"))
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!outcome.isCompleted) {
                    outcome.complete(Result.Failed("connection closed before decision"))
                }
            }
        }

        httpClient.newWebSocket(request, listener)

        // Flow (post-2026-07-08 split): PairRequest → PairChallenge (server
        // sends its pubkey immediately; phone displays safety code) →
        // desktop user confirms → PairAccept (server sends encrypted
        // pairing JSON). Phone user sees the code in parallel with the
        // desktop, not retroactively.

        return try {
            withTimeout(70_000L) { outcome.await() }
        } catch (e: TimeoutCancellationException) {
            socket?.close(1000, "timeout")
            Result.Failed("timed out waiting for desktop confirmation")
        } catch (e: Exception) {
            Result.Failed(e.message ?: "unknown error")
        }
    }

    // ------------------------------------------------------------------
    // Crypto helpers. Everything below uses the JDK's built-in providers —
    // P-256 ECDH via `KeyAgreement`, HMAC-SHA256 via `Mac`, AES-256-GCM via
    // `Cipher`. All API 26+. No BouncyCastle needed.
    // ------------------------------------------------------------------

    /** SEC1 uncompressed encoding: `0x04 || X (32B) || Y (32B)`. Same format
     *  the Rust `p256` crate's `to_sec1_bytes()` produces. */
    private fun encodeSec1Uncompressed(pub: ECPublicKey): ByteArray {
        val w = pub.w
        val x = bigIntToFixed(w.affineX, 32)
        val y = bigIntToFixed(w.affineY, 32)
        return byteArrayOf(0x04) + x + y
    }

    private fun bigIntToFixed(v: BigInteger, len: Int): ByteArray {
        val raw = v.toByteArray()
        val out = ByteArray(len)
        // `toByteArray` produces the two's-complement big-endian representation,
        // which may have a leading 0x00 (sign bit) or be shorter than `len`.
        val start = if (raw.size > len) raw.size - len else 0
        val length = minOf(raw.size, len)
        System.arraycopy(raw, start, out, len - length, length)
        return out
    }

    private fun decodeSec1Uncompressed(bytes: ByteArray, params: ECParameterSpec): PublicKey {
        require(bytes.isNotEmpty() && bytes[0] == 0x04.toByte()) { "not SEC1 uncompressed" }
        require(bytes.size == 65) { "not P-256 uncompressed (65 bytes expected)" }
        val x = BigInteger(1, bytes.copyOfRange(1, 33))
        val y = BigInteger(1, bytes.copyOfRange(33, 65))
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(ECPublicKeySpec(ECPoint(x, y), params))
    }

    private fun ecdhSharedSecret(privateKey: PrivateKey, remotePub: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(privateKey)
        ka.doPhase(remotePub, true)
        return ka.generateSecret() // 32 bytes for P-256
    }

    /** Same domain-separation label as `ncy-core/src/sync/pair_crypto.rs`. */
    private val HKDF_INFO = "ncy-pair-v1: aes-256-gcm + 6-digit safety code".toByteArray(Charsets.UTF_8)

    /** HKDF-SHA256, salt = empty (both sides contribute unique ephemeral keys). */
    private fun hkdfKeyAndCode(sharedSecret: ByteArray): Pair<ByteArray, String> {
        val mac = Mac.getInstance("HmacSHA256")
        // Extract: PRK = HMAC(salt=zeroes32, ikm=sharedSecret)
        mac.init(SecretKeySpec(ByteArray(32), "HmacSHA256"))
        val prk = mac.doFinal(sharedSecret)

        // Expand to 36 bytes: 32 for AES key, 4 for the safety-code seed.
        // OKM = T(1) || T(2), where T(1) = HMAC(PRK, info || 0x01),
        // T(2) = HMAC(PRK, T(1) || info || 0x02), truncated to 36 bytes total.
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(HKDF_INFO)
        mac.update(0x01)
        val t1 = mac.doFinal()

        mac.reset()
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(t1)
        mac.update(HKDF_INFO)
        mac.update(0x02)
        val t2 = mac.doFinal()

        val okm = t1 + t2 // 64 bytes; we need 36
        val aesKey = okm.copyOfRange(0, 32)
        val seedBytes = okm.copyOfRange(32, 36)
        val seed = ((seedBytes[0].toInt() and 0xff) shl 24) or
            ((seedBytes[1].toInt() and 0xff) shl 16) or
            ((seedBytes[2].toInt() and 0xff) shl 8) or
            (seedBytes[3].toInt() and 0xff)
        val codeNum = (seed.toLong() and 0xffffffffL) % 1_000_000L
        val safetyCode = "%06d".format(codeNum)
        return aesKey to safetyCode
    }

    private fun aesGcmOpen(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, nonce),
        )
        return cipher.doFinal(ciphertext)
    }
}
