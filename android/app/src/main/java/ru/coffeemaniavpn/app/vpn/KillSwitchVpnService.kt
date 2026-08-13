package ru.coffeemaniavpn.app.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ru.coffeemaniavpn.app.MainActivity
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.util.AppLog

/**
 * Блокирует весь трафик, пока VPN не подключён снова (при включённом Kill Switch).
 * Запускается как foreground service — иначе Android 12+ блокирует startService из фона.
 */
class KillSwitchVpnService : VpnService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RELEASE -> {
                teardown(stopService = true)
                return START_NOT_STICKY
            }
            ACTION_ENGAGE -> {
                // startForeground must run quickly after startForegroundService.
                startKillSwitchForeground()
                if (!engageBlockingTun()) {
                    teardown(stopService = true)
                    return START_NOT_STICKY
                }
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        teardown(stopService = false)
        super.onDestroy()
    }

    override fun onRevoke() {
        AppLog.w("KillSwitchVpnService revoked")
        teardown(stopService = false)
        super.onRevoke()
    }

    private fun startKillSwitchForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.kill_switch_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            flags,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_notif)
            .setContentTitle(getString(R.string.kill_switch_notification_title))
            .setContentText(getString(R.string.kill_switch_notification_text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openApp)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun engageBlockingTun(): Boolean {
        if (prepare(this) != null) {
            AppLog.w("KillSwitch: нет разрешения VPN")
            return false
        }
        closeTun()
        val builder = Builder()
            .setSession(getString(R.string.vpn_session_name))
            .setMtu(1500)
            .addAddress("172.31.0.1", 32)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
            builder.setBlocking(true)
        }
        blockingInterface = builder.establish()
        return if (blockingInterface != null) {
            AppLog.i("KillSwitch: трафик заблокирован")
            true
        } else {
            AppLog.e("KillSwitch: не удалось установить блокирующий TUN")
            false
        }
    }

    private fun teardown(stopService: Boolean) {
        closeTun()
        runCatching {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        if (stopService) {
            stopSelf()
        }
    }

    private fun closeTun() {
        runCatching { blockingInterface?.close() }
        blockingInterface = null
    }

    companion object {
        private const val ACTION_ENGAGE = "ru.coffeemaniavpn.app.vpn.KILL_SWITCH_ENGAGE"
        private const val ACTION_RELEASE = "ru.coffeemaniavpn.app.vpn.KILL_SWITCH_RELEASE"
        private const val CHANNEL_ID = "coffemania_kill_switch"
        private const val NOTIFICATION_ID = 2

        @Volatile
        private var blockingInterface: ParcelFileDescriptor? = null

        val isActive: Boolean
            get() = blockingInterface != null

        fun engage(context: android.content.Context) {
            val intent = Intent(context, KillSwitchVpnService::class.java).apply {
                action = ACTION_ENGAGE
            }
            runCatching {
                ContextCompat.startForegroundService(context, intent)
            }.onFailure {
                AppLog.w("KillSwitch engage startForegroundService failed: ${it.message}")
                // Fallback for odd OEM / older paths.
                runCatching { context.startService(intent) }
                    .onFailure { e -> AppLog.w("KillSwitch engage startService failed: ${e.message}") }
            }
        }

        fun release(context: android.content.Context) {
            // TUN закрываем сразу: иначе основной VPN не займёт слот.
            releaseImmediate()
            runCatching {
                context.stopService(Intent(context, KillSwitchVpnService::class.java))
            }.onFailure {
                AppLog.w("KillSwitch stopService failed: ${it.message}")
            }
        }

        /** Закрывает TUN до запуска основного VPN (Android — один VPN-слот). */
        fun releaseImmediate() {
            runCatching { blockingInterface?.close() }
            blockingInterface = null
        }
    }
}
