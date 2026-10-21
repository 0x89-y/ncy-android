package com.ncy.notechaty.ui.sync

import com.ncy.notechaty.R
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
import com.ncy.notechaty.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val peerStore: SyncPeerStore,
    private val syncManager: SyncManager,
    private val mdnsDiscovery: MdnsDiscovery,
    private val pairingClient: PairingClient,
    private val deviceIdentity: DeviceIdentity,
    private val phoneSyncServer: PhoneSyncServer,
    private val settingsRepository: SettingsRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    val peers: StateFlow<List<SyncPeer>> = peerStore.peers
    val states: StateFlow<Map<String, SyncManager.PeerState>> = syncManager.states
    val discovered: StateFlow<List<DiscoveredPeer>> = mdnsDiscovery.peers
    val hostState: StateFlow<PhoneSyncServer.HostState> = phoneSyncServer.state
    val hostingEnabled: StateFlow<Boolean> = phoneSyncServer.enabled
    val hostedPeers: StateFlow<List<PhoneSyncServer.HostedPeer>> = phoneSyncServer.hostedPeers
    val pendingPairing: StateFlow<PhoneSyncServer.PendingPairing?> = phoneSyncServer.pendingPairing
    val settings = settingsRepository.settings

    /** Which discovered peer (by mDNS serviceName) has an in-flight pair request. */
    private val _pairingBusy = MutableStateFlow<String?>(null)
    val pairingBusy: StateFlow<String?> = _pairingBusy.asStateFlow()

    /** One-shot toast/snackbar messages surfaced from async pair attempts. */
    /** A pairing outcome to show once. [success] decides its styling, so the text can be translated. */
    data class PairingMessage(val text: String, val success: Boolean)

    private val _pairingMessage = MutableStateFlow<PairingMessage?>(null)
    val pairingMessage: StateFlow<PairingMessage?> = _pairingMessage.asStateFlow()

    /** ECDH safety code from the current pair-in-flight request. Shown to the
     *  user to visually verify it matches the same code displayed on the
     *  desktop's confirm modal (MITM detection). */
    private val _pairingSafetyCode = MutableStateFlow<String?>(null)
    val pairingSafetyCode: StateFlow<String?> = _pairingSafetyCode.asStateFlow()

    /** The phone user has said the safety code matches the desktop's. */
    private val _pairingCodeConfirmed = MutableStateFlow(false)
    val pairingCodeConfirmed: StateFlow<Boolean> = _pairingCodeConfirmed.asStateFlow()

    /** The desktop sent its pairing data; it is held until the code is confirmed. */
    private val _pairingDesktopAccepted = MutableStateFlow(false)
    val pairingDesktopAccepted: StateFlow<Boolean> = _pairingDesktopAccepted.asStateFlow()

    private var codeConfirmation: CompletableDeferred<Unit>? = null

    /** Handle to the running pair coroutine, so the UI's Cancel button can
     *  abort the WebSocket wait if the user notices the safety code doesn't
     *  match the desktop's before the desktop user confirms. */
    private var pairingJob: kotlinx.coroutines.Job? = null

    /**
     * Choose whether media predating this pairing arrives by itself.
     *
     * Turning it off stamps now, so only media from here on is fetched; older
     * messages keep their Download button.
     */
    fun setMediaBackfill(everything: Boolean) {
        viewModelScope.launch {
            val current = settingsRepository.settings.value
            settingsRepository.updateSettings(
                current.copy(
                    mediaBackfillCutoffMs = if (everything) 0L else System.currentTimeMillis(),
                ),
            )
        }
    }

    /** Returns null on success, an error message on failure. */
    fun pair(rawText: String): String? {
        val payload = PairingPayload.parse(rawText)
            ?: return context.getString(R.string.pairing_data_unreadable)
        peerStore.addOrReplace(payload.toPeer(System.currentTimeMillis()))
        // Joining a mesh: media older than this moment is listed, not downloaded.
        viewModelScope.launch { settingsRepository.stampMediaBackfillCutoff() }
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
        resetCodeCheck()
        val confirmation = CompletableDeferred<Unit>()
        codeConfirmation = confirmation
        pairingJob = viewModelScope.launch {
            val phoneName = settings.value.deviceName
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
                    // Never import on the desktop's word alone. A rogue device
                    // advertising itself on the LAN can accept within milliseconds,
                    // before anyone has read the code — the code only protects the
                    // pairing if nothing is imported until the user has compared it.
                    _pairingDesktopAccepted.value = true
                    val confirmed = withTimeoutOrNull(CODE_CONFIRM_TIMEOUT_MS) {
                        confirmation.await()
                    } != null
                    _pairingMessage.value = if (confirmed) {
                        pair(result.pairingJson)?.let { PairingMessage(it, success = false) }
                            ?: PairingMessage(context.getString(R.string.paired_with, peer.name), success = true)
                    } else {
                        PairingMessage(context.getString(R.string.safety_code_timeout), success = false)
                    }
                }
                is PairingClient.Result.Rejected -> {
                    _pairingMessage.value = PairingMessage(
                        when (result.reason) {
                            "timeout" -> context.getString(R.string.pair_rejected_timeout)
                            "cancelled" -> context.getString(R.string.pair_rejected_cancelled)
                            "discovery-off" -> context.getString(R.string.pair_rejected_discovery_off)
                            "rate-limited" -> context.getString(R.string.pair_rejected_rate_limited)
                            "bad-pubkey" -> context.getString(R.string.pair_rejected_bad_key)
                            else -> context.getString(R.string.pair_rejected_other)
                        },
                        success = false,
                    )
                }
                is PairingClient.Result.Failed -> {
                    _pairingMessage.value = PairingMessage(context.getString(R.string.pair_failed, result.message), success = false)
                }
            }
            _pairingBusy.value = null
            resetCodeCheck()
            pairingJob = null
        }
    }

    /** The user compared the codes and they match. Either order works: before the
     *  desktop accepts, the import simply follows its acceptance. */
    fun confirmPairingCode() {
        if (_pairingSafetyCode.value == null) return
        _pairingCodeConfirmed.value = true
        codeConfirmation?.complete(Unit)
    }

    /** Called from the waiting card's Cancel button. Aborts the in-flight
     *  pair request — closes the WebSocket, which the desktop server sees
     *  as a peer disconnect. Use if the safety code on the phone doesn't
     *  match the one shown on the desktop's modal. */
    fun cancelPairing() {
        val job = pairingJob ?: return
        val desktopAccepted = _pairingDesktopAccepted.value
        job.cancel()
        pairingJob = null
        _pairingBusy.value = null
        resetCodeCheck()
        _pairingMessage.value = PairingMessage(
            if (desktopAccepted) context.getString(R.string.pairing_cancelled_after_accept) else context.getString(R.string.pair_request_cancelled),
            success = false,
        )
    }

    private fun resetCodeCheck() {
        _pairingSafetyCode.value = null
        _pairingCodeConfirmed.value = false
        _pairingDesktopAccepted.value = false
        codeConfirmation?.cancel()
        codeConfirmation = null
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

    fun setPeerEnabled(peerId: String, enabled: Boolean) =
        syncManager.setPeerEnabled(peerId, enabled)

    fun regeneratePairing() = phoneSyncServer.regeneratePairing()

    fun revokeHostedPeer(peerId: String) = phoneSyncServer.revokeHostedPeer(peerId)

    fun approvePendingPairing() = phoneSyncServer.approvePendingPairing()

    fun rejectPendingPairing() = phoneSyncServer.rejectPendingPairing()

    fun setAutomaticFileDownloadLimit(bytes: Long) {
        viewModelScope.launch {
            settingsRepository.updateSettings(
                settings.value.copy(
                    automaticFileDownloadLimitBytes = bytes.coerceIn(0L, 1024L * 1024 * 1024),
                ),
            )
        }
    }

    fun setHostingEnabled(enabled: Boolean) = phoneSyncServer.setEnabled(enabled)

    private companion object {
        /** How long accepted pairing data waits for the code check before it is dropped. */
        const val CODE_CONFIRM_TIMEOUT_MS = 2 * 60_000L
    }
}
