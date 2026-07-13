package com.ncy.notechaty.data.sync.peer

/**
 * A paired remote device the phone can push events to. Created from a [PairingPayload]
 * scanned/pasted from the desktop and persisted by [SyncPeerStore].
 *
 * - [id]: deviceId of the remote, used for HLC tiebreaking and wire-level identification.
 * - [secret]: pre-shared key (base64) used to HMAC challenge nonces during handshake.
 * - [lastSentHlcMs]/[lastSentHlcLogical]/[lastSentHlcNode]: high-water mark of events that
 *   the peer has acked. Updated only on `ack`. Null for never-synced peers.
 */
data class SyncPeer(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val secret: String,
    val pairedAt: Long,
    val lastSyncAt: Long? = null,
    /** Highest HLC the peer has acked from us. Drives our outbound catchup. */
    val lastSentHlcMs: Long? = null,
    val lastSentHlcLogical: Int? = null,
    val lastSentHlcNode: String? = null,
    /** Highest HLC we've received and applied from this peer. Sent as Hello.sinceHlc. */
    val lastReceivedHlcMs: Long? = null,
    val lastReceivedHlcLogical: Int? = null,
    val lastReceivedHlcNode: String? = null,
    /** Host lifecycle capability from pairing data; null means always available. */
    val availability: String? = null,
    /** User-controlled outbound connection switch. Missing legacy values default to enabled. */
    val disabled: Boolean = false,
    val lastDiscoveredAt: Long? = null,
    val lastAuthenticatedAt: Long? = null,
    val lastAuthenticatedHost: String? = null,
    val lastAuthenticatedPort: Int? = null,
    val lastError: String? = null,
    val nextRetryAt: Long? = null,
) {
    val foregroundOnly: Boolean
        get() = availability == PairingPayload.AVAILABILITY_FOREGROUND
}
