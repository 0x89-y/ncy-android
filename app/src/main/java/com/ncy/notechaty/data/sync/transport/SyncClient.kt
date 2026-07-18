package com.ncy.notechaty.data.sync.transport

import android.os.Build
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
) {

    private val codec = SyncWireCodec()

    sealed class Event {
        object Connecting : Event()
        data class Authenticated(val peerName: String) : Event()
        data class Acked(val upToHlc: HlcTimestamp) : Event()
        /** Server pushed an event back at us (phase 2 bidirectional). */
        data class EventReceived(val event: ChatEvent) : Event()
        // Phase 2b — media blob wire frames surfaced to SyncManager for handling.
        data class BlobRequested(val hash: String, val mediaType: String) : Event()
        data class BlobChunkReceived(
            val hash: String,
            val mediaType: String,
            val offset: Int,
            val totalSize: Int,
            val data: String,
            val isLast: Boolean
        ) : Event()
        data class BlobMissingReceived(val hash: String) : Event()
        data class Failed(val reason: String) : Event()
        data class Disconnected(val reason: String = "") : Event()
    }

    interface Session {
        fun send(event: ChatEvent): Boolean
        fun ack(upToHlc: HlcTimestamp): Boolean
        fun requestBlob(hash: String, mediaType: String): Boolean
        fun sendBlobChunk(
            hash: String,
            mediaType: String,
            offset: Int,
            totalSize: Int,
            data: String,
            isLast: Boolean
        ): Boolean
        fun sendBlobMissing(hash: String): Boolean
        fun sendError(code: String, message: String): Boolean
        fun close()
    }

    fun connect(
        peer: SyncPeer,
        localDeviceId: String,
        sinceHlc: HlcTimestamp?,
        onEvent: (Event) -> Unit
    ): Session {
        onEvent(Event.Connecting)

        var authenticated = false
        var closed = false
        var secure: TransportCrypto? = null
        lateinit var connection: ClientSyncTransport.Connection

        connection = frameTransport.connect(peer.host, peer.port, "sync", object : ClientSyncTransport.Listener {
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
                                deviceName = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Android phone",
                                transport = TransportCrypto.TRANSPORT,
                                clientNonce = clientNonce,
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
                        onEvent(Event.Authenticated(msg.name))
                    }
                    is SyncWireMessage.Ack -> onEvent(Event.Acked(msg.upToHlc))
                    is SyncWireMessage.Event -> onEvent(Event.EventReceived(msg.event))
                    is SyncWireMessage.BlobRequest -> onEvent(Event.BlobRequested(msg.hash, msg.mediaType))
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
                        onEvent(Event.Failed("${msg.code}: ${msg.message}"))
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

            override fun send(event: ChatEvent): Boolean {
                if (!authenticated || closed) return false
                return sendSecure(SyncWireMessage.Event(event))
            }

            override fun ack(upToHlc: HlcTimestamp): Boolean {
                if (!authenticated || closed) return false
                return sendSecure(SyncWireMessage.Ack(upToHlc))
            }

            override fun requestBlob(hash: String, mediaType: String): Boolean {
                if (!authenticated || closed) return false
                return sendSecure(SyncWireMessage.BlobRequest(hash, mediaType))
            }

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
