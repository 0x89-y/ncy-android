package com.ncy.notechaty.data.sync

import android.util.Log
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.ChatEventOp
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
import kotlinx.coroutines.delay
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

    /** Manually (re)connect to a single peer, regardless of [start]/[stop] state.
     *  Resets the auto-reconnect backoff so the user gets an immediate retry. */
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

    /**
     * Per-hash receive buffer for an in-flight `BlobRequest`/`BlobChunk` exchange.
     * Lifted out of [PeerSession] because Kotlin disallows `data class` inside an
     * inner class — the data class lives at the manager level instead.
     */
    private data class PendingFetch(
        val chatId: String,
        val mediaType: String,
        val buffer: ByteArrayOutputStream
    )

    private inner class PeerSession(private val peer: SyncPeer) {
        private var session: SyncClient.Session? = null
        private var liveJob: Job? = null
        private var reconnectJob: Job? = null
        private var reconnectAttempt = 0
        @Volatile private var canceled = false
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
                        reconnectAttempt = 0 // healthy session — reset backoff
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
                    is SyncClient.Event.BlobRequested -> {
                        scope.launch { serveBlob(event.hash, event.mediaType) }
                    }
                    is SyncClient.Event.BlobChunkReceived -> {
                        scope.launch {
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
                    is SyncClient.Event.BlobMissingReceived -> {
                        synchronized(this@PeerSession) { pendingFetches.remove(event.hash) }
                    }
                    is SyncClient.Event.Failed -> {
                        Log.w(tag, "Peer ${peer.id} failed: ${event.reason}")
                        updateState(peer.id, PeerState.Failed(event.reason))
                        scheduleReconnect()
                    }
                    is SyncClient.Event.Disconnected -> {
                        liveJob?.cancel()
                        liveJob = null
                        if (_states.value[peer.id] !is PeerState.Failed) {
                            updateState(peer.id, PeerState.Idle)
                        }
                        scheduleReconnect()
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

            // Phase 2b: kick off blob fetches for any media references we don't have yet.
            scope.launch { fetchMissingMedia(event) }
        }

        private suspend fun fetchMissingMedia(event: ChatEvent) {
            val s = session ?: return
            for ((hash, mediaType) in extractMediaRefs(event)) {
                if (chatRepository.mediaFileForHash(hash, mediaType) != null) continue
                val newlyPending = synchronized(this@PeerSession) {
                    if (pendingFetches.containsKey(hash)) false
                    else {
                        pendingFetches[hash] = PendingFetch(
                            chatId = event.chatId,
                            mediaType = mediaType,
                            buffer = ByteArrayOutputStream()
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
            val bytes = file.readBytes()
            val total = bytes.size
            val encoder = Base64.getEncoder()
            if (total == 0) {
                s.sendBlobChunk(hash, mediaType, 0, 0, "", isLast = true)
                return
            }
            var offset = 0
            while (offset < total) {
                val n = minOf(CHUNK_SIZE, total - offset)
                val slice = bytes.copyOfRange(offset, offset + n)
                val data = encoder.encodeToString(slice)
                val isLast = offset + n >= total
                if (!s.sendBlobChunk(hash, mediaType, offset, total, data, isLast)) return
                offset += n
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
            val bytes = try {
                Base64.getDecoder().decode(data)
            } catch (e: Exception) {
                Log.w(tag, "blob chunk: bad base64 for $hash", e)
                synchronized(this@PeerSession) { pendingFetches.remove(hash) }
                return
            }
            // Expect strictly in-order chunks. Drop the fetch if the sender deviates so we
            // don't end up with garbage assembled bytes.
            if (offset != pending.buffer.size()) {
                Log.w(tag, "out-of-order blob chunk for $hash (got $offset, expected ${pending.buffer.size()})")
                synchronized(this@PeerSession) { pendingFetches.remove(hash) }
                return
            }
            pending.buffer.write(bytes)
            if (!isLast) return

            val collected = pending.buffer.toByteArray()
            synchronized(this@PeerSession) { pendingFetches.remove(hash) }
            if (collected.size != totalSize) {
                Log.w(tag, "blob $hash size mismatch: got ${collected.size} expected $totalSize")
                return
            }
            val computed = MediaHash.ofBytes(collected)
            if (computed != hash) {
                Log.w(tag, "blob hash mismatch: expected=$hash got=$computed")
                return
            }
            chatRepository.saveMediaBlob(pending.chatId, hash, pending.mediaType, collected)
            // Tell any open ChatScreen to re-render — the materialized message hasn't
            // changed but the file it references now exists on disk.
            eventBus.notifyMediaArrived(pending.chatId)
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

        /**
         * Auto-reconnect with capped exponential backoff. Idempotent — calls during an
         * already-pending retry are no-ops. Reset by [SyncClient.Event.Authenticated] and
         * by `shutdown()`.
         */
        private fun scheduleReconnect() {
            if (canceled) return
            if (reconnectJob != null) return
            val attempt = reconnectAttempt
            reconnectAttempt = attempt + 1
            val delayMs = (1_000L shl attempt.coerceAtMost(6)).coerceAtMost(60_000L)
            Log.i(tag, "Reconnecting peer ${peer.id} in ${delayMs}ms (attempt ${attempt + 1})")
            reconnectJob = scope.launch {
                delay(delayMs)
                reconnectJob = null
                if (canceled) return@launch
                synchronized(this@SyncManager) {
                    if (!running) return@launch
                    if (perPeer[peer.id] !== this@PeerSession) return@launch
                }
                // Tear down whatever's left of the prior socket and start fresh.
                session?.close()
                session = null
                liveJob?.cancel()
                liveJob = null
                run()
            }
        }

        fun shutdown() {
            canceled = true
            reconnectJob?.cancel()
            reconnectJob = null
            liveJob?.cancel()
            session?.close()
        }
    }

    private companion object {
        /** Outbound blob chunk size, bytes (pre-base64). 64 KiB keeps each WS frame small. */
        const val CHUNK_SIZE = 64 * 1024

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
            for (block in message.content) {
                for (span in block.spans) {
                    MediaHash.extractHash(span.inlineImage)?.let { refs += it to "image" }
                }
            }
        }
    }
}
