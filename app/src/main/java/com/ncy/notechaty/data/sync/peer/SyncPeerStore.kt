package com.ncy.notechaty.data.sync.peer

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ncy.notechaty.data.sync.event.HlcTimestamp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the list of paired remote devices in SharedPreferences. Phase 1 stores secrets
 * in plain prefs — they're scoped to LAN sync and only useful to someone already on the
 * device. EncryptedSharedPreferences is a phase-3 hardening once cloud relay arrives.
 */
@Singleton
class SyncPeerStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _peers = MutableStateFlow<List<SyncPeer>>(emptyList())
    val peers: StateFlow<List<SyncPeer>> = _peers.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val json = prefs.getString(KEY_PEERS, null) ?: return
        val type = object : TypeToken<List<SyncPeer>>() {}.type
        val loaded = try {
            gson.fromJson<List<SyncPeer>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val now = System.currentTimeMillis()
        var repaired = false
        _peers.value = loaded.map { peer ->
            val clearSent = lastSentHlc(peer)?.let { !HlcTimestamp.isAcceptableRemote(it, now) } == true
            val clearReceived = lastReceivedHlc(peer)?.let { !HlcTimestamp.isAcceptableRemote(it, now) } == true
            if (!clearSent && !clearReceived) peer else {
                repaired = true
                peer.copy(
                    lastSentHlcMs = if (clearSent) null else peer.lastSentHlcMs,
                    lastSentHlcLogical = if (clearSent) null else peer.lastSentHlcLogical,
                    lastSentHlcNode = if (clearSent) null else peer.lastSentHlcNode,
                    lastReceivedHlcMs = if (clearReceived) null else peer.lastReceivedHlcMs,
                    lastReceivedHlcLogical = if (clearReceived) null else peer.lastReceivedHlcLogical,
                    lastReceivedHlcNode = if (clearReceived) null else peer.lastReceivedHlcNode,
                )
            }
        }
        if (repaired) persist()
    }

    private fun persist() {
        prefs.edit().putString(KEY_PEERS, gson.toJson(_peers.value)).apply()
    }

    fun addOrReplace(peer: SyncPeer) {
        _peers.value = (_peers.value.filterNot { it.id == peer.id } + peer)
            .sortedBy { it.pairedAt }
        persist()
    }

    fun remove(peerId: String) {
        _peers.value = _peers.value.filterNot { it.id == peerId }
        persist()
    }

    fun byId(peerId: String): SyncPeer? = _peers.value.firstOrNull { it.id == peerId }

    /** Refresh a paired device's last-known LAN endpoint without touching its secret or watermarks. */
    fun updateEndpoint(peerId: String, host: String, port: Int): Boolean {
        val existing = byId(peerId) ?: return false
        if (existing.host == host && existing.port == port) return false
        _peers.value = _peers.value.map {
            if (it.id == peerId) it.copy(host = host, port = port) else it
        }
        persist()
        return true
    }

    /** Learn an optional lifecycle capability from trusted-ID mDNS discovery. */
    fun updateAvailability(peerId: String, availability: String?): Boolean {
        if (availability.isNullOrBlank()) return false
        val existing = byId(peerId) ?: return false
        if (existing.availability == availability) return false
        _peers.value = _peers.value.map {
            if (it.id == peerId) it.copy(availability = availability) else it
        }
        persist()
        return true
    }

    fun setEnabled(peerId: String, enabled: Boolean): Boolean {
        val existing = byId(peerId) ?: return false
        if (existing.disabled == !enabled) return false
        _peers.value = _peers.value.map {
            if (it.id == peerId) it.copy(disabled = !enabled) else it
        }
        persist()
        return true
    }

    /** Durably reset progress before restore so every restored event is replayed idempotently. */
    fun resetAllWatermarks(): Boolean {
        val previous = _peers.value
        val reset = previous.map { peer ->
            if (peer.lastSentHlcMs == null && peer.lastReceivedHlcMs == null) return@map peer
            peer.copy(
                lastSentHlcMs = null,
                lastSentHlcLogical = null,
                lastSentHlcNode = null,
                lastReceivedHlcMs = null,
                lastReceivedHlcLogical = null,
                lastReceivedHlcNode = null,
            )
        }
        if (reset == previous) return true
        _peers.value = reset
        val committed = prefs.edit().putString(KEY_PEERS, gson.toJson(reset)).commit()
        if (!committed) _peers.value = previous
        return committed
    }

    /**
     * Update the high-water-mark HLC after a successful ack from the peer. Refuses to
     * regress — out-of-order acks for older HLCs are ignored so the watermark only ever
     * advances.
     */
    fun updateLastSent(peerId: String, hlc: HlcTimestamp, syncedAt: Long) {
        var changed = false
        _peers.value = _peers.value.map { existing ->
            if (existing.id != peerId) return@map existing
            val current = lastSentHlc(existing)
            if (current != null && hlc <= current) {
                existing.copy(lastSyncAt = syncedAt).also { changed = true }
            } else {
                changed = true
                existing.copy(
                    lastSentHlcMs = hlc.physicalMs,
                    lastSentHlcLogical = hlc.logical,
                    lastSentHlcNode = hlc.nodeId,
                    lastSyncAt = syncedAt
                )
            }
        }
        if (changed) persist()
    }

    fun lastSentHlc(peer: SyncPeer): HlcTimestamp? {
        val ms = peer.lastSentHlcMs ?: return null
        val logical = peer.lastSentHlcLogical ?: return null
        val node = peer.lastSentHlcNode ?: return null
        return HlcTimestamp(ms, logical, node)
    }

    fun lastReceivedHlc(peer: SyncPeer): HlcTimestamp? {
        val ms = peer.lastReceivedHlcMs ?: return null
        val logical = peer.lastReceivedHlcLogical ?: return null
        val node = peer.lastReceivedHlcNode ?: return null
        return HlcTimestamp(ms, logical, node)
    }

    /**
     * Update the high-water mark of events we've successfully applied from this peer.
     * Refuses to regress; sets the watermark for the next reconnect's `Hello.sinceHlc`.
     */
    fun updateLastReceived(peerId: String, hlc: HlcTimestamp) {
        var changed = false
        _peers.value = _peers.value.map { existing ->
            if (existing.id != peerId) return@map existing
            val current = lastReceivedHlc(existing)
            if (current != null && hlc <= current) existing
            else {
                changed = true
                existing.copy(
                    lastReceivedHlcMs = hlc.physicalMs,
                    lastReceivedHlcLogical = hlc.logical,
                    lastReceivedHlcNode = hlc.nodeId
                )
            }
        }
        if (changed) persist()
    }

    private companion object {
        const val PREFS = "ncy_sync_peers"
        const val KEY_PEERS = "peers"
    }
}
