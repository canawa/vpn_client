package ru.nubovpn.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Палитра NUBO VPN — спокойная тёмная тема без неонового свечения.
 */
data class NuboExtraColors(
    val background: Color,
    val backgroundDeep: Color,
    val card: Color,
    val cardHigh: Color,
    val border: Color,
    val borderStrong: Color,
    val blue: Color,
    val blueDeep: Color,
    val cyan: Color,
    val sky: Color,
    val textMain: Color,
    val textMid: Color,
    val textDim: Color,
    val textFaint: Color,
    val red: Color,
    val yellow: Color,
    val green: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
) {
    fun pingColor(latencyMs: Int): Color = when {
        latencyMs <= 200 -> green
        latencyMs <= 400 -> yellow
        else -> red
    }
}

val LocalNuboExtraColors = staticCompositionLocalOf { darkNuboExtraColors() }

@Composable
fun nuboColors(): NuboExtraColors = LocalNuboExtraColors.current

fun darkNuboExtraColors(): NuboExtraColors = NuboExtraColors(
    background = Color(0xFF080A0F),
    backgroundDeep = Color(0xFF0F131A),
    card = Color(0xFF141922),
    cardHigh = Color(0xFF1A2030),
    border = Color(0xFF232833),
    borderStrong = Color(0xFF2D3544),
    blue = Color(0xFF2563EB),
    blueDeep = Color(0xFF1D4ED8),
    cyan = Color(0xFF60A5FA),
    sky = Color(0xFF60A5FA),
    textMain = Color(0xFFF3F4F6),
    textMid = Color(0xFF9CA3AF),
    textDim = Color(0xFF626978),
    textFaint = Color(0xFF626978),
    red = Color(0xFFF87171),
    yellow = Color(0xFFFBBF24),
    green = Color(0xFF34D399),
    error = Color(0xFFF87171),
    onError = Color(0xFF3A0A0A),
    errorContainer = Color(0xFF3A1220),
    onErrorContainer = Color(0xFFFFB4AB),
)

fun lightNuboExtraColors(): NuboExtraColors = NuboExtraColors(
    background = Color(0xFFF3F7FD),
    backgroundDeep = Color(0xFFE9F0FA),
    card = Color(0xFFFFFFFF),
    cardHigh = Color(0xFFEAF1FB),
    border = Color(0xFFE2E8F0),
    borderStrong = Color(0xFFCBD5E1),
    blue = Color(0xFF2563EB),
    blueDeep = Color(0xFF1D4ED8),
    cyan = Color(0xFF2563EB),
    sky = Color(0xFF60A5FA),
    textMain = Color(0xFF0B1220),
    textMid = Color(0xFF64748B),
    textDim = Color(0xFF94A3B8),
    textFaint = Color(0xFF94A3B8),
    red = Color(0xFFF87171),
    yellow = Color(0xFFFBBF24),
    green = Color(0xFF34D399),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
)

private fun colorSchemeFrom(extra: NuboExtraColors, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = extra.blue,
        onPrimary = Color.White,
        primaryContainer = extra.blueDeep,
        onPrimaryContainer = extra.textMain,
        secondary = extra.sky,
        onSecondary = extra.backgroundDeep,
        secondaryContainer = extra.cardHigh,
        onSecondaryContainer = extra.textMain,
        tertiary = extra.sky,
        onTertiary = extra.backgroundDeep,
        tertiaryContainer = extra.cardHigh,
        onTertiaryContainer = extra.textMain,
        background = extra.background,
        onBackground = extra.textMain,
        surface = extra.background,
        onSurface = extra.textMain,
        surfaceVariant = extra.card,
        onSurfaceVariant = extra.textMid,
        outline = extra.borderStrong,
        outlineVariant = extra.border,
        error = extra.error,
        onError = extra.onError,
        errorContainer = extra.errorContainer,
        onErrorContainer = extra.onErrorContainer,
        surfaceContainer = extra.card,
        surfaceContainerLow = extra.card,
        surfaceContainerHigh = extra.cardHigh,
        surfaceContainerHighest = extra.cardHigh,
        surfaceContainerLowest = extra.backgroundDeep,
        surfaceDim = extra.backgroundDeep,
        surfaceBright = extra.cardHigh,
    )
} else {
    lightColorScheme(
        primary = extra.blue,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E6FF),
        onPrimaryContainer = extra.blueDeep,
        secondary = extra.sky,
        onSecondary = Color.White,
        secondaryContainer = extra.cardHigh,
        onSecondaryContainer = extra.textMain,
        tertiary = extra.sky,
        onTertiary = Color.White,
        tertiaryContainer = extra.cardHigh,
        onTertiaryContainer = extra.textMain,
        background = extra.background,
        onBackground = extra.textMain,
        surface = extra.background,
        onSurface = extra.textMain,
        surfaceVariant = extra.cardHigh,
        onSurfaceVariant = extra.textMid,
        outline = extra.borderStrong,
        outlineVariant = extra.border,
        error = extra.error,
        onError = extra.onError,
        errorContainer = extra.errorContainer,
        onErrorContainer = extra.onErrorContainer,
        surfaceContainer = extra.card,
        surfaceContainerLow = extra.card,
        surfaceContainerHigh = extra.cardHigh,
        surfaceContainerHighest = extra.cardHigh,
        surfaceContainerLowest = extra.backgroundDeep,
        surfaceDim = extra.backgroundDeep,
        surfaceBright = extra.card,
    )
}

@Composable
fun NuboTheme(
    content: @Composable () -> Unit,
) {
    val extra = darkNuboExtraColors()
    val scheme = colorSchemeFrom(extra, dark = true)

    CompositionLocalProvider(LocalNuboExtraColors provides extra) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NuboTypography,
            content = content,
        )
    }
}
