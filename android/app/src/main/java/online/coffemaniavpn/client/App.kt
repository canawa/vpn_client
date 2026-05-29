package online.coffemaniavpn.client

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.PowerManager
import androidx.core.content.getSystemService
import io.nekohasekai.libbox.Libbox
import io.nekohasekai.libbox.SetupOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import online.coffemaniavpn.client.util.AppLog
import java.io.File
import java.util.Locale
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

        setupLibboxLocale()

        val baseDir = filesDir.apply { mkdirs() }
        val workingDir = getExternalFilesDir(null)?.apply { mkdirs() }
        val tempDir = cacheDir.apply { mkdirs() }

        AppLog.i("dirs base=$baseDir working=$workingDir temp=$tempDir")

        if (workingDir != null) {
            applicationScope.launch(Dispatchers.IO) {
                setupLibbox(baseDir, workingDir, tempDir)
            }
        } else {
            AppLog.e("workingDir is null, libbox setup skipped")
        }
    }

    private fun setupLibboxLocale() {
        val candidates = listOf(
            Locale.getDefault().language.lowercase(Locale.ROOT),
            "en",
        ).distinct()
        for (locale in candidates) {
            try {
                Libbox.setLocale(locale)
                AppLog.i("Libbox.setLocale ok locale=$locale")
                return
            } catch (t: Throwable) {
                AppLog.w("Libbox.setLocale failed for $locale", t)
            }
        }
    }

    private fun setupLibbox(baseDir: File, workingDir: File, tempDir: File) {
        try {
            AppLog.i("Libbox.setup start")
            val options = SetupOptions().apply {
                basePath = baseDir.path
                workingPath = workingDir.path
                tempPath = tempDir.path
                fixAndroidStack = true
                logMaxLines = 3000
                debug = BuildConfig.DEBUG
            }
            Libbox.setup(options)
            libboxReady.set(true)
            AppLog.i("Libbox.setup ok")
        } catch (t: Throwable) {
            libboxReady.set(false)
            AppLog.e("Libbox.setup failed", t)
        }
    }

    companion object {
        lateinit var instance: App
            private set

        val libboxReady = AtomicBoolean(false)

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
