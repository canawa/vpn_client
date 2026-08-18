package ru.coffeemaniavpn.app.widget

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.VpnManager
import ru.coffeemaniavpn.app.vpn.VpnStatus

/**
 * Покадровая анимация орбиты кнопки виджета (зеркало [ru.coffeemaniavpn.app.ui.XenoConnectButton]).
 */
object VpnHomeWidgetAnimator {
    private val mutex = Mutex()
    private var loopJob: Job? = null
    @Volatile
    var state: WidgetConnectAnimState = WidgetConnectAnimState.snapped(VpnManager.status.value)
        private set

    private var lastStatus: VpnStatus = VpnManager.status.value

    fun ensureStarted(context: Context) {
        val app = context.applicationContext
        if (loopJob?.isActive == true) return
        loopJob = App.applicationScope.launch(Dispatchers.Default) {
            AppLog.i("VpnHomeWidgetAnimator started")
            lastStatus = VpnManager.status.value
            state = WidgetConnectAnimState.snapped(lastStatus)
            while (isActive) {
                if (!VpnHomeWidgetUpdater.hasWidgets(app)) {
                    delay(2_000)
                    continue
                }
                val status = VpnManager.status.value
                if (status != lastStatus) {
                    mutex.withLock {
                        if (status != lastStatus) {
                            lastStatus = status
                            state = WidgetConnectAnimState.snapped(status).copy(spinAngle = state.spinAngle)
                        }
                    }
                }
                when (status) {
                    VpnStatus.Starting, VpnStatus.Stopping, VpnStatus.Started -> {
                        // ~8s на полный оборот, как в Compose.
                        state = state.copy(
                            labelMode = if (status == VpnStatus.Started) {
                                WidgetConnectAnimState.LabelMode.On
                            } else {
                                WidgetConnectAnimState.LabelMode.Busy
                            },
                            spinAngle = (state.spinAngle + 360f * (FRAME_MS / SPIN_LAP_MS)) % 360f,
                        )
                        VpnHomeWidgetUpdater.updateConnectButton(app, state)
                        delay(FRAME_MS)
                    }
                    VpnStatus.Stopped -> {
                        state = state.copy(
                            labelMode = WidgetConnectAnimState.LabelMode.Off,
                            spinAngle = 0f,
                        )
                        VpnHomeWidgetUpdater.updateConnectButton(app, state)
                        delay(IDLE_MS)
                    }
                }
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        loopJob = null
    }

    private const val FRAME_MS = 50L
    private const val SPIN_LAP_MS = 8_000f
    private const val IDLE_MS = 1_500L
}
