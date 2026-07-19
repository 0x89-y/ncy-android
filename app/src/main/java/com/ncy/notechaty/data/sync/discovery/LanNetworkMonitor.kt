package com.ncy.notechaty.data.sync.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Foreground LAN signal used to suspend retry timers when no local transport exists. */
@Singleton
class LanNetworkMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val connectivity =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networks = ConcurrentHashMap<Network, NetworkCapabilities>()
    private val _available = MutableStateFlow(hasLanInterface())
    val available: StateFlow<Boolean> = _available.asStateFlow()
    private val _internetAvailable = MutableStateFlow(hasInternetNetwork())
    val internetAvailable: StateFlow<Boolean> = _internetAvailable.asStateFlow()
    private var callback: ConnectivityManager.NetworkCallback? = null

    @Synchronized
    fun start() {
        if (callback != null) return
        val next = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh(network)
            override fun onLost(network: Network) {
                networks.remove(network)
                publish()
            }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                networks[network] = capabilities
                publish()
            }
            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) =
                publish()
        }
        callback = next
        _available.value = hasLanInterface()
        _internetAvailable.value = hasInternetNetwork()
        runCatching {
            connectivity.registerNetworkCallback(
                NetworkRequest.Builder().clearCapabilities().build(),
                next,
            )
        }.onFailure { callback = null }
    }

    @Synchronized
    fun stop() {
        val current = callback
        callback = null
        networks.clear()
        if (current != null) runCatching { connectivity.unregisterNetworkCallback(current) }
    }

    private fun refresh(network: Network) {
        connectivity.getNetworkCapabilities(network)?.let { networks[network] = it }
        publish()
    }

    private fun publish() {
        _available.value = networks.values.any(::isLan) || hasLanInterface()
        _internetAvailable.value = networks.values.any(::hasInternet) || hasInternetNetwork()
    }

    private fun isLan(capabilities: NetworkCapabilities): Boolean =
        !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
            (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))

    private fun hasInternet(capabilities: NetworkCapabilities): Boolean =
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    private fun hasInternetNetwork(): Boolean =
        connectivity.activeNetwork?.let(connectivity::getNetworkCapabilities)?.let(::hasInternet) == true

    private fun hasLanInterface(): Boolean = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().any { network ->
            network.isUp && !network.isLoopback && isLanInterfaceName(network.name) &&
                network.inetAddresses.toList().any {
                    !it.isAnyLocalAddress && !it.isLoopbackAddress && !it.isLinkLocalAddress
                }
        }
    }.getOrDefault(false)

    private fun isLanInterfaceName(name: String): Boolean =
        name.startsWith("wlan", ignoreCase = true) ||
            name.startsWith("swlan", ignoreCase = true) ||
            name.startsWith("ap", ignoreCase = true) ||
            name.startsWith("eth", ignoreCase = true)
}
