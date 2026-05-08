package com.ncy.notechaty.ui.sync

import androidx.lifecycle.ViewModel
import com.ncy.notechaty.data.sync.SyncManager
import com.ncy.notechaty.data.sync.peer.PairingPayload
import com.ncy.notechaty.data.sync.peer.SyncPeer
import com.ncy.notechaty.data.sync.peer.SyncPeerStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val peerStore: SyncPeerStore,
    private val syncManager: SyncManager
) : ViewModel() {

    val peers: StateFlow<List<SyncPeer>> = peerStore.peers
    val states: StateFlow<Map<String, SyncManager.PeerState>> = syncManager.states

    /** Returns null on success, an error message on failure. */
    fun pair(rawText: String): String? {
        val payload = PairingPayload.parse(rawText)
            ?: return "Couldn't read pairing data — paste the JSON shown by the desktop server."
        peerStore.addOrReplace(payload.toPeer(System.currentTimeMillis()))
        // Re-trigger so the new peer connects right away if the manager is running.
        syncManager.syncNow(payload.deviceId)
        return null
    }

    fun unpair(peerId: String) {
        syncManager.disconnect(peerId)
        peerStore.remove(peerId)
    }

    fun syncNow(peerId: String) {
        syncManager.syncNow(peerId)
    }
}
