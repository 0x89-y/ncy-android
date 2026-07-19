package com.ncy.notechaty.data.sync.transport

import com.ncy.notechaty.data.repository.SettingsRepository
import com.ncy.notechaty.data.sync.discovery.LanNetworkMonitor
import com.ncy.notechaty.data.sync.pear.PearRuntimeController
import com.ncy.notechaty.data.sync.pear.PearRuntimeListener
import com.ncy.notechaty.data.sync.pear.PearRuntimeStatus
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Uses the local WebSocket first and falls back to Pear only after an explicit opt-in. */
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
        val lock = Any()
        var pearId: String? = null
        var closed = false
        var fallingBack = false
        var lanFrameReceived = false
        var pearTimeout: Job? = null
        var lanConnection: ClientSyncTransport.Connection? = null

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
                if (connectionId == synchronized(lock) { pearId }) listener.onFrame(text)
            }

            override fun onDisconnected(connectionId: String, publicKey: String) {
                if (connectionId != synchronized(lock) { pearId }) return
                pear.removeListener(this)
                listener.onClosed(1000, "Pear connection closed")
            }

            override fun onConnectionError(publicKey: String?, connectionId: String?, message: String) {
                if (!fallingBack || (publicKey != null && publicKey != pearPublicKey)) return
                if (connectionId != null && connectionId != synchronized(lock) { pearId }) return
                pearTimeout?.cancel()
                pearPublicKey?.let(pear::cancelConnect)
                pear.removeListener(this)
                listener.onFailure(IllegalStateException(message))
            }
        }

        fun tryPear(cause: Throwable) {
            val enabled = settings.settings.value.pearSyncEnabled
            val ready = pear.status.value is PearRuntimeStatus.Ready
            if (!enabled || !ready || pearPublicKey == null) {
                listener.onFailure(cause)
                return
            }
            synchronized(lock) {
                if (closed || fallingBack) return
                if (lanFrameReceived) {
                    listener.onFailure(cause)
                    return
                }
                fallingBack = true
            }
            allowedKeys += pearPublicKey
            pear.allowPeers(allowedKeys)
            pear.addListener(pearListener)
            pear.connect(pearPublicKey)
            pearTimeout = scope.launch {
                delay(PEAR_CONNECT_TIMEOUT_MS)
                val timedOut = synchronized(lock) { !closed && pearId == null }
                if (timedOut) {
                    pear.cancelConnect(pearPublicKey)
                    pear.removeListener(pearListener)
                    listener.onFailure(IllegalStateException("Pear connection timed out"))
                }
            }
        }

        if (!networkMonitor.available.value && pearPublicKey != null) {
            tryPear(IllegalStateException("No usable LAN"))
        } else {
            lanConnection = lan.connect(host, port, path, pearPublicKey, object : ClientSyncTransport.Listener {
                override fun onTransportSelected(kind: ClientSyncTransport.Kind) =
                    listener.onTransportSelected(kind)
                override fun onFrame(text: String) {
                    synchronized(lock) { lanFrameReceived = true }
                    listener.onFrame(text)
                }
                override fun onFailure(cause: Throwable) = tryPear(cause)
                override fun onClosed(code: Int, reason: String) {
                    if (!fallingBack) listener.onClosed(code, reason)
                }
            })
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
                    if (fallingBack && pearId == null && pearPublicKey != null) {
                        pear.cancelConnect(pearPublicKey)
                    }
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
