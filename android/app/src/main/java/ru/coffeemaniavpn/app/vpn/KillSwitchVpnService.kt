package ru.coffeemaniavpn.app.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.util.AppLog

/**
 * Блокирует весь трафик, пока VPN не подключён снова (при включённом Kill Switch).
 */
class KillSwitchVpnService : VpnService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RELEASE -> {
                release()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ENGAGE -> {
                engage()
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    override fun onRevoke() {
        AppLog.w("KillSwitchVpnService revoked")
        release()
        super.onRevoke()
    }

    private fun engage() {
        if (prepare(this) != null) {
            AppLog.w("KillSwitch: нет разрешения VPN")
            stopSelf()
            return
        }
        release()
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
        if (blockingInterface != null) {
            AppLog.i("KillSwitch: трафик заблокирован")
        } else {
            AppLog.e("KillSwitch: не удалось установить блокирующий TUN")
            stopSelf()
        }
    }

    private fun release() {
        runCatching { blockingInterface?.close() }
        blockingInterface = null
    }

    companion object {
        private const val ACTION_ENGAGE = "ru.coffeemaniavpn.app.vpn.KILL_SWITCH_ENGAGE"
        private const val ACTION_RELEASE = "ru.coffeemaniavpn.app.vpn.KILL_SWITCH_RELEASE"

        @Volatile
        private var blockingInterface: ParcelFileDescriptor? = null

        val isActive: Boolean
            get() = blockingInterface != null

        fun engage(context: android.content.Context) {
            val intent = Intent(context, KillSwitchVpnService::class.java).apply {
                action = ACTION_ENGAGE
            }
            context.startService(intent)
        }

        fun release(context: android.content.Context) {
            val intent = Intent(context, KillSwitchVpnService::class.java).apply {
                action = ACTION_RELEASE
            }
            context.startService(intent)
        }
    }
}
