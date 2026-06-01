package online.coffemaniavpn.client.vpn

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import online.coffemaniavpn.client.App
import online.coffemaniavpn.client.data.ConnectionSettingsStore
import online.coffemaniavpn.client.data.ProxyNode
import online.coffemaniavpn.client.data.SingBoxConfigBuilder
import online.coffemaniavpn.client.util.AppLog

object VpnManager {
    @Volatile
    var userInitiatedDisconnect: Boolean = false

    private val _status = MutableStateFlow(VpnStatus.Stopped)
    val status = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    private val _connectionElapsedMs = MutableStateFlow(0L)
    val connectionElapsedMs = _connectionElapsedMs.asStateFlow()

    private var connectedSinceMs: Long? = null
    private var elapsedTickerJob: Job? = null

    internal fun setStatus(value: VpnStatus) {
        when (value) {
            VpnStatus.Started -> {
                if (connectedSinceMs == null) {
                    connectedSinceMs = System.currentTimeMillis()
                }
                startElapsedTicker()
            }
            VpnStatus.Stopped -> {
                connectedSinceMs = null
                _connectionElapsedMs.value = 0L
                stopElapsedTicker()
            }
            else -> Unit
        }
        _status.value = value
    }

    private fun startElapsedTicker() {
        stopElapsedTicker()
        elapsedTickerJob = App.applicationScope.launch {
            while (connectedSinceMs != null) {
                val since = connectedSinceMs ?: break
                _connectionElapsedMs.value = System.currentTimeMillis() - since
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
        _lastError.value = null
        KillSwitchVpnService.release(App.instance)
        userInitiatedDisconnect = false
        try {
            val config = SingBoxConfigBuilder.build(node)
            AppLog.i("VpnManager.connect node=${node.name} protocol=${node.protocol}")
            AppLog.i("VpnManager config preview:\n${config.take(1200)}")
            App.configFile.writeText(config)
            BoxService.start()
        } catch (t: Throwable) {
            AppLog.e("VpnManager.connect failed", t)
            _lastError.value = t.message ?: "Ошибка подключения"
        }
    }

    fun disconnect(userInitiated: Boolean = true) {
        AppLog.i("VpnManager.disconnect userInitiated=$userInitiated")
        userInitiatedDisconnect = userInitiated
        if (userInitiated) {
            KillSwitchVpnService.release(App.instance)
        }
        BoxService.stop()
    }

    internal fun onVpnFullyStopped() {
        val killSwitch = ConnectionSettingsStore.state.killSwitchEnabled
        if (killSwitch && !userInitiatedDisconnect) {
            KillSwitchVpnService.engage(App.instance)
        }
        userInitiatedDisconnect = false
    }
}
