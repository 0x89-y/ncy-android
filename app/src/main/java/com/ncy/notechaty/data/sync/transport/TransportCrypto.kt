package com.ncy.notechaty.data.sync.transport

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Mandatory PSK-derived, directional authenticated transport for sync frames. */
class TransportCrypto private constructor(
    private val sendKey: ByteArray,
    private val receiveKey: ByteArray,
    private val codec: SyncWireCodec,
) {
    private var sendSeq = 0L
    private var receiveSeq = 0L

    @Synchronized
    fun seal(message: SyncWireMessage): SyncWireMessage.Secure {
        require(message !is SyncWireMessage.Secure) { "Secure envelopes cannot be nested" }
        val plaintext = codec.encode(message).toByteArray(StandardCharsets.UTF_8)
        require(plaintext.size <= SyncLimits.MAX_TEXT_FRAME_CHARS) { "Inner frame exceeds 1 MiB" }
        val seq = sendSeq
        require(seq >= 0) { "Secure send sequence exhausted" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(sendKey, "AES"), GCMParameterSpec(128, nonce(seq)))
            updateAAD(aad(seq))
        }
        val encrypted = cipher.doFinal(plaintext)
        sendSeq++
        return SyncWireMessage.Secure(seq, Base64.getEncoder().encodeToString(encrypted))
    }

    @Synchronized
    fun open(envelope: SyncWireMessage): SyncWireMessage {
        require(envelope is SyncWireMessage.Secure) { "Plaintext frame received after encrypted handshake" }
        require(envelope.seq == receiveSeq) { "Unexpected secure sequence ${envelope.seq}; expected $receiveSeq" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(receiveKey, "AES"), GCMParameterSpec(128, nonce(envelope.seq)))
            updateAAD(aad(envelope.seq))
        }
        val plaintext = cipher.doFinal(Base64.getDecoder().decode(envelope.data))
        require(plaintext.size <= SyncLimits.MAX_TEXT_FRAME_CHARS) { "Inner frame exceeds 1 MiB" }
        val message = codec.decode(plaintext.toString(StandardCharsets.UTF_8))
        require(message !is SyncWireMessage.Secure) { "Nested secure envelope rejected" }
        receiveSeq++
        return message
    }

    companion object {
        const val TRANSPORT = "psk-aead-v1"
        private val AUTH_LABEL = "ncy-sync-auth-v2\u0000".toByteArray(StandardCharsets.UTF_8)
        private val HKDF_LABEL = "ncy-sync-transport-v1\u0000".toByteArray(StandardCharsets.UTF_8)
        private val AAD_LABEL = "ncy-sync-secure-v1\u0000".toByteArray(StandardCharsets.UTF_8)

        fun randomClientNonce(): String = ByteArray(32).also(SecureRandom()::nextBytes)
            .let(Base64.getEncoder()::encodeToString)

        fun sign(secret64: String, serverNonce64: String, clientNonce64: String, deviceId: String): String? = runCatching {
            val mac = Mac.getInstance("HmacSHA256").apply {
                init(SecretKeySpec(Base64.getDecoder().decode(secret64), "HmacSHA256"))
            }
            mac.update(AUTH_LABEL)
            mac.update(Base64.getDecoder().decode(serverNonce64))
            mac.update(Base64.getDecoder().decode(clientNonce64))
            mac.update(deviceId.toByteArray(StandardCharsets.UTF_8))
            mac.update(0)
            mac.update(TRANSPORT.toByteArray(StandardCharsets.UTF_8))
            Base64.getEncoder().encodeToString(mac.doFinal())
        }.getOrNull()

        fun verify(secret64: String, serverNonce64: String, clientNonce64: String, deviceId: String, auth64: String): Boolean {
            val expected = sign(secret64, serverNonce64, clientNonce64, deviceId) ?: return false
            return runCatching { MessageDigest.isEqual(Base64.getDecoder().decode(expected), Base64.getDecoder().decode(auth64)) }.getOrDefault(false)
        }

        fun forClient(secret64: String, serverNonce64: String, clientNonce64: String, codec: SyncWireCodec = SyncWireCodec()): TransportCrypto {
            val (c2s, s2c) = derive(secret64, serverNonce64, clientNonce64)
            return TransportCrypto(c2s, s2c, codec)
        }

        fun forServer(secret64: String, serverNonce64: String, clientNonce64: String, codec: SyncWireCodec = SyncWireCodec()): TransportCrypto {
            val (c2s, s2c) = derive(secret64, serverNonce64, clientNonce64)
            return TransportCrypto(s2c, c2s, codec)
        }

        private fun derive(secret64: String, serverNonce64: String, clientNonce64: String): Pair<ByteArray, ByteArray> {
            val secret = Base64.getDecoder().decode(secret64)
            val serverNonce = Base64.getDecoder().decode(serverNonce64)
            val clientNonce = Base64.getDecoder().decode(clientNonce64)
            require(secret.size >= 32 && serverNonce.size == 16 && clientNonce.size == 32) { "Invalid transport key material length" }
            val prk = hmac(serverNonce, secret)
            val info = HKDF_LABEL + clientNonce
            val keys = hkdfExpand(prk, info, 64)
            return keys.copyOfRange(0, 32) to keys.copyOfRange(32, 64)
        }

        private fun hkdfExpand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
            val output = ByteArray(length)
            var previous = ByteArray(0)
            var offset = 0
            var counter = 1
            while (offset < length) {
                previous = hmac(prk, previous + info + byteArrayOf(counter.toByte()))
                val count = minOf(previous.size, length - offset)
                previous.copyInto(output, offset, 0, count)
                offset += count
                counter++
            }
            return output
        }

        private fun hmac(key: ByteArray, data: ByteArray): ByteArray = Mac.getInstance("HmacSHA256").run {
            init(SecretKeySpec(key, "HmacSHA256")); doFinal(data)
        }

        private fun nonce(seq: Long): ByteArray = ByteBuffer.allocate(12).putInt(0).putLong(seq).array()
        private fun aad(seq: Long): ByteArray = AAD_LABEL + ByteBuffer.allocate(8).putLong(seq).array()
    }
}
