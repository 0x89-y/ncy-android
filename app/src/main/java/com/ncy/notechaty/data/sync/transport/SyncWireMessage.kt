package com.ncy.notechaty.data.sync.transport

import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.HlcTimestamp

/**
 * Messages exchanged on the live WebSocket. One JSON object per WebSocket text frame.
 * Phase 1 carries a one-way push protocol; phase 2 will reuse [Hello]/[Welcome]
 * for bidirectional pulls.
 *
 * Wire form: `{"type":"<Type>", ...}`
 */
sealed class SyncWireMessage {

    /** Server → client. First frame after connect. Contains the auth nonce to sign. */
    data class Challenge(val nonce: String, val transport: String = "") : SyncWireMessage()

    /**
     * Client → server. Authenticates the client and announces its catch-up watermark.
     * `auth` = base64(HMAC-SHA256(secret, nonce)).
     */
    data class Hello(
        val deviceId: String,
        val auth: String,
        val sinceHlc: HlcTimestamp?,
        /** Optional for compatibility with older clients. */
        val deviceName: String = "",
        val transport: String = "",
        val clientNonce: String = "",
    ) : SyncWireMessage()

    /** Server → client. Authentication accepted; client may begin streaming. */
    data class Welcome(val deviceId: String, val name: String) : SyncWireMessage()

    /** Client → server. A single chat event. */
    data class Event(val event: ChatEvent) : SyncWireMessage()

    /**
     * Server → client. Acknowledges that all events up to and including this HLC have been
     * persisted. Client may advance its `lastSentHlc` watermark for this peer.
     */
    data class Ack(val upToHlc: HlcTimestamp) : SyncWireMessage()

    /** Either side. Aborts the session with a human-readable reason. */
    data class Error(val code: String, val message: String) : SyncWireMessage()

    /** AES-256-GCM authenticated envelope used for every post-Hello message. */
    data class Secure(val seq: Long, val data: String) : SyncWireMessage()

    // ----- Phase 2b: media blob transfer -----

    /**
     * Either side asks the other for the bytes of a content-addressed blob.
     * `mediaType` is a hint for how the receiver should store/render the bytes
     * (currently `"voice"` or `"image"`).
     */
    data class BlobRequest(val hash: String, val mediaType: String) : SyncWireMessage()

    /**
     * One chunk of a previously-requested blob. Receivers concatenate by `offset`,
     * verify the SHA-256 of the assembled bytes against `hash` on `isLast`, then
     * persist. `data` is base64-encoded so the wire stays uniform JSON text.
     */
    data class BlobChunk(
        val hash: String,
        val mediaType: String,
        val offset: Int,
        val totalSize: Int,
        val data: String,
        val isLast: Boolean
    ) : SyncWireMessage()

    /** Sender does not have the requested blob. Receiver should drop the pending fetch. */
    data class BlobMissing(val hash: String) : SyncWireMessage()

    /**
     * Receiver -> sender: stop streaming this blob, it is no longer wanted.
     *
     * Without this a cancelled download still costs the sender the full upload;
     * the receiver just discards what arrives. Peers that predate this frame
     * ignore it and keep sending, which is the old behaviour, so it is safe in
     * both directions.
     */
    data class BlobCancel(val hash: String) : SyncWireMessage()
}
