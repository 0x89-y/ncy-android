package com.ncy.notechaty.data.sync

import android.util.Log
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.ChatEventOp
import com.ncy.notechaty.data.sync.event.HlcTimestamp
import com.ncy.notechaty.data.sync.event.Receipts
import com.ncy.notechaty.data.sync.event.SyncSnapshot
import com.ncy.notechaty.data.sync.media.MediaBackfill
import com.ncy.notechaty.data.sync.media.MediaHash
import com.ncy.notechaty.data.sync.media.MediaRefs
import com.ncy.notechaty.data.sync.media.MediaTransferState
import com.ncy.notechaty.data.sync.discovery.MdnsDiscovery
import com.ncy.notechaty.data.sync.discovery.LanNetworkMonitor
import com.ncy.notechaty.data.sync.discovery.EndpointCandidateTracker
import com.ncy.notechaty.data.sync.peer.SyncPeer
import com.ncy.notechaty.data.sync.peer.SyncPeerStore
import com.ncy.notechaty.data.sync.transport.SyncClient
import com.ncy.notechaty.data.sync.transport.SyncCatchupPolicy
import com.ncy.notechaty.data.sync.transport.SyncLimits
import com.ncy.notechaty.data.sync.transport.ClientSyncTransport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
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
    private val transferState: MediaTransferState,
) {
    private val tag = "SyncManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val perPeer = mutableMapOf<String, PeerSession>()
    private val endpointCandidates = EndpointCandidateTracker()

    private val _states = MutableStateFlow<Map<String, PeerState>>(emptyMap())
    val states: StateFlow<Map<String, PeerState>> = _states.asStateFlow()

    fun downloadFileAnyway(
        chatId: String,
        profileId: String,
        hash: String,
        mediaType: String = "file",
    ) {
        scope.launch {
            val sessions = synchronized(this@SyncManager) { perPeer.values.toList() }
            for (peerSession in sessions) {
                if (peerSession.requestFileAnyway(chatId, profileId, hash, mediaType)) break
            }
        }
    }

    /** Stop an in-flight download on whichever session owns it. */
    fun cancelInboundBlob(hash: String) {
        val sessions = synchronized(this) { perPeer.values.toList() }
        sessions.forEach { it.cancelInboundBlob(hash) }
    }

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
            val receivedCount: Int = 0,
            val transport: ClientSyncTransport.Kind? = null,
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
            lanNetworkMonitor.available.collect { lan ->
                peerStore.peers.value.forEach { peer ->
                    val usable = canUseNetwork(peer, lan)
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

    private fun canUseNetwork(peer: SyncPeer): Boolean =
        canUseNetwork(peer, lanNetworkMonitor.available.value)

    /** Sync runs over the local network only, so a peer needs both it and an address on it. */
    private fun canUseNetwork(peer: SyncPeer, lanAvailable: Boolean): Boolean =
        lanAvailable && peer.hasLanEndpoint

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
        var output: BufferedOutputStream,
        var received: Int = 0,
        var lastActivityAt: Long = System.currentTimeMillis(),
    ) {
        // Shutdown can race the final chunk; only the first release may drop the
        // claim, or it could drop one another transfer has taken since.
        private val released = java.util.concurrent.atomic.AtomicBoolean(false)

        fun releaseClaim() {
            if (released.compareAndSet(false, true)) SyncLimits.releasePartial(tempFile)
        }
    }

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
        private var activeTransport: ClientSyncTransport.Kind? = null
        private val blobMutex = Mutex()

        // Outbound watermark: highest HLC the peer has acked from us. Drives our catchup.
        @Volatile private var lastAckedHlc: HlcTimestamp? = peerStore.lastSentHlc(peer)

        // Inbound watermark: highest HLC we've received and applied from the peer. Sent
        // as `Hello.sinceHlc` so the peer knows where to resume sending us.
        @Volatile private var lastReceivedHlc: HlcTimestamp? = peerStore.lastReceivedHlc(peer)

        // Receipt positions (§10.9), mirrored from the store like the HLC watermarks.
        @Volatile private var remoteReceiptEpoch: String? = peer.remoteReceiptEpoch
        @Volatile private var lastReceivedReceipt: Long? = peer.lastReceivedReceipt
        @Volatile private var ackedReceiptEpoch: String? = peer.ackedReceiptEpoch
        @Volatile private var lastAckedReceipt: Long? = peer.lastAckedReceipt
        /** This connection's server numbers what it sends and echoes our receipts. */
        @Volatile private var receiptMode = false
        /** Highest of our receipts sent on this connection; dedupes catch-up vs live. */
        private var lastSentReceipt = 0L

        fun run() {
            peerStore.recordRetry(peer.id, null)
            startTransferCleanup()
            receiptMode = false
            // A full replay (no HLC watermark) must not carry a receipt position, or
            // the server would send only what follows it.
            val (helloEpoch, helloSince) =
                if (lastReceivedHlc == null) "" to null
                else (remoteReceiptEpoch ?: "") to lastReceivedReceipt
            session = syncClient.connect(
                peer = peer,
                localDeviceId = deviceIdentity.deviceId,
                sinceHlc = lastReceivedHlc,
                receiptEpoch = helloEpoch,
                sinceReceipt = helloSince,
            ) { event ->
                when (event) {
                    is SyncClient.Event.Connecting -> {
                        updateState(peer.id, PeerState.Connecting)
                    }
                    is SyncClient.Event.TransportSelected -> {
                        activeTransport = event.kind
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
                        event.receiptEpoch?.let { epoch ->
                            val fullReplay = lastReceivedHlc == null
                            if (fullReplay || remoteReceiptEpoch != epoch) {
                                remoteReceiptEpoch = epoch
                                lastReceivedReceipt = null
                            }
                            peerStore.adoptRemoteReceiptEpoch(peer.id, epoch, fullReplay)
                        }
                        receiptMode = event.receiptEpoch != null && Receipts.epoch() != null
                        reconnectAttempt = 0 // healthy session — reset backoff
                        updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount, activeTransport))
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
                            val ownEpoch = Receipts.epoch()
                            val acked = event.upToReceipt
                            if (acked != null && ownEpoch != null) {
                                val same = ackedReceiptEpoch == ownEpoch
                                if (!same || (lastAckedReceipt ?: 0L) < acked) {
                                    ackedReceiptEpoch = ownEpoch
                                    lastAckedReceipt = acked
                                }
                                peerStore.updateLastAckedReceipt(peer.id, acked, ownEpoch)
                            }
                        }
                    }
                    is SyncClient.Event.EventReceived -> {
                        inbound.trySend(event)
                    }
                    is SyncClient.Event.BlobRequested -> {
                        val hash = event.hash
                        val job = scope.launch {
                            serveBlob(hash, event.mediaType, event.offset.toInt())
                        }
                        servingJobs[hash] = job
                        job.invokeOnCompletion { servingJobs.remove(hash) }
                    }
                    is SyncClient.Event.BlobCancelled -> {
                        // The peer gave up on this download. Stop mid-stream rather than
                        // uploading the rest of a file nobody is going to keep.
                        servingJobs.remove(event.hash)?.let {
                            Log.i(tag, "blob ${event.hash}: peer cancelled; stopping upload")
                            it.cancel()
                        }
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
                        discardPending(event.hash, "the other device no longer has this file")
                    }
                    is SyncClient.Event.Failed -> {
                        Log.w(tag, "Peer ${peer.id} failed: ${event.reason}")
                        identityFailure =
                            SyncFailurePolicy.stopsAutomaticRetry(event.reason, event.authenticated)
                        val displayReason =
                            SyncFailurePolicy.userFacingReason(event.reason, event.authenticated)
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

        /**
         * Inbound events, applied one at a time in arrival order. Each used to get its
         * own coroutine, and the watermark only moves forward — so a later event that
         * finished first could carry it past one that then failed, or was lost to the
         * process dying, and catch-up would never resend it. Outlives reconnects;
         * closed by [shutdown], which lets what already arrived finish applying.
         */
        private val inbound = Channel<SyncClient.Event.EventReceived>(Channel.UNLIMITED)

        init {
            scope.launch { applyInboundInOrder() }
        }

        private suspend fun applyInboundInOrder() {
            for (frame in inbound) {
                val event = frame.event
                val applied = try {
                    handleInbound(event, frame.receipt)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Unacked, so the reconnect's Hello.sinceHlc asks for it again.
                    Log.w(tag, "could not apply event ${event.eventId}; reconnecting", e)
                    session?.close()
                    false
                }
                // Nothing queued behind a rejected event may be acked, or the
                // watermark would skip it. The peer resends all of it.
                if (!applied) while (inbound.tryReceive().isSuccess) Unit
            }
        }

        /** False when the event was rejected and the session is being torn down. */
        private suspend fun handleInbound(event: ChatEvent, receipt: Long?): Boolean {
            // Defensive loop guard: an event that originated from this device should never
            // be applied (the peer is echoing it back). Should not happen if the peer
            // implements its own sender-side loop guard, but cheap to check.
            // Always offer own-origin replays to the idempotent event store. A normal
            // echo is rejected by eventId; a reinstall that lost the event recovers it.
            if (!HlcTimestamp.isAcceptableRemote(event.hlc, System.currentTimeMillis())) {
                rejectClockSkew("event")
                return false
            }
            val routedEvent = chatRepository.canonicalizeInboundEvent(event)
            val newlyApplied = chatRepository.applyExternalEvent(routedEvent)
            session?.ack(routedEvent.hlc, receipt)
            peerStore.updateLastReceived(peer.id, routedEvent.hlc)
            val cur = lastReceivedHlc
            if (cur == null || routedEvent.hlc > cur) lastReceivedHlc = routedEvent.hlc
            // Applied strictly in arrival order, so this never passes an unstored event.
            if (receipt != null && receiptMode && receipt > (lastReceivedReceipt ?: 0L)) {
                lastReceivedReceipt = receipt
                peerStore.updateLastReceivedReceipt(peer.id, receipt)
            }
            if (!newlyApplied) return true
            receivedCount++
            updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount, activeTransport))

            // Phase 2b: kick off blob fetches for any media references we don't have yet.
            scope.launch { fetchMissingMedia(routedEvent) }
            return true
        }

        private suspend fun fetchMissingMedia(event: ChatEvent) {
            val s = session ?: return
            // Predates this device joining the mesh, so it is history rather than
            // something just sent: list it, do not pull it.
            if (!MediaBackfill.wantsAutomaticFetch(
                    event.hlc.physicalMs,
                    settingsRepository.settings.value.mediaBackfillCutoffMs,
                )
            ) return
            for (ref in extractMediaRefs(event)) {
                val hash = ref.hash
                val mediaType = ref.mediaType
                if (chatRepository.mediaFileForHash(hash, mediaType, event.profileId) != null) continue
                if (
                    mediaType == "file" &&
                    ref.sizeBytes != null &&
                    ref.sizeBytes > settingsRepository.settings.value.automaticFileDownloadLimitBytes
                ) continue
                val newlyPending = synchronized(this@PeerSession) {
                    if (pendingFetches.containsKey(hash) || pendingFetches.size >= SyncLimits.MAX_PENDING_BLOBS) false
                    else openPendingFetch(event.chatId, event.profileId, hash, mediaType)
                        ?.also { pendingFetches[hash] = it } != null
                }
                if (newlyPending) {
                    val from = pendingFetches[hash]?.received?.toLong() ?: 0L
                    s.requestBlob(hash, mediaType, from)
                }
            }
        }

        suspend fun requestFileAnyway(
            chatId: String,
            profileId: String,
            hash: String,
            mediaType: String = "file",
        ): Boolean {
            val s = session ?: return false
            if (!MediaHash.isValidHash(hash)) return false
            if (chatRepository.mediaFileForHash(hash, mediaType, profileId) != null) return true
            val newlyPending = synchronized(this@PeerSession) {
                if (pendingFetches.containsKey(hash) || pendingFetches.size >= SyncLimits.MAX_PENDING_BLOBS) {
                    false
                } else {
                    openPendingFetch(chatId, profileId, hash, mediaType)
                        ?.also { pendingFetches[hash] = it } != null
                }
            }
            if (!newlyPending) return pendingFetches.containsKey(hash)
            val from = pendingFetches[hash]?.received?.toLong() ?: 0L
            if (s.requestBlob(hash, mediaType, from)) return true
            discardPending(hash)
            return false
        }

        /**
         * Start tracking a fetch into its hash-named partial, resuming from whatever an
         * earlier attempt kept. Null when another transfer — this role or the phone
         * host, any peer — already owns that partial. Caller holds the session lock.
         */
        private fun openPendingFetch(
            chatId: String,
            profileId: String,
            hash: String,
            mediaType: String,
        ): PendingFetch? {
            val temp = SyncLimits.blobPartialFile(chatRepository.getBaseDir(), hash, mediaType)
            if (!SyncLimits.claimPartial(temp)) return null
            return runCatching {
                val alreadyHave = if (temp.isFile) temp.length() else 0L
                PendingFetch(
                    chatId = chatId,
                    profileId = profileId,
                    mediaType = mediaType,
                    tempFile = temp,
                    output = BufferedOutputStream(java.io.FileOutputStream(temp, true)),
                    received = alreadyHave.toInt(),
                )
            }.getOrElse {
                SyncLimits.releasePartial(temp)
                throw it
            }
        }

        /**
         * Block the serve loop while the transport's send queue is above
         * [SEND_QUEUE_HIGH_WATER]. Returns false if the session went away while waiting.
         */
        /** Give up on an inbound blob and ask the sender to stop streaming it. */
        fun cancelInboundBlob(hash: String) {
            discardPending(hash, MediaTransferState.CANCELLED)
            session?.cancelBlob(hash)
        }

        private suspend fun awaitSendQueueDrain(s: SyncClient.Session): Boolean {
            var waited = 0L
            while (s.queuedBytes() > SEND_QUEUE_HIGH_WATER) {
                if (session !== s) return false
                if (waited >= SEND_QUEUE_DRAIN_TIMEOUT_MS) {
                    Log.w(tag, "send queue did not drain in ${waited}ms; abandoning blob transfer")
                    return false
                }
                delay(SEND_QUEUE_POLL_MS)
                waited += SEND_QUEUE_POLL_MS
            }
            return true
        }

        /** In-flight uploads by blob hash, so a peer's cancel can interrupt one. */
        private val servingJobs = java.util.concurrent.ConcurrentHashMap<String, Job>()

        private suspend fun serveBlob(hash: String, mediaType: String, resumeFrom: Int = 0) {
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
            Log.i(tag, "serving blob $hash ($mediaType), $total bytes")
            val startedAt = System.currentTimeMillis()
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(CHUNK_SIZE)
                // Resume where the receiver stopped. Past the end it is clamped, so
                // a stale offset simply sends the whole blob again.
                var offset = resumeFrom.coerceIn(0, total)
                if (offset > 0) input.skip(offset.toLong())
                while (offset < total) {
                    // Cooperative cancellation *and* fairness. Nothing else in this
                    // loop suspends when the send queue is draining normally, so a
                    // `cancel()` would be ignored until the whole file finished, and
                    // the coroutine would hold its thread — contending on the channel
                    // lock for every chunk — while ordinary event frames waited behind
                    // it. `yield` both observes cancellation and lets others run.
                    yield()
                    // `sendBlobChunk` only enqueues. Serving at full speed outruns the
                    // socket, and OkHttp responds to a full 16 MiB buffer by *closing the
                    // connection* rather than blocking — which took the whole sync session
                    // down for any attachment over ~9.4 MB (a chunk is base64'd, then the
                    // sealed frame is base64'd again, so 16 MiB of queue is 9 MB of file).
                    // Wait for it to drain instead.
                    if (!awaitSendQueueDrain(s)) {
                        Log.w(tag, "blob $hash abandoned at $offset/$total (send queue never drained)")
                        return
                    }
                    val n = input.read(buffer)
                    if (n <= 0) return
                    val data = encoder.encodeToString(if (n == buffer.size) buffer else buffer.copyOf(n))
                    val isLast = offset + n >= total
                    if (!s.sendBlobChunk(hash, mediaType, offset, total, data, isLast)) {
                        Log.w(tag, "blob $hash send failed at $offset/$total")
                        return
                    }
                    offset += n
                }
            }
            val elapsed = System.currentTimeMillis() - startedAt
            Log.i(tag, "served blob $hash: $total bytes in ${elapsed}ms")
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
            if (offset == 0 && pending.received > 0) {
                // Older peer ignoring the resume offset. Start the file over in place:
                // abandoning the fetch left the sender streaming a file nobody kept,
                // and the user retrying by hand.
                runCatching { pending.output.close() }
                pending.output = BufferedOutputStream(java.io.FileOutputStream(pending.tempFile, false))
                pending.received = 0
            }
            if (offset != pending.received) {
                Log.w(tag, "out-of-order blob chunk for $hash (got $offset, expected ${pending.received})")
                discardPending(hash, keepPartial = false)
                return
            }
            pending.output.write(bytes)
            pending.received += bytes.size
            pending.lastActivityAt = System.currentTimeMillis()
            transferState.advanced(hash, pending.received.toLong(), totalSize.toLong())
            if (!isLast) return

            synchronized(this@PeerSession) { pendingFetches.remove(hash) }
            // Hold the claim until the file is moved or deleted, so nothing reopens
            // the partial while it is being hashed.
            try {
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
                transferState.finished(hash)
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
            } finally {
                pending.releaseClaim()
            }
        }

        private fun pushCatchup() {
            val s = session ?: return
            if (receiptMode) {
                pushReceiptCatchup(s)
            } else {
                val fullReplay = lastAckedHlc == null
                val pending = snapshot.eventsSince(lastAckedHlc)
                for (event in pending) {
                    if (!sendEvent(s, event, fullReplay)) return
                }
            }
            updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount, activeTransport))
        }

        /**
         * Toward a server that follows receipts (§10.9): everything after its last
         * acknowledged receipt while our epoch is unchanged; otherwise one transition
         * pass that sends what HLC catch-up would have, numbered, so the
         * acknowledgements set the position from then on.
         */
        private fun pushReceiptCatchup(s: SyncClient.Session) {
            val resume = lastAckedReceipt.takeIf { ackedReceiptEpoch == Receipts.epoch() }
            val floor = if (resume == null) lastAckedHlc else null
            // Never acknowledged anything: the server may be empty, so include what it
            // authored too, as HLC catch-up's full replay does.
            val fullReplay = resume == null && lastAckedHlc == null
            lastSentReceipt = resume ?: 0L
            for ((receipt, event) in snapshot.eventsAfterReceipt(resume ?: 0L, hlcFloor = floor)) {
                if (!sendEvent(s, event, fullReplay, receipt)) return
            }
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
            knownReceipt: Long? = null,
        ): Boolean {
            // Loop prevention: don't send events back to the peer that authored them.
            // (Matters once events from peer A get republished to the bus by `applyExternalEvent`
            // and would otherwise be forwarded straight back to A.)
            if (!SyncCatchupPolicy.shouldSend(event.originDevice, peer.id, fullReplay)) return true
            val receipt = if (receiptMode) knownReceipt ?: Receipts.receiptOf(event.eventId) else null
            if (receiptMode) {
                // No HLC filter: an event relayed here late with an old clock (from a
                // phone this device hosts) is exactly what that filter used to drop.
                // The receipt only removes the catch-up/live overlap.
                if (receipt != null && receipt <= lastSentReceipt) return true
            } else {
                // Skip events the peer already acked (cheap dedupe; the receiver also tolerates
                // duplicates by event id, but we save bandwidth).
                val acked = lastAckedHlc
                if (acked != null && event.hlc <= acked) return true
            }
            if (!s.send(event, receipt)) return false
            if (receipt != null && receipt > lastSentReceipt) lastSentReceipt = receipt
            sentCount++
            updateState(peer.id, PeerState.Connected(peerName, sentCount, receivedCount, activeTransport))
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
            inbound.close()
            reconnectJob?.cancel()
            reconnectJob = null
            liveJob?.cancel()
            transferCleanupJob?.cancel()
            transferCleanupJob = null
            synchronized(this@PeerSession) {
                pendingFetches.values.forEach { pending ->
                    // Kept on purpose: the next connection resumes from here.
                    runCatching { pending.output.close() }
                    pending.releaseClaim()
                }
                pendingFetches.clear()
            }
            session?.close()
        }

        /**
         * Stop tracking a transfer, keeping what it received unless the bytes are
         * known to be bad.
         *
         * A stall or a dropped connection leaves a usable prefix, and discarding it
         * is what made a large transfer over a flaky link unable to finish: every
         * attempt started from zero and met the same timeout.
         */
        private fun discardPending(
            hash: String,
            reason: String = "transfer failed",
            keepPartial: Boolean = true,
        ) {
            val pending = synchronized(this@PeerSession) { pendingFetches.remove(hash) } ?: return
            runCatching { pending.output.close() }
            if (!keepPartial) pending.tempFile.delete()
            pending.releaseClaim()
            transferState.failed(hash, reason)
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
                            discardPending(hash, "no data for ${SyncLimits.TRANSFER_TIMEOUT_MS / 1000}s")
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
         * Pause blob serving once this many bytes are queued but unsent. Well under
         * OkHttp's 16 MiB ceiling, which it enforces by closing the connection.
         */
        const val SEND_QUEUE_HIGH_WATER = 2L * 1024 * 1024
        const val SEND_QUEUE_POLL_MS = 25L
        /**
         * Give up on a transfer rather than hanging forever behind a dead socket.
         * Matched to [SyncLimits.TRANSFER_TIMEOUT_MS]: a shorter ceiling here abandons
         * transfers the receiver still considers healthy, which is what capped large
         * attachments at the point where the in-flight backlog took over a minute to
         * drain.
         */
        const val SEND_QUEUE_DRAIN_TIMEOUT_MS = SyncLimits.TRANSFER_TIMEOUT_MS

        /** Media a peer should fetch for an event. Shared with storage cleanup so
         *  "still referenced" means the same thing to both. */
        fun extractMediaRefs(event: ChatEvent): List<MediaRefs.Ref> = MediaRefs.extract(event)
    }
}
