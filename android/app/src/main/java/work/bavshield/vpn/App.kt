package work.bavshield.vpn

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import work.bavshield.vpn.util.AppLog
import work.bavshield.vpn.vpn.VpnManager
import work.bavshield.vpn.vpn.XrayCoreManager
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let(work.bavshield.vpn.data.LocaleHelper::wrap))
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AppLog.installCrashHandler(Thread.getDefaultUncaughtExceptionHandler())
        AppLog.i("Application.onCreate start, version=${BuildConfig.VERSION_NAME}")
        VpnManager.init()

        applicationScope.launch(Dispatchers.IO) {
            runCatching {
                XrayCoreManager.init(this@App)
                xrayReady.set(true)
            }.onFailure {
                xrayReady.set(false)
                AppLog.e("Xray init failed", it)
            }
        }
    }

    companion object {
        lateinit var instance: App
            private set

        fun instanceOrNull(): App? = if (::instance.isInitialized) instance else null

        val xrayReady = AtomicBoolean(false)

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        val notificationManager by lazy { instance.getSystemService<NotificationManager>()!! }
        val connectivity by lazy { instance.getSystemService<ConnectivityManager>()!! }
        val packageManager by lazy { instance.packageManager }
        val powerManager by lazy { instance.getSystemService<PowerManager>()!! }
        val wifiManager by lazy { instance.getSystemService<WifiManager>()!! }

        val configFile: File
            get() = File(instance.filesDir, "config.json")
    }
}
