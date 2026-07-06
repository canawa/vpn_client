package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ru.coffeemaniavpn.app.data.AppThemeMode

/**
 * Палитра NUBO VPN.
 * Тёмная тема — основная (неоново-синяя, как в макете),
 * светлая — адаптированный бело-голубой вариант.
 */
data class NuboExtraColors(
    /** Основной фон экрана */
    val background: Color,
    /** Более глубокий фон (градиенты, нижняя навигация) */
    val backgroundDeep: Color,
    /** Фон карточек */
    val card: Color,
    /** Приподнятые элементы поверх карточек */
    val cardHigh: Color,
    /** Мягкая синяя обводка */
    val border: Color,
    /** Более заметная обводка (выделение) */
    val borderStrong: Color,
    /** Акцентный синий */
    val blue: Color,
    /** Глубокий синий (градиенты кнопок) */
    val blueDeep: Color,
    /** Циан — состояние «подключено», пинг */
    val cyan: Color,
    /** Светло-голубой (свечение, иконки в активном состоянии) */
    val sky: Color,
    /** Основной текст */
    val textMain: Color,
    /** Второстепенный текст */
    val textMid: Color,
    /** Приглушённый текст */
    val textDim: Color,
    /** Едва заметный текст */
    val textFaint: Color,
    /** Красный — ошибки, «отключено» */
    val red: Color,
    /** Жёлтый — предупреждения, средний пинг, избранное */
    val yellow: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
) {
    /** Цвет пинга: ≤200 ms — хорошо, ≤400 ms — средне, иначе плохо. */
    fun pingColor(latencyMs: Int): Color = when {
        latencyMs <= 200 -> cyan
        latencyMs <= 400 -> yellow
        else -> red
    }
}

val LocalNuboExtraColors = staticCompositionLocalOf { darkNuboExtraColors() }

@Composable
fun nuboColors(): NuboExtraColors = LocalNuboExtraColors.current

fun darkNuboExtraColors(): NuboExtraColors = NuboExtraColors(
    background = Color(0xFF070D1A),
    backgroundDeep = Color(0xFF040810),
    card = Color(0xFF0D1628),
    cardHigh = Color(0xFF13223C),
    border = Color(0x261A7FFF),
    borderStrong = Color(0x661A7FFF),
    blue = Color(0xFF1A7FFF),
    blueDeep = Color(0xFF0A2A9A),
    cyan = Color(0xFF22D3EE),
    sky = Color(0xFF7DD3FC),
    textMain = Color(0xFFE8F0FF),
    textMid = Color(0xFFA0B8D8),
    textDim = Color(0xFF5A7A9A),
    textFaint = Color(0xFF3A5A7A),
    red = Color(0xFFF87171),
    yellow = Color(0xFFFACC15),
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
    border = Color(0x331A7FFF),
    borderStrong = Color(0x801A7FFF),
    blue = Color(0xFF1A7FFF),
    blueDeep = Color(0xFF0A2A9A),
    cyan = Color(0xFF0891B2),
    sky = Color(0xFF0369A1),
    textMain = Color(0xFF0B1B33),
    textMid = Color(0xFF3E5B7A),
    textDim = Color(0xFF64809D),
    textFaint = Color(0xFF8FA6BF),
    red = Color(0xFFDC2626),
    yellow = Color(0xFFB45309),
    error = Color(0xFFBA1A1A),
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
        secondary = extra.cyan,
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
        secondary = extra.cyan,
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
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val extra = if (useDarkTheme) darkNuboExtraColors() else lightNuboExtraColors()
    val scheme = colorSchemeFrom(extra, useDarkTheme)

    CompositionLocalProvider(LocalNuboExtraColors provides extra) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NuboTypography,
            content = content,
        )
    }
}
