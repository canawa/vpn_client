package work.bavshield.vpn.vpn

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import work.bavshield.vpn.util.AppLog

class VPNService : VpnService() {
    private val service = BoxService(this)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(work.bavshield.vpn.data.LocaleHelper.wrap(newBase))
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
        AppLog.i("VPNService.onStartCommand")
        return service.onStartCommand()
    }

    override fun onBind(intent: Intent): IBinder? {
        val binder = super.onBind(intent)
        return binder ?: service.onBind()
    }

    override fun onDestroy() {
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
