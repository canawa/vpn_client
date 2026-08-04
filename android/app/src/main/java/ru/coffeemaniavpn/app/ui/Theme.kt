package ru.coffeemaniavpn.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalTextStyle

/**
 * Палитра ClevVPN: чёрный фон + жёлтый акцент (как Theme.swift).
 * API coffemaniaColors() сохранён для совместимости вызовов.
 */
object CoffemaniaColors {
    val Background = Color(0xFF0B0B0D)
    val Surface = Color(0xFF16161A)
    val SurfaceLight = Color(0xFF1F1F25)
    val Stroke = Color(0xFF2A2A31)

    val Yellow = Color(0xFFFFC400)
    val Amber = Color(0xFFD18700)
    val LogoYellow = Color(0xFFFAC300)
    val LogoAmber = Color(0xFFE39A00)

    val TextPrimary = Color(0xFFF2F2F5)
    val TextSecondary = Color(0xFF9A9AA3)

    val Green = Color(0xFF30D158)
    val Orange = Color(0xFFFF9F0A)
    val Red = Color(0xFFFF453A)

    val YellowGradient = Brush.linearGradient(listOf(Yellow, Amber))

    // Aliases used across existing UI
    val MilkFoam = Background
    val Espresso = TextPrimary
    val Mocha = TextSecondary
    val Cappuccino = Surface
    val Latte = Stroke
    val OnBackground = TextPrimary
    val Primary = Yellow
    val OnPrimary = Color(0xFF0B0B0D)
    val OnSurface = TextPrimary
    val OnSurfaceVariant = TextSecondary
    val SurfaceBright = SurfaceLight
    val SurfaceDim = Surface
    val SurfaceContainer = Surface
    val SurfaceContainerLow = Surface
    val SurfaceContainerLowest = Background
    val SurfaceContainerHigh = SurfaceLight
    val SurfaceContainerHighest = Stroke
    val SurfaceVariant = SurfaceLight
    val Outline = Stroke
    val OutlineVariant = Stroke
    val PrimaryContainer = SurfaceLight
    val OnPrimaryContainer = TextPrimary
    val PrimaryFixedDim = Amber
    val Secondary = TextSecondary
    val OnSecondary = Background
    val SecondaryContainer = SurfaceLight
    val OnSecondaryContainer = TextPrimary
    val Tertiary = Yellow
    val OnTertiary = Background
    val TertiaryContainer = SurfaceLight
    val OnTertiaryContainer = TextPrimary
    val BrandRed = Red
    val Error = Red
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFF3A1010)
    val OnErrorContainer = Color(0xFFFFD6DC)
    val PingGood = Green
    val PingMedium = Orange
    val PingBad = Red
    val ConnectDisabledOuter = SurfaceLight
    val ConnectDisabledInner = Surface
    val ConnectDisabledBorder = Stroke
    val ConnectDisabledIcon = TextSecondary

    fun pingColor(latencyMs: Int): Color = when {
        latencyMs <= 100 -> PingGood
        latencyMs <= 250 -> PingMedium
        else -> PingBad
    }
}

data class CoffemaniaExtraColors(
    val milkFoam: Color,
    val espresso: Color,
    val mocha: Color,
    val cappuccino: Color,
    val latte: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val surfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceDim: Color,
    val outline: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val connectDisabledOuter: Color,
    val connectDisabledInner: Color,
    val connectDisabledBorder: Color,
    val connectDisabledIcon: Color,
    val yellow: Color = CoffemaniaColors.Yellow,
    val amber: Color = CoffemaniaColors.Amber,
    val logoYellow: Color = CoffemaniaColors.LogoYellow,
    val green: Color = CoffemaniaColors.Green,
    val orange: Color = CoffemaniaColors.Orange,
)

val LocalCoffemaniaExtraColors = staticCompositionLocalOf { clevExtraColors() }

@Composable
fun coffemaniaColors(): CoffemaniaExtraColors = LocalCoffemaniaExtraColors.current

fun clevExtraColors(): CoffemaniaExtraColors = CoffemaniaExtraColors(
    milkFoam = CoffemaniaColors.Background,
    espresso = CoffemaniaColors.TextPrimary,
    mocha = CoffemaniaColors.TextSecondary,
    cappuccino = CoffemaniaColors.Surface,
    latte = CoffemaniaColors.Stroke,
    background = CoffemaniaColors.Background,
    onBackground = CoffemaniaColors.TextPrimary,
    surface = CoffemaniaColors.Surface,
    onSurface = CoffemaniaColors.TextPrimary,
    onSurfaceVariant = CoffemaniaColors.TextSecondary,
    surfaceVariant = CoffemaniaColors.SurfaceLight,
    surfaceContainer = CoffemaniaColors.Surface,
    surfaceContainerHigh = CoffemaniaColors.SurfaceLight,
    surfaceContainerHighest = CoffemaniaColors.Stroke,
    surfaceDim = CoffemaniaColors.Surface,
    outline = CoffemaniaColors.Stroke,
    primary = CoffemaniaColors.Yellow,
    onPrimary = CoffemaniaColors.Background,
    primaryContainer = CoffemaniaColors.SurfaceLight,
    onPrimaryContainer = CoffemaniaColors.TextPrimary,
    error = CoffemaniaColors.Error,
    onError = CoffemaniaColors.OnError,
    errorContainer = CoffemaniaColors.ErrorContainer,
    onErrorContainer = CoffemaniaColors.OnErrorContainer,
    connectDisabledOuter = CoffemaniaColors.ConnectDisabledOuter,
    connectDisabledInner = CoffemaniaColors.ConnectDisabledInner,
    connectDisabledBorder = CoffemaniaColors.ConnectDisabledBorder,
    connectDisabledIcon = CoffemaniaColors.ConnectDisabledIcon,
)

fun lightCoffemaniaExtraColors(): CoffemaniaExtraColors = clevExtraColors()
fun darkCoffemaniaExtraColors(): CoffemaniaExtraColors = clevExtraColors()

private fun colorSchemeFrom(extra: CoffemaniaExtraColors) = darkColorScheme(
    primary = extra.primary,
    onPrimary = extra.onPrimary,
    primaryContainer = extra.primaryContainer,
    onPrimaryContainer = extra.onPrimaryContainer,
    secondary = extra.mocha,
    onSecondary = extra.onPrimary,
    secondaryContainer = extra.primaryContainer,
    onSecondaryContainer = extra.onPrimaryContainer,
    tertiary = extra.primary,
    onTertiary = extra.onPrimary,
    tertiaryContainer = extra.primaryContainer,
    onTertiaryContainer = extra.onPrimaryContainer,
    background = extra.background,
    onBackground = extra.onBackground,
    surface = extra.surface,
    onSurface = extra.onSurface,
    surfaceVariant = extra.surfaceVariant,
    onSurfaceVariant = extra.onSurfaceVariant,
    outline = extra.outline,
    outlineVariant = extra.outline,
    error = extra.error,
    onError = extra.onError,
    errorContainer = extra.errorContainer,
    onErrorContainer = extra.onErrorContainer,
    surfaceContainer = extra.surfaceContainer,
    surfaceContainerLow = extra.surfaceContainer,
    surfaceContainerHigh = extra.surfaceContainerHigh,
    surfaceContainerHighest = extra.surfaceContainerHighest,
    surfaceContainerLowest = extra.background,
    surfaceDim = extra.surfaceDim,
    surfaceBright = extra.surfaceVariant,
)

@Composable
fun CoffemaniaTheme(
    content: @Composable () -> Unit,
) {
    val extra = clevExtraColors()
    CompositionLocalProvider(
        LocalCoffemaniaExtraColors provides extra,
        LocalTextStyle provides ClevTextStyle,
    ) {
        MaterialTheme(
            colorScheme = colorSchemeFrom(extra),
            typography = CoffemaniaTypography,
            content = content,
        )
    }
}
