package com.ncy.notechaty.data.sync.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ncy.notechaty.data.sync.DeviceIdentity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One desktop the phone has discovered on the LAN via Bonjour/mDNS-SD. Pairing still
 * requires the QR-encoded shared secret — discovery just removes the IP-typing step
 * (and confirms the desktop is actually reachable).
 */
data class DiscoveredPeer(
    val name: String,
    val host: String,
    val port: Int,
    val deviceId: String?,
    val pairingMode: String,
) {
    val supportsPairRequest: Boolean get() = pairingMode == PAIRING_MODE_REQUEST

    companion object {
        const val PAIRING_MODE_REQUEST = "request"
        const val PAIRING_MODE_QR = "qr"
    }
}

@Singleton
class MdnsDiscovery @Inject constructor(
    @ApplicationContext private val context: Context,
    private val identity: DeviceIdentity,
) {
    private val tag = "MdnsDiscovery"
    private val serviceType = "_ncy-sync._tcp."

    private val nsd: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val _peers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val peers: StateFlow<List<DiscoveredPeer>> = _peers.asStateFlow()

    private var listener: NsdManager.DiscoveryListener? = null

    @Synchronized
    fun start() {
        if (listener != null) return
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.i(tag, "discovery started for $serviceType")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(tag, "discovery stopped for $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(tag, "start discovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(tag, "stop discovery failed: $errorCode")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                resolve(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                _peers.value = _peers.value.filterNot { it.name == service.serviceName }
            }
        }
        listener = l
        try {
            nsd.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, l)
        } catch (e: Exception) {
            Log.w(tag, "could not start discovery", e)
            listener = null
        }
    }

    @Synchronized
    fun stop() {
        val l = listener ?: return
        try {
            nsd.stopServiceDiscovery(l)
        } catch (e: Exception) {
            Log.w(tag, "could not stop discovery", e)
        }
        listener = null
        _peers.value = emptyList()
    }

    private fun resolve(service: NsdServiceInfo) {
        // The single-arg `resolveService(info, ResolveListener)` is available across
        // every API level we target (min SDK 31). Newer overloads (executor variant,
        // ServiceInfoCallback) exist on API 34+ but offer no new behavior we need.
        val listener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w(tag, "resolve failed for ${serviceInfo.serviceName}: $errorCode")
            }
            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                onResolveResult(serviceInfo)
            }
        }
        try {
            @Suppress("DEPRECATION")
            nsd.resolveService(service, listener)
        } catch (e: Exception) {
            Log.w(tag, "could not resolve ${service.serviceName}", e)
        }
    }

    private fun onResolveResult(info: NsdServiceInfo) {
        @Suppress("DEPRECATION")
        val host = info.host?.hostAddress ?: return
        val port = info.port
        if (port <= 0) return
        val deviceId = info.attributes["deviceId"]?.let { String(it) }
        if (deviceId == identity.deviceId) return
        // Pre-capability desktop builds supported PairRequest, so retain that as
        // the compatibility default when the TXT field is absent.
        val pairingMode = info.attributes["pairingMode"]?.let { String(it) }
            ?: DiscoveredPeer.PAIRING_MODE_REQUEST
        val peer = DiscoveredPeer(
            name = info.serviceName ?: "Unknown",
            host = host,
            port = port,
            deviceId = deviceId,
            pairingMode = pairingMode,
        )
        _peers.value = (_peers.value.filterNot { it.name == peer.name } + peer)
            .sortedBy { it.name.lowercase() }
    }
}
