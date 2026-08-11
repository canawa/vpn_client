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
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Покадровая анимация кнопки виджета через [AppWidgetManager.partiallyUpdateAppWidget].
 * RemoteViews не умеет Compose-анимации — обновляем bitmap.
 */
object VpnHomeWidgetAnimator {
    private val mutex = Mutex()
    private var loopJob: Job? = null
    @Volatile
    var state: WidgetConnectAnimState = WidgetConnectAnimState.snapped(VpnManager.status.value)
        private set

    private var lastStatus: VpnStatus = VpnManager.status.value
    private var transitioning = false

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
                if (status != lastStatus && !transitioning) {
                    mutex.withLock {
                        if (status != lastStatus) {
                            transitioning = true
                            runTransition(app, lastStatus, status)
                            lastStatus = status
                            transitioning = false
                        }
                    }
                }
                if (transitioning) {
                    delay(16)
                    continue
                }
                when (status) {
                    VpnStatus.Starting, VpnStatus.Stopping -> {
                        state = state.copy(
                            showSpinner = true,
                            showComet = false,
                            labelMode = WidgetConnectAnimState.LabelMode.Busy,
                            spinAngle = (state.spinAngle + 360f * (FRAME_MS / 420f)) % 360f,
                            haloPulse = 0.85f + 0.15f * pulse(1.8f),
                        )
                        VpnHomeWidgetUpdater.updateConnectButton(app, state)
                        delay(FRAME_MS)
                    }
                    VpnStatus.Started -> {
                        state = state.copy(
                            plateOn = 1f,
                            ringFill = 1f,
                            showSpinner = false,
                            showComet = false,
                            burstAlpha = 0f,
                            labelMode = WidgetConnectAnimState.LabelMode.On,
                            spinAngle = (state.spinAngle + 12f * (TIMER_MS / 1000f)) % 360f,
                            haloPulse = 0.9f + 0.1f * pulse(0.7f),
                        )
                        VpnHomeWidgetUpdater.updateConnectButton(app, state)
                        delay(TIMER_MS)
                    }
                    VpnStatus.Stopped -> {
                        state = state.copy(
                            plateOn = 0f,
                            ringFill = 0f,
                            showSpinner = false,
                            showComet = false,
                            burstAlpha = 0f,
                            labelMode = WidgetConnectAnimState.LabelMode.Off,
                            haloPulse = 0.72f + 0.28f * pulse(1.2f),
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

    private suspend fun runTransition(context: Context, from: VpnStatus, to: VpnStatus) {
        AppLog.i("VpnHomeWidgetAnimator $from -> $to")
        when (to) {
            VpnStatus.Starting -> animateBusyIn(context)
            VpnStatus.Started -> animateConnect(context)
            VpnStatus.Stopping -> animateBusyFromOn(context)
            VpnStatus.Stopped -> animateDisconnect(context)
        }
    }

    private suspend fun animateBusyIn(context: Context) {
        val startPlate = state.plateOn
        val startRing = state.ringFill
        val duration = 280L
        val start = System.nanoTime()
        while (true) {
            val t = ((System.nanoTime() - start) / 1_000_000L).toFloat() / duration
            val e = easeOutCubic(t.coerceIn(0f, 1f))
            state = state.copy(
                plateOn = startPlate * (1f - e),
                ringFill = startRing * (1f - e),
                showSpinner = true,
                showComet = false,
                labelMode = WidgetConnectAnimState.LabelMode.Busy,
                spinAngle = (state.spinAngle + 360f * (FRAME_MS / 420f)) % 360f,
                haloPulse = 0.9f,
                burstAlpha = 0f,
            )
            VpnHomeWidgetUpdater.updateConnectButton(context, state)
            if (t >= 1f) break
            delay(FRAME_MS)
        }
    }

    private suspend fun animateBusyFromOn(context: Context) {
        animateBusyIn(context)
    }

    private suspend fun animateConnect(context: Context) {
        // Комета + заливка кольца + пластина → On (как ClevConnectButton).
        val cometMs = 500L
        val fillDelay = 150L
        val fillMs = 550L
        val plateMs = 300L
        val burstMs = 900L
        val total = fillDelay + fillMs
        val start = System.nanoTime()
        state = state.copy(
            showSpinner = false,
            showComet = true,
            cometAngle = -90f,
            ringFill = 0f,
            plateOn = 0f,
            labelMode = WidgetConnectAnimState.LabelMode.On,
            burstAlpha = 0f,
        )
        while (true) {
            val elapsed = (System.nanoTime() - start) / 1_000_000L
            val t = elapsed.toFloat()
            val cometT = easeOutQuad((t / cometMs).coerceIn(0f, 1f))
            val cometAngle = -90f + 900f * cometT
            val fillT = if (elapsed < fillDelay) {
                0f
            } else {
                easeOutCubic(((elapsed - fillDelay).toFloat() / fillMs).coerceIn(0f, 1f))
            }
            val plateT = easeOutCubic((t / plateMs).coerceIn(0f, 1f))
            val burstT = (t / burstMs).coerceIn(0f, 1f)
            val burstAlpha = when {
                burstT < 0.2f -> burstT / 0.2f * 0.45f
                burstT < 1f -> 0.45f * (1f - (burstT - 0.2f) / 0.8f)
                else -> 0f
            }
            state = state.copy(
                cometAngle = cometAngle,
                showComet = fillT < 1f && cometT < 1f,
                ringFill = fillT,
                plateOn = plateT,
                burstAlpha = burstAlpha,
                showSpinner = false,
                labelMode = WidgetConnectAnimState.LabelMode.On,
                haloPulse = 1f,
            )
            VpnHomeWidgetUpdater.updateConnectButton(context, state)
            if (elapsed >= total && burstT >= 1f) break
            delay(FRAME_MS)
        }
        state = state.copy(
            plateOn = 1f,
            ringFill = 1f,
            showComet = false,
            burstAlpha = 0f,
            labelMode = WidgetConnectAnimState.LabelMode.On,
        )
        VpnHomeWidgetUpdater.updateConnectButton(context, state)
    }

    private suspend fun animateDisconnect(context: Context) {
        val startPlate = state.plateOn.coerceAtLeast(0.01f)
        val startRing = state.ringFill.coerceAtLeast(0.01f)
        val duration = 280L
        val start = System.nanoTime()
        while (true) {
            val t = ((System.nanoTime() - start) / 1_000_000L).toFloat() / duration
            val e = easeOutCubic(t.coerceIn(0f, 1f))
            state = state.copy(
                plateOn = startPlate * (1f - e),
                ringFill = startRing * (1f - e),
                showSpinner = false,
                showComet = false,
                burstAlpha = 0f,
                labelMode = if (e < 0.5f) {
                    WidgetConnectAnimState.LabelMode.On
                } else {
                    WidgetConnectAnimState.LabelMode.Off
                },
                haloPulse = 1f,
            )
            VpnHomeWidgetUpdater.updateConnectButton(context, state)
            if (t >= 1f) break
            delay(FRAME_MS)
        }
        state = WidgetConnectAnimState.snapped(VpnStatus.Stopped)
        VpnHomeWidgetUpdater.updateConnectButton(context, state)
    }

    private fun pulse(speed: Float): Float {
        val t = System.nanoTime() / 1_000_000_000.0
        return ((sin(t * speed * PI) + 1.0) / 2.0).toFloat()
    }

    private fun easeOutCubic(t: Float): Float = 1f - (1f - t).pow(3)
    private fun easeOutQuad(t: Float): Float = 1f - (1f - t) * (1f - t)

    private const val FRAME_MS = 16L
    private const val IDLE_MS = 33L
    private const val TIMER_MS = 1000L
}
