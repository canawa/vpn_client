package ru.coffeemaniavpn.app

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.core.content.getSystemService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import ru.coffeemaniavpn.app.data.AppLanguage
import ru.coffeemaniavpn.app.data.AppPreferences
import ru.coffeemaniavpn.app.util.AppLocale
import ru.coffeemaniavpn.app.data.PingNetworkBypass
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.VpnManager
import ru.coffeemaniavpn.app.vpn.XrayCoreManager
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base?.let { AppLocale.wrap(it) } ?: base)
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AppLog.installCrashHandler(Thread.getDefaultUncaughtExceptionHandler())
        AppLog.i("Application.onCreate start, version=${BuildConfig.VERSION_NAME}")
        applyStoredLanguage()
        VpnManager.init()
        PingNetworkBypass.ensureListening(this)

        applicationScope.launch(Dispatchers.IO) {
            runCatching {
                val prefs = AppPreferences(this@App)
                prefs.loadTrafficRoutingModeIntoMemory()
                prefs.loadConnectionSettingsIntoMemory()
                AppLog.i("App: connection settings loaded into memory")
            }.onFailure {
                AppLog.e("App: failed to load connection settings", it)
            }
            settingsReady.complete(Unit)

            runCatching {
                XrayCoreManager.init(this@App)
                xrayReady.set(true)
            }.onFailure {
                xrayReady.set(false)
                AppLog.e("Xray init failed", it)
            }
        }
    }

    private fun applyStoredLanguage() {
        val language = runBlocking(Dispatchers.IO) {
            runCatching {
                AppPreferences(this@App).appLanguage.first()
            }.getOrDefault(AppLanguage.DEFAULT)
        }
        AppLocale.apply(language)
    }

    companion object {
        lateinit var instance: App
            private set

        val xrayReady = AtomicBoolean(false)

        private val settingsReady = CompletableDeferred<Unit>()

        val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        /** Waits until ConnectionSettingsStore / TrafficRoutingStore are loaded (tile/widget-safe). */
        suspend fun awaitSettingsReady(timeoutMs: Long = 5_000L) {
            if (settingsReady.isCompleted) return
            withTimeoutOrNull(timeoutMs) { settingsReady.await() }
        }

        val notificationManager by lazy { instance.getSystemService<NotificationManager>()!! }
        val connectivity by lazy { instance.getSystemService<ConnectivityManager>()!! }
        val packageManager by lazy { instance.packageManager }
        val powerManager by lazy { instance.getSystemService<PowerManager>()!! }
        val wifiManager by lazy { instance.getSystemService<WifiManager>()!! }

        val configFile: File
            get() = File(instance.filesDir, "config.json")
    }
}
