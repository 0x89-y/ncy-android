package com.ncy.notechaty.data.sync.transport

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ncy.notechaty.data.model.Message
import com.ncy.notechaty.data.repository.ChatRepository
import com.ncy.notechaty.data.sync.ChatEventBus
import com.ncy.notechaty.data.sync.DeviceIdentity
import com.ncy.notechaty.data.sync.event.ChatEvent
import com.ncy.notechaty.data.sync.event.ChatEventOp
import com.ncy.notechaty.data.sync.event.SyncSnapshot
import com.ncy.notechaty.data.sync.media.MediaHash
import com.ncy.notechaty.data.sync.peer.PhonePairingIdentity
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
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
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

    sealed class HostState {
        object Disabled : HostState()
        object Stopped : HostState()
        object Starting : HostState()
        data class Ready(
            val pairingJson: String,
            val host: String,
            val port: Int,
            val usedFallbackPort: Boolean,
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
    private var server: Server? = null
    private var liveJob: Job? = null
    private var transferCleanupJob: Job? = null
    private var networkRefreshJob: Job? = null
    private var advertisingRestartJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val knownNetworks = java.util.concurrent.ConcurrentHashMap.newKeySet<Network>()
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
        val port = selectServerPort()
        if (port == null) {
            _state.value = HostState.Failed("Could not find an available port for phone sync.")
            return
        }
        _state.value = HostState.Starting
        val next = Server(port)
        server = next
        next.isReuseAddr = true
        registerNetworkCallback()
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
    fun stop() {
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
        server?.let { runCatching { it.stop(500) } }
        server = null
        _hostedPeers.value = _hostedPeers.value.map { it.copy(connected = false) }
        _state.value = if (_enabled.value) HostState.Stopped else HostState.Disabled
    }

    fun setEnabled(enabled: Boolean) {
        if (_enabled.value == enabled) return
        _enabled.value = enabled
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (enabled) start() else stop()
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

    private fun refreshPairingPayload() {
        val current = server
        val host = localIpv4()
        _state.value = if (current?.listening == true && host != null) {
            HostState.Ready(
                pairingJson = pairingIdentity.payload(host, current.boundPort).toJson(),
                host = host,
                port = current.boundPort,
                usedFallbackPort = current.boundPort != DEFAULT_PORT,
            )
        } else HostState.Failed("Connect this phone to Wi-Fi to share pairing data.")
    }

    private inner class Server(val boundPort: Int) :
        WebSocketServer(InetSocketAddress(boundPort)) {
        private val clients = java.util.concurrent.ConcurrentHashMap<WebSocket, Client>()
        @Volatile var listening: Boolean = false

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
            val nonce = ByteArray(32).also(SecureRandom()::nextBytes)
            val nonce64 = Base64.encodeToString(nonce, Base64.NO_WRAP)
            clients[conn] = Client(nonce64)
            conn.send(codec.encode(SyncWireMessage.Challenge(nonce64)))
        }

        override fun onMessage(conn: WebSocket, text: String) {
            if (text.length > SyncLimits.MAX_TEXT_FRAME_CHARS) {
                conn.close(1009, "frame too large")
                return
            }
            val msg = runCatching { codec.decode(text) }.getOrElse {
                conn.close(1002, "bad frame")
                return
            }
            val client = clients[conn] ?: return
            if (client.deviceId == null) {
                val hello = msg as? SyncWireMessage.Hello
                if (hello == null) {
                    conn.send(codec.encode(SyncWireMessage.Error("auth_failed", "Invalid pairing secret")))
                    conn.close(1002, "auth failed")
                    return
                }
                val name = hello.deviceName.ifBlank { "Phone ${hello.deviceId.take(8)}" }
                val auth = authenticate(client.nonce, hello.auth, hello.deviceId, name)
                if (auth == null) {
                    conn.send(codec.encode(SyncWireMessage.Error("auth_failed", "Invalid or already-claimed pairing code")))
                    conn.close(1002, "auth failed")
                    return
                }
                client.deviceId = hello.deviceId
                client.deviceName = name
                recordHostedPeer(
                    id = hello.deviceId,
                    name = name,
                    connected = true,
                    credential = auth.credential,
                    legacySharedCredential = auth.legacySharedCredential,
                )
                if (auth.claimedInvitation) {
                    refreshPairingPayload()
                }
                conn.send(codec.encode(SyncWireMessage.Welcome(identity.deviceId, android.os.Build.MODEL ?: "Android phone")))
                scope.launch {
                    snapshot.eventsSince(hello.sinceHlc).forEach { event ->
                        if (event.originDevice != hello.deviceId && conn.isOpen) {
                            conn.send(codec.encode(SyncWireMessage.Event(event)))
                        }
                    }
                }
                return
            }
            when (msg) {
                is SyncWireMessage.Event -> scope.launch { receiveEvent(conn, client, msg.event) }
                is SyncWireMessage.BlobRequest -> scope.launch { serveBlob(conn, msg.hash, msg.mediaType) }
                is SyncWireMessage.BlobChunk -> scope.launch {
                    client.blobMutex.withLock { receiveChunk(client, msg) }
                }
                is SyncWireMessage.BlobMissing -> discardPending(client, msg.hash)
                else -> Unit // Acks are intentionally client-owned via Hello.sinceHlc.
            }
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
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
                    socket.send(codec.encode(SyncWireMessage.Event(event)))
                }
            }
        }

        fun closeClient(deviceId: String) = clients.entries
            .filter { it.value.deviceId == deviceId }
            .forEach { it.key.close(1000, "pairing revoked") }

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

        private suspend fun receiveEvent(conn: WebSocket, client: Client, event: ChatEvent) {
            if (event.originDevice == identity.deviceId) {
                conn.send(codec.encode(SyncWireMessage.Ack(event.hlc)))
                return
            }
            repository.applyExternalEvent(event)
            conn.send(codec.encode(SyncWireMessage.Ack(event.hlc)))
            mediaRefs(event).forEach { (hash, mediaType) ->
                if (client.pending.size < SyncLimits.MAX_PENDING_BLOBS &&
                    repository.mediaFileForHash(hash, mediaType) == null
                ) {
                    val candidate = newPending(event.chatId, mediaType)
                    if (client.pending.putIfAbsent(hash, candidate) == null) {
                        conn.send(codec.encode(SyncWireMessage.BlobRequest(hash, mediaType)))
                    } else {
                        candidate.discard()
                    }
                }
            }
        }

        private suspend fun serveBlob(conn: WebSocket, hash: String, mediaType: String) {
            val file = repository.mediaFileForHash(hash, mediaType)
            if (file == null || !file.isFile) {
                conn.send(codec.encode(SyncWireMessage.BlobMissing(hash)))
                return
            }
            if (file.length() > SyncLimits.MAX_BLOB_BYTES) {
                conn.send(codec.encode(SyncWireMessage.BlobMissing(hash)))
                return
            }
            val total = file.length().toInt()
            if (total == 0) {
                conn.send(codec.encode(SyncWireMessage.BlobChunk(hash, mediaType, 0, 0, "", true)))
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
                    conn.send(codec.encode(SyncWireMessage.BlobChunk(hash, mediaType, offset, total, encoded, offset + n >= total)))
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
                repository.saveMediaBlobFromFile(pending.chatId, chunk.hash, pending.mediaType, pending.tempFile)
                eventBus.notifyMediaArrived(pending.chatId)
            } else {
                pending.tempFile.delete()
            }
        }
    }

    private data class Client(
        val nonce: String,
        @Volatile var deviceId: String? = null,
        @Volatile var deviceName: String? = null,
        val pending: java.util.concurrent.ConcurrentHashMap<String, Pending> = java.util.concurrent.ConcurrentHashMap(),
        val blobMutex: Mutex = Mutex(),
    )
    private data class Pending(
        val chatId: String,
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

    private fun newPending(chatId: String, mediaType: String): Pending {
        val temp = SyncLimits.newBlobTempFile(repository.getBaseDir())
        return Pending(chatId, mediaType, temp, BufferedOutputStream(temp.outputStream()))
    }

    private fun discardPending(client: Client, hash: String) {
        client.pending.remove(hash)?.discard()
    }

    private data class AuthResult(
        val credential: String,
        val claimedInvitation: Boolean,
        val legacySharedCredential: Boolean,
    )

    @Synchronized
    private fun authenticate(
        nonce64: String,
        auth64: String,
        deviceId: String,
        deviceName: String,
    ): AuthResult? {
        val existing = _hostedPeers.value.firstOrNull { it.id == deviceId }
        if (existing != null && validAuth(existing.credential, nonce64, auth64)) {
            return AuthResult(existing.credential, false, existing.legacySharedCredential)
        }
        val invitation = pairingIdentity.secret
        if (!validAuth(invitation, nonce64, auth64)) return null

        // Claim the current invitation for this device. Rotation happens before
        // another Hello can claim it because this method is synchronized.
        recordHostedPeer(deviceId, deviceName, true, invitation, false)
        pairingIdentity.regenerateSecret()
        return AuthResult(invitation, true, false)
    }

    private fun validAuth(secret: String, nonce64: String, auth64: String): Boolean = runCatching {
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(Base64.decode(secret, Base64.NO_WRAP), "HmacSHA256"))
        }
        val expected = mac.doFinal(Base64.decode(nonce64, Base64.NO_WRAP))
        MessageDigest.isEqual(expected, Base64.decode(auth64, Base64.NO_WRAP))
    }.getOrDefault(false)

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

    private fun localIpv4(): String? {
        val active = connectivity.activeNetwork
        val fromConnectivity = runCatching {
            knownNetworks.asSequence()
                .mapNotNull { network ->
                    val capabilities = connectivity.getNetworkCapabilities(network) ?: return@mapNotNull null
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
                    val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    val isEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    if (!isWifi && !isEthernet) return@mapNotNull null
                    val rank = when {
                        network == active && isWifi -> 0
                        network == active && isEthernet -> 1
                        isWifi -> 2
                        else -> 3
                    }
                    val address = connectivity.getLinkProperties(network)
                        ?.linkAddresses
                        ?.asSequence()
                        ?.map { it.address }
                        ?.filterIsInstance<Inet4Address>()
                        ?.firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }
                        ?.hostAddress
                    address?.let { rank to it }
                }
                .sortedBy { it.first }
                .map { it.second }
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
                .filterIsInstance<Inet4Address>()
                .mapNotNull { it.hostAddress }
                .firstOrNull { !it.startsWith("169.254.") }
        }.getOrNull()
    }

    private fun selectServerPort(): Int? = probePort(DEFAULT_PORT) ?: probePort(0)

    private fun probePort(port: Int): Int? = runCatching {
        ServerSocket().use { socket ->
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(port))
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
        val host = localIpv4()
        if (host == null) {
            _state.value = HostState.Failed("Connect this phone to Wi-Fi or Ethernet to share pairing data.")
            advertisedHost = null
            advertisedPort = null
            advertisingRestartJob?.cancel()
            advertisingRestartJob = null
            stopAdvertising()
            return
        }

        _state.value = HostState.Ready(
            pairingJson = pairingIdentity.payload(host, current.boundPort).toJson(),
            host = host,
            port = current.boundPort,
            usedFallbackPort = current.boundPort != DEFAULT_PORT,
        )
        if (forceAdvertising || advertisedHost != host || advertisedPort != current.boundPort) {
            restartAdvertising(host, current.boundPort)
        }
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
                if (current?.listening == true && current.boundPort == port && localIpv4() == host) {
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
            serviceName = "Ncy ${Build.MODEL ?: "Android"}"
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("deviceId", identity.deviceId)
            setAttribute("pairingMode", "qr")
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

    private fun mediaRefs(event: ChatEvent): List<Pair<String, String>> {
        val refs = mutableListOf<Pair<String, String>>()
        fun fromMessage(message: Message) {
            MediaHash.extractHash(message.voiceFile)?.let { refs += it to "voice" }
            message.imageFiles.forEach { file -> MediaHash.extractHash(file)?.let { refs += it to "image" } }
            message.content.flatMap { it.spans }.forEach { span ->
                MediaHash.extractHash(span.inlineImage)?.let { refs += it to "image" }
            }
        }
        when (val op = event.op) {
            is ChatEventOp.MessageCreated -> fromMessage(op.message)
            is ChatEventOp.MessageEdited -> fromMessage(op.message)
            is ChatEventOp.ChatMetadataChanged -> {
                MediaHash.extractHash(op.metadata.avatarFile)?.let { refs += it to "image" }
                MediaHash.extractHash(op.metadata.backgroundFile)?.let { refs += it to "image" }
            }
            else -> Unit
        }
        return refs.distinct()
    }

    private companion object {
        const val TAG = "PhoneSyncServer"
        const val DEFAULT_PORT = 8765
        const val CHUNK_SIZE = 64 * 1024
        const val NETWORK_REFRESH_DEBOUNCE_MS = 500L
        const val ADVERTISEMENT_RESTART_DELAY_MS = 500L
        const val PREFS = "ncy_phone_sync_server"
        const val KEY_ENABLED = "enabled"
        const val KEY_HOSTED_PEERS = "hosted_peers"
        const val SERVICE_TYPE = "_ncy-sync._tcp."
    }
}
