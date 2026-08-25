package work.bavshield.vpn.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import work.bavshield.vpn.data.AppColorScheme
import work.bavshield.vpn.data.AppThemeMode

object BavShieldColors {
    val PingGood = Color(0xFF2E7D32)
    val PingMedium = Color(0xFFD4A017)
    val PingBad = Color(0xFFC62828)

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

private data class SchemeTokens(
    val canvas: Color,
    val onCanvas: Color,
    val muted: Color,
    val card: Color,
    val outline: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
)

val LocalBavShieldExtraColors = staticCompositionLocalOf {
    extraColors(AppColorScheme.DEFAULT, dark = false)
}

@Composable
fun bavShieldColors(): BavShieldExtraColors = LocalBavShieldExtraColors.current

fun extraColors(scheme: AppColorScheme, dark: Boolean): BavShieldExtraColors {
    val tokens = if (dark) scheme.darkTokens() else scheme.lightTokens()
    return BavShieldExtraColors(
        milkFoam = tokens.canvas,
        espresso = tokens.onCanvas,
        mocha = tokens.muted,
        cappuccino = tokens.card,
        latte = tokens.outline,
        background = tokens.canvas,
        onBackground = tokens.onCanvas,
        surface = tokens.canvas,
        onSurface = tokens.onCanvas,
        onSurfaceVariant = tokens.muted,
        surfaceVariant = tokens.card,
        surfaceContainer = tokens.card,
        surfaceContainerHigh = tokens.canvas,
        surfaceContainerHighest = tokens.outline,
        surfaceDim = tokens.card,
        outline = tokens.outline,
        primary = tokens.onCanvas,
        onPrimary = tokens.canvas,
        primaryContainer = tokens.outline,
        onPrimaryContainer = tokens.onCanvas,
        error = tokens.error,
        onError = tokens.onError,
        errorContainer = tokens.errorContainer,
        onErrorContainer = tokens.onErrorContainer,
        connectDisabledOuter = tokens.card,
        connectDisabledInner = tokens.canvas,
        connectDisabledBorder = tokens.outline,
        connectDisabledIcon = tokens.muted,
    )
}

private fun AppColorScheme.lightTokens(): SchemeTokens = when (this) {
    AppColorScheme.FOREST -> SchemeTokens(
        canvas = Color(0xFFF3F6F3),
        onCanvas = Color(0xFF1A2B22),
        muted = Color(0xFF7C9D8A),
        card = Color(0xFFE4EBE6),
        outline = Color(0xFFC5D4CB),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFF3F6F3),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
    )
    AppColorScheme.OCEAN -> SchemeTokens(
        canvas = Color(0xFFF2F6FA),
        onCanvas = Color(0xFF1B3A4B),
        muted = Color(0xFF6A8AA0),
        card = Color(0xFFE3EAF1),
        outline = Color(0xFFC5D3E0),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFF2F6FA),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
    )
    AppColorScheme.SLATE -> SchemeTokens(
        canvas = Color(0xFFF4F4F5),
        onCanvas = Color(0xFF2A2D34),
        muted = Color(0xFF8B8F98),
        card = Color(0xFFE8E8EA),
        outline = Color(0xFFD0D1D4),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFF4F4F5),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
    )
    AppColorScheme.TEAL -> SchemeTokens(
        canvas = Color(0xFFF1F7F6),
        onCanvas = Color(0xFF134E4A),
        muted = Color(0xFF5F8F8A),
        card = Color(0xFFDDECEA),
        outline = Color(0xFFB9D4D0),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFF1F7F6),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
    )
    AppColorScheme.INDIGO -> SchemeTokens(
        canvas = Color(0xFFF4F3F8),
        onCanvas = Color(0xFF2E2A4A),
        muted = Color(0xFF7A74A0),
        card = Color(0xFFE6E3F0),
        outline = Color(0xFFCBC6DC),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFF4F3F8),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
    )
    AppColorScheme.AMBER -> SchemeTokens(
        canvas = Color(0xFFFBF6EE),
        onCanvas = Color(0xFF4A3418),
        muted = Color(0xFFB08A4A),
        card = Color(0xFFF1E6D2),
        outline = Color(0xFFE0CFA8),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFBF6EE),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
    )
    AppColorScheme.CRIMSON -> SchemeTokens(
        canvas = Color(0xFFF8F2F2),
        onCanvas = Color(0xFF4A1C22),
        muted = Color(0xFFB07078),
        card = Color(0xFFEEDFDF),
        outline = Color(0xFFD9C0C3),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFF8F2F2),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
    )
}

private fun AppColorScheme.darkTokens(): SchemeTokens = when (this) {
    AppColorScheme.FOREST -> SchemeTokens(
        canvas = Color(0xFF0A1510),
        onCanvas = Color(0xFFE4EDE7),
        muted = Color(0xFF8FB5A0),
        card = Color(0xFF14201A),
        outline = Color(0xFF24362C),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
    AppColorScheme.OCEAN -> SchemeTokens(
        canvas = Color(0xFF0C141C),
        onCanvas = Color(0xFFE6EEF4),
        muted = Color(0xFF7A96AB),
        card = Color(0xFF15202A),
        outline = Color(0xFF243544),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
    AppColorScheme.SLATE -> SchemeTokens(
        canvas = Color(0xFF121316),
        onCanvas = Color(0xFFECECEE),
        muted = Color(0xFF8E929A),
        card = Color(0xFF1C1D21),
        outline = Color(0xFF2C2E34),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
    AppColorScheme.TEAL -> SchemeTokens(
        canvas = Color(0xFF0B1615),
        onCanvas = Color(0xFFE4F0EE),
        muted = Color(0xFF7FB0AA),
        card = Color(0xFF142422),
        outline = Color(0xFF1F3532),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
    AppColorScheme.INDIGO -> SchemeTokens(
        canvas = Color(0xFF12101C),
        onCanvas = Color(0xFFEAE7F4),
        muted = Color(0xFF9A94B8),
        card = Color(0xFF1C1930),
        outline = Color(0xFF2C2744),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
    AppColorScheme.AMBER -> SchemeTokens(
        canvas = Color(0xFF16110A),
        onCanvas = Color(0xFFF3E9D6),
        muted = Color(0xFFC4A46A),
        card = Color(0xFF241C12),
        outline = Color(0xFF3A2E1C),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
    AppColorScheme.CRIMSON -> SchemeTokens(
        canvas = Color(0xFF160C0E),
        onCanvas = Color(0xFFF1E4E6),
        muted = Color(0xFFC48890),
        card = Color(0xFF241416),
        outline = Color(0xFF3A2226),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
    )
}

private fun colorSchemeFrom(extra: BavShieldExtraColors, dark: Boolean) = if (!dark) {
    lightColorScheme(
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
} else {
    darkColorScheme(
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
}

@Composable
fun BavShieldTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    colorScheme: AppColorScheme = AppColorScheme.DEFAULT,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val extra = extraColors(colorScheme, useDarkTheme)
    val scheme = colorSchemeFrom(extra, useDarkTheme)

    CompositionLocalProvider(LocalBavShieldExtraColors provides extra) {
        MaterialTheme(
            colorScheme = scheme,
            typography = BavShieldTypography,
            content = content,
        )
    }
}
