package com.ncy.notechaty.data.sync.transport

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.ChatEventBus
import com.ncy.notechaty.data.sync.DeviceIdentity
import com.ncy.notechaty.data.sync.AuthenticatedDeviceName
import com.ncy.notechaty.data.sync.discovery.MdnsPrivacyLabel
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.ChatEventOp
import com.ncy.notechaty.data.sync.event.SyncSnapshot
import com.ncy.notechaty.data.sync.media.MediaHash
import com.ncy.notechaty.data.sync.peer.PhonePairingIdentity
import com.ncy.notechaty.data.sync.peer.PairingPayload
import com.ncy.notechaty.data.sync.pear.PearRuntimeController
import com.ncy.notechaty.data.sync.pear.PearRuntimeListener
import com.ncy.notechaty.data.sync.pear.PearRuntimeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.io.BufferedOutputStream
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/** Hosts the normal Ncy sync protocol so another Android phone can connect to this one. */
@Singleton
class PhoneSyncServer @Inject constructor(
    @ApplicationContext context: Context,
    private val identity: DeviceIdentity,
    private val pairingIdentity: PhonePairingIdentity,
    private val snapshot: SyncSnapshot,
    private val repository: ChatRepository,
    private val eventBus: ChatEventBus,
    private val settingsRepository: SettingsRepository,
    private val pearRuntimeController: PearRuntimeController,
) {
    data class HostedPeer(
        val id: String,
        val name: String,
        val lastSeenAt: Long,
        val connected: Boolean = false,
        /** Never rendered; persisted credential used to authenticate this device. */
        val credential: String = "",
        /** Pre-per-device migration record. Re-pairing replaces it with a unique credential. */
        val legacySharedCredential: Boolean = false,
    )

    data class PendingPairing(val deviceId: String, val deviceName: String)

    sealed class HostState {
        object Disabled : HostState()
        object Stopped : HostState()
        object Starting : HostState()
        data class Ready(
            val pairingJson: String,
            val host: String,
            val port: Int,
            val usedFallbackPort: Boolean,
            val lanAvailable: Boolean = true,
        ) : HostState()
        data class Failed(val reason: String) : HostState()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val connectivity =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val codec = SyncWireCodec()
    private val _enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    private val _state = MutableStateFlow<HostState>(
        if (_enabled.value) HostState.Stopped else HostState.Disabled
    )
    val state: StateFlow<HostState> = _state.asStateFlow()
    private val _hostedPeers = MutableStateFlow(loadHostedPeers())
    val hostedPeers: StateFlow<List<HostedPeer>> = _hostedPeers.asStateFlow()
    private val _pendingPairing = MutableStateFlow<PendingPairing?>(null)
    val pendingPairing: StateFlow<PendingPairing?> = _pendingPairing.asStateFlow()
    private var server: Server? = null

    fun downloadFileAnyway(chatId: String, profileId: String, hash: String) {
        scope.launch { server?.requestFileAnyway(chatId, profileId, hash) }
    }
    private var liveJob: Job? = null
    private var transferCleanupJob: Job? = null
    private var networkRefreshJob: Job? = null
    private var advertisingRestartJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val knownNetworks = java.util.concurrent.ConcurrentHashMap.newKeySet<Network>()
    private var familyRestartPending = false
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var advertisedHost: String? = null
    private var advertisedPort: Int? = null

    init {
        // Existing development builds recorded peer IDs but used one host-wide
        // secret. Preserve those connections, mark them visibly as legacy, and
        // rotate the invitation so every newly scanned code is one-device-only.
        if (_hostedPeers.value.any { it.credential.isBlank() }) {
            val legacy = pairingIdentity.secret
            _hostedPeers.value = _hostedPeers.value.map {
                if (it.credential.isBlank()) it.copy(
                    credential = legacy,
                    legacySharedCredential = true,
                ) else it
            }
            persistHostedPeers()
            pairingIdentity.regenerateSecret()
        }
    }

    @Synchronized
    fun start() {
        if (!_enabled.value) {
            _state.value = HostState.Disabled
            return
        }
        if (server != null) return
        val initialHost = localLanAddress()
        val port = selectServerPort(initialHost?.let(::isIpv6Host) == true)
        if (port == null) {
            _state.value = HostState.Failed("Could not find an available port for phone sync.")
            return
        }
        _state.value = HostState.Starting
        val next = Server(port, initialHost?.let(::isIpv6Host) == true)
        server = next
        next.isReuseAddr = true
        registerNetworkCallback()
        next.startPearSessions()
        next.start()
        liveJob = scope.launch {
            eventBus.events.collect { event -> next.broadcastEvent(event) }
        }
        transferCleanupJob = scope.launch {
            while (isActive) {
                delay(SyncLimits.TRANSFER_SWEEP_MS)
                next.expireStalledTransfers()
            }
        }
    }

    @Synchronized
    fun stop(reason: String = CLOSE_REASON_FOREGROUND) {
        liveJob?.cancel()
        liveJob = null
        transferCleanupJob?.cancel()
        transferCleanupJob = null
        networkRefreshJob?.cancel()
        networkRefreshJob = null
        advertisingRestartJob?.cancel()
        advertisingRestartJob = null
        unregisterNetworkCallback()
        stopAdvertising()
        server?.let { current ->
            current.closeAll(reason)
            current.stopPearSessions()
            runCatching { current.stop(500) }
        }
        server = null
        _pendingPairing.value = null
        _hostedPeers.value = _hostedPeers.value.map { it.copy(connected = false) }
        _state.value = if (_enabled.value) HostState.Stopped else HostState.Disabled
    }

    fun pauseForRestore() = stop(CLOSE_REASON_RESTORE)

    fun setEnabled(enabled: Boolean) {
        if (_enabled.value == enabled) return
        _enabled.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) start() else stop(CLOSE_REASON_DISABLED)
    }

    @Synchronized
    fun regeneratePairing() {
        if (!_enabled.value) return
        pairingIdentity.regenerateSecret()
        refreshPairingPayload()
    }

    @Synchronized
    fun revokeHostedPeer(peerId: String) {
        _hostedPeers.value = _hostedPeers.value.filterNot { it.id == peerId }
        persistHostedPeers()
        server?.closeClient(peerId)
    }

    fun approvePendingPairing() = server?.approvePendingPairing()

    fun rejectPendingPairing() = server?.rejectPendingPairing("Pairing rejected on host")

    private fun refreshPairingPayload() {
        val current = server
        val host = localLanAddress()
        val pearReady = pearRuntimeController.status.value is PearRuntimeStatus.Ready
        _state.value = if (current?.listening == true && (host != null || pearReady)) {
            HostState.Ready(
                pairingJson = pairingPayload(host, if (host != null) current.boundPort else 0).toJson(),
                host = host.orEmpty(),
                port = if (host != null) current.boundPort else 0,
                usedFallbackPort = host != null && current.boundPort != DEFAULT_PORT,
                lanAvailable = host != null,
            )
        } else HostState.Failed("Connect to Wi-Fi/Ethernet, or enable a Pear connection mode, to share pairing data.")
    }

    private inner class Server(val boundPort: Int, val ipv6: Boolean) :
        WebSocketServer(
            if (ipv6) InetSocketAddress(InetAddress.getByName("::"), boundPort)
            else InetSocketAddress(boundPort)
        ) {
        private val clients = java.util.concurrent.ConcurrentHashMap<ServerFrameConnection, Client>()
        @Volatile private var pendingApproval: PendingApproval? = null
        private val pearListener = object : PearRuntimeListener {
            override fun onConnected(connectionId: String, publicKey: String, outgoing: Boolean) {
                if (!outgoing) openConnection(PearFrameConnection(connectionId))
            }

            override fun onFrame(connectionId: String, text: String) {
                val connection = PearFrameConnection(connectionId)
                if (clients.containsKey(connection)) receiveFrame(connection, text)
            }

            override fun onDisconnected(connectionId: String, publicKey: String) {
                closeConnection(PearFrameConnection(connectionId))
            }
        }
        @Volatile var listening: Boolean = false

        fun startPearSessions() = pearRuntimeController.addListener(pearListener)

        fun stopPearSessions() = pearRuntimeController.removeListener(pearListener)

        override fun onStart() {
            connectionLostTimeout = 30
            listening = true
            refreshNetworkEndpoint(forceAdvertising = true)
        }

        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            if (handshake.resourceDescriptor != "/sync") {
                conn.close(1002, "wrong path")
                return
            }
            openConnection(WebSocketFrameConnection(conn))
        }

        private fun openConnection(conn: ServerFrameConnection) {
            val nonce = ByteArray(16).also(SecureRandom()::nextBytes)
            val nonce64 = Base64.encodeToString(nonce, Base64.NO_WRAP)
            clients[conn] = Client(nonce64)
            conn.send(codec.encode(SyncWireMessage.Challenge(nonce64, TransportCrypto.TRANSPORT)))
        }

        override fun onMessage(conn: WebSocket, text: String) {
            receiveFrame(WebSocketFrameConnection(conn), text)
        }

        private fun receiveFrame(conn: ServerFrameConnection, text: String) {
            if (text.length > SyncLimits.MAX_WIRE_FRAME_CHARS) {
                conn.close(1009, "frame too large")
                return
            }
            val outer = runCatching { codec.decode(text) }.getOrElse {
                conn.close(1002, "bad frame")
                return
            }
            val client = clients[conn] ?: return
            val msg = if (client.deviceId == null) outer else runCatching {
                client.secure?.open(outer) ?: error("Missing encrypted transport")
            }.getOrElse {
                Log.w(TAG, "Rejected encrypted sync frame", it)
                conn.close(1002, "secure frame rejected")
                return
            }
            if (client.deviceId == null) {
                val hello = msg as? SyncWireMessage.Hello
                if (hello == null) {
                    conn.send(codec.encode(SyncWireMessage.Error("auth_failed", "Invalid pairing secret")))
                    conn.close(1002, "auth failed")
                    return
                }
                if (hello.transport != TransportCrypto.TRANSPORT) {
                    conn.send(codec.encode(SyncWireMessage.Error("upgrade_required", "Encrypted transport psk-aead-v1 is required")))
                    conn.close(1002, "encrypted transport required")
                    return
                }
                val name = AuthenticatedDeviceName.refresh(
                    "Phone ${hello.deviceId.take(8)}",
                    hello.deviceName,
                )
                val auth = authenticate(client.nonce, hello.clientNonce, hello.auth, hello.deviceId)
                if (auth == null) {
                    conn.send(codec.encode(SyncWireMessage.Error("auth_failed", "Invalid or already-claimed pairing code")))
                    conn.close(1002, "auth failed")
                    return
                }
                client.secure = runCatching {
                    TransportCrypto.forServer(auth.credential, client.nonce, hello.clientNonce, codec)
                }.getOrElse {
                    conn.close(1002, "invalid transport key material")
                    return
                }
                if (hello.sinceHlc?.let {
                        !com.ncy.notechaty.data.sync.event.HlcTimestamp.isAcceptableRemote(
                            it,
                            System.currentTimeMillis(),
                        )
                    } == true
                ) {
                    sendSecure(conn, client, SyncWireMessage.Error(
                        "clock_skew",
                        "Remote catch-up clock is more than 5 minutes ahead",
                    ))
                    conn.close(1002, "clock skew")
                    return
                }
                if (hello.deviceId == identity.deviceId) {
                    sendSecure(conn, client, SyncWireMessage.Error(
                        "identity_collision",
                        "Remote device is using this phone's device ID",
                    ))
                    conn.close(1008, "identity collision")
                    return
                }
                val collision = claimClientIdentity(client, hello.deviceId)
                if (collision != null) {
                    sendSecure(conn, client, SyncWireMessage.Error("identity_collision", collision))
                    conn.close(1008, "identity collision")
                    return
                }
                if (auth.claimedInvitation) {
                    // Proving possession of a QR credential is necessary but does not
                    // authorize access. Pause before persisting the peer or sending Welcome.
                    client.deviceId = null
                    val pending = PendingApproval(conn, client, hello, name, auth)
                    synchronized(this) {
                        if (pendingApproval != null) {
                            sendSecure(conn, client, SyncWireMessage.Error("pairing_busy", "Another pairing awaits confirmation"))
                            conn.close(1008, "pairing busy")
                            return
                        }
                        pendingApproval = pending
                        _pendingPairing.value = PendingPairing(hello.deviceId, name)
                    }
                    scope.launch {
                        delay(PAIRING_CONFIRM_TIMEOUT_MS)
                        synchronized(this@Server) {
                            if (pendingApproval === pending) {
                                rejectPendingPairing("Pairing confirmation timed out")
                            }
                        }
                    }
                    return
                }
                finishAuthentication(conn, client, hello, name, auth, claimInvitation = false)
                return
            }
            when (msg) {
                is SyncWireMessage.Event -> scope.launch { receiveEvent(conn, client, msg.event) }
                is SyncWireMessage.BlobRequest -> scope.launch { serveBlob(conn, client, msg.hash, msg.mediaType) }
                is SyncWireMessage.BlobChunk -> scope.launch {
                    client.blobMutex.withLock { receiveChunk(client, msg) }
                }
                is SyncWireMessage.BlobMissing -> discardPending(client, msg.hash)
                else -> Unit // Acks are intentionally client-owned via Hello.sinceHlc.
            }
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            closeConnection(WebSocketFrameConnection(conn))
        }

        private fun closeConnection(conn: ServerFrameConnection) {
            synchronized(this) {
                if (pendingApproval?.connection == conn) {
                    pendingApproval = null
                    _pendingPairing.value = null
                }
            }
            val removed = clients.remove(conn)
            removed?.pending?.values?.forEach { it.discard() }
            removed?.deviceId?.let { id ->
                markDisconnected(id)
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception) {
            Log.w(TAG, "Phone sync server error", ex)
            if (conn == null && clients.isEmpty()) {
                listening = false
                handleFatalServerError(this, ex)
            }
        }

        fun broadcastEvent(event: ChatEvent) {
            clients.forEach { (socket, client) ->
                if (socket.isOpen && client.deviceId != null && event.originDevice != client.deviceId) {
                    synchronized(client) {
                        if (client.catchingUp) {
                            client.queuedLive.addLast(event)
                        } else {
                            sendSecure(socket, client, SyncWireMessage.Event(event))
                        }
                    }
                }
            }
        }

        private fun finishCatchupAndGoLive(
            conn: ServerFrameConnection,
            client: Client,
            remoteDeviceId: String,
            sinceHlc: com.ncy.notechaty.data.sync.event.HlcTimestamp?,
        ) {
            val fullReplay = sinceHlc == null
            snapshot.eventsSince(sinceHlc).forEach { event ->
                synchronized(client) {
                    if (!conn.isOpen) return
                    if (SyncCatchupPolicy.markForSend(
                            event.eventId,
                            event.originDevice,
                            remoteDeviceId,
                            fullReplay,
                            client.catchupSentEventIds,
                        )
                    ) {
                        sendSecure(conn, client, SyncWireMessage.Event(event))
                    }
                }
            }
            synchronized(client) {
                if (!conn.isOpen) {
                    client.queuedLive.clear()
                    client.catchingUp = false
                    return
                }
                client.queuedLive.sortedBy { it.hlc }.forEach { event ->
                    if (SyncCatchupPolicy.markForSend(
                            event.eventId,
                            event.originDevice,
                            remoteDeviceId,
                            fullReplay = false,
                            sentEventIds = client.catchupSentEventIds,
                        )
                    ) {
                        sendSecure(conn, client, SyncWireMessage.Event(event))
                    }
                }
                client.queuedLive.clear()
                client.catchupSentEventIds.clear()
                client.catchingUp = false
            }
        }

        private fun sendSecure(conn: ServerFrameConnection, client: Client, message: SyncWireMessage): Boolean =
            synchronized(client) {
                val channel = client.secure ?: return@synchronized false
                runCatching {
                    conn.send(codec.encode(channel.seal(message)))
                    true
                }.getOrDefault(false)
            }

        fun closeClient(deviceId: String) = clients.entries
            .filter { it.value.deviceId == deviceId }
            .forEach { (connection, client) ->
                sendSecure(
                    connection,
                    client,
                    SyncWireMessage.Error(
                        "pairing_revoked",
                        "Pairing credential was revoked; pair again",
                    ),
                )
                connection.close(1000, "pairing revoked")
            }

        fun closeAll(reason: String) = clients.keys.forEach {
            it.close(CLOSE_GOING_AWAY, reason)
        }

        @Synchronized
        fun approvePendingPairing() {
            val pending = pendingApproval ?: return
            pendingApproval = null
            _pendingPairing.value = null
            if (!pending.connection.isOpen || pairingIdentity.secret != pending.auth.credential) {
                pending.connection.close(1008, "pairing invitation expired")
                return
            }
            if (claimClientIdentity(pending.client, pending.hello.deviceId) != null) {
                pending.connection.close(1008, "identity collision")
                return
            }
            finishAuthentication(
                pending.connection,
                pending.client,
                pending.hello,
                pending.deviceName,
                pending.auth,
                claimInvitation = true,
            )
        }

        @Synchronized
        fun rejectPendingPairing(reason: String) {
            val pending = pendingApproval ?: return
            pendingApproval = null
            _pendingPairing.value = null
            sendSecure(pending.connection, pending.client, SyncWireMessage.Error("pairing_rejected", reason))
            pending.connection.close(1008, "pairing rejected")
        }

        private fun finishAuthentication(
            conn: ServerFrameConnection,
            client: Client,
            hello: SyncWireMessage.Hello,
            name: String,
            auth: AuthResult,
            claimInvitation: Boolean,
        ) {
            client.deviceName = name
            recordHostedPeer(
                id = hello.deviceId,
                name = name,
                connected = true,
                credential = auth.credential,
                legacySharedCredential = auth.legacySharedCredential,
            )
            if (claimInvitation) {
                pairingIdentity.regenerateSecret()
                refreshPairingPayload()
            }
            synchronized(client) {
                client.catchingUp = true
                client.queuedLive.clear()
                client.catchupSentEventIds.clear()
            }
            sendSecure(conn, client, SyncWireMessage.Welcome(identity.deviceId, android.os.Build.MODEL ?: "Android phone"))
            scope.launch { finishCatchupAndGoLive(conn, client, hello.deviceId, hello.sinceHlc) }
        }

        @Synchronized
        private fun claimClientIdentity(client: Client, deviceId: String): String? {
            if (deviceId == identity.deviceId) {
                return "Remote device is using this phone's device ID"
            }
            if (clients.values.any { it !== client && it.deviceId == deviceId }) {
                return "Another live connection is already using this device ID"
            }
            client.deviceId = deviceId
            return null
        }

        suspend fun expireStalledTransfers() {
            val cutoff = System.currentTimeMillis() - SyncLimits.TRANSFER_TIMEOUT_MS
            clients.values.forEach { client ->
                client.blobMutex.withLock {
                    client.pending.entries.removeIf { (_, pending) ->
                        if (pending.lastActivityAt < cutoff) {
                            pending.discard()
                            true
                        } else false
                    }
                }
            }
        }

        suspend fun requestFileAnyway(chatId: String, profileId: String, hash: String): Boolean {
            clients.forEach { (conn, client) ->
                if (client.deviceId == null || client.pending.size >= SyncLimits.MAX_PENDING_BLOBS) {
                    return@forEach
                }
                if (repository.mediaFileForHash(hash, "file", profileId) != null) return true
                val candidate = newPending(chatId, profileId, "file")
                if (client.pending.putIfAbsent(hash, candidate) == null) {
                    sendSecure(conn, client, SyncWireMessage.BlobRequest(hash, "file"))
                    return true
                }
                candidate.discard()
            }
            return false
        }

        private suspend fun receiveEvent(conn: ServerFrameConnection, client: Client, event: ChatEvent) {
            // Event-id dedup makes normal echoes harmless while allowing a restored
            // installation to recover an own-origin event it no longer has locally.
            if (!com.ncy.notechaty.data.sync.event.HlcTimestamp.isAcceptableRemote(
                    event.hlc,
                    System.currentTimeMillis(),
                )
            ) {
                sendSecure(conn, client, SyncWireMessage.Error(
                    "clock_skew",
                    "Remote event clock is more than 5 minutes ahead",
                ))
                conn.close(1002, "clock skew")
                return
            }
            val routedEvent = repository.canonicalizeInboundEvent(event)
            val newlyApplied = repository.applyExternalEvent(routedEvent)
            sendSecure(conn, client, SyncWireMessage.Ack(routedEvent.hlc))
            if (!newlyApplied) return
            mediaRefs(routedEvent).forEach { ref ->
                val hash = ref.hash
                val mediaType = ref.mediaType
                if (
                    mediaType == "file" &&
                    ref.sizeBytes != null &&
                    ref.sizeBytes > settingsRepository.settings.value.automaticFileDownloadLimitBytes
                ) return@forEach
                if (client.pending.size < SyncLimits.MAX_PENDING_BLOBS &&
                    repository.mediaFileForHash(hash, mediaType, routedEvent.profileId) == null
                ) {
                    val candidate = newPending(routedEvent.chatId, routedEvent.profileId, mediaType)
                    if (client.pending.putIfAbsent(hash, candidate) == null) {
                        sendSecure(conn, client, SyncWireMessage.BlobRequest(hash, mediaType))
                    } else {
                        candidate.discard()
                    }
                }
            }
        }

        private suspend fun serveBlob(conn: ServerFrameConnection, client: Client, hash: String, mediaType: String) {
            val file = repository.mediaFileForHash(hash, mediaType)
            if (file == null || !file.isFile) {
                sendSecure(conn, client, SyncWireMessage.BlobMissing(hash))
                return
            }
            if (file.length() > SyncLimits.MAX_BLOB_BYTES) {
                sendSecure(conn, client, SyncWireMessage.BlobMissing(hash))
                return
            }
            val total = file.length().toInt()
            if (total == 0) {
                sendSecure(conn, client, SyncWireMessage.BlobChunk(hash, mediaType, 0, 0, "", true))
                return
            }
            file.inputStream().buffered().use { input ->
                val buffer = ByteArray(CHUNK_SIZE)
                var offset = 0
                while (offset < total && conn.isOpen) {
                    val n = input.read(buffer)
                    if (n <= 0) return
                    val encoded = Base64.encodeToString(
                        if (n == buffer.size) buffer else buffer.copyOf(n),
                        Base64.NO_WRAP,
                    )
                    sendSecure(conn, client, SyncWireMessage.BlobChunk(hash, mediaType, offset, total, encoded, offset + n >= total))
                    offset += n
                }
            }
        }

        private suspend fun receiveChunk(client: Client, chunk: SyncWireMessage.BlobChunk) {
            val pending = client.pending[chunk.hash] ?: return
            if (chunk.totalSize !in 0..SyncLimits.MAX_BLOB_BYTES ||
                chunk.data.length > SyncLimits.MAX_BASE64_CHARS ||
                chunk.offset < 0 || chunk.offset > chunk.totalSize
            ) {
                discardPending(client, chunk.hash)
                return
            }
            if (chunk.offset != pending.received) {
                discardPending(client, chunk.hash)
                return
            }
            val decoded = runCatching { Base64.decode(chunk.data, Base64.NO_WRAP) }.getOrElse {
                discardPending(client, chunk.hash)
                return
            }
            if (decoded.size > SyncLimits.MAX_CHUNK_BYTES ||
                pending.received + decoded.size > chunk.totalSize ||
                pending.received + decoded.size > SyncLimits.MAX_BLOB_BYTES
            ) {
                discardPending(client, chunk.hash)
                return
            }
            pending.output.write(decoded)
            pending.received += decoded.size
            pending.lastActivityAt = System.currentTimeMillis()
            if (!chunk.isLast) return
            client.pending.remove(chunk.hash)
            pending.output.close()
            if (pending.received == chunk.totalSize && MediaHash.ofFile(pending.tempFile) == chunk.hash) {
                repository.saveMediaBlobFromFile(
                    pending.chatId,
                    chunk.hash,
                    pending.mediaType,
                    pending.tempFile,
                    pending.profileId,
                )
                eventBus.notifyMediaArrived(pending.chatId)
            } else {
                pending.tempFile.delete()
            }
        }
    }

    /** Transport-neutral connection used by the authenticated sync session host. */
    private interface ServerFrameConnection {
        val isOpen: Boolean
        fun send(text: String)
        fun close(code: Int, reason: String)
    }

    private data class WebSocketFrameConnection(
        private val socket: WebSocket,
    ) : ServerFrameConnection {
        override val isOpen: Boolean get() = socket.isOpen
        override fun send(text: String) = socket.send(text)
        override fun close(code: Int, reason: String) = socket.close(code, reason)
    }

    private inner class PearFrameConnection(
        private val connectionId: String,
    ) : ServerFrameConnection {
        override val isOpen: Boolean get() = true
        override fun send(text: String) {
            pearRuntimeController.send(connectionId, text)
        }
        override fun close(code: Int, reason: String) = pearRuntimeController.close(connectionId)
        override fun equals(other: Any?): Boolean =
            other is PhoneSyncServer.PearFrameConnection && other.connectionId == connectionId
        override fun hashCode(): Int = connectionId.hashCode()
    }

    private data class Client(
        val nonce: String,
        @Volatile var secure: TransportCrypto? = null,
        @Volatile var deviceId: String? = null,
        @Volatile var deviceName: String? = null,
        var catchingUp: Boolean = false,
        val queuedLive: ArrayDeque<ChatEvent> = ArrayDeque(),
        val catchupSentEventIds: MutableSet<String> = HashSet(),
        val pending: java.util.concurrent.ConcurrentHashMap<String, Pending> = java.util.concurrent.ConcurrentHashMap(),
        val blobMutex: Mutex = Mutex(),
    )
    private data class Pending(
        val chatId: String,
        val profileId: String,
        val mediaType: String,
        val tempFile: File,
        val output: BufferedOutputStream,
        var received: Int = 0,
        var lastActivityAt: Long = System.currentTimeMillis(),
    ) {
        fun discard() {
            runCatching { output.close() }
            tempFile.delete()
        }
    }

    private fun newPending(chatId: String, profileId: String, mediaType: String): Pending {
        val temp = SyncLimits.newBlobTempFile(repository.getBaseDir())
        return Pending(chatId, profileId, mediaType, temp, BufferedOutputStream(temp.outputStream()))
    }

    private fun discardPending(client: Client, hash: String) {
        client.pending.remove(hash)?.discard()
    }

    private data class AuthResult(
        val credential: String,
        val claimedInvitation: Boolean,
        val legacySharedCredential: Boolean,
    )

    private data class PendingApproval(
        val connection: ServerFrameConnection,
        val client: Client,
        val hello: SyncWireMessage.Hello,
        val deviceName: String,
        val auth: AuthResult,
    )

    @Synchronized
    private fun authenticate(
        nonce64: String,
        clientNonce64: String,
        auth64: String,
        deviceId: String,
    ): AuthResult? {
        val existing = _hostedPeers.value.firstOrNull { it.id == deviceId }
        if (existing != null && validAuth(existing.credential, nonce64, clientNonce64, deviceId, auth64)) {
            return AuthResult(existing.credential, false, existing.legacySharedCredential)
        }
        val invitation = pairingIdentity.secret
        if (!validAuth(invitation, nonce64, clientNonce64, deviceId, auth64)) return null

        return AuthResult(invitation, true, false)
    }

    private fun validAuth(secret: String, nonce64: String, clientNonce64: String, deviceId: String, auth64: String): Boolean =
        TransportCrypto.verify(secret, nonce64, clientNonce64, deviceId, auth64)

    @Synchronized
    private fun recordHostedPeer(
        id: String,
        name: String,
        connected: Boolean,
        credential: String,
        legacySharedCredential: Boolean,
    ) {
        val now = System.currentTimeMillis()
        _hostedPeers.value = (_hostedPeers.value.filterNot { it.id == id } + HostedPeer(
            id = id,
            name = name,
            lastSeenAt = now,
            connected = connected,
            credential = credential,
            legacySharedCredential = legacySharedCredential,
        )).sortedBy { it.name.lowercase() }
        persistHostedPeers()
    }

    @Synchronized
    private fun markDisconnected(id: String) {
        if (_hostedPeers.value.none { it.id == id }) return
        _hostedPeers.value = _hostedPeers.value.map {
            if (it.id == id) it.copy(connected = false, lastSeenAt = System.currentTimeMillis()) else it
        }
        persistHostedPeers()
    }

    private fun loadHostedPeers(): List<HostedPeer> {
        val json = prefs.getString(KEY_HOSTED_PEERS, null) ?: return emptyList()
        val type = object : TypeToken<List<HostedPeer>>() {}.type
        return runCatching { gson.fromJson<List<HostedPeer>>(json, type).orEmpty() }
            .getOrDefault(emptyList())
            .map { it.copy(connected = false) }
    }

    private fun persistHostedPeers() {
        val persisted = _hostedPeers.value.map { it.copy(connected = false) }
        prefs.edit().putString(KEY_HOSTED_PEERS, gson.toJson(persisted)).apply()
    }

    private fun localLanAddress(): String? {
        val active = connectivity.activeNetwork
        val fromConnectivity = runCatching {
            knownNetworks.asSequence()
                .flatMap { network ->
                    val capabilities = connectivity.getNetworkCapabilities(network)
                        ?: return@flatMap emptySequence()
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        return@flatMap emptySequence()
                    }
                    val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    val isEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    if (!isWifi && !isEthernet) return@flatMap emptySequence()
                    val rank = when {
                        network == active && isWifi -> 0
                        network == active && isEthernet -> 1
                        isWifi -> 2
                        else -> 3
                    }
                    connectivity.getLinkProperties(network)
                        ?.linkAddresses
                        ?.asSequence()
                        ?.map { it.address }
                        ?.filter(::isUsableLanAddress)
                        ?.map { address -> Triple(rank, addressRank(address), address.hostAddress ?: "") }
                        ?: emptySequence()
                }
                .sortedWith(compareBy<Triple<Int, Int, String>> { it.first }.thenBy { it.second }.thenBy { it.third })
                .map { it.third }
                .firstOrNull()
        }.getOrNull()
        if (fromConnectivity != null) return fromConnectivity

        return runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .filter {
                    it.name.startsWith("wlan", ignoreCase = true) ||
                        it.name.startsWith("swlan", ignoreCase = true) ||
                        it.name.startsWith("ap", ignoreCase = true) ||
                        it.name.startsWith("eth", ignoreCase = true)
                }
                .sortedBy {
                    when {
                        it.name.startsWith("wlan", ignoreCase = true) -> 0
                        it.name.startsWith("swlan", ignoreCase = true) -> 1
                        it.name.startsWith("ap", ignoreCase = true) -> 2
                        else -> 3
                    }
                }
                .flatMap { it.inetAddresses.toList().asSequence() }
                .filter(::isUsableLanAddress)
                .sortedWith(compareBy<InetAddress>(::addressRank).thenBy { it.hostAddress })
                .mapNotNull { it.hostAddress }
                .firstOrNull()
        }.getOrNull()
    }

    private fun isUsableLanAddress(address: InetAddress): Boolean =
        !address.isAnyLocalAddress &&
            !address.isLoopbackAddress &&
            !address.isLinkLocalAddress &&
            !address.isMulticastAddress

    private fun addressRank(address: InetAddress): Int = when (address) {
        is Inet4Address -> {
            val octets = address.address.map { it.toInt() and 0xff }
            when {
                octets[0] == 192 && octets[1] == 168 -> 0
                octets[0] == 172 && octets[1] in 16..31 -> 1
                octets[0] == 10 -> 2
                else -> 3
            }
        }
        is Inet6Address -> if ((address.address[0].toInt() and 0xfe) == 0xfc) 4 else 5
        else -> 6
    }

    private fun isIpv6Host(host: String): Boolean =
        host.trim().removePrefix("[").removeSuffix("]").contains(':')

    private fun selectServerPort(ipv6: Boolean): Int? =
        probePort(DEFAULT_PORT, ipv6) ?: probePort(0, ipv6)

    private fun probePort(port: Int, ipv6: Boolean): Int? = runCatching {
        ServerSocket().use { socket ->
            socket.reuseAddress = true
            socket.bind(
                if (ipv6) InetSocketAddress(InetAddress.getByName("::"), port)
                else InetSocketAddress(port)
            )
            socket.localPort
        }
    }.getOrNull()

    @Synchronized
    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                knownNetworks += network
                scheduleNetworkRefresh()
            }
            override fun onLost(network: Network) {
                knownNetworks -= network
                scheduleNetworkRefresh()
            }
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                knownNetworks += network
                scheduleNetworkRefresh()
            }
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                knownNetworks += network
                scheduleNetworkRefresh()
            }
        }
        networkCallback = callback
        runCatching {
            connectivity.registerNetworkCallback(
                NetworkRequest.Builder().clearCapabilities().build(),
                callback,
            )
        }.onFailure {
            Log.w(TAG, "Could not monitor network changes", it)
            networkCallback = null
        }
    }

    @Synchronized
    private fun unregisterNetworkCallback() {
        knownNetworks.clear()
        val callback = networkCallback ?: return
        networkCallback = null
        runCatching { connectivity.unregisterNetworkCallback(callback) }
            .onFailure { Log.w(TAG, "Could not stop network monitoring", it) }
    }

    private fun scheduleNetworkRefresh() {
        synchronized(this) {
            networkRefreshJob?.cancel()
            networkRefreshJob = scope.launch {
                delay(NETWORK_REFRESH_DEBOUNCE_MS)
                refreshNetworkEndpoint()
            }
        }
    }

    @Synchronized
    private fun refreshNetworkEndpoint(forceAdvertising: Boolean = false) {
        val current = server ?: return
        if (!current.listening) return
        val host = localLanAddress()
        if (host == null) {
            val pearReady = pearRuntimeController.status.value is PearRuntimeStatus.Ready
            _state.value = if (pearReady) HostState.Ready(
                pairingJson = pairingPayload(null, 0).toJson(),
                host = "",
                port = 0,
                usedFallbackPort = false,
                lanAvailable = false,
            ) else HostState.Failed(
                "Connect to Wi-Fi/Ethernet, or enable a Pear connection mode, to share pairing data."
            )
            advertisedHost = null
            advertisedPort = null
            advertisingRestartJob?.cancel()
            advertisingRestartJob = null
            stopAdvertising()
            return
        }
        if (current.ipv6 != isIpv6Host(host)) {
            if (!familyRestartPending) {
                familyRestartPending = true
                _state.value = HostState.Starting
                scope.launch {
                    stop(CLOSE_REASON_NETWORK_CHANGED)
                    synchronized(this@PhoneSyncServer) { familyRestartPending = false }
                    if (_enabled.value) start()
                }
            }
            return
        }

        _state.value = HostState.Ready(
            pairingJson = pairingPayload(host, current.boundPort).toJson(),
            host = host,
            port = current.boundPort,
            usedFallbackPort = current.boundPort != DEFAULT_PORT,
        )
        if (forceAdvertising || advertisedHost != host || advertisedPort != current.boundPort) {
            restartAdvertising(host, current.boundPort)
        }
    }

    /** Re-publish pairing metadata after an optional transport identity becomes ready. */
    fun refreshPublishedPairing() {
        if (_enabled.value) refreshPairingPayload()
    }

    private fun pairingPayload(host: String?, port: Int): PairingPayload {
        val pearKey = (pearRuntimeController.status.value as? PearRuntimeStatus.Ready)?.publicKey
        return pairingIdentity.payload(host.orEmpty(), port).copy(pearPublicKey = pearKey)
    }

    @Synchronized
    private fun restartAdvertising(host: String, port: Int) {
        advertisingRestartJob?.cancel()
        if (registrationListener == null) {
            startAdvertising(host, port)
            return
        }
        stopAdvertising()
        advertisingRestartJob = scope.launch {
            delay(ADVERTISEMENT_RESTART_DELAY_MS)
            synchronized(this@PhoneSyncServer) {
                val current = server
                if (current?.listening == true && current.boundPort == port && localLanAddress() == host) {
                    startAdvertising(host, port)
                }
            }
        }
    }

    @Synchronized
    private fun handleFatalServerError(failed: Server, error: Exception) {
        if (server !== failed) return
        liveJob?.cancel()
        liveJob = null
        transferCleanupJob?.cancel()
        transferCleanupJob = null
        networkRefreshJob?.cancel()
        networkRefreshJob = null
        advertisingRestartJob?.cancel()
        advertisingRestartJob = null
        unregisterNetworkCallback()
        stopAdvertising()
        server = null
        _state.value = HostState.Failed(error.message ?: "Could not start phone sync")
    }

    @Synchronized
    private fun startAdvertising(host: String, port: Int) {
        if (registrationListener != null) return
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.i(TAG, "Advertising ${serviceInfo.serviceName} via mDNS")
            }
            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "mDNS registration failed: $errorCode")
                synchronized(this@PhoneSyncServer) {
                    if (registrationListener === this) registrationListener = null
                    advertisedHost = null
                    advertisedPort = null
                }
            }
            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(TAG, "mDNS unregistration failed: $errorCode")
            }
        }
        val info = NsdServiceInfo().apply {
            serviceName = MdnsPrivacyLabel.fromDeviceId(identity.deviceId)
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("deviceId", identity.deviceId)
            setAttribute("pairingMode", "qr")
            setAttribute("availability", PairingPayload.AVAILABILITY_FOREGROUND)
        }
        registrationListener = listener
        advertisedHost = host
        advertisedPort = port
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure {
                Log.w(TAG, "Could not advertise phone sync server", it)
                registrationListener = null
                advertisedHost = null
                advertisedPort = null
            }
    }

    @Synchronized
    private fun stopAdvertising() {
        val listener = registrationListener ?: return
        registrationListener = null
        advertisedHost = null
        advertisedPort = null
        runCatching { nsd.unregisterService(listener) }
            .onFailure { Log.w(TAG, "Could not stop phone sync advertisement", it) }
    }

    private data class MediaRef(
        val hash: String,
        val mediaType: String,
        val sizeBytes: Long? = null,
    )

    private fun mediaRefs(event: ChatEvent): List<MediaRef> {
        val refs = mutableListOf<MediaRef>()
        fun fromMessage(message: Message) {
            MediaHash.extractHash(message.voiceFile)?.let { refs += MediaRef(it, "voice") }
            message.imageFiles.forEach { file -> MediaHash.extractHash(file)?.let { refs += MediaRef(it, "image") } }
            message.fileAttachments.forEach { attachment ->
                MediaHash.extractHash(attachment.file)?.let {
                    refs += MediaRef(it, "file", attachment.sizeBytes)
                }
            }
            message.content.flatMap { it.spans }.forEach { span ->
                MediaHash.extractHash(span.inlineImage)?.let { refs += MediaRef(it, "image") }
            }
        }
        when (val op = event.op) {
            is ChatEventOp.MessageCreated -> fromMessage(op.message)
            is ChatEventOp.MessageEdited -> fromMessage(op.message)
            is ChatEventOp.ChatMetadataChanged -> {
                MediaHash.extractHash(op.metadata.avatarFile)?.let { refs += MediaRef(it, "image") }
                MediaHash.extractHash(op.metadata.backgroundFile)?.let { refs += MediaRef(it, "image") }
            }
            else -> Unit
        }
        return refs.distinctBy { it.hash to it.mediaType }
    }

    private companion object {
        const val TAG = "PhoneSyncServer"
        const val DEFAULT_PORT = 8765
        const val PAIRING_CONFIRM_TIMEOUT_MS = 60_000L
        const val CHUNK_SIZE = 64 * 1024
        const val NETWORK_REFRESH_DEBOUNCE_MS = 500L
        const val ADVERTISEMENT_RESTART_DELAY_MS = 500L
        const val CLOSE_GOING_AWAY = 1001
        const val CLOSE_REASON_FOREGROUND = "phone_host_left_foreground"
        const val CLOSE_REASON_DISABLED = "phone_host_disabled"
        const val CLOSE_REASON_NETWORK_CHANGED = "phone_host_network_changed"
        const val CLOSE_REASON_RESTORE = "phone_host_restore"
        const val PREFS = "ncy_phone_sync_server"
        const val KEY_ENABLED = "enabled"
        const val KEY_HOSTED_PEERS = "hosted_peers"
        const val SERVICE_TYPE = "_ncy-sync._tcp."
    }
}
