package ru.coffeemaniavpn.app.vpn

import android.net.Network
import android.net.VpnService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.util.AppLog

/**
 * Планирует переподключение к последнему серверу после неожиданного обрыва VPN.
 */
internal object VpnAutoReconnect {
    private const val INITIAL_DELAY_MS = 2_000L
    private const val MAX_DELAY_MS = 30_000L

    private var lastNode: ProxyNode? = null
    private var reconnectJob: Job? = null
    private var attempt = 0
    private var networkWatcherStarted = false

    fun rememberNode(node: ProxyNode) {
        lastNode = node
        attempt = 0
    }

    fun connectedNode(): ProxyNode? = lastNode

    fun clear() {
        lastNode = null
        attempt = 0
        cancelScheduled()
    }

    fun onConnected() {
        attempt = 0
        cancelScheduled()
    }

    fun onUnexpectedDisconnect() {
        if (!shouldReconnect()) return
        AppLog.i("VpnAutoReconnect unexpected disconnect, scheduling reconnect")
        schedule()
    }

    fun onConnectFailed() {
        if (!shouldReconnect()) return
        AppLog.w("VpnAutoReconnect connect failed, scheduling retry")
        schedule()
    }

    fun tryReconnectOnResume() {
        if (VpnManager.status.value != VpnStatus.Stopped) return
        if (!shouldReconnect()) return
        AppLog.i("VpnAutoReconnect resume, scheduling reconnect")
        schedule()
    }

    fun startNetworkWatcher() {
        if (networkWatcherStarted) return
        networkWatcherStarted = true
        App.applicationScope.launch {
            DefaultNetworkListener.start { network ->
                onNetworkAvailable(network)
            }
        }
    }

    private fun onNetworkAvailable(network: Network?) {
        if (network == null) return
        if (VpnManager.status.value != VpnStatus.Stopped) return
        if (!shouldReconnect()) return
        AppLog.i("VpnAutoReconnect network restored, scheduling reconnect")
        schedule()
    }

    private fun shouldReconnect(): Boolean {
        if (lastNode == null) return false
        if (VpnManager.userInitiatedDisconnect) return false
        if (VpnService.prepare(App.instance) != null) return false
        return true
    }

    private fun schedule() {
        cancelScheduled()
        reconnectJob = App.applicationScope.launch {
            waitUntilStopped()
            val node = lastNode ?: return@launch
            if (!shouldReconnect()) return@launch

            val delayMs = nextDelay()
            attempt++
            AppLog.i("VpnAutoReconnect attempt=$attempt delay=${delayMs}ms node=${node.name}")
            delay(delayMs)

            if (!isActive || !shouldReconnect()) return@launch
            if (VpnManager.status.value != VpnStatus.Stopped) return@launch

            VpnManager.connectForReconnect(node)
        }
    }

    private suspend fun waitUntilStopped() {
        repeat(40) {
            if (VpnManager.status.value == VpnStatus.Stopped) return
            delay(250)
        }
    }

    private fun nextDelay(): Long {
        val multiplier = 1L shl attempt.coerceAtMost(4)
        return (INITIAL_DELAY_MS * multiplier).coerceAtMost(MAX_DELAY_MS)
    }

    private fun cancelScheduled() {
        reconnectJob?.cancel()
        reconnectJob = null
    }
}
