package com.ncy.notechaty.data.sync

import android.util.Log
import com.ncy.notechaty.data.model.ChatEventOp
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.HlcTimestamp
import com.ncy.notechaty.data.sync.event.SyncSnapshot
import com.ncy.notechaty.data.sync.media.MediaHash
import com.ncy.notechaty.data.sync.peer.SyncPeer
import com.ncy.notechaty.data.sync.peer.SyncPeerStore
import com.ncy.notechaty.data.sync.transport.SyncClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates one-way push sync to all paired peers. While [start]ed, opens a WebSocket
 * to each peer, streams catch-up events since their last-acked HLC, and forwards new events
 * from [ChatEventBus] in near-real-time. Peer high-water marks update only on `Ack`, so
 * a clean disconnect doesn't lose un-acknowledged events.
 *
 * Phase 1 lifetime: app foreground. NcyApp wires start/stop to ProcessLifecycleOwner.
 */
@Singleton
class SyncManager @Inject constructor(
    private val peerStore: SyncPeerStore,
    private val deviceIdentity: DeviceIdentity,
    private val eventBus: ChatEventBus,
    private val syncClient: SyncClient,
    private val snapshot: SyncSnapshot,
    private val chatRepository: ChatRepository
) {
    private val tag = "SyncManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val perPeer = mutableMapOf<String, PeerSession>()

    private val _states = MutableStateFlow<Map<String, PeerState>>(emptyMap())
    val states: StateFlow<Map<String, PeerState>> = _states.asStateFlow()

    private var running = false

    sealed class PeerState {
        object Idle : PeerState()
        object Connecting : PeerState()
        data class Connected(
            val peerName: String,
            val sentCount: Int,
            val receivedCount: Int = 0
        ) : PeerState()
        data class Failed(val reason: String) : PeerState()
    }

    @Synchronized
    fun start() {
        if (running) return
        running = true
        peerStore.peers.value.forEach { startSession(it) }
    }

    @Synchronized
    fun stop() {
        if (!running) return
        running = false
        perPeer.values.toList().forEach { it.shutdown() }
        perPeer.clear()
        _states.value = emptyMap()
    }

    /** Manually (re)connect to a single peer, regardless of [start]/[stop] state. */
    @Synchronized
    fun syncNow(peerId: String) {
        val peer = peerStore.byId(peerId) ?: return
        perPeer[peerId]?.shutdown()
        perPeer.remove(peerId)
        startSession(peer)
    }

    @Synchronized
    fun disconnect(peerId: String) {
        perPeer.remove(peerId)?.shutdown()
        updateState(peerId, PeerState.Idle)
    }

    private fun startSession(peer: SyncPeer) {
        updateState(peer.id, PeerState.Connecting)
        val session = PeerSession(peer)
        perPeer[peer.id] = session
        session.run()
    }

    private fun updateState(peerId: String, state: PeerState) {
        _states.value = _states.value.toMutableMap().apply { put(peerId, state) }
    }

    private inner class PeerSession(private val peer: SyncPeer) {
        private var session: SyncClient.Session? = null
        private var liveJob: Job? = null
        private var sentCount = 0
        private var receivedCount = 0
        private var peerName: String = peer.name

        // Outbound watermark: highest HLC the peer has acked from us. Drives our catchup.
        @Volatile private var lastAckedHlc: HlcTimestamp? = peerStore.lastSentHlc(peer)

        // Inbound watermark: highest HLC we've received and applied from the peer. Sent
        // as `Hello.sinceHlc` so the peer knows where to resume sending us.
        @Volatile private var lastReceivedHlc: HlcTimestamp? = peerStore.lastReceivedHlc(peer)

        fun run() {
            session = syncClient.connect(
                peer = peer,
                localDeviceId = deviceIdentity.deviceId,
                sinceHlc = lastReceivedHlc
            ) { event ->
                when (event) {
                    is SyncClient.Event.Connecting -> {
                        updateState(peer.id, PeerState.Connecting)
                    }
                    is SyncClient.Event.Authenticated -> {
                        peerName = event.peerName
                        updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount))
                        // Subscribe to live events BEFORE reading the catchup snapshot so
                        // events appended during snapshot read don't fall through the gap.
                        startLiveForwarding(checkNotNull(session))
                        scope.launch { pushCatchup() }
                    }
                    is SyncClient.Event.Acked -> {
                        peerStore.updateLastSent(peer.id, event.upToHlc, System.currentTimeMillis())
                        val cur = lastAckedHlc
                        if (cur == null || event.upToHlc > cur) lastAckedHlc = event.upToHlc
                    }
                    is SyncClient.Event.EventReceived -> {
                        scope.launch { handleInbound(event.event) }
                    }
                    is SyncClient.Event.Failed -> {
                        Log.w(tag, "Peer ${peer.id} failed: ${event.reason}")
                        updateState(peer.id, PeerState.Failed(event.reason))
                    }
                    is SyncClient.Event.Disconnected -> {
                        liveJob?.cancel()
                        liveJob = null
                        if (_states.value[peer.id] !is PeerState.Failed) {
                            updateState(peer.id, PeerState.Idle)
                        }
                    }
                }
            }
        }

        private suspend fun handleInbound(event: ChatEvent) {
            // Defensive loop guard: an event that originated from this device should never
            // be applied (the peer is echoing it back). Should not happen if the peer
            // implements its own sender-side loop guard, but cheap to check.
            if (event.originDevice == deviceIdentity.deviceId) {
                session?.ack(event.hlc)
                return
            }
            chatRepository.applyExternalEvent(event)
            session?.ack(event.hlc)
            peerStore.updateLastReceived(peer.id, event.hlc)
            val cur = lastReceivedHlc
            if (cur == null || event.hlc > cur) lastReceivedHlc = event.hlc
            receivedCount++
            updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount))
        }

        private fun pushCatchup() {
            val s = session ?: return
            val pending = snapshot.eventsSince(lastAckedHlc)
            for (event in pending) {
                if (!sendEvent(s, event)) return
            }
            updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount))
        }

        private fun startLiveForwarding(s: SyncClient.Session) {
            liveJob?.cancel()
            liveJob = scope.launch {
                eventBus.events.collect { event: ChatEvent ->
                    sendEvent(s, event)
                }
            }
        }

        @Synchronized
        private fun sendEvent(s: SyncClient.Session, event: ChatEvent): Boolean {
            // Loop prevention: don't send events back to the peer that authored them.
            // (Matters once events from peer A get republished to the bus by `applyExternalEvent`
            // and would otherwise be forwarded straight back to A.)
            if (event.originDevice == peer.id) return true
            // Skip events the peer already acked (cheap dedupe; the receiver also tolerates
            // duplicates by event id, but we save bandwidth).
            val acked = lastAckedHlc
            if (acked != null && event.hlc <= acked) return true
            if (!s.send(event)) return false
            sentCount++
            updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount))
            return true
        }

        fun shutdown() {
            liveJob?.cancel()
            session?.close()
        }
    }
}
