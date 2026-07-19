package com.ncy.notechaty.data.sync.transport

/**
 * A client-side, ordered text-frame connection used by the Ncy sync protocol.
 *
 * Authentication, encryption, catch-up, and event semantics deliberately live above this
 * boundary. A transport only finds a peer, carries complete frames in order, and reports its
 * lifecycle. That allows LAN WebSockets and an eventual Pear stream to share one protocol.
 */
interface ClientSyncTransport {
    enum class Kind { LAN, PEAR }
    interface Connection {
        fun send(text: String): Boolean
        fun close(code: Int, reason: String): Boolean
    }

    interface Listener {
        fun onTransportSelected(kind: Kind) = Unit
        fun onFrame(text: String)
        fun onFailure(cause: Throwable)
        fun onClosed(code: Int, reason: String)
    }

    fun connect(
        host: String,
        port: Int,
        path: String,
        pearPublicKey: String?,
        listener: Listener,
    ): Connection
}
