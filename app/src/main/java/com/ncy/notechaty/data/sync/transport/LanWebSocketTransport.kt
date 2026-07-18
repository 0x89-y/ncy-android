package com.ncy.notechaty.data.sync.transport

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Ordered text-frame transport over the existing LAN WebSocket endpoint. */
@Singleton
class LanWebSocketTransport @Inject constructor() : ClientSyncTransport {
    private val httpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    override fun connect(
        host: String,
        port: Int,
        path: String,
        pearPublicKey: String?,
        listener: ClientSyncTransport.Listener,
    ): ClientSyncTransport.Connection {
        val request = Request.Builder()
            .url(SyncEndpoint.webSocketUrl(host, port, path))
            .build()
        val socket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) = listener.onFrame(text)

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                listener.onFailure(t)

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) =
                listener.onClosed(code, reason)
        })
        return object : ClientSyncTransport.Connection {
            override fun send(text: String): Boolean = socket.send(text)
            override fun close(code: Int, reason: String): Boolean = socket.close(code, reason)
        }
    }
}
