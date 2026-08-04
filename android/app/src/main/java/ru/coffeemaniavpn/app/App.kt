package ru.coffeemaniavpn.app

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.coffeemaniavpn.app.data.AppLanguage
import ru.coffeemaniavpn.app.data.AppPreferences
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.VpnManager
import ru.coffeemaniavpn.app.vpn.XrayCoreManager
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class App : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AppLog.installCrashHandler(Thread.getDefaultUncaughtExceptionHandler())
        AppLog.i("Application.onCreate start, version=${BuildConfig.VERSION_NAME}")
        applyStoredLanguage()
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

    private fun applyStoredLanguage() {
        // Сразу RU, пока DataStore не ответит — иначе на EN-эмуляторе мелькают английские строки.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
        applicationScope.launch(Dispatchers.IO) {
            val language = runCatching {
                AppPreferences(this@App).appLanguage.first()
            }.getOrDefault(AppLanguage.DEFAULT)
            val tags = when (language) {
                AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
                AppLanguage.RU -> LocaleListCompat.forLanguageTags("ru")
                AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
            }
            launch(Dispatchers.Main) {
                AppCompatDelegate.setApplicationLocales(tags)
            }
        }
    }

    companion object {
        lateinit var instance: App
            private set

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
