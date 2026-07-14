package ru.nubovpn.app.vpn

import android.content.Context
import go.Seq
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray
import ru.nubovpn.app.util.AppLog
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

    fun startLoop(config: String, tunFd: Int) {
        ensureController(XrayCoreCallback).startLoop(config, tunFd)
    }

    fun stopLoop() {
        runCatching { coreController?.stopLoop() }
    }

    private object XrayCoreCallback : CoreCallbackHandler {
        override fun startup(): Long = 0

        override fun shutdown(): Long {
            AppLog.i("XrayCoreManager shutdown callback")
            // Ядро остановилось само (крэш/обрыв) — сообщаем сервису,
            // иначе приложение продолжает показывать «подключено» при мёртвом туннеле
            BoxService.onCoreStopped()
            return 0
        }

        override fun onEmitStatus(code: Long, message: String?): Long {
            AppLog.i("xray: ${message.orEmpty()}")
            if (message?.contains("Core stopped", ignoreCase = true) == true) {
                BoxService.onCoreStopped()
            }
            return 0
        }
    }
}
