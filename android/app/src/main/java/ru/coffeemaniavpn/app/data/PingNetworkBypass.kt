package ru.coffeemaniavpn.app.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.ParcelFileDescriptor
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.VPNService
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * Пинг мимо VPN: DNS и TCP через физическую сеть клиента (Wi‑Fi / LTE),
 * чтобы RTT отражал реальное местоположение, а не маршрут через туннель.
 */
internal object PingNetworkBypass {
    private val ipCache = ConcurrentHashMap<String, String>()

    @Volatile
    private var physicalNetwork: Network? = null

    private var registered = false

    fun ensureListening(context: Context = App.instance) {
        if (registered) return
        synchronized(this) {
            if (registered) return
            registered = true
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
            cm.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        physicalNetwork = network
                        AppLog.i("PingNetworkBypass onAvailable network=$network")
                    }

                    override fun onLost(network: Network) {
                        if (physicalNetwork == network) {
                            physicalNetwork = null
                        }
                    }
                },
            )
            refreshPhysicalNetwork(cm)
        }
    }

    fun resolveHost(host: String): String? {
        ensureListening()
        val trimmed = host.trim()
        if (trimmed.isEmpty()) return null
        if (isIpAddress(trimmed)) return trimmed
        ipCache[trimmed]?.let { return it }

        val network = currentPhysicalNetwork()
        return runCatching {
            val addresses = if (network != null) {
                network.getAllByName(trimmed)
            } else {
                arrayOf(java.net.InetAddress.getByName(trimmed))
            }
            addresses.firstOrNull()?.hostAddress?.also { ip -> ipCache[trimmed] = ip }
        }.getOrNull()
    }

    fun tcpConnect(host: String, port: Int, timeoutMs: Int): Int? {
        if (port !in 1..65535) return null
        ensureListening()
        val network = currentPhysicalNetwork()
        val socket = network?.socketFactory?.createSocket() ?: Socket()
        try {
            network?.bindSocket(socket)
            VPNService.tryProtect(socket)
            val start = System.nanoTime()
            socket.connect(InetSocketAddress(host, port), timeoutMs)
            return ((System.nanoTime() - start) / 1_000_000L).toInt().coerceAtLeast(1)
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun currentPhysicalNetwork(): Network? {
        physicalNetwork?.let { return it }
        val cm = App.connectivity
        refreshPhysicalNetwork(cm)
        return physicalNetwork
    }

    private fun refreshPhysicalNetwork(cm: ConnectivityManager) {
        val active = cm.activeNetwork
        val activeCaps = active?.let { cm.getNetworkCapabilities(it) }
        if (activeCaps != null &&
            !activeCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
            activeCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        ) {
            physicalNetwork = active
            return
        }

        physicalNetwork = cm.allNetworks
            .mapNotNull { net ->
                val caps = cm.getNetworkCapabilities(net) ?: return@mapNotNull null
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return@mapNotNull null
                if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return@mapNotNull null
                net to caps
            }
            .maxByOrNull { (_, caps) -> networkScore(caps) }
            ?.first
    }

    private fun networkScore(caps: NetworkCapabilities): Int {
        var score = 0
        if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) score += 100
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) score += 30
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) score += 25
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) score += 10
        return score
    }

    private fun isIpAddress(host: String): Boolean =
        host.all { it.isDigit() || it == '.' || it == ':' || it == '[' || it == ']' }
}
