package online.coffemaniavpn.client.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import online.coffemaniavpn.client.App
import online.coffemaniavpn.client.data.ProxyNode
import online.coffemaniavpn.client.data.SingBoxConfigBuilder
import online.coffemaniavpn.client.util.AppLog

object VpnManager {
    private val _status = MutableStateFlow(VpnStatus.Stopped)
    val status = _status.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError = _lastError.asStateFlow()

    internal fun setStatus(value: VpnStatus) {
        _status.value = value
    }

    internal fun setError(message: String?) {
        _lastError.value = message
    }

    fun connect(node: ProxyNode) {
        _lastError.value = null
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

    fun disconnect() {
        AppLog.i("VpnManager.disconnect")
        BoxService.stop()
    }
}
