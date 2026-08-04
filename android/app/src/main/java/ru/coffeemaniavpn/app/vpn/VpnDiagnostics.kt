package ru.coffeemaniavpn.app.vpn

import android.os.Build
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.data.ConnectionSettingsStore
import ru.coffeemaniavpn.app.data.TrafficRoutingStore
import ru.coffeemaniavpn.app.util.AppLog

object VpnDiagnostics {
    fun snapshot(tag: String) {
        val settings = ConnectionSettingsStore.state
        val node = VpnAutoReconnect.connectedNode()
        AppLog.i(
            "diag[$tag] status=${VpnManager.status.value} " +
                "xrayRunning=${XrayCoreManager.isRunning()} " +
                "xrayReady=${App.xrayReady.get()} " +
                "killSwitchActive=${KillSwitchVpnService.isActive}",
        )
        AppLog.i(
            "diag[$tag] routing=${TrafficRoutingStore.mode} " +
                "appsEnabled=${settings.appsEnabled} appsMode=${settings.appsMode} " +
                "appCount=${settings.appPackages.size} " +
                "killSwitch=${settings.killSwitchEnabled} " +
                "customRules=${settings.customRules.count { it.isEnabled }}",
        )
        AppLog.i(
            "diag[$tag] node=${node?.name ?: "—"} " +
                "protocol=${node?.protocol ?: "—"} " +
                "host=${node?.host ?: "—"}:${node?.port ?: "—"} " +
                "vpnError=${VpnManager.lastError.value ?: "—"}",
        )
        AppLog.i(
            "diag[$tag] device=${Build.MANUFACTURER} ${Build.MODEL} " +
                "sdk=${Build.VERSION.SDK_INT} release=${Build.VERSION.RELEASE}",
        )
    }
}
