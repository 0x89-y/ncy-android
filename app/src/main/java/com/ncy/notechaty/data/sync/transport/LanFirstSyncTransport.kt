package com.ncy.notechaty.data.sync.transport

import com.ncy.notechaty.data.model.SyncTransportMode
import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.discovery.LanNetworkMonitor
import com.ncy.notechaty.data.sync.pear.PearRuntimeController
import com.ncy.notechaty.data.sync.pear.PearRuntimeListener
import com.ncy.notechaty.data.sync.pear.PearRuntimeStatus
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Applies the user's local/Pear transport policy beneath the shared sync protocol. */
@Singleton
class LanFirstSyncTransport @Inject constructor(
    private val lan: LanWebSocketTransport,
    private val pear: PearRuntimeController,
    private val settings: SettingsRepository,
    private val networkMonitor: LanNetworkMonitor,
) : ClientSyncTransport {
    private val allowedKeys = ConcurrentHashMap.newKeySet<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun connect(
        host: String,
        port: Int,
        path: String,
        pearPublicKey: String?,
        listener: ClientSyncTransport.Listener,
    ): ClientSyncTransport.Connection {
        val mode = settings.settings.value.syncTransportMode
        val lock = Any()
        var pearId: String? = null
        var closed = false
        var pearStarted = false
        var lanStarted = false
        var pearFrameReceived = false
        var lanFrameReceived = false
        var pearTimeout: Job? = null
        var lanConnection: ClientSyncTransport.Connection? = null

        fun pearAvailable(): Boolean =
            mode.usesPear && pearPublicKey != null && pear.status.value is PearRuntimeStatus.Ready

        lateinit var startLan: (Throwable) -> Unit
        lateinit var startPear: (Throwable) -> Unit

        lateinit var pearListener: PearRuntimeListener
        pearListener = object : PearRuntimeListener {
            override fun onConnected(connectionId: String, publicKey: String, outgoing: Boolean) {
                if (!outgoing || publicKey != pearPublicKey) return
                synchronized(lock) {
                    if (closed) pear.close(connectionId) else {
                        pearId = connectionId
                        pearTimeout?.cancel()
                        pearTimeout = null
                        listener.onTransportSelected(ClientSyncTransport.Kind.PEAR)
                    }
                }
            }

            override fun onFrame(connectionId: String, text: String) {
                if (connectionId != synchronized(lock) { pearId }) return
                pearFrameReceived = true
                listener.onFrame(text)
            }

            override fun onDisconnected(connectionId: String, publicKey: String) {
                if (connectionId != synchronized(lock) { pearId }) return
                pear.removeListener(this)
                listener.onClosed(1000, "Pear connection closed")
            }

            override fun onConnectionError(publicKey: String?, connectionId: String?, message: String) {
                if (!pearStarted || (publicKey != null && publicKey != pearPublicKey)) return
                if (connectionId != null && connectionId != synchronized(lock) { pearId }) return
                pearTimeout?.cancel()
                pearPublicKey?.let(pear::cancelConnect)
                pear.removeListener(this)
                val failure = IllegalStateException(message)
                if (mode == SyncTransportMode.PEAR_PREFERRED && !pearFrameReceived) {
                    pearStarted = false
                    startLan(failure)
                }
                else listener.onFailure(failure)
            }
        }

        startPear = { cause ->
            if (closed || pearStarted) Unit
            else if (!pearAvailable()) {
                if (mode == SyncTransportMode.PEAR_PREFERRED) startLan(cause)
                else listener.onFailure(cause)
            } else {
                pearStarted = true
                allowedKeys += checkNotNull(pearPublicKey)
                pear.allowPeers(allowedKeys)
                pear.addListener(pearListener)
                pear.connect(pearPublicKey)
                pearTimeout = scope.launch {
                    delay(PEAR_CONNECT_TIMEOUT_MS)
                    val timedOut = synchronized(lock) { !closed && pearId == null }
                    if (timedOut) {
                        pear.cancelConnect(pearPublicKey)
                        pear.removeListener(pearListener)
                        val failure = IllegalStateException("Pear connection timed out")
                        if (mode == SyncTransportMode.PEAR_PREFERRED) {
                            pearStarted = false
                            startLan(failure)
                        }
                        else listener.onFailure(failure)
                    }
                }
            }
        }

        startLan = { cause ->
            if (closed || lanStarted) Unit
            else if (mode == SyncTransportMode.PEAR_ONLY) listener.onFailure(cause)
            else {
                lanStarted = true
                lanConnection = lan.connect(host, port, path, pearPublicKey, object : ClientSyncTransport.Listener {
                    override fun onTransportSelected(kind: ClientSyncTransport.Kind) =
                        listener.onTransportSelected(kind)

                    override fun onFrame(text: String) {
                        lanFrameReceived = true
                        listener.onFrame(text)
                    }

                    override fun onFailure(lanFailure: Throwable) {
                        if (mode == SyncTransportMode.LOCAL_PREFERRED && !lanFrameReceived) startPear(lanFailure)
                        else listener.onFailure(lanFailure)
                    }

                    override fun onClosed(code: Int, reason: String) {
                        if (!pearStarted) listener.onClosed(code, reason)
                    }
                })
            }
        }

        when (mode) {
            SyncTransportMode.LOCAL_ONLY -> startLan(IllegalStateException("Local transport unavailable"))
            SyncTransportMode.PEAR_ONLY -> startPear(IllegalStateException("Pear transport unavailable or peer has no Pear key"))
            SyncTransportMode.LOCAL_PREFERRED -> {
                if (networkMonitor.available.value) startLan(IllegalStateException("Local transport unavailable"))
                else startPear(IllegalStateException("No usable LAN"))
            }
            SyncTransportMode.PEAR_PREFERRED -> startPear(IllegalStateException("Pear transport unavailable or peer has no Pear key"))
        }

        return object : ClientSyncTransport.Connection {
            override fun send(text: String): Boolean = synchronized(lock) {
                pearId?.let { pear.send(it, text) } ?: lanConnection?.send(text) ?: false
            }

            override fun close(code: Int, reason: String): Boolean {
                synchronized(lock) {
                    if (closed) return false
                    closed = true
                    pearTimeout?.cancel()
                    if (pearStarted && pearId == null && pearPublicKey != null) pear.cancelConnect(pearPublicKey)
                    pearId?.let(pear::close)
                }
                pear.removeListener(pearListener)
                return lanConnection?.close(code, reason) ?: true
            }
        }
    }

    private companion object {
        const val PEAR_CONNECT_TIMEOUT_MS = 20_000L
    }
}
