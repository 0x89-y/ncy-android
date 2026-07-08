package com.ncy.notechaty.data.sync.transport

import android.util.Base64
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.HlcTimestamp
import com.ncy.notechaty.data.sync.peer.SyncPeer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot WebSocket session against a paired peer. Performs the challenge/HMAC handshake,
 * then exposes a [Session] the caller uses to push events. State changes flow back through
 * the [onEvent] callback. The caller (SyncManager) is responsible for retry/backoff and for
 * deciding which events to push in what order.
 *
 * Wire path: `ws://<peer host>:<peer port>/sync`. Phase 1 is plaintext-on-LAN; phase 3
 * upgrades to TLS or Noise.
 */
@Singleton
class SyncClient @Inject constructor() {

    private val codec = SyncWireCodec()

    private val httpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // long-lived stream
        .build()

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
        object Disconnected : Event()
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
        fun close()
    }

    fun connect(
        peer: SyncPeer,
        localDeviceId: String,
        sinceHlc: HlcTimestamp?,
        onEvent: (Event) -> Unit
    ): Session {
        onEvent(Event.Connecting)

        val request = Request.Builder()
            .url("ws://${peer.host}:${peer.port}/sync")
            .build()

        var authenticated = false
        var closed = false
        lateinit var webSocket: WebSocket

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val msg = try {
                    codec.decode(text)
                } catch (e: Exception) {
                    onEvent(Event.Failed("Malformed wire frame: ${e.message}"))
                    webSocket.close(CLOSE_PROTOCOL_ERROR, "decode failed")
                    return
                }
                when (msg) {
                    is SyncWireMessage.Challenge -> {
                        val signed = sign(peer.secret, msg.nonce)
                        if (signed == null) {
                            onEvent(Event.Failed("Could not sign challenge"))
                            webSocket.close(CLOSE_PROTOCOL_ERROR, "auth failed")
                            return
                        }
                        webSocket.send(codec.encode(
                            SyncWireMessage.Hello(
                                deviceId = localDeviceId,
                                auth = signed,
                                sinceHlc = sinceHlc
                            )
                        ))
                    }
                    is SyncWireMessage.Welcome -> {
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
                        webSocket.close(CLOSE_PROTOCOL_ERROR, msg.code)
                    }
                    is SyncWireMessage.Hello -> {
                        // Server doesn't send Hello — that's the client's frame. Ignore.
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (closed) return
                closed = true
                onEvent(Event.Failed(t.message ?: t.javaClass.simpleName))
                onEvent(Event.Disconnected)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (closed) return
                closed = true
                onEvent(Event.Disconnected)
            }
        })

        return object : Session {
            override fun send(event: ChatEvent): Boolean {
                if (!authenticated || closed) return false
                return webSocket.send(codec.encode(SyncWireMessage.Event(event)))
            }

            override fun ack(upToHlc: HlcTimestamp): Boolean {
                if (!authenticated || closed) return false
                return webSocket.send(codec.encode(SyncWireMessage.Ack(upToHlc)))
            }

            override fun requestBlob(hash: String, mediaType: String): Boolean {
                if (!authenticated || closed) return false
                return webSocket.send(codec.encode(SyncWireMessage.BlobRequest(hash, mediaType)))
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
                return webSocket.send(
                    codec.encode(
                        SyncWireMessage.BlobChunk(
                            hash = hash,
                            mediaType = mediaType,
                            offset = offset,
                            totalSize = totalSize,
                            data = data,
                            isLast = isLast
                        )
                    )
                )
            }

            override fun sendBlobMissing(hash: String): Boolean {
                if (!authenticated || closed) return false
                return webSocket.send(codec.encode(SyncWireMessage.BlobMissing(hash)))
            }

            override fun close() {
                if (closed) return
                closed = true
                webSocket.close(CLOSE_NORMAL, "client closing")
                onEvent(Event.Disconnected)
            }
        }
    }

    private fun sign(secretBase64: String, nonceBase64: String): String? = try {
        val secret = Base64.decode(secretBase64, Base64.NO_WRAP)
        val nonce = Base64.decode(nonceBase64, Base64.NO_WRAP)
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secret, "HmacSHA256"))
        }
        Base64.encodeToString(mac.doFinal(nonce), Base64.NO_WRAP)
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val CLOSE_NORMAL = 1000
        const val CLOSE_PROTOCOL_ERROR = 1002
    }
}
