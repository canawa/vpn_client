package ru.coffeemaniavpn.app.vpn

import android.net.VpnService
import android.os.Build
import android.content.pm.PackageManager.NameNotFoundException
import ru.coffeemaniavpn.app.data.ConnectionSettingsStore
import ru.coffeemaniavpn.app.data.SplitTunnelAppsMode
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.util.AppLog

object VpnTunBuilder {
    private const val VPN_ADDRESS = "172.19.0.2"
    private const val VPN_PREFIX = 30
    private const val VPN_MTU = 1500
    private const val VPN_DNS = "8.8.8.8"

    fun establish(service: VpnService): android.os.ParcelFileDescriptor {
        if (VpnService.prepare(service) != null) error("android: missing vpn permission")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = service.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
                as android.net.ConnectivityManager
            cm.activeNetwork?.let { service.setUnderlyingNetworks(arrayOf(it)) }
        }

        val builder = service.Builder()
            .setSession(service.getString(R.string.vpn_session_name))
            .setMtu(VPN_MTU)
            .addAddress(VPN_ADDRESS, VPN_PREFIX)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)
            .addDnsServer(VPN_DNS)
            .addDnsServer("1.1.1.1")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        applyPerAppSplit(builder, service.packageName)
        val pfd = builder.establish() ?: error("android: vpn establish failed")
        AppLog.i("VpnTunBuilder established fd=${pfd.fd}")
        return pfd
    }

    private fun applyPerAppSplit(builder: VpnService.Builder, selfPackage: String) {
        val settings = ConnectionSettingsStore.state
        if (!settings.appsEnabled || settings.appPackages.isEmpty()) {
            builder.addDisallowedApplication(selfPackage)
            return
        }

        val packages = settings.appPackages.filter { pkg ->
            runCatching {
                ru.coffeemaniavpn.app.App.packageManager.getApplicationInfo(
                    pkg,
                    android.content.pm.PackageManager.GET_META_DATA,
                )
                true
            }.getOrElse { false }
        }

        if (packages.isEmpty()) {
            builder.addDisallowedApplication(selfPackage)
            return
        }

        when (settings.appsMode) {
            SplitTunnelAppsMode.IncludeOnly -> {
                val allowed = packages.filter { it != selfPackage }
                if (allowed.isEmpty()) {
                    // Нет приложений в whitelist — полный туннель (иначе трафик вообще не пойдёт в VPN).
                    builder.addDisallowedApplication(selfPackage)
                    AppLog.w("VpnTunBuilder IncludeOnly with empty allow-list → full tunnel")
                    return
                }
                allowed.forEach { pkg ->
                    try {
                        builder.addAllowedApplication(pkg)
                    } catch (e: NameNotFoundException) {
                        AppLog.e("addAllowedApplication failed pkg=$pkg", e)
                    }
                }
            }
            SplitTunnelAppsMode.ExcludeSelected -> {
                packages.toMutableSet().apply { add(selfPackage) }.forEach { pkg ->
                    try {
                        builder.addDisallowedApplication(pkg)
                    } catch (e: NameNotFoundException) {
                        AppLog.e("addDisallowedApplication failed pkg=$pkg", e)
                    }
                }
            }
        }
        AppLog.i("VpnTunBuilder per-app mode=${settings.appsMode} packages=${packages.size}")
    }
}
