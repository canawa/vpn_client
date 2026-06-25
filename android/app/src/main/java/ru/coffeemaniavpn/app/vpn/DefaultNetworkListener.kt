package ru.coffeemaniavpn.app.vpn

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import ru.coffeemaniavpn.app.App

internal object DefaultNetworkListener {
    private var registered = false

    fun start(onNetwork: (Network?) -> Unit) {
        if (registered) return
        registered = true
        val cm = App.connectivity
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    onNetwork(network)
                }

                override fun onLost(network: Network) {
                    onNetwork(null)
                }
            },
        )
    }
}
