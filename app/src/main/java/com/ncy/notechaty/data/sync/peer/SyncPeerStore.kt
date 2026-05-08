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
        _peers.value = try {
            gson.fromJson<List<SyncPeer>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
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
