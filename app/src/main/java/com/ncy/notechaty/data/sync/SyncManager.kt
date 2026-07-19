package com.ncy.notechaty.data.sync

import android.util.Log
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.ChatEventOp
import com.ncy.notechaty.data.sync.event.HlcTimestamp
import com.ncy.notechaty.data.sync.event.SyncSnapshot
import com.ncy.notechaty.data.sync.media.MediaHash
import com.ncy.notechaty.data.sync.discovery.MdnsDiscovery
import com.ncy.notechaty.data.sync.discovery.LanNetworkMonitor
import com.ncy.notechaty.data.sync.discovery.EndpointCandidateTracker
import com.ncy.notechaty.data.sync.peer.SyncPeer
import com.ncy.notechaty.data.sync.peer.SyncPeerStore
import com.ncy.notechaty.data.sync.pear.PearRuntimeController
import com.ncy.notechaty.data.sync.pear.PearRuntimeStatus
import com.ncy.notechaty.data.sync.transport.SyncClient
import com.ncy.notechaty.data.sync.transport.SyncCatchupPolicy
import com.ncy.notechaty.data.sync.transport.SyncLimits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedOutputStream
import java.io.File
import java.util.ArrayDeque
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
    private val chatRepository: ChatRepository,
    private val mdnsDiscovery: MdnsDiscovery,
    private val lanNetworkMonitor: LanNetworkMonitor,
    private val settingsRepository: SettingsRepository,
    private val pearRuntimeController: PearRuntimeController,
) {
    private val tag = "SyncManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val perPeer = mutableMapOf<String, PeerSession>()
    private val endpointCandidates = EndpointCandidateTracker()

    private val _states = MutableStateFlow<Map<String, PeerState>>(emptyMap())
    val states: StateFlow<Map<String, PeerState>> = _states.asStateFlow()

    private var running = false
    private var discoveryJob: Job? = null
    private var networkJob: Job? = null

    sealed class PeerState {
        object Idle : PeerState()
        object Disabled : PeerState()
        object WaitingForNetwork : PeerState()
        object Connecting : PeerState()
        data class Connected(
            val peerName: String,
            val sentCount: Int,
            val receivedCount: Int = 0
        ) : PeerState()
        data class Unavailable(val reason: String) : PeerState()
        data class Failed(val reason: String) : PeerState()
    }

    @Synchronized
    fun start() {
        if (running) return
        running = true
        lanNetworkMonitor.start()
        mdnsDiscovery.start()
        networkJob = scope.launch {
            val previous = mutableMapOf<String, Boolean>()
            combine(
                lanNetworkMonitor.available,
                lanNetworkMonitor.internetAvailable,
                settingsRepository.settings,
                pearRuntimeController.status,
            ) { lan, internet, settings, pearStatus ->
                NetworkAvailability(lan, internet, settings.pearSyncEnabled, pearStatus is PearRuntimeStatus.Ready)
            }.collect { availability ->
                peerStore.peers.value.forEach { peer ->
                    val usable = canUseNetwork(peer, availability)
                    val wasUsable = previous.put(peer.id, usable)
                    if (!usable) {
                        synchronized(this@SyncManager) { perPeer[peer.id]?.pauseForNetwork() }
                        if (!peer.disabled) updateState(peer.id, PeerState.WaitingForNetwork)
                    } else if (wasUsable == false && !peer.disabled && shouldAutomaticallyRetry(peer.id)) {
                        syncNow(peer.id)
                    }
                }
            }
        }
        discoveryJob = scope.launch {
            mdnsDiscovery.peers.collect { discovered ->
                discovered.forEach { found ->
                    val id = found.deviceId ?: return@forEach
                    val trusted = peerStore.byId(id) ?: return@forEach
                    peerStore.recordDiscovery(id)
                    val state = _states.value[id]
                    if (trusted.host == found.host && trusted.port == found.port) {
                        if (shouldAutomaticallyRetry(id) &&
                            (state is PeerState.Unavailable || state is PeerState.Failed || state is PeerState.Idle)
                        ) {
                            syncNow(id)
                        }
                        return@forEach
                    }
                    val accepted = endpointCandidates.observe(
                        id, found.host, found.port)
                    if (accepted && shouldAutomaticallyRetry(id)) {
                        Log.i(tag, "Recorded untrusted mDNS candidate for $id at ${found.host}:${found.port}")
                    } else {
                        Log.d(tag, "Ignored duplicate or rapidly flapping mDNS endpoint for $id")
                    }
                    if (accepted) {
                        when (state) {
                            is PeerState.Unavailable, is PeerState.Failed -> syncCandidateNow(id)
                            is PeerState.Idle -> syncNow(id) // authenticated endpoint still gets first attempt
                            else -> Unit
                        }
                    }
                }
            }
        }
        peerStore.peers.value.forEach { peer ->
            when {
                peer.disabled -> updateState(peer.id, PeerState.Disabled)
                !canUseNetwork(peer) ->
                    updateState(peer.id, PeerState.WaitingForNetwork)
                else -> startSession(peer)
            }
        }
    }

    private fun shouldAutomaticallyRetry(peerId: String): Boolean =
        (_states.value[peerId] as? PeerState.Failed)?.reason != SyncFailurePolicy.REPAIR_MESSAGE

    @Synchronized
    fun stop() {
        if (!running) return
        running = false
        discoveryJob?.cancel()
        discoveryJob = null
        networkJob?.cancel()
        networkJob = null
        mdnsDiscovery.stop()
        lanNetworkMonitor.stop()
        perPeer.values.toList().forEach { it.shutdown() }
        perPeer.clear()
        _states.value = emptyMap()
    }

    /** Manually (re)connect to a single peer, regardless of [start]/[stop] state.
     *  Resets the auto-reconnect backoff so the user gets an immediate retry. */
    @Synchronized
    fun syncNow(peerId: String) {
        val peer = peerStore.byId(peerId) ?: return
        perPeer[peerId]?.shutdown()
        perPeer.remove(peerId)
        if (peer.disabled) {
            updateState(peerId, PeerState.Disabled)
            return
        }
        if (!canUseNetwork(peer)) {
            updateState(peerId, PeerState.WaitingForNetwork)
            return
        }
        startSession(peer, candidateMode = false)
    }

    @Synchronized
    private fun syncCandidateNow(peerId: String) {
        val trusted = peerStore.byId(peerId) ?: return
        val candidate = endpointCandidates.fresh(peerId) ?: return
        perPeer.remove(peerId)?.shutdown()
        val candidatePeer = trusted.copy(
            host = candidate.host,
            port = candidate.port,
        )
        startSession(candidatePeer, candidateMode = true)
    }

    @Synchronized
    fun setPeerEnabled(peerId: String, enabled: Boolean) {
        if (!peerStore.setEnabled(peerId, enabled)) return
        if (enabled) syncNow(peerId) else {
            perPeer.remove(peerId)?.shutdown()
            updateState(peerId, PeerState.Disabled)
        }
    }

    @Synchronized
    fun disconnect(peerId: String) {
        perPeer.remove(peerId)?.shutdown()
        updateState(peerId, PeerState.Idle)
    }

    private fun startSession(
        peer: SyncPeer,
        candidateMode: Boolean = false,
    ) {
        if (peer.disabled) {
            updateState(peer.id, PeerState.Disabled)
            return
        }
        if (!canUseNetwork(peer)) {
            updateState(peer.id, PeerState.WaitingForNetwork)
            return
        }
        updateState(peer.id, PeerState.Connecting)
        val session = PeerSession(peer, candidateMode)
        perPeer[peer.id] = session
        session.run()
    }

    private data class NetworkAvailability(
        val lan: Boolean,
        val internet: Boolean,
        val pearEnabled: Boolean,
        val pearReady: Boolean,
    )

    private fun canUseNetwork(peer: SyncPeer): Boolean = canUseNetwork(
        peer,
        NetworkAvailability(
            lan = lanNetworkMonitor.available.value,
            internet = lanNetworkMonitor.internetAvailable.value,
            pearEnabled = settingsRepository.settings.value.pearSyncEnabled,
            pearReady = pearRuntimeController.status.value is PearRuntimeStatus.Ready,
        ),
    )

    private fun canUseNetwork(peer: SyncPeer, availability: NetworkAvailability): Boolean =
        availability.lan || (
            availability.internet &&
                availability.pearEnabled &&
                availability.pearReady &&
                peer.pearPublicKey != null
            )

    private fun updateState(peerId: String, state: PeerState) {
        _states.value = _states.value.toMutableMap().apply { put(peerId, state) }
    }

    /**
     * Per-hash receive buffer for an in-flight `BlobRequest`/`BlobChunk` exchange.
     * Lifted out of [PeerSession] because Kotlin disallows `data class` inside an
     * inner class — the data class lives at the manager level instead.
     */
    private data class PendingFetch(
        val chatId: String,
        val profileId: String,
        val mediaType: String,
        val tempFile: File,
        val output: BufferedOutputStream,
        var received: Int = 0,
        var lastActivityAt: Long = System.currentTimeMillis(),
    )

    private inner class PeerSession(
        private val peer: SyncPeer,
        private var candidateMode: Boolean,
    ) {
        private var session: SyncClient.Session? = null
        private var liveJob: Job? = null
        private var reconnectJob: Job? = null
        private var transferCleanupJob: Job? = null
        private var reconnectAttempt = 0
        @Volatile private var canceled = false
        @Volatile private var identityFailure = false
        @Volatile private var switchingEndpoint = false
        private var sentCount = 0
        private var receivedCount = 0
        private var peerName: String = peer.name
        private val blobMutex = Mutex()

        // Outbound watermark: highest HLC the peer has acked from us. Drives our catchup.
        @Volatile private var lastAckedHlc: HlcTimestamp? = peerStore.lastSentHlc(peer)

        // Inbound watermark: highest HLC we've received and applied from the peer. Sent
        // as `Hello.sinceHlc` so the peer knows where to resume sending us.
        @Volatile private var lastReceivedHlc: HlcTimestamp? = peerStore.lastReceivedHlc(peer)

        fun run() {
            peerStore.recordRetry(peer.id, null)
            startTransferCleanup()
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
                        identityFailure = false
                        if (candidateMode) {
                            peerStore.promoteAuthenticatedEndpoint(
                                peer.id,
                                peer.host,
                                peer.port,
                            )
                            endpointCandidates.remove(peer.id)
                            candidateMode = false
                            Log.i(tag, "Promoted authenticated endpoint for ${peer.id}")
                        }
                        peerName = AuthenticatedDeviceName.refresh(peer.name, event.peerName)
                        peerStore.updateAuthenticatedName(peer.id, peerName)
                        peerStore.recordAuthenticated(peer.id, peer.host, peer.port)
                        reconnectAttempt = 0 // healthy session — reset backoff
                        updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount))
                        startCatchupAndLiveForwarding(checkNotNull(session))
                    }
                    is SyncClient.Event.Acked -> {
                        val now = System.currentTimeMillis()
                        if (!HlcTimestamp.isAcceptableRemote(event.upToHlc, now)) {
                            rejectClockSkew("acknowledgement")
                        } else {
                            peerStore.updateLastSent(peer.id, event.upToHlc, now)
                            val cur = lastAckedHlc
                            if (cur == null || event.upToHlc > cur) lastAckedHlc = event.upToHlc
                        }
                    }
                    is SyncClient.Event.EventReceived -> {
                        scope.launch { handleInbound(event.event) }
                    }
                    is SyncClient.Event.BlobRequested -> {
                        scope.launch { serveBlob(event.hash, event.mediaType) }
                    }
                    is SyncClient.Event.BlobChunkReceived -> {
                        scope.launch {
                            blobMutex.withLock {
                                handleBlobChunkInbound(
                                    hash = event.hash,
                                    mediaType = event.mediaType,
                                    offset = event.offset,
                                    totalSize = event.totalSize,
                                    data = event.data,
                                    isLast = event.isLast
                                )
                            }
                        }
                    }
                    is SyncClient.Event.BlobMissingReceived -> {
                        discardPending(event.hash)
                    }
                    is SyncClient.Event.Failed -> {
                        Log.w(tag, "Peer ${peer.id} failed: ${event.reason}")
                        identityFailure = SyncFailurePolicy.stopsAutomaticRetry(event.reason)
                        val displayReason = SyncFailurePolicy.userFacingReason(event.reason)
                        peerStore.recordError(peer.id, displayReason)
                        updateState(
                            peer.id,
                            if (peer.foregroundOnly && isTemporaryTransportFailure(event.reason)) {
                                PeerState.Unavailable("Open Ncy and enable phone sync on ${peer.name}")
                            } else PeerState.Failed(displayReason)
                        )
                        if (!switchToAlternativeEndpoint() && !identityFailure) scheduleReconnect()
                    }
                    is SyncClient.Event.Disconnected -> {
                        liveJob?.cancel()
                        liveJob = null
                        if (_states.value[peer.id] !is PeerState.Failed &&
                            _states.value[peer.id] !is PeerState.Unavailable
                        ) {
                            updateState(
                                peer.id,
                                if (peer.foregroundOnly && event.reason == PHONE_HOST_BACKGROUND_REASON) {
                                    PeerState.Unavailable("Open Ncy and enable phone sync on ${peer.name}")
                                } else PeerState.Idle
                            )
                        }
                        if (!switchingEndpoint && !identityFailure) scheduleReconnect()
                    }
                }
            }
        }

        // Phase 2b: per-hash receive buffers. Populated when we send a BlobRequest, drained
        // by incoming BlobChunk frames. Caller-side state — the `peer` we requested from
        // is implicit (this PeerSession owns it).
        private val pendingFetches = mutableMapOf<String, PendingFetch>()

        private suspend fun handleInbound(event: ChatEvent) {
            // Defensive loop guard: an event that originated from this device should never
            // be applied (the peer is echoing it back). Should not happen if the peer
            // implements its own sender-side loop guard, but cheap to check.
            // Always offer own-origin replays to the idempotent event store. A normal
            // echo is rejected by eventId; a reinstall that lost the event recovers it.
            if (!HlcTimestamp.isAcceptableRemote(event.hlc, System.currentTimeMillis())) {
                rejectClockSkew("event")
                return
            }
            val routedEvent = chatRepository.canonicalizeInboundEvent(event)
            val newlyApplied = chatRepository.applyExternalEvent(routedEvent)
            session?.ack(routedEvent.hlc)
            peerStore.updateLastReceived(peer.id, routedEvent.hlc)
            val cur = lastReceivedHlc
            if (cur == null || routedEvent.hlc > cur) lastReceivedHlc = routedEvent.hlc
            if (!newlyApplied) return
            receivedCount++
            updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount))

            // Phase 2b: kick off blob fetches for any media references we don't have yet.
            scope.launch { fetchMissingMedia(routedEvent) }
        }

        private suspend fun fetchMissingMedia(event: ChatEvent) {
            val s = session ?: return
            for ((hash, mediaType) in extractMediaRefs(event)) {
                if (chatRepository.mediaFileForHash(hash, mediaType, event.profileId) != null) continue
                val newlyPending = synchronized(this@PeerSession) {
                    if (pendingFetches.containsKey(hash) || pendingFetches.size >= SyncLimits.MAX_PENDING_BLOBS) false
                    else {
                        val temp = SyncLimits.newBlobTempFile(chatRepository.getBaseDir())
                        pendingFetches[hash] = PendingFetch(
                            chatId = event.chatId,
                            profileId = event.profileId,
                            mediaType = mediaType,
                            tempFile = temp,
                            output = BufferedOutputStream(temp.outputStream())
                        )
                        true
                    }
                }
                if (newlyPending) s.requestBlob(hash, mediaType)
            }
        }

        private suspend fun serveBlob(hash: String, mediaType: String) {
            val s = session ?: return
            val file = chatRepository.mediaFileForHash(hash, mediaType)
            if (file == null || !file.isFile) {
                s.sendBlobMissing(hash)
                return
            }
            if (file.length() > SyncLimits.MAX_BLOB_BYTES) {
                Log.w(tag, "refusing oversized blob $hash (${file.length()} bytes)")
                s.sendBlobMissing(hash)
                return
            }
            val total = file.length().toInt()
            val encoder = Base64.getEncoder()
            if (total == 0) {
                s.sendBlobChunk(hash, mediaType, 0, 0, "", isLast = true)
                return
            }
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(CHUNK_SIZE)
                var offset = 0
                while (offset < total) {
                    val n = input.read(buffer)
                    if (n <= 0) return
                    val data = encoder.encodeToString(if (n == buffer.size) buffer else buffer.copyOf(n))
                    val isLast = offset + n >= total
                    if (!s.sendBlobChunk(hash, mediaType, offset, total, data, isLast)) return
                    offset += n
                }
            }
        }

        private suspend fun handleBlobChunkInbound(
            hash: String,
            mediaType: String,
            offset: Int,
            totalSize: Int,
            data: String,
            isLast: Boolean
        ) {
            val pending = synchronized(this@PeerSession) { pendingFetches[hash] }
            if (pending == null) return // unsolicited / already finalized; ignore
            if (totalSize !in 0..SyncLimits.MAX_BLOB_BYTES ||
                data.length > SyncLimits.MAX_BASE64_CHARS ||
                offset < 0 || offset > totalSize
            ) {
                discardPending(hash)
                return
            }
            val bytes = try {
                Base64.getDecoder().decode(data)
            } catch (e: Exception) {
                Log.w(tag, "blob chunk: bad base64 for $hash", e)
                discardPending(hash)
                return
            }
            if (bytes.size > SyncLimits.MAX_CHUNK_BYTES ||
                pending.received + bytes.size > totalSize ||
                pending.received + bytes.size > SyncLimits.MAX_BLOB_BYTES
            ) {
                discardPending(hash)
                return
            }
            // Expect strictly in-order chunks. Drop the fetch if the sender deviates so we
            // don't end up with garbage assembled bytes.
            if (offset != pending.received) {
                Log.w(tag, "out-of-order blob chunk for $hash (got $offset, expected ${pending.received})")
                discardPending(hash)
                return
            }
            pending.output.write(bytes)
            pending.received += bytes.size
            pending.lastActivityAt = System.currentTimeMillis()
            if (!isLast) return

            synchronized(this@PeerSession) { pendingFetches.remove(hash) }
            pending.output.close()
            if (pending.received != totalSize) {
                Log.w(tag, "blob $hash size mismatch: got ${pending.received} expected $totalSize")
                pending.tempFile.delete()
                return
            }
            val computed = MediaHash.ofFile(pending.tempFile)
            if (computed != hash) {
                Log.w(tag, "blob hash mismatch: expected=$hash got=$computed")
                pending.tempFile.delete()
                return
            }
            chatRepository.saveMediaBlobFromFile(
                pending.chatId,
                hash,
                pending.mediaType,
                pending.tempFile,
                pending.profileId,
            )
            // Tell any open ChatScreen to re-render — the materialized message hasn't
            // changed but the file it references now exists on disk.
            eventBus.notifyMediaArrived(pending.chatId)
        }

        private fun pushCatchup() {
            val s = session ?: return
            val fullReplay = lastAckedHlc == null
            val pending = snapshot.eventsSince(lastAckedHlc)
            for (event in pending) {
                if (!sendEvent(s, event, fullReplay)) return
            }
            updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount))
        }

        private fun startCatchupAndLiveForwarding(s: SyncClient.Session) {
            liveJob?.cancel()
            liveJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
                coroutineScope {
                    // Subscribe synchronously before reading the snapshot. Local events
                    // queue only for the finite handoff, then forwarding becomes direct.
                    val handoffLock = Mutex()
                    val queuedLive = ArrayDeque<ChatEvent>()
                    var handoffComplete = false
                    val collector = launch(start = CoroutineStart.UNDISPATCHED) {
                        eventBus.events.collect { event ->
                            handoffLock.withLock {
                                if (handoffComplete) sendEvent(s, event)
                                else queuedLive.addLast(event)
                            }
                        }
                    }
                    try {
                        yield()
                        pushCatchup()
                        handoffLock.withLock {
                            handoffComplete = true
                            while (queuedLive.isNotEmpty()) {
                                if (!sendEvent(s, queuedLive.removeFirst())) break
                            }
                        }
                        collector.join()
                    } finally {
                        collector.cancel()
                        queuedLive.clear()
                    }
                }
            }
        }

        @Synchronized
        private fun sendEvent(
            s: SyncClient.Session,
            event: ChatEvent,
            fullReplay: Boolean = false,
        ): Boolean {
            // Loop prevention: don't send events back to the peer that authored them.
            // (Matters once events from peer A get republished to the bus by `applyExternalEvent`
            // and would otherwise be forwarded straight back to A.)
            if (!SyncCatchupPolicy.shouldSend(event.originDevice, peer.id, fullReplay)) return true
            // Skip events the peer already acked (cheap dedupe; the receiver also tolerates
            // duplicates by event id, but we save bandwidth).
            val acked = lastAckedHlc
            if (acked != null && event.hlc <= acked) return true
            if (!s.send(event)) return false
            sentCount++
            updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount))
            return true
        }

        /**
         * Auto-reconnect with capped exponential backoff. Idempotent — calls during an
         * already-pending retry are no-ops. Reset by [SyncClient.Event.Authenticated] and
         * by `shutdown()`.
         */
        private fun scheduleReconnect() {
            if (canceled || identityFailure) return
            if (!canUseNetwork(peer)) {
                reconnectJob?.cancel()
                reconnectJob = null
                updateState(peer.id, PeerState.WaitingForNetwork)
                return
            }
            if (reconnectJob != null) return
            val attempt = reconnectAttempt
            reconnectAttempt = attempt + 1
            val delayMs = (1_000L shl attempt.coerceAtMost(6)).coerceAtMost(60_000L)
            peerStore.recordRetry(peer.id, System.currentTimeMillis() + delayMs)
            Log.i(tag, "Reconnecting peer ${peer.id} in ${delayMs}ms (attempt ${attempt + 1})")
            reconnectJob = scope.launch {
                delay(delayMs)
                reconnectJob = null
                if (canceled) return@launch
                synchronized(this@SyncManager) {
                    if (!running) return@launch
                    if (perPeer[peer.id] !== this@PeerSession) return@launch
                }
                if (!canUseNetwork(peer)) {
                    updateState(peer.id, PeerState.WaitingForNetwork)
                    return@launch
                }
                // Tear down whatever's left of the prior socket and start fresh.
                session?.close()
                session = null
                liveJob?.cancel()
                liveJob = null
                run()
            }
        }

        /**
         * A trusted endpoint is always attempted first. After it fails, one fresh
         * mDNS candidate may be tried; a failed candidate is discarded and the
         * persisted authenticated endpoint becomes the fallback again.
         */
        private fun switchToAlternativeEndpoint(): Boolean {
            if (switchingEndpoint || canceled) return switchingEndpoint
            val trusted = peerStore.byId(peer.id) ?: return false
            val (nextPeer, nextIsCandidate) = if (candidateMode) {
                endpointCandidates.remove(peer.id)
                if (trusted.host == peer.host && trusted.port == peer.port) return false
                trusted to false
            } else {
                val candidate = endpointCandidates.fresh(peer.id) ?: return false
                trusted.copy(
                    host = candidate.host,
                    port = candidate.port,
                ) to true
            }
            switchingEndpoint = true
            scope.launch {
                yield()
                synchronized(this@SyncManager) {
                    if (!running || perPeer[peer.id] !== this@PeerSession) return@synchronized
                    perPeer.remove(peer.id)
                    this@PeerSession.shutdown()
                    startSession(nextPeer, nextIsCandidate)
                }
            }
            return true
        }

        fun pauseForNetwork() {
            reconnectJob?.cancel()
            reconnectJob = null
            liveJob?.cancel()
            liveJob = null
            session?.close()
            session = null
            updateState(peer.id, PeerState.WaitingForNetwork)
        }

        private fun isTemporaryTransportFailure(reason: String): Boolean {
            val lower = reason.lowercase()
            return !lower.contains("auth_failed") &&
                !lower.contains("invalid pairing secret") &&
                !lower.contains("protocol") &&
                !lower.contains("malformed") &&
                !lower.contains("rejected")
        }

        private fun rejectClockSkew(kind: String) {
            val reason = "Clock mismatch: remote $kind is more than 5 minutes ahead"
            updateState(peer.id, PeerState.Failed(reason))
            session?.sendError("clock_skew", reason)
            session?.close()
        }

        fun shutdown() {
            canceled = true
            reconnectJob?.cancel()
            reconnectJob = null
            liveJob?.cancel()
            transferCleanupJob?.cancel()
            transferCleanupJob = null
            synchronized(this@PeerSession) {
                pendingFetches.values.forEach { pending ->
                    runCatching { pending.output.close() }
                    pending.tempFile.delete()
                }
                pendingFetches.clear()
            }
            session?.close()
        }

        private fun discardPending(hash: String) {
            val pending = synchronized(this@PeerSession) { pendingFetches.remove(hash) } ?: return
            runCatching { pending.output.close() }
            pending.tempFile.delete()
        }

        private fun startTransferCleanup() {
            if (transferCleanupJob != null) return
            transferCleanupJob = scope.launch {
                while (isActive && !canceled) {
                    delay(SyncLimits.TRANSFER_SWEEP_MS)
                    blobMutex.withLock {
                        val cutoff = System.currentTimeMillis() - SyncLimits.TRANSFER_TIMEOUT_MS
                        val stale = synchronized(this@PeerSession) {
                            pendingFetches.filterValues { it.lastActivityAt < cutoff }.keys.toList()
                        }
                        stale.forEach { hash ->
                            Log.w(tag, "discarding stalled blob transfer $hash")
                            discardPending(hash)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        /** Outbound blob chunk size, bytes (pre-base64). 64 KiB keeps each WS frame small. */
        const val CHUNK_SIZE = 64 * 1024
        const val PHONE_HOST_BACKGROUND_REASON = "phone_host_left_foreground"

        /**
         * Pull every (hash, mediaType) reference out of an event's payload. Skips refs
         * that don't match the post-phase-2b content-addressed naming pattern (legacy
         * random-named media stays untransferable).
         */
        fun extractMediaRefs(event: ChatEvent): List<Pair<String, String>> {
            val refs = mutableListOf<Pair<String, String>>()
            when (val op = event.op) {
                is ChatEventOp.MessageCreated -> collectFromMessage(op.message, refs)
                is ChatEventOp.MessageEdited -> collectFromMessage(op.message, refs)
                is ChatEventOp.ChatMetadataChanged -> {
                    // Phase 2b slice 2: avatars (and backgrounds, even though no client
                    // renders them yet) carry hashed refs that the receiver fetches.
                    MediaHash.extractHash(op.metadata.avatarFile)?.let { refs += it to "image" }
                    MediaHash.extractHash(op.metadata.backgroundFile)?.let { refs += it to "image" }
                }
                else -> { /* tombstones carry no media refs */ }
            }
            return refs.distinct()
        }

        private fun collectFromMessage(
            message: com.ncy.notechaty.data.model.Message,
            refs: MutableList<Pair<String, String>>
        ) {
            MediaHash.extractHash(message.voiceFile)?.let { refs += it to "voice" }
            for (img in message.imageFiles) {
                MediaHash.extractHash(img)?.let { refs += it to "image" }
            }
            for (attachment in message.fileAttachments) {
                MediaHash.extractHash(attachment.file)?.let { refs += it to "file" }
            }
            for (block in message.content) {
                for (span in block.spans) {
                    MediaHash.extractHash(span.inlineImage)?.let { refs += it to "image" }
                }
            }
        }
    }
}
