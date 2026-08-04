package ru.coffeemaniavpn.app.vpn

import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.util.AppLog
import java.net.Socket

class VPNService : VpnService() {
    private val service = BoxService(this)

    companion object {
        @Volatile
        private var instance: VPNService? = null

        /** Исключает сокет из VPN-туннеля (дополнительно к bindSocket на физической сети). */
        fun tryProtect(socket: Socket): Boolean {
            val vpn = instance ?: return false
            return runCatching {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    vpn.protect(ParcelFileDescriptor.fromSocket(socket).fd)
                } else {
                    val getFd = Socket::class.java.getDeclaredMethod("getFileDescriptor\$")
                    getFd.isAccessible = true
                    val fd = getFd.invoke(socket) as java.io.FileDescriptor
                    val getInt = java.io.FileDescriptor::class.java.getDeclaredMethod("getInt\$")
                    getInt.isAccessible = true
                    vpn.protect(getInt.invoke(fd) as Int)
                }
            }.getOrDefault(false)
        }
    }

    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkRequest by lazy {
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
    }

    private val connectivity by lazy { getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager }

    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                setUnderlyingNetworks(arrayOf(network))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                setUnderlyingNetworks(arrayOf(network))
            }

            override fun onLost(network: Network) {
                setUnderlyingNetworks(null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        AppLog.i("VPNService.onCreate")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                connectivity.requestNetwork(defaultNetworkRequest, defaultNetworkCallback)
            }.onFailure {
                AppLog.w("VPNService requestNetwork failed", it)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLog.i("VPNService.onStartCommand action=${intent?.action}")
        if (intent?.action == VpnAction.SERVICE_CLOSE) {
            service.requestStop()
            return START_NOT_STICKY
        }
        return service.onStartCommand()
    }

    override fun onBind(intent: Intent): IBinder? {
        val binder = super.onBind(intent)
        return binder ?: service.onBind()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { connectivity.unregisterNetworkCallback(defaultNetworkCallback) }
        }
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
}
