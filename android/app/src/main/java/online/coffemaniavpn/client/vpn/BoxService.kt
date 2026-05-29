package online.coffemaniavpn.client.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import io.nekohasekai.libbox.CommandServer
import io.nekohasekai.libbox.CommandServerHandler
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.OverrideOptions
import io.nekohasekai.libbox.SystemProxyStatus
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.coffemaniavpn.client.util.AppLog
import online.coffemaniavpn.client.App

class BoxService(
    private val service: android.app.Service,
    private val platformInterface: io.nekohasekai.libbox.PlatformInterface,
) : CommandServerHandler {
    var fileDescriptor: ParcelFileDescriptor? = null

    private val notification = ServiceNotification(service)
    private lateinit var commandServer: CommandServer

    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == VpnAction.SERVICE_CLOSE) {
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
                waitForLibbox()
                AppLog.i("BoxService startCommandServer")
                startCommandServer()
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
        stopService()
    }

    private fun startCommandServer() {
        commandServer = CommandServer(this, platformInterface).also { it.start() }
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

        DefaultNetworkMonitor.start()

        try {
            commandServer.startOrReloadService(content, OverrideOptions())
        } catch (t: Throwable) {
            AppLog.e("startOrReloadService failed", t)
            stopServiceWithError(t.message ?: "Не удалось запустить sing-box")
            return
        }

        VpnManager.setStatus(VpnStatus.Started)
        AppLog.i("BoxService started")
        withContext(Dispatchers.Main) {
            notification.show(service.getString(online.coffemaniavpn.client.R.string.vpn_connected))
        }
    }

    override fun serviceStop() {
        notification.close()
        fileDescriptor?.close()
        fileDescriptor = null
        closeService()
    }

    override fun serviceReload() {
        GlobalScope.launch(Dispatchers.IO) {
            runCatching {
                val content = App.configFile.readText()
                commandServer.startOrReloadService(content, OverrideOptions())
            }
        }
    }

    override fun getSystemProxyStatus(): SystemProxyStatus? = null

    override fun setSystemProxyEnabled(isEnabled: Boolean) = Unit

    override fun writeDebugMessage(message: String?) {
        AppLog.i("sing-box: ${message.orEmpty()}")
    }

    override fun triggerNativeCrash() = Unit

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
            fileDescriptor?.close()
            fileDescriptor = null
            DefaultNetworkMonitor.stop()
            runCatching { commandServer.closeService() }
            runCatching { commandServer.close() }
            withContext(Dispatchers.Main) {
                VpnManager.setStatus(VpnStatus.Stopped)
                service.stopSelf()
            }
        }
    }

    private suspend fun stopServiceWithError(message: String) {
        DefaultNetworkMonitor.stop()
        if (::commandServer.isInitialized) {
            runCatching { commandServer.closeService() }
            runCatching { commandServer.close() }
        }
        withContext(Dispatchers.Main) {
            if (receiverRegistered) {
                service.unregisterReceiver(receiver)
                receiverRegistered = false
            }
            notification.close()
            VpnManager.setError(message)
            VpnManager.setStatus(VpnStatus.Stopped)
            service.stopSelf()
        }
    }

    private fun closeService() {
        runCatching { commandServer.closeService() }
    }

    private fun waitForLibbox() {
        repeat(50) {
            if (App.libboxReady.get()) return
            Thread.sleep(100)
        }
        if (!App.libboxReady.get()) {
            error("libbox не инициализирован")
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
