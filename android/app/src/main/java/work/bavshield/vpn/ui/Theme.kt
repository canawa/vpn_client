package work.bavshield.vpn.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object BavShieldColors {
    val Canvas = Color(0xFF050807)
    val Neon = Color(0xFF39FF88)
    val Muted = Color(0xFF737D77)
    val Card = Color(0xFF0A100D)
    val CardDeep = Color(0xFF080D0A)
    val PingGood = Color(0xFF39FF88)
    val PingMedium = Color(0xFFD4A017)
    val PingBad = Color(0xFFC62828)
    val ConnectOrange = Color(0xFFFF8A00)
    val ConnectThumb = Color(0xFFE8E8E8)

    fun pingColor(latencyMs: Int): Color = when {
        latencyMs <= 200 -> PingGood
        latencyMs <= 400 -> PingMedium
        else -> PingBad
    }
}

/**
 * Semantic slots used across the UI.
 * milkFoam = canvas, espresso = titles/icons, mocha = muted,
 * cappuccino = cards, latte = outlines.
 */
data class BavShieldExtraColors(
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
)

val LocalBavShieldExtraColors = staticCompositionLocalOf { extraColors() }

@Composable
fun bavShieldColors(): BavShieldExtraColors = LocalBavShieldExtraColors.current

fun extraColors(): BavShieldExtraColors {
    val canvas = BavShieldColors.Canvas
    val onCanvas = BavShieldColors.Neon
    val muted = BavShieldColors.Muted
    val card = BavShieldColors.Card
    val outline = BavShieldColors.Neon.copy(alpha = 0.22f)
    return BavShieldExtraColors(
        milkFoam = canvas,
        espresso = onCanvas,
        mocha = muted,
        cappuccino = card,
        latte = outline,
        background = canvas,
        onBackground = onCanvas,
        surface = canvas,
        onSurface = onCanvas,
        onSurfaceVariant = muted,
        surfaceVariant = card,
        surfaceContainer = card,
        surfaceContainerHigh = BavShieldColors.CardDeep,
        surfaceContainerHighest = Color(0xFF0B120E),
        surfaceDim = BavShieldColors.CardDeep,
        outline = outline,
        primary = onCanvas,
        onPrimary = canvas,
        primaryContainer = Color(0xFF0D1711),
        onPrimaryContainer = onCanvas,
        error = Color(0xFFFF6B6B),
        onError = canvas,
        errorContainer = Color(0xFF3A1515),
        onErrorContainer = Color(0xFFFFB4AB),
        connectDisabledOuter = card,
        connectDisabledInner = canvas,
        connectDisabledBorder = outline,
        connectDisabledIcon = muted,
    )
}

private fun colorSchemeFrom(extra: BavShieldExtraColors) = darkColorScheme(
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
    surfaceContainerLowest = extra.surfaceContainer,
    surfaceDim = extra.surfaceDim,
    surfaceBright = extra.surface,
)

@Composable
fun BavShieldTheme(content: @Composable () -> Unit) {
    val extra = extraColors()
    CompositionLocalProvider(LocalBavShieldExtraColors provides extra) {
        MaterialTheme(
            colorScheme = colorSchemeFrom(extra),
            typography = BavShieldTypography,
            content = content,
        )
    }
}
