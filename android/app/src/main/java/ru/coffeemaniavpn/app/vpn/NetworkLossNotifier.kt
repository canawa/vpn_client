package ru.coffeemaniavpn.app.vpn

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.util.AppLog

/**
 * In-app тост при пропаже физической сети (Wi‑Fi / LTE).
 * Не срабатывает при переключении Wi‑Fi ↔ LTE.
 * Системное уведомление в шторку не показывается.
 */
internal object NetworkLossNotifier {
    private const val DEBOUNCE_MS = 2_000L

    private var started = false
    private var hadPhysicalNetwork = false
    private var notified = false
    private var debounceJob: Job? = null
    private val physicalNetworks = mutableSetOf<Network>()

    private val _lostEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** Событие для in-app тоста. */
    val lostEvents: SharedFlow<Unit> = _lostEvents.asSharedFlow()

    private val _restoredEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val restoredEvents: SharedFlow<Unit> = _restoredEvents.asSharedFlow()

    fun start() {
        if (started) return
        started = true
        runCatching {
            seedPhysicalNetworks()
            hadPhysicalNetwork = physicalNetworks.isNotEmpty()
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
            App.connectivity.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        App.applicationScope.launch {
                            physicalNetworks.add(network)
                            onConnectivityChanged()
                        }
                    }

                    override fun onLost(network: Network) {
                        App.applicationScope.launch {
                            physicalNetworks.remove(network)
                            onConnectivityChanged()
                        }
                    }
                },
            )
            AppLog.i("NetworkLossNotifier started hadPhysical=$hadPhysicalNetwork")
        }.onFailure {
            started = false
            AppLog.e("NetworkLossNotifier start failed", it)
        }
    }

    private fun onConnectivityChanged() {
        val online = physicalNetworks.isNotEmpty()
        if (online) {
            debounceJob?.cancel()
            debounceJob = null
            hadPhysicalNetwork = true
            if (notified) {
                _restoredEvents.tryEmit(Unit)
                notified = false
            }
            return
        }
        if (!hadPhysicalNetwork || notified) return
        debounceJob?.cancel()
        debounceJob = App.applicationScope.launch {
            delay(DEBOUNCE_MS)
            if (physicalNetworks.isNotEmpty()) return@launch
            if (!hadPhysicalNetwork || notified) return@launch
            hadPhysicalNetwork = false
            notified = true
            _lostEvents.tryEmit(Unit)
        }
    }

    private fun seedPhysicalNetworks() {
        physicalNetworks.clear()
        App.connectivity.allNetworks.forEach { network ->
            val caps = App.connectivity.getNetworkCapabilities(network) ?: return@forEach
            if (isPhysicalInternet(caps)) physicalNetworks.add(network)
        }
    }

    private fun isPhysicalInternet(caps: NetworkCapabilities): Boolean {
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
