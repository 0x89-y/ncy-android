package com.ncy.notechaty.data.sync.transport

/**
 * A client-side, ordered text-frame connection used by the Ncy sync protocol.
 *
 * Authentication, encryption, catch-up, and event semantics deliberately live above this
 * boundary. A transport only finds a peer, carries complete frames in order, and reports its
 * lifecycle.
 */
interface ClientSyncTransport {
    enum class Kind { LAN }
    interface Connection {
        fun send(text: String): Boolean
        fun close(code: Int, reason: String): Boolean

        /**
         * Bytes accepted by [send] but not yet written to the network, or 0 when the
         * transport cannot report it.
         *
         * `send` only enqueues. A blob served at full speed enqueues far faster than
         * the socket drains, and OkHttp caps its buffer at 16 MiB — past that it does
         * not block or reject, it *closes the connection*. Blob serving throttles on
         * this so a large attachment can never take the session down with it.
         */
        fun queuedBytes(): Long = 0L
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
        listener: Listener,
    ): Connection
}
