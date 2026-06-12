package online.coffemaniavpn.client.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.coffemaniavpn.client.App
import online.coffemaniavpn.client.util.AppLog

class BoxService(
    private val service: android.app.Service,
) {
    var fileDescriptor: ParcelFileDescriptor? = null

    private val notification = ServiceNotification(service)

    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == VpnAction.SERVICE_CLOSE) {
                VpnManager.markUserDisconnectRequested()
                stopService()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("SameReturnValue")
    internal fun onStartCommand(): Int {
        if (VpnManager.status.value != VpnStatus.Stopped) {
            return android.app.Service.START_NOT_STICKY
        }
        VpnManager.setStatus(VpnStatus.Starting)

        if (!receiverRegistered) {
            ContextCompat.registerReceiver(
                service,
                receiver,
                IntentFilter(VpnAction.SERVICE_CLOSE),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        }

        GlobalScope.launch(Dispatchers.IO) {
            try {
                waitForXray()
                startService()
            } catch (t: Throwable) {
                AppLog.e("BoxService start failed", t)
                stopServiceWithError(t.message ?: "Ошибка запуска VPN")
            }
        }
        return android.app.Service.START_NOT_STICKY
    }

    internal fun onBind(): IBinder? = null

    internal fun onDestroy() {
        notification.close()
    }

    internal fun onRevoke() {
        VpnManager.markUserDisconnectRequested()
        stopService()
    }

    private suspend fun startService() {
        withContext(Dispatchers.Main) {
            notification.show(service.getString(online.coffemaniavpn.client.R.string.vpn_starting))
        }

        val configFile = App.configFile
        if (!configFile.exists()) {
            stopServiceWithError("Конфиг не найден")
            return
        }

        val content = configFile.readText()
        AppLog.i("BoxService config size=${content.length}")
        if (content.isBlank()) {
            stopServiceWithError("Пустой конфиг")
            return
        }

        XrayGeoAssets.ensureInstalled(service)

        val vpnService = service as? VPNService
            ?: error("BoxService requires VPNService")
        val pfd = VpnTunBuilder.establish(vpnService)
        fileDescriptor = pfd

        try {
            XrayCoreManager.startLoop(content, pfd.fd)
        } catch (t: Throwable) {
            AppLog.e("Xray startLoop failed", t)
            stopServiceWithError(t.message ?: "Не удалось запустить Xray")
            return
        }

        VpnManager.setStatus(VpnStatus.Started)
        AppLog.i("BoxService started xray=${XrayCoreManager.isRunning()}")
        withContext(Dispatchers.Main) {
            notification.show(service.getString(online.coffemaniavpn.client.R.string.vpn_connected))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun stopService() {
        if (VpnManager.status.value != VpnStatus.Started &&
            VpnManager.status.value != VpnStatus.Starting
        ) {
            return
        }
        VpnManager.setStatus(VpnStatus.Stopping)

        if (receiverRegistered) {
            service.unregisterReceiver(receiver)
            receiverRegistered = false
        }
        notification.close()

        GlobalScope.launch(Dispatchers.IO) {
            XrayCoreManager.stopLoop()
            fileDescriptor?.close()
            fileDescriptor = null
            withContext(Dispatchers.Main) {
                VpnManager.setStatus(VpnStatus.Stopped)
                VpnManager.onVpnFullyStopped()
                service.stopSelf()
            }
        }
    }

    private suspend fun stopServiceWithError(message: String) {
        XrayCoreManager.stopLoop()
        fileDescriptor?.close()
        fileDescriptor = null
        withContext(Dispatchers.Main) {
            if (receiverRegistered) {
                service.unregisterReceiver(receiver)
                receiverRegistered = false
            }
            notification.close()
            VpnManager.setError(message)
            VpnManager.setStatus(VpnStatus.Stopped)
            VpnManager.onVpnFullyStopped()
            service.stopSelf()
        }
    }

    private fun waitForXray() {
        repeat(50) {
            if (App.xrayReady.get()) return
            Thread.sleep(100)
        }
        if (!App.xrayReady.get()) {
            error("Xray не инициализирован")
        }
    }

    companion object {
        fun start() {
            AppLog.i("BoxService.start requested")
            val intent = Intent(App.instance, VPNService::class.java)
            ContextCompat.startForegroundService(App.instance, intent)
        }

        fun stop() {
            App.instance.sendBroadcast(
                Intent(VpnAction.SERVICE_CLOSE).setPackage(App.instance.packageName),
            )
        }
    }
}
