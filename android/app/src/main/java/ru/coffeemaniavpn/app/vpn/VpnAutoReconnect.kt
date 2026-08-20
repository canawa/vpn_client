package ru.coffeemaniavpn.app.vpn

import android.net.Network
import android.net.VpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.data.AppPreferences
import ru.coffeemaniavpn.app.data.LoadBalancer
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
        App.applicationScope.launch(Dispatchers.IO) {
            maybeLeaveLteOnWifi()
            if (VpnManager.status.value != VpnStatus.Stopped) return@launch
            if (!shouldReconnect()) return@launch
            AppLog.i("VpnAutoReconnect network restored, scheduling reconnect")
            withContext(Dispatchers.Main) { schedule() }
        }
    }

    /** На Wi‑Fi не переподключаемся к LTE/BS, если есть обычный сервер. */
    private suspend fun maybeLeaveLteOnWifi() {
        if (!LoadBalancer.isOnWifi()) return
        val current = connectedNode() ?: return
        if (!LoadBalancer.isBsServer(current)) return
        val status = VpnManager.status.value
        if (status != VpnStatus.Started && status != VpnStatus.Starting) return
        val prefs = AppPreferences(App.instance)
        if (prefs.selectedNodeId.first() != LoadBalancer.AUTO_NODE_ID) return
        // Возврат на NORMAL делает VpnPoolBalancer по health-check; здесь только
        // не оставляем BS из-за «привычки» LTE→Wi‑Fi без проверки.
        val next = LoadBalancer.pickBestNormal(prefs.nodes.first(), emptyMap()) ?: return
        if (next.id == current.id) return
        AppLog.i("WiFi: AUTO leaving BS/LTE ${current.name} -> ${next.name}")
        withContext(Dispatchers.Main) {
            VpnManager.switchToNode(next)
        }
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
            var node = lastNode ?: return@launch
            if (LoadBalancer.isBsServer(node)) {
                val prefs = AppPreferences(App.instance)
                if (prefs.selectedNodeId.first() == LoadBalancer.AUTO_NODE_ID) {
                    // При реконнекте сначала NORMAL; BS только если обычных нет.
                    val preferred = LoadBalancer.pickBestNormal(prefs.nodes.first(), emptyMap())
                    if (preferred != null) {
                        node = preferred
                        lastNode = node
                        AppLog.i("VpnAutoReconnect prefer NORMAL over BS: ${node.name}")
                    }
                }
            }
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
