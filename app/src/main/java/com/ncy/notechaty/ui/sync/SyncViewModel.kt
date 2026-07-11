package com.ncy.notechaty.ui.sync

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ncy.notechaty.data.sync.DeviceIdentity
import com.ncy.notechaty.data.sync.SyncManager
import com.ncy.notechaty.data.sync.discovery.DiscoveredPeer
import com.ncy.notechaty.data.sync.discovery.MdnsDiscovery
import com.ncy.notechaty.data.sync.pairing.PairingClient
import com.ncy.notechaty.data.sync.peer.PairingPayload
import com.ncy.notechaty.data.sync.peer.SyncPeer
import com.ncy.notechaty.data.sync.peer.SyncPeerStore
import com.ncy.notechaty.data.sync.transport.PhoneSyncServer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val peerStore: SyncPeerStore,
    private val syncManager: SyncManager,
    private val mdnsDiscovery: MdnsDiscovery,
    private val pairingClient: PairingClient,
    private val deviceIdentity: DeviceIdentity,
    private val phoneSyncServer: PhoneSyncServer,
) : ViewModel() {

    val peers: StateFlow<List<SyncPeer>> = peerStore.peers
    val states: StateFlow<Map<String, SyncManager.PeerState>> = syncManager.states
    val discovered: StateFlow<List<DiscoveredPeer>> = mdnsDiscovery.peers
    val hostState: StateFlow<PhoneSyncServer.HostState> = phoneSyncServer.state
    val hostingEnabled: StateFlow<Boolean> = phoneSyncServer.enabled
    val hostedPeers: StateFlow<List<PhoneSyncServer.HostedPeer>> = phoneSyncServer.hostedPeers

    /** Which discovered peer (by mDNS serviceName) has an in-flight pair request. */
    private val _pairingBusy = MutableStateFlow<String?>(null)
    val pairingBusy: StateFlow<String?> = _pairingBusy.asStateFlow()

    /** One-shot toast/snackbar messages surfaced from async pair attempts. */
    private val _pairingMessage = MutableStateFlow<String?>(null)
    val pairingMessage: StateFlow<String?> = _pairingMessage.asStateFlow()

    /** ECDH safety code from the current pair-in-flight request. Shown to the
     *  user to visually verify it matches the same code displayed on the
     *  desktop's confirm modal (MITM detection). */
    private val _pairingSafetyCode = MutableStateFlow<String?>(null)
    val pairingSafetyCode: StateFlow<String?> = _pairingSafetyCode.asStateFlow()

    /** Handle to the running pair coroutine, so the UI's Cancel button can
     *  abort the WebSocket wait if the user notices the safety code doesn't
     *  match the desktop's before the desktop user confirms. */
    private var pairingJob: kotlinx.coroutines.Job? = null

    /** Returns null on success, an error message on failure. */
    fun pair(rawText: String): String? {
        val payload = PairingPayload.parse(rawText)
            ?: return "Couldn't read pairing data — paste the JSON shown by the desktop server."
        peerStore.addOrReplace(payload.toPeer(System.currentTimeMillis()))
        // Re-trigger so the new peer connects right away if the manager is running.
        syncManager.syncNow(payload.deviceId)
        return null
    }

    /**
     * Discovery-initiated pairing: ask a mDNS-visible desktop to pair without a
     * QR code. Desktop shows a modal and the user confirms; the returned
     * pairing JSON flows through the same import path as a scanned QR.
     */
    fun pairViaDiscovery(peer: DiscoveredPeer) {
        if (_pairingBusy.value != null) return
        _pairingBusy.value = peer.name
        _pairingMessage.value = null
        _pairingSafetyCode.value = null
        pairingJob = viewModelScope.launch {
            val phoneName = Build.MODEL ?: "Phone"
            val result = withContext(Dispatchers.IO) {
                pairingClient.request(
                    host = peer.host,
                    port = peer.port,
                    phoneId = deviceIdentity.deviceId,
                    phoneName = phoneName,
                    onSafetyCode = { code -> _pairingSafetyCode.value = code },
                )
            }
            when (result) {
                is PairingClient.Result.Success -> {
                    // Auto-import: the phone user has had the safety code
                    // visible on the waiting card for the entire time the
                    // desktop was deciding. If the codes didn't match, they
                    // would have hit Cancel already. Skipping a second
                    // confirmation dialog here removes a redundant tap.
                    val err = pair(result.pairingJson)
                    _pairingMessage.value = err ?: "Paired with ${peer.name}."
                }
                is PairingClient.Result.Rejected -> {
                    _pairingMessage.value = when (result.reason) {
                        "timeout" -> "Desktop didn't confirm in time."
                        "cancelled" -> "Desktop cancelled the request."
                        "discovery-off" -> "Desktop has discoverability disabled."
                        "rate-limited" -> "Too many attempts; try again in a few minutes."
                        "bad-pubkey" -> "Handshake failed (bad pubkey)."
                        else -> "Desktop rejected the request."
                    }
                }
                is PairingClient.Result.Failed -> {
                    _pairingMessage.value = "Couldn't pair: ${result.message}"
                }
            }
            _pairingBusy.value = null
            _pairingSafetyCode.value = null
            pairingJob = null
        }
    }

    /** Called from the waiting card's Cancel button. Aborts the in-flight
     *  pair request — closes the WebSocket, which the desktop server sees
     *  as a peer disconnect. Use if the safety code on the phone doesn't
     *  match the one shown on the desktop's modal. */
    fun cancelPairing() {
        val job = pairingJob ?: return
        job.cancel()
        pairingJob = null
        _pairingBusy.value = null
        _pairingSafetyCode.value = null
        _pairingMessage.value = "Pair request cancelled."
    }

    /** UI acknowledges the most recent snackbar. */
    fun clearPairingMessage() {
        _pairingMessage.value = null
    }

    fun unpair(peerId: String) {
        syncManager.disconnect(peerId)
        peerStore.remove(peerId)
    }

    fun syncNow(peerId: String) {
        syncManager.syncNow(peerId)
    }

    fun regeneratePairing() = phoneSyncServer.regeneratePairing()

    fun revokeHostedPeer(peerId: String) = phoneSyncServer.revokeHostedPeer(peerId)

    fun setHostingEnabled(enabled: Boolean) = phoneSyncServer.setEnabled(enabled)
}
