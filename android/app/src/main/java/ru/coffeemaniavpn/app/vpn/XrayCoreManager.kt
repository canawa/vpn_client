package ru.coffeemaniavpn.app.vpn

import android.content.Context
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.util.AppLog
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

object XrayCoreManager {
    private val initialized = AtomicBoolean(false)
    private var coreController: CoreController? = null
    private val measureExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "xray-measure-delay").apply { isDaemon = true }
    }

    private val HEALTH_CHECK_URLS = listOf(
        "https://www.gstatic.com/generate_204",
        "https://cp.cloudflare.com/generate_204",
        "https://connectivitycheck.gstatic.com/generate_204",
    )
    private const val MEASURE_TIMEOUT_MS = 6_000L

    fun init(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        try {
            Seq.setContext(context.applicationContext)
            val assetPath = XrayGeoAssets.ensureInstalled(context).absolutePath
            Libv2ray.initCoreEnv(assetPath, "")
            AppLog.i("XrayCoreManager init ok version=${Libv2ray.checkVersionX()} assets=$assetPath")
        } catch (t: Throwable) {
            initialized.set(false)
            AppLog.e("XrayCoreManager init failed", t)
            throw t
        }
    }

    fun ensureController(callback: CoreCallbackHandler): CoreController {
        val existing = coreController
        if (existing != null) return existing
        return Libv2ray.newCoreController(callback).also { coreController = it }
    }

    fun isRunning(): Boolean = coreController?.isRunning == true

    /**
     * Байты uplink/downlink с прошлого вызова (счётчики сбрасываются).
     * Только outbound `proxy` — иначе «трафик» растёт и при прямом обходе.
     */
    fun queryTrafficDelta(): Pair<Long, Long> {
        val controller = coreController ?: return 0L to 0L
        if (!controller.isRunning) return 0L to 0L
        return runCatching {
            val uplink = controller.queryStats("proxy", "uplink").coerceAtLeast(0L)
            val downlink = controller.queryStats("proxy", "downlink").coerceAtLeast(0L)
            uplink to downlink
        }.getOrElse {
            AppLog.e("queryTrafficDelta failed", it)
            0L to 0L
        }
    }

    fun startLoop(config: String, tunFd: Int) {
        val controller = ensureController(XrayCoreCallback)
        if (controller.isRunning) {
            AppLog.w("XrayCoreManager: stopLoop before restart")
            runCatching { controller.stopLoop() }
        }
        controller.startLoop(config, tunFd)
        if (!controller.isRunning) {
            error("Xray core failed to start")
        }
    }

    fun stopLoop() {
        runCatching { coreController?.stopLoop() }
    }

    /** Задержка до URL через текущий прокси-outbound (мс), для проверки после подключения. */
    fun measureDelay(
        url: String = HEALTH_CHECK_URLS.first(),
        timeoutMs: Long = MEASURE_TIMEOUT_MS,
    ): Long? {
        val controller = coreController ?: return null
        if (!controller.isRunning) return null
        val future = measureExecutor.submit(
            Callable {
                runCatching { controller.measureDelay(url) }
                    .onFailure { AppLog.w("measureDelay failed url=$url", it) }
                    .getOrNull()
            },
        )
        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)?.takeIf { it > 0L }
        } catch (_: TimeoutException) {
            future.cancel(true)
            AppLog.w("measureDelay timeout ${timeoutMs}ms url=$url")
            null
        } catch (t: Exception) {
            future.cancel(true)
            AppLog.w("measureDelay failed url=$url", t)
            null
        }
    }

    /**
     * Пробует несколько URL — gstatic иногда недоступен через часть серверов.
     * [tryFallbacks]=false — один URL, чтобы failover не ждал ~30 с.
     */
    fun measureDelayAny(tryFallbacks: Boolean = true): Long? {
        val urls = if (tryFallbacks) HEALTH_CHECK_URLS else HEALTH_CHECK_URLS.take(1)
        for (url in urls) {
            val delay = measureDelay(url)
            if (delay != null && delay > 0) return delay
        }
        return null
    }

    private object XrayCoreCallback : CoreCallbackHandler {
        override fun startup(): Long = 0

        override fun shutdown(): Long {
            AppLog.i("XrayCoreManager shutdown callback")
            if (VpnManager.status.value == VpnStatus.Started) {
                AppLog.w("XrayCore unexpected shutdown while VPN started")
                App.applicationScope.launch(Dispatchers.Main) {
                    VpnManager.disconnect(userInitiated = false)
                }
            }
            return 0
        }

        override fun onEmitStatus(code: Long, message: String?): Long {
            AppLog.i("xray: ${message.orEmpty()}")
            return 0
        }
    }
}
