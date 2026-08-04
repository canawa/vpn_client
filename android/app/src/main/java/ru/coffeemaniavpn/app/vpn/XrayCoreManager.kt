package ru.coffeemaniavpn.app.vpn

import android.content.Context
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import ru.coffeemaniavpn.app.util.AppLog
import java.util.concurrent.atomic.AtomicBoolean

object XrayCoreManager {
    private val initialized = AtomicBoolean(false)
    private var coreController: CoreController? = null

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
     * Сумма по outbound `proxy` и `direct`.
     */
    fun queryTrafficDelta(): Pair<Long, Long> {
        val controller = coreController ?: return 0L to 0L
        if (!controller.isRunning) return 0L to 0L
        return runCatching {
            var uplink = 0L
            var downlink = 0L
            for (tag in listOf("proxy", "direct")) {
                uplink += controller.queryStats(tag, "uplink").coerceAtLeast(0L)
                downlink += controller.queryStats(tag, "downlink").coerceAtLeast(0L)
            }
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
    fun measureDelay(url: String = "https://www.gstatic.com/generate_204"): Long? {
        val controller = coreController ?: return null
        if (!controller.isRunning) return null
        return runCatching { controller.measureDelay(url) }
            .onFailure { AppLog.w("measureDelay failed url=$url", it) }
            .getOrNull()
    }

    private object XrayCoreCallback : CoreCallbackHandler {
        override fun startup(): Long = 0

        override fun shutdown(): Long {
            AppLog.i("XrayCoreManager shutdown callback")
            return 0
        }

        override fun onEmitStatus(code: Long, message: String?): Long {
            AppLog.i("xray: ${message.orEmpty()}")
            return 0
        }
    }
}
