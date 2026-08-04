package ru.coffeemaniavpn.app.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.tween

/** Параметры анимаций из Theme/Components.swift и ConnectionEffects.swift (macOS/iOS). */
object ClevMotion {
    val pressSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 480f)
    const val pressScale = 0.93f

    val chipSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 350f)
    val toastSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 280f)

    val statusGlowColorSpec = tween<androidx.compose.ui.graphics.Color>(
        durationMillis = 600,
        easing = FastOutSlowInEasing,
    )
    val plateOnSpec = tween<Float>(durationMillis = 300, easing = FastOutSlowInEasing)

    val connectCometSpec = tween<Float>(durationMillis = 500, easing = FastOutLinearInEasing)
    val connectRingFillSpec = tween<Float>(durationMillis = 550, easing = FastOutSlowInEasing)
    const val connectRingFillDelayMs = 150L
    const val connectBurstDurationMs = 1300L

    val disconnectRingSpec = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)

    const val busySpinConnectMs = 500
    const val busySpinPingMs = 900

    val settingsEnterSpec = tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing)
    val settingsExitSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)
}
