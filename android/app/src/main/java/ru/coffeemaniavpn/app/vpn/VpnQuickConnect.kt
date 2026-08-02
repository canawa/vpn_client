package ru.coffeemaniavpn.app.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.MainActivity
import ru.coffeemaniavpn.app.data.AppPreferences
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.util.AppLog

/** Подключение VPN из Quick Settings без ViewModel. */
object VpnQuickConnect {
    const val ACTION_CONNECT = "ru.coffeemaniavpn.app.action.QUICK_TILE_CONNECT"

    fun toggleFromTile(context: Context) {
        when (VpnManager.status.value) {
            VpnStatus.Started -> {
                AppLog.i("VpnQuickConnect tile disconnect")
                VpnManager.disconnect(userInitiated = true)
            }
            VpnStatus.Starting, VpnStatus.Stopping -> Unit
            VpnStatus.Stopped -> requestConnect(context)
        }
    }

    fun requestConnect(context: Context) {
        App.applicationScope.launch {
            val node = withContext(Dispatchers.IO) {
                resolveSelectedNode(context)
            }
            if (node == null) {
                AppLog.w("VpnQuickConnect: no node / expired, opening app")
                openApp(context, requestConnect = false)
                return@launch
            }

            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                AppLog.i("VpnQuickConnect: need VPN permission, opening app")
                openApp(context, requestConnect = true)
                return@launch
            }

            AppLog.i("VpnQuickConnect connect node=${node.name}")
            VpnManager.connect(node)
        }
    }

    suspend fun resolveSelectedNode(context: Context): ProxyNode? {
        val preferences = AppPreferences(context.applicationContext)
        val nodes = preferences.nodes.first()
        if (nodes.isEmpty()) return null
        val info = preferences.subscriptionInfo.first()
        if (info?.isExpired() == true) return null
        val selectedId = preferences.selectedNodeId.first()
        return nodes.find { it.id == selectedId } ?: nodes.firstOrNull()
    }

    private fun openApp(context: Context, requestConnect: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            if (requestConnect) {
                action = ACTION_CONNECT
            }
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )
        }
        context.startActivity(intent)
    }
}
