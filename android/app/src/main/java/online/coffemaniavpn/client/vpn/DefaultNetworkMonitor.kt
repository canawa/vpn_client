package online.coffemaniavpn.client.vpn

import android.net.Network
import android.os.Build
import android.os.Handler
import android.os.Looper
import io.nekohasekai.libbox.InterfaceUpdateListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.runBlocking
import online.coffemaniavpn.client.App

object DefaultNetworkListener {
    private sealed class NetworkMessage {
        class Start(val key: Any, val listener: (Network?) -> Unit) : NetworkMessage()
        class Get : NetworkMessage() {
            val response = CompletableDeferred<Network>()
        }
        class Stop(val key: Any) : NetworkMessage()
        class Put(val network: Network) : NetworkMessage()
        class Update(val network: Network) : NetworkMessage()
        class Lost(val network: Network) : NetworkMessage()
    }

    @OptIn(DelicateCoroutinesApi::class, ObsoleteCoroutinesApi::class)
    private val networkActor = GlobalScope.actor<NetworkMessage>(Dispatchers.Unconfined) {
        val listeners = mutableMapOf<Any, (Network?) -> Unit>()
        var network: Network? = null
        val pendingRequests = arrayListOf<NetworkMessage.Get>()
        for (message in channel) {
            when (message) {
                is NetworkMessage.Start -> {
                    if (listeners.isEmpty()) register()
                    listeners[message.key] = message.listener
                    if (network != null) message.listener(network)
                }
                is NetworkMessage.Get -> {
                    if (network == null) pendingRequests += message else message.response.complete(network)
                }
                is NetworkMessage.Stop -> {
                    if (listeners.remove(message.key) != null && listeners.isEmpty()) {
                        network = null
                        unregister()
                    }
                }
                is NetworkMessage.Put -> {
                    network = message.network
                    pendingRequests.forEach { it.response.complete(message.network) }
                    pendingRequests.clear()
                    listeners.values.forEach { it(network) }
                }
                is NetworkMessage.Update -> if (network == message.network) {
                    listeners.values.forEach { it(network) }
                }
                is NetworkMessage.Lost -> if (network == message.network) {
                    network = null
                    listeners.values.forEach { it(null) }
                }
            }
        }
    }

    suspend fun start(key: Any, listener: (Network?) -> Unit) {
        networkActor.send(NetworkMessage.Start(key, listener))
    }

    suspend fun get(): Network {
        if (fallback) {
            return App.connectivity.activeNetwork ?: error("missing default network")
        }
        return NetworkMessage.Get().run {
            networkActor.send(this)
            response.await()
        }
    }

    suspend fun stop(key: Any) {
        networkActor.send(NetworkMessage.Stop(key))
    }

    private object Callback : android.net.ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = runBlocking {
            networkActor.send(NetworkMessage.Put(network))
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: android.net.NetworkCapabilities,
        ) = runBlocking {
            networkActor.send(NetworkMessage.Update(network))
        }

        override fun onLost(network: Network) = runBlocking {
            networkActor.send(NetworkMessage.Lost(network))
        }
    }

    private var fallback = false
    private val request = android.net.NetworkRequest.Builder().apply {
        addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
    }.build()
    private val mainHandler = Handler(Looper.getMainLooper())

    private fun register() {
        when {
            Build.VERSION.SDK_INT >= 31 -> {
                App.connectivity.registerBestMatchingNetworkCallback(request, Callback, mainHandler)
            }
            Build.VERSION.SDK_INT >= 28 -> {
                App.connectivity.requestNetwork(request, Callback, mainHandler)
            }
            Build.VERSION.SDK_INT >= 26 -> {
                App.connectivity.registerDefaultNetworkCallback(Callback, mainHandler)
            }
            Build.VERSION.SDK_INT >= 24 -> {
                App.connectivity.registerDefaultNetworkCallback(Callback)
            }
            else -> {
                try {
                    fallback = false
                    App.connectivity.requestNetwork(request, Callback)
                } catch (_: RuntimeException) {
                    fallback = true
                }
            }
        }
    }

    private fun unregister() {
        runCatching { App.connectivity.unregisterNetworkCallback(Callback) }
    }
}

object DefaultNetworkMonitor {
    var defaultNetwork: Network? = null
        private set
    private var listener: InterfaceUpdateListener? = null

    suspend fun start() {
        DefaultNetworkListener.start(this) {
            defaultNetwork = it
            checkDefaultInterfaceUpdate(it)
        }
        defaultNetwork = App.connectivity.activeNetwork
    }

    suspend fun stop() {
        DefaultNetworkListener.stop(this)
    }

    fun setListener(listener: InterfaceUpdateListener?) {
        this.listener = listener
        checkDefaultInterfaceUpdate(defaultNetwork)
    }

    private fun checkDefaultInterfaceUpdate(newNetwork: Network?) {
        val listener = listener ?: return
        if (newNetwork != null) {
            repeat(10) {
                val linkProperties = App.connectivity.getLinkProperties(newNetwork) ?: run {
                    Thread.sleep(100)
                    return@repeat
                }
                val interfaceIndex = runCatching {
                    java.net.NetworkInterface.getByName(linkProperties.interfaceName).index
                }.getOrNull() ?: run {
                    Thread.sleep(100)
                    return@repeat
                }
                listener.updateDefaultInterface(linkProperties.interfaceName, interfaceIndex, false, false)
                return
            }
        } else {
            listener.updateDefaultInterface("", -1, false, false)
        }
    }
}
