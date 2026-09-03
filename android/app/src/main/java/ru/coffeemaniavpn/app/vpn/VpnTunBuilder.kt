package ru.coffeemaniavpn.app.vpn

import android.net.VpnService
import android.os.Build
import android.content.pm.PackageManager.NameNotFoundException
import org.json.JSONObject
import ru.coffeemaniavpn.app.data.ConnectionSettingsStore
import ru.coffeemaniavpn.app.data.DnsMode
import ru.coffeemaniavpn.app.data.RoutingProfileStore
import ru.coffeemaniavpn.app.data.SplitTunnelAppsMode
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.util.AppLog

object VpnTunBuilder {
    private const val VPN_ADDRESS = "172.19.0.2"
    private const val VPN_PREFIX = 30
    private const val VPN_MTU = 1500

    fun establish(service: VpnService): android.os.ParcelFileDescriptor {
        if (VpnService.prepare(service) != null) error("android: missing vpn permission")

        val dns = resolveVpnDns()
        val builder = service.Builder()
            .setSession(service.getString(R.string.vpn_session_name))
            .setMtu(VPN_MTU)
            .addAddress(VPN_ADDRESS, VPN_PREFIX)
            .addRoute("0.0.0.0", 0)
            .addDnsServer(dns)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        applyPerAppSplit(builder, service.packageName)
        val pfd = builder.establish() ?: error("android: vpn establish failed")
        AppLog.i("VpnTunBuilder established fd=${pfd.fd} dns=$dns")
        return pfd
    }

    private fun resolveVpnDns(): String {
        if (ConnectionSettingsStore.state.dnsMode != DnsMode.Subscription) {
            return DnsMode.CLOUDFLARE_DNS
        }
        val raw = RoutingProfileStore.activeProfileJson ?: return DnsMode.CLOUDFLARE_DNS
        val remote = runCatching { JSONObject(raw).optString("RemoteDns").trim() }
            .getOrDefault("")
        return plainDnsHost(remote) ?: DnsMode.CLOUDFLARE_DNS
    }

    private fun plainDnsHost(value: String): String? {
        if (value.isBlank()) return null
        val cleaned = value
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("tls://")
            .removePrefix("udp://")
            .removePrefix("tcp://")
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore(':')
            .trim()
        if (cleaned.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) return cleaned
        return cleaned.takeIf { it.isNotBlank() && '.' in it && !it.contains(' ') }
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
                packages.filter { it != selfPackage }.forEach { pkg ->
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
