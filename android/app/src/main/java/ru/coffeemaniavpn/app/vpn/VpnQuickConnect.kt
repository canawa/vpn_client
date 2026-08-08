package ru.coffeemaniavpn.app.vpn

import android.content.Context
import android.net.VpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.data.AppPreferences
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.util.AppLog

sealed class VpnQuickConnectResult {
    data class Connect(val node: ProxyNode) : VpnQuickConnectResult()
    data object Disconnect : VpnQuickConnectResult()
    data object NeedsVpnPermission : VpnQuickConnectResult()
    data object OpenApp : VpnQuickConnectResult()
}

object VpnQuickConnect {
    suspend fun resolveToggle(context: Context): VpnQuickConnectResult = withContext(Dispatchers.IO) {
        when (VpnManager.status.value) {
            VpnStatus.Started, VpnStatus.Starting -> VpnQuickConnectResult.Disconnect
            VpnStatus.Stopping -> VpnQuickConnectResult.OpenApp
            VpnStatus.Stopped -> resolveConnect(context)
        }
    }

    suspend fun resolveConnect(context: Context): VpnQuickConnectResult = withContext(Dispatchers.IO) {
        val node = loadSelectedNode(context) ?: return@withContext VpnQuickConnectResult.OpenApp
        if (VpnService.prepare(context) != null) {
            VpnQuickConnectResult.NeedsVpnPermission
        } else {
            VpnQuickConnectResult.Connect(node)
        }
    }

    suspend fun loadSelectedNode(context: Context): ProxyNode? {
        val preferences = AppPreferences(context.applicationContext)
        val url = preferences.subscriptionUrl.first()
        val nodes = preferences.nodes.first()
        val selectedId = preferences.selectedNodeId.first()
        val info = preferences.subscriptionInfo.first()

        if (url.isBlank() || nodes.isEmpty()) {
            AppLog.w("VpnQuickConnect: no subscription or nodes")
            return null
        }
        if (info?.isExpired() == true) {
            AppLog.w("VpnQuickConnect: subscription expired")
            return null
        }

        return nodes.find { it.id == selectedId } ?: nodes.firstOrNull()
    }
}
