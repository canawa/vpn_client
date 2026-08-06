package ru.coffeemaniavpn.app.vpn

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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.data.formatTrafficSpeedLine
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.VpnDiagnostics

class BoxService(
    private val service: android.app.Service,
) {
    var fileDescriptor: ParcelFileDescriptor? = null

    private val notification = ServiceNotification(service)

    private var watchdogJob: Job? = null
    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == VpnAction.SERVICE_CLOSE) {
                val userInitiated = intent.getBooleanExtra(VpnAction.EXTRA_USER_INITIATED, true)
                requestStop(userInitiated)
            }
        }
    }

    /** Остановка из уведомления / VpnManager.disconnect / broadcast. */
    fun requestStop(userInitiated: Boolean = true) {
        if (userInitiated) {
            VpnManager.markUserDisconnectRequested()
        } else {
            VpnManager.userInitiatedDisconnect = false
        }
        val status = VpnManager.status.value
        if (status == VpnStatus.Started || status == VpnStatus.Starting) {
            stopService()
        } else {
            activeNotification = null
            notification.close()
            service.stopSelf()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Suppress("SameReturnValue")
    internal fun onStartCommand(): Int {
        when (val status = VpnManager.status.value) {
            VpnStatus.Started -> {
                if (XrayCoreManager.isRunning()) {
                    AppLog.i("BoxService: already connected")
                    return android.app.Service.START_NOT_STICKY
                }
                AppLog.w("BoxService: status Started but core dead, restarting")
            }
            VpnStatus.Starting, VpnStatus.Stopping -> {
                AppLog.w("BoxService: stale status=$status, restarting")
            }
            VpnStatus.Stopped -> Unit
        }
        val needsReset = VpnManager.status.value != VpnStatus.Stopped
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
                if (needsReset) resetForRestart()
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
        cancelWatchdog()
        notification.close()
        val status = VpnManager.status.value
        if (status == VpnStatus.Starting || status == VpnStatus.Started) {
            AppLog.w("BoxService.onDestroy with status=$status, resetting")
            XrayCoreManager.stopLoop()
            fileDescriptor?.close()
            fileDescriptor = null
            VpnManager.setStatus(VpnStatus.Stopped)
            VpnManager.onVpnFullyStopped()
        }
    }

    internal fun onRevoke() {
        AppLog.w("BoxService.onRevoke (system revoked VPN)")
        requestStop(userInitiated = false)
    }

    private suspend fun startService() {
        withContext(Dispatchers.Main) {
            notification.show(connectedNotificationText())
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

        AppLog.i("BoxService TUN ready fd=${pfd.fd}, starting xray loop")
        try {
            XrayCoreManager.startLoop(content, pfd.fd)
        } catch (t: Throwable) {
            AppLog.e("Xray startLoop failed", t)
            if (VpnManager.status.value != VpnStatus.Stopping) {
                stopServiceWithError(t.message ?: "Не удалось запустить Xray")
            }
            return
        }

        VpnManager.setStatus(VpnStatus.Started)
        AppLog.i("BoxService started xrayRunning=${XrayCoreManager.isRunning()}")
        scheduleProxyHealthCheck()
        startConnectionWatchdog()
        withContext(Dispatchers.Main) {
            activeNotification = notification
            notification.show(connectedNotificationText(connected = true))
        }
    }

    private fun resetForRestart() {
        XrayCoreManager.stopLoop()
        KillSwitchVpnService.releaseImmediate()
        runCatching { fileDescriptor?.close() }
        fileDescriptor = null
        VpnManager.setStatus(VpnStatus.Stopped)
        Thread.sleep(200)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun stopService() {
        cancelWatchdog()
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
        activeNotification = null
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
        VpnDiagnostics.snapshot("stop-error: $message")
        XrayCoreManager.stopLoop()
        fileDescriptor?.close()
        fileDescriptor = null
        withContext(Dispatchers.Main) {
            if (receiverRegistered) {
                service.unregisterReceiver(receiver)
                receiverRegistered = false
            }
            activeNotification = null
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

    @OptIn(DelicateCoroutinesApi::class)
    private fun scheduleProxyHealthCheck() {
        GlobalScope.launch(Dispatchers.IO) {
            delay(2_500)
            if (VpnManager.status.value != VpnStatus.Started) return@launch
            val delayMs = XrayCoreManager.measureDelayAny()
            if (delayMs != null && delayMs > 0) {
                AppLog.i("BoxService proxy health ok delayMs=$delayMs")
            } else {
                AppLog.w("BoxService proxy health failed (no response through proxy)")
                VpnDiagnostics.snapshot("proxy-health-failed")
            }
        }
    }

    /** Периодически проверяет ядро и прокси; при сбоях — авто-переподключение. */
    @OptIn(DelicateCoroutinesApi::class)
    private fun startConnectionWatchdog() {
        cancelWatchdog()
        watchdogJob = GlobalScope.launch(Dispatchers.IO) {
            var failures = 0
            delay(WATCHDOG_INITIAL_DELAY_MS)
            while (isActive && VpnManager.status.value == VpnStatus.Started) {
                val healthy = when {
                    !XrayCoreManager.isRunning() -> {
                        AppLog.w("BoxService watchdog: xray core not running")
                        false
                    }
                    else -> {
                        val delayMs = XrayCoreManager.measureDelayAny()
                        if (delayMs != null && delayMs > 0) {
                            true
                        } else {
                            AppLog.w("BoxService watchdog: proxy health failed")
                            false
                        }
                    }
                }
                if (healthy) {
                    failures = 0
                } else {
                    failures++
                    VpnDiagnostics.snapshot("watchdog-failure-$failures")
                    if (failures >= WATCHDOG_FAILURE_THRESHOLD) {
                        AppLog.w("BoxService watchdog: reconnecting after $failures failures")
                        withContext(Dispatchers.Main) {
                            VpnManager.disconnect(userInitiated = false)
                        }
                        break
                    }
                }
                delay(WATCHDOG_INTERVAL_MS)
            }
        }
    }

    private fun cancelWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = null
    }

    companion object {
        private const val WATCHDOG_INITIAL_DELAY_MS = 45_000L
        private const val WATCHDOG_INTERVAL_MS = 45_000L
        private const val WATCHDOG_FAILURE_THRESHOLD = 2

        @Volatile
        private var activeNotification: ServiceNotification? = null

        fun start() {
            AppLog.i("BoxService.start requested")
            val intent = Intent(App.instance, VPNService::class.java)
            ContextCompat.startForegroundService(App.instance, intent)
        }

        fun stop(userInitiated: Boolean = true) {
            val intent = Intent(App.instance, VPNService::class.java).apply {
                action = VpnAction.SERVICE_CLOSE
                putExtra(VpnAction.EXTRA_USER_INITIATED, userInitiated)
            }
            runCatching {
                App.instance.startService(intent)
            }.onFailure {
                AppLog.w("BoxService.stop startService failed, fallback broadcast", it)
                App.instance.sendBroadcast(
                    Intent(VpnAction.SERVICE_CLOSE)
                        .setPackage(App.instance.packageName)
                        .putExtra(VpnAction.EXTRA_USER_INITIATED, userInitiated),
                )
            }
        }

        fun updateTrafficNotification(rates: VpnTrafficRates) {
            val notification = activeNotification ?: return
            val speed = formatTrafficSpeedLine(
                downlinkBytesPerSec = rates.downlinkBytesPerSec,
                uplinkBytesPerSec = rates.uplinkBytesPerSec,
            )
            val (serverLine, statusLine) = connectedNotificationLines(
                connected = true,
                speed = speed,
            )
            notification.show(serverLine = serverLine, statusLine = statusLine)
        }

        private fun connectedNotificationText(
            connected: Boolean = false,
            speed: String? = null,
        ): String {
            val (serverLine, statusLine) = connectedNotificationLines(connected, speed)
            return if (serverLine.isNotBlank()) {
                "$serverLine\n$statusLine"
            } else {
                statusLine
            }
        }

        private fun connectedNotificationLines(
            connected: Boolean = false,
            speed: String? = null,
        ): Pair<String, String> {
            val status = App.instance.getString(
                if (connected) {
                    ru.coffeemaniavpn.app.R.string.vpn_connected
                } else {
                    ru.coffeemaniavpn.app.R.string.vpn_starting
                },
            )
            val statusLine = buildList {
                add(status)
                if (!speed.isNullOrBlank()) add(speed)
            }.joinToString(" · ")

            val node = VpnAutoReconnect.connectedNode() ?: return "" to statusLine
            val display = ru.coffeemaniavpn.app.ui.ServerDisplayMapper.map(node)
            val serverLine = listOf(display.flag, display.title)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .ifBlank { node.name.trim() }
            return serverLine to statusLine
        }
    }
}
