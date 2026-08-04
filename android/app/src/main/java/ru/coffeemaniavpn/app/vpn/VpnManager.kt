package ru.coffeemaniavpn.app.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.data.ConnectionSettingsStore
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.data.XrayConfigBuilder
import ru.coffeemaniavpn.app.util.AppLog

data class VpnTrafficRates(
    val downlinkBytesPerSec: Long = 0L,
    val uplinkBytesPerSec: Long = 0L,
)

object VpnManager {
    @Volatile
    var userInitiatedDisconnect: Boolean = false

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

    fun init() {
        VpnAutoReconnect.startNetworkWatcher()
    }

    internal fun setStatus(value: VpnStatus) {
        when (value) {
            VpnStatus.Started -> {
                if (connectedSinceMs == null) {
                    connectedSinceMs = System.currentTimeMillis()
                }
                startElapsedTicker()
                VpnAutoReconnect.onConnected()
            }
            VpnStatus.Stopped -> {
                connectedSinceMs = null
                _connectionElapsedMs.value = 0L
                _trafficRates.value = VpnTrafficRates()
                stopElapsedTicker()
            }
            else -> Unit
        }
        _status.value = value
    }

    private fun startElapsedTicker() {
        stopElapsedTicker()
        elapsedTickerJob = App.applicationScope.launch {
            var primed = false
            while (connectedSinceMs != null) {
                val since = connectedSinceMs ?: break
                _connectionElapsedMs.value = System.currentTimeMillis() - since

                val (uplink, downlink) = withContext(Dispatchers.IO) {
                    XrayCoreManager.queryTrafficDelta()
                }
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
    }

    internal fun setError(message: String?) {
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
        KillSwitchVpnService.releaseImmediate()
        KillSwitchVpnService.release(App.instance)
        XrayCoreManager.stopLoop()
        try {
            val config = XrayConfigBuilder.build(node)
            AppLog.i("VpnManager.connect node=${node.name} protocol=${node.protocol}")
            AppLog.i("VpnManager config preview:\n${config.take(1200)}")
            App.configFile.writeText(config)
            BoxService.start()
        } catch (t: Throwable) {
            AppLog.e("VpnManager.connect failed", t)
            _lastError.value = t.message ?: "Ошибка подключения"
            setStatus(VpnStatus.Stopped)
            VpnAutoReconnect.onConnectFailed()
        }
    }

    fun disconnect(userInitiated: Boolean = true) {
        AppLog.i("VpnManager.disconnect userInitiated=$userInitiated")
        if (userInitiated) {
            markUserDisconnectRequested()
        } else {
            userInitiatedDisconnect = false
        }
        BoxService.stop()
    }

    /** Остановка из уведомления, отзыва VPN системой и т.п. — без автопереподключения. */
    internal fun markUserDisconnectRequested() {
        userInitiatedDisconnect = true
        VpnAutoReconnect.clear()
        KillSwitchVpnService.release(App.instance)
    }

    internal fun onVpnFullyStopped() {
        val killSwitch = ConnectionSettingsStore.state.killSwitchEnabled
        if (killSwitch && !userInitiatedDisconnect) {
            KillSwitchVpnService.engage(App.instance)
        }

        VpnAutoReconnect.onUnexpectedDisconnect()

        userInitiatedDisconnect = false
    }
}
