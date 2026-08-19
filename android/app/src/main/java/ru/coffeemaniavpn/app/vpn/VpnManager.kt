package ru.coffeemaniavpn.app.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.data.ConnectionSettingsStore
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.data.XrayConfigBuilder
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.widget.VpnHomeWidgetUpdater

data class VpnTrafficRates(
    val downlinkBytesPerSec: Long = 0L,
    val uplinkBytesPerSec: Long = 0L,
)

object VpnManager {
    @Volatile
    var userInitiatedDisconnect: Boolean = false

    @Volatile
    private var pendingReconnectNode: ProxyNode? = null

    private val _status = MutableStateFlow(VpnStatus.Stopped)
    val status = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    private val _connectionElapsedMs = MutableStateFlow(0L)
    val connectionElapsedMs = _connectionElapsedMs.asStateFlow()

    private val _trafficRates = MutableStateFlow(VpnTrafficRates())
    val trafficRates = _trafficRates.asStateFlow()

    private var connectedSinceMs: Long? = null
    private var elapsedTickerJob: Job? = null
    private var trafficTickerJob: Job? = null

    fun init() {
        VpnAutoReconnect.startNetworkWatcher()
    }

    internal fun setStatus(value: VpnStatus) {
        val previous = _status.value
        if (previous != value) {
            AppLog.i("VpnManager status $previous -> $value")
            VpnDiagnostics.snapshot("status-$value")
        }
        when (value) {
            VpnStatus.Started -> {
                if (connectedSinceMs == null) {
                    connectedSinceMs = System.currentTimeMillis()
                }
                startElapsedTicker()
                VpnAutoReconnect.onConnected()
                VpnPoolBalancer.onVpnStarted()
            }
            VpnStatus.Stopped -> {
                connectedSinceMs = null
                _connectionElapsedMs.value = 0L
                _trafficRates.value = VpnTrafficRates()
                stopElapsedTicker()
                VpnPoolBalancer.onVpnStopped()
            }
            else -> Unit
        }
        _status.value = value
        if (previous != value) {
            runCatching { VpnHomeWidgetUpdater.updateAll(App.instance) }
        }
    }

    private fun startElapsedTicker() {
        stopElapsedTicker()
        // Отдельно от Main и от queryStats: иначе таймер «замирает» в настройках.
        elapsedTickerJob = App.applicationScope.launch(Dispatchers.Default) {
            while (connectedSinceMs != null) {
                val since = connectedSinceMs ?: break
                _connectionElapsedMs.value = System.currentTimeMillis() - since
                delay(1_000)
            }
        }
        trafficTickerJob = App.applicationScope.launch(Dispatchers.IO) {
            var primed = false
            while (connectedSinceMs != null) {
                val (uplink, downlink) = runCatching {
                    XrayCoreManager.queryTrafficDelta()
                }.getOrDefault(0L to 0L)
                if (primed) {
                    val rates = VpnTrafficRates(
                        downlinkBytesPerSec = downlink,
                        uplinkBytesPerSec = uplink,
                    )
                    _trafficRates.value = rates
                    BoxService.updateTrafficNotification(rates)
                } else {
                    primed = true
                }
                delay(1_000)
            }
        }
    }

    private fun stopElapsedTicker() {
        elapsedTickerJob?.cancel()
        elapsedTickerJob = null
        trafficTickerJob?.cancel()
        trafficTickerJob = null
    }

    internal fun setError(message: String?) {
        if (message != null) {
            AppLog.e("VpnManager error: $message")
            VpnDiagnostics.snapshot("error")
        }
        _lastError.value = message
    }

    fun connect(node: ProxyNode) {
        userInitiatedDisconnect = false
        VpnAutoReconnect.rememberNode(node)
        performConnect(node)
    }

    /** Переподключение без сброса счётчика попыток auto-reconnect. */
    internal fun connectForReconnect(node: ProxyNode) {
        userInitiatedDisconnect = false
        performConnect(node)
    }

    private fun performConnect(node: ProxyNode) {
        _lastError.value = null
        VpnDiagnostics.snapshot("connect-start")
        KillSwitchVpnService.releaseImmediate()
        KillSwitchVpnService.release(App.instance)
        XrayCoreManager.stopLoop()
        App.applicationScope.launch(Dispatchers.IO) {
            App.awaitSettingsReady()
            try {
                val config = XrayConfigBuilder.build(node)
                AppLog.i(
                    "VpnManager.connect node=${node.name} protocol=${node.protocol} " +
                        "transport=${node.transport} host=${node.host}:${node.port} " +
                        "rawOutbound=${node.rawOutboundJson != null}",
                )
                AppLog.i("VpnManager config (${config.length} bytes):\n$config")
                App.configFile.writeText(config)
                BoxService.start()
            } catch (t: Throwable) {
                AppLog.e("VpnManager.connect failed", t)
                _lastError.value = t.message ?: "Ошибка подключения"
                setStatus(VpnStatus.Stopped)
                VpnAutoReconnect.onConnectFailed()
            }
        }
    }

    /** Смена узла: остановить туннель и поднять его на [node] без kill switch. */
    fun switchToNode(node: ProxyNode) {
        AppLog.i("VpnManager.switchToNode ${node.name}")
        pendingReconnectNode = node
        disconnect(userInitiated = true)
    }

    fun disconnect(userInitiated: Boolean = true) {
        AppLog.i("VpnManager.disconnect userInitiated=$userInitiated")
        if (userInitiated) {
            markUserDisconnectRequested()
        } else {
            userInitiatedDisconnect = false
        }
        BoxService.stop(userInitiated)
    }

    /** Остановка из уведомления, отзыва VPN системой и т.п. — без автопереподключения. */
    internal fun markUserDisconnectRequested() {
        userInitiatedDisconnect = true
        VpnAutoReconnect.clear()
        KillSwitchVpnService.release(App.instance)
    }

    internal fun onVpnFullyStopped() {
        VpnDiagnostics.snapshot("fully-stopped")
        val next = pendingReconnectNode
        pendingReconnectNode = null
        if (next != null) {
            AppLog.i("VpnManager reconnect after switch node=${next.name}")
            userInitiatedDisconnect = false
            connect(next)
            return
        }
        val engageKillSwitch = !userInitiatedDisconnect
        App.applicationScope.launch(Dispatchers.IO) {
            App.awaitSettingsReady()
            if (engageKillSwitch && ConnectionSettingsStore.state.killSwitchEnabled) {
                KillSwitchVpnService.engage(App.instance)
            }
        }

        VpnAutoReconnect.onUnexpectedDisconnect()

        userInitiatedDisconnect = false
    }
}
