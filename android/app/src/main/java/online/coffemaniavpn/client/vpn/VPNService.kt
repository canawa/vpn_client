package online.coffemaniavpn.client.vpn

import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.Notification
import io.nekohasekai.libbox.TunOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import online.coffemaniavpn.client.util.AppLog
import online.coffemaniavpn.client.ktx.toIpPrefix
import online.coffemaniavpn.client.ktx.toList

class VPNService : VpnService(), PlatformInterfaceWrapper {
    private val service = BoxService(this, this)

    override fun onCreate() {
        super.onCreate()
        AppLog.i("VPNService.onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.i("VPNService.onStartCommand")
        return service.onStartCommand()
    }

    override fun onBind(intent: Intent): IBinder? {
        val binder = super.onBind(intent)
        return binder ?: service.onBind()
    }

    override fun onDestroy() {
        service.onDestroy()
        super.onDestroy()
    }

    override fun onRevoke() {
        runBlocking {
            withContext(Dispatchers.Main) {
                service.onRevoke()
            }
        }
    }

    override fun autoDetectInterfaceControl(fd: Int) {
        protect(fd)
    }

    override fun openTun(options: TunOptions): Int {
        if (prepare(this) != null) error("android: missing vpn permission")

        val builder = Builder()
            .setSession("КОФЕМАНИЯ ВПН")
            .setMtu(options.mtu)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        val inet4Address = options.inet4Address
        while (inet4Address.hasNext()) {
            val address = inet4Address.next()
            builder.addAddress(address.address(), address.prefix())
        }

        val inet6Address = options.inet6Address
        while (inet6Address.hasNext()) {
            val address = inet6Address.next()
            builder.addAddress(address.address(), address.prefix())
        }

        if (options.autoRoute) {
            if (options.dnsMode.value != Libbox.DNSModeDisabled) {
                val dnsServerAddress = options.dnsServerAddress
                while (dnsServerAddress.hasNext()) {
                    builder.addDnsServer(dnsServerAddress.next())
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val inet4RouteAddress = options.inet4RouteAddress
                if (inet4RouteAddress.hasNext()) {
                    while (inet4RouteAddress.hasNext()) {
                        builder.addRoute(inet4RouteAddress.next().toIpPrefix())
                    }
                } else if (options.inet4Address.hasNext()) {
                    builder.addRoute("0.0.0.0", 0)
                }

                val inet6RouteAddress = options.inet6RouteAddress
                if (inet6RouteAddress.hasNext()) {
                    while (inet6RouteAddress.hasNext()) {
                        builder.addRoute(inet6RouteAddress.next().toIpPrefix())
                    }
                } else if (options.inet6Address.hasNext()) {
                    builder.addRoute("::", 0)
                }
            } else {
                val inet4RouteAddress = options.inet4RouteRange
                if (inet4RouteAddress.hasNext()) {
                    while (inet4RouteAddress.hasNext()) {
                        val address = inet4RouteAddress.next()
                        builder.addRoute(address.address(), address.prefix())
                    }
                }

                val inet6RouteAddress = options.inet6RouteRange
                if (inet6RouteAddress.hasNext()) {
                    while (inet6RouteAddress.hasNext()) {
                        val address = inet6RouteAddress.next()
                        builder.addRoute(address.address(), address.prefix())
                    }
                }
            }

            val includePackage = options.includePackage
            while (includePackage.hasNext()) {
                try {
                    builder.addAllowedApplication(includePackage.next())
                } catch (e: NameNotFoundException) {
                    AppLog.e("addAllowedApplication failed", e)
                }
            }

            val excludePackage = options.excludePackage
            while (excludePackage.hasNext()) {
                try {
                    builder.addDisallowedApplication(excludePackage.next())
                } catch (e: NameNotFoundException) {
                    AppLog.e("addDisallowedApplication failed", e)
                }
            }
        }

        val pfd = builder.establish() ?: error("android: vpn establish failed")
        AppLog.i("VPNService openTun fd=${pfd.fd}")
        service.fileDescriptor = pfd
        return pfd.fd
    }

    override fun sendNotification(notification: Notification) = Unit
}
