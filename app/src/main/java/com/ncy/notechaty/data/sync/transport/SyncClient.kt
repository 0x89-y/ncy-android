package com.ncy.notechaty.data.sync.transport

import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.HlcTimestamp
import com.ncy.notechaty.data.sync.peer.SyncPeer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot WebSocket session against a paired peer. Performs the challenge/HMAC handshake,
 * then exposes a [Session] the caller uses to push events. State changes flow back through
 * the [onEvent] callback. The caller (SyncManager) is responsible for retry/backoff and for
 * deciding which events to push in what order.
 *
 * Wire path: `ws://<peer host>:<peer port>/sync`; all post-Hello application frames
 * are protected by the mandatory PSK-derived AEAD transport.
 */
@Singleton
class SyncClient @Inject constructor(
    private val frameTransport: ClientSyncTransport,
    private val settingsRepository: SettingsRepository,
) {

    private val codec = SyncWireCodec()

    sealed class Event {
        object Connecting : Event()
        data class TransportSelected(val kind: ClientSyncTransport.Kind) : Event()
        /** @param receiptEpoch the server's receipt epoch; null when it has none (§10.9). */
        data class Authenticated(val peerName: String, val receiptEpoch: String? = null) : Event()
        data class Acked(val upToHlc: HlcTimestamp, val upToReceipt: Long? = null) : Event()
        /** Server pushed an event back at us (phase 2 bidirectional). */
        data class EventReceived(val event: ChatEvent, val receipt: Long? = null) : Event()
        // Phase 2b — media blob wire frames surfaced to SyncManager for handling.
        data class BlobRequested(
            val hash: String,
            val mediaType: String,
            /** Byte to stream from, so a broken transfer resumes. */
            val offset: Long = 0L,
        ) : Event()
        data class BlobChunkReceived(
            val hash: String,
            val mediaType: String,
            val offset: Int,
            val totalSize: Int,
            val data: String,
            val isLast: Boolean
        ) : Event()
        data class BlobMissingReceived(val hash: String) : Event()
        /** Peer no longer wants a blob we are streaming — stop and free the link. */
        data class BlobCancelled(val hash: String) : Event()
        /**
         * @param authenticated false for an Error frame that arrived in plaintext,
         *   before the encrypted session — unverifiable, so never a trust failure.
         */
        data class Failed(val reason: String, val authenticated: Boolean = true) : Event()
        data class Disconnected(val reason: String = "") : Event()
    }

    interface Session {
        fun send(event: ChatEvent, receipt: Long? = null): Boolean
        fun ack(upToHlc: HlcTimestamp, upToReceipt: Long? = null): Boolean
        fun requestBlob(hash: String, mediaType: String, offset: Long = 0L): Boolean
        fun sendBlobChunk(
            hash: String,
            mediaType: String,
            offset: Int,
            totalSize: Int,
            data: String,
            isLast: Boolean
        ): Boolean
        fun sendBlobMissing(hash: String): Boolean
        /** Ask the peer to stop streaming this blob. */
        fun cancelBlob(hash: String): Boolean
        fun sendError(code: String, message: String): Boolean

        /** Bytes queued for the network but not yet written — see [ClientSyncTransport.Connection.queuedBytes]. */
        fun queuedBytes(): Long
        fun close()
    }

    /**
     * @param receiptEpoch the server epoch last seen, `""` before any; always sent,
     *   which declares that this client follows receipts (§10.9).
     * @param sinceReceipt highest server receipt applied in [receiptEpoch].
     */
    fun connect(
        peer: SyncPeer,
        localDeviceId: String,
        sinceHlc: HlcTimestamp?,
        receiptEpoch: String = "",
        sinceReceipt: Long? = null,
        onEvent: (Event) -> Unit
    ): Session {
        onEvent(Event.Connecting)

        var authenticated = false
        var closed = false
        var secure: TransportCrypto? = null
        lateinit var connection: ClientSyncTransport.Connection

        connection = frameTransport.connect(peer.host, peer.port, "sync", object : ClientSyncTransport.Listener {
            override fun onTransportSelected(kind: ClientSyncTransport.Kind) {
                onEvent(Event.TransportSelected(kind))
            }
            override fun onFrame(text: String) {
                if (text.length > SyncLimits.MAX_WIRE_FRAME_CHARS) {
                    onEvent(Event.Failed("Wire frame exceeds the encrypted transport limit"))
                    connection.close(CLOSE_PROTOCOL_ERROR, "frame too large")
                    return
                }
                val outer = try {
                    codec.decode(text)
                } catch (e: Exception) {
                    onEvent(Event.Failed("Malformed wire frame: ${e.message}"))
                    connection.close(CLOSE_PROTOCOL_ERROR, "decode failed")
                    return
                }
                val msg = try {
                    when (outer) {
                        is SyncWireMessage.Secure -> secure?.open(outer)
                            ?: throw IllegalStateException("Secure frame arrived before Challenge")
                        is SyncWireMessage.Error -> {
                            if (authenticated) throw IllegalStateException("Plaintext error after authentication")
                            outer // an auth failure cannot be encrypted without an accepted credential
                        }
                        else -> {
                            if (secure != null) throw IllegalStateException("Plaintext frame after Hello")
                            outer
                        }
                    }
                } catch (e: Exception) {
                    onEvent(Event.Failed("Encrypted transport rejected frame: ${e.message}"))
                    connection.close(CLOSE_PROTOCOL_ERROR, "secure frame rejected")
                    return
                }
                when (msg) {
                    is SyncWireMessage.Challenge -> {
                        if (msg.transport != TransportCrypto.TRANSPORT) {
                            onEvent(Event.Failed("Peer requires an incompatible or plaintext sync transport; upgrade required"))
                            connection.close(CLOSE_PROTOCOL_ERROR, "encrypted transport required")
                            return
                        }
                        val clientNonce = TransportCrypto.randomClientNonce()
                        val signed = TransportCrypto.sign(peer.secret, msg.nonce, clientNonce, localDeviceId)
                        if (signed == null) {
                            onEvent(Event.Failed("Could not sign challenge"))
                            connection.close(CLOSE_PROTOCOL_ERROR, "auth failed")
                            return
                        }
                        secure = TransportCrypto.forClient(peer.secret, msg.nonce, clientNonce, codec)
                        connection.send(codec.encode(
                            SyncWireMessage.Hello(
                                deviceId = localDeviceId,
                                auth = signed,
                                sinceHlc = sinceHlc,
                                deviceName = settingsRepository.settings.value.deviceName,
                                transport = TransportCrypto.TRANSPORT,
                                clientNonce = clientNonce,
                                receiptEpoch = receiptEpoch,
                                sinceReceipt = sinceReceipt,
                            )
                        ))
                    }
                    is SyncWireMessage.Welcome -> {
                        if (msg.deviceId == localDeviceId) {
                            onEvent(Event.Failed(
                                "identity_collision: Peer is using this installation's device ID"
                            ))
                            connection.close(CLOSE_PROTOCOL_ERROR, "identity collision")
                            return
                        }
                        if (msg.deviceId != peer.id) {
                            onEvent(Event.Failed(
                                "identity_mismatch: Expected ${peer.id}, received ${msg.deviceId}"
                            ))
                            connection.close(CLOSE_PROTOCOL_ERROR, "identity mismatch")
                            return
                        }
                        authenticated = true
                        onEvent(Event.Authenticated(msg.name, msg.receiptEpoch))
                    }
                    is SyncWireMessage.Ack -> onEvent(Event.Acked(msg.upToHlc, msg.upToReceipt))
                    is SyncWireMessage.Event -> onEvent(Event.EventReceived(msg.event, msg.receipt))
                    is SyncWireMessage.BlobRequest ->
                        onEvent(Event.BlobRequested(msg.hash, msg.mediaType, msg.offset))
                    is SyncWireMessage.BlobCancel -> onEvent(Event.BlobCancelled(msg.hash))
                    is SyncWireMessage.BlobChunk -> onEvent(
                        Event.BlobChunkReceived(
                            hash = msg.hash,
                            mediaType = msg.mediaType,
                            offset = msg.offset,
                            totalSize = msg.totalSize,
                            data = msg.data,
                            isLast = msg.isLast
                        )
                    )
                    is SyncWireMessage.BlobMissing -> onEvent(Event.BlobMissingReceived(msg.hash))
                    is SyncWireMessage.Error -> {
                        onEvent(Event.Failed(
                            "${msg.code}: ${msg.message}",
                            authenticated = outer is SyncWireMessage.Secure,
                        ))
                        connection.close(CLOSE_PROTOCOL_ERROR, msg.code)
                    }
                    is SyncWireMessage.Hello -> {
                        // Server doesn't send Hello — that's the client's frame. Ignore.
                    }
                    is SyncWireMessage.Secure -> error("Secure envelope must be opened before dispatch")
                }
            }

            override fun onFailure(cause: Throwable) {
                if (closed) return
                closed = true
                onEvent(Event.Failed(cause.message ?: cause.javaClass.simpleName))
                onEvent(Event.Disconnected(cause.message.orEmpty()))
            }

            override fun onClosed(code: Int, reason: String) {
                if (closed) return
                closed = true
                onEvent(Event.Disconnected(reason))
            }
        })

        return object : Session {
            private fun sendSecure(message: SyncWireMessage): Boolean {
                if (closed) return false
                val channel = secure ?: return false
                return synchronized(channel) {
                    runCatching { connection.send(codec.encode(channel.seal(message))) }.getOrDefault(false)
                }
            }

            override fun send(event: ChatEvent, receipt: Long?): Boolean {
                if (!authenticated || closed) return false
                return sendSecure(SyncWireMessage.Event(event, receipt))
            }

            override fun ack(upToHlc: HlcTimestamp, upToReceipt: Long?): Boolean {
                if (!authenticated || closed) return false
                return sendSecure(SyncWireMessage.Ack(upToHlc, upToReceipt))
            }

            override fun requestBlob(hash: String, mediaType: String, offset: Long): Boolean {
                if (!authenticated || closed) return false
                return sendSecure(SyncWireMessage.BlobRequest(hash, mediaType, offset))
            }

            override fun cancelBlob(hash: String): Boolean {
                if (!authenticated || closed) return false
                return sendSecure(SyncWireMessage.BlobCancel(hash))
            }

            override fun queuedBytes(): Long = if (closed) 0L else connection.queuedBytes()

            override fun sendBlobChunk(
                hash: String,
                mediaType: String,
                offset: Int,
                totalSize: Int,
                data: String,
                isLast: Boolean
            ): Boolean {
                if (!authenticated || closed) return false
                return sendSecure(
                        SyncWireMessage.BlobChunk(
                            hash = hash,
                            mediaType = mediaType,
                            offset = offset,
                            totalSize = totalSize,
                            data = data,
                            isLast = isLast
                        )
                )
            }

            override fun sendBlobMissing(hash: String): Boolean {
                if (!authenticated || closed) return false
                return sendSecure(SyncWireMessage.BlobMissing(hash))
            }

            override fun sendError(code: String, message: String): Boolean {
                if (closed) return false
                return sendSecure(SyncWireMessage.Error(code, message))
            }

            override fun close() {
                if (closed) return
                closed = true
                connection.close(CLOSE_NORMAL, "client closing")
                onEvent(Event.Disconnected("client closing"))
            }
        }
    }

    private companion object {
        const val CLOSE_NORMAL = 1000
        const val CLOSE_PROTOCOL_ERROR = 1002
    }
}
