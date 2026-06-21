package online.coffemaniavpn.client.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import online.coffemaniavpn.client.data.AppThemeMode

object CoffemaniaColors {
    /** Молочная пена — основной фон */
    val MilkFoam = Color(0xFFEDE0D8)

    /** Эспрессо — заголовки, иконки, акцентный текст */
    val Espresso = Color(0xFF3D1C1C)

    /** Мокко — подписи, неактивные элементы */
    val Mocha = Color(0xFFB09080)

    /** Капучино — карточки, tab bar, внешний круг кнопки */
    val Cappuccino = Color(0xFFDDD0C8)

    /** Латте — обводки, кнопки в карточках, фон активной вкладки */
    val Latte = Color(0xFFC8B8A8)

    val Background = MilkFoam
    val OnBackground = Espresso
    val Primary = Espresso
    val OnPrimary = MilkFoam
    val OnSurface = Espresso
    val OnSurfaceVariant = Mocha

    val Surface = MilkFoam
    val SurfaceBright = MilkFoam
    val SurfaceDim = Cappuccino
    val SurfaceContainer = Cappuccino
    val SurfaceContainerLow = Cappuccino
    val SurfaceContainerLowest = Cappuccino
    val SurfaceContainerHigh = MilkFoam
    val SurfaceContainerHighest = Latte
    val SurfaceVariant = Cappuccino

    val Outline = Latte
    val OutlineVariant = Latte

    val PrimaryContainer = Latte
    val OnPrimaryContainer = Espresso
    val PrimaryFixedDim = Latte

    val Secondary = Mocha
    val OnSecondary = MilkFoam
    val SecondaryContainer = Latte
    val OnSecondaryContainer = Espresso

    val Tertiary = Espresso
    val OnTertiary = MilkFoam
    val TertiaryContainer = Latte
    val OnTertiaryContainer = Espresso

    val Error = Color(0xFFBA1A1A)
    val OnError = MilkFoam
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)

    /** Пинг ≤ 200 ms */
    val PingGood = Color(0xFF2E7D32)

    /** Пинг 201–400 ms */
    val PingMedium = Color(0xFFD4A017)

    /** Пинг > 400 ms или недоступен */
    val PingBad = Color(0xFFC62828)

    /** Неактивная кнопка подключения (нет подписки) */
    val ConnectDisabledOuter = Color(0xFFD8D0CA)
    val ConnectDisabledInner = Color(0xFFE8E2DC)
    val ConnectDisabledBorder = Color(0xFFC4BAB2)
    val ConnectDisabledIcon = Color(0xFF9E9088)

    fun pingColor(latencyMs: Int): Color = when {
        latencyMs <= 200 -> PingGood
        latencyMs <= 400 -> PingMedium
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
)

val LocalCoffemaniaExtraColors = staticCompositionLocalOf { lightCoffemaniaExtraColors() }

@Composable
fun coffemaniaColors(): CoffemaniaExtraColors = LocalCoffemaniaExtraColors.current

fun lightCoffemaniaExtraColors(): CoffemaniaExtraColors = CoffemaniaExtraColors(
    milkFoam = CoffemaniaColors.MilkFoam,
    espresso = CoffemaniaColors.Espresso,
    mocha = CoffemaniaColors.Mocha,
    cappuccino = CoffemaniaColors.Cappuccino,
    latte = CoffemaniaColors.Latte,
    background = CoffemaniaColors.Background,
    onBackground = CoffemaniaColors.OnBackground,
    surface = CoffemaniaColors.Surface,
    onSurface = CoffemaniaColors.OnSurface,
    onSurfaceVariant = CoffemaniaColors.OnSurfaceVariant,
    surfaceVariant = CoffemaniaColors.SurfaceVariant,
    surfaceContainer = CoffemaniaColors.SurfaceContainer,
    surfaceContainerHigh = CoffemaniaColors.SurfaceContainerHigh,
    surfaceContainerHighest = CoffemaniaColors.SurfaceContainerHighest,
    surfaceDim = CoffemaniaColors.SurfaceDim,
    outline = CoffemaniaColors.Outline,
    primary = CoffemaniaColors.Primary,
    onPrimary = CoffemaniaColors.OnPrimary,
    primaryContainer = CoffemaniaColors.PrimaryContainer,
    onPrimaryContainer = CoffemaniaColors.OnPrimaryContainer,
    error = CoffemaniaColors.Error,
    onError = CoffemaniaColors.OnError,
    errorContainer = CoffemaniaColors.ErrorContainer,
    onErrorContainer = CoffemaniaColors.OnErrorContainer,
    connectDisabledOuter = CoffemaniaColors.ConnectDisabledOuter,
    connectDisabledInner = CoffemaniaColors.ConnectDisabledInner,
    connectDisabledBorder = CoffemaniaColors.ConnectDisabledBorder,
    connectDisabledIcon = CoffemaniaColors.ConnectDisabledIcon,
)

fun darkCoffemaniaExtraColors(): CoffemaniaExtraColors = CoffemaniaExtraColors(
    milkFoam = Color(0xFF1A1210),
    espresso = Color(0xFFE8DED6),
    mocha = Color(0xFF9A8578),
    cappuccino = Color(0xFF2C221F),
    latte = Color(0xFF423530),
    background = Color(0xFF1A1210),
    onBackground = Color(0xFFE8DED6),
    surface = Color(0xFF1A1210),
    onSurface = Color(0xFFE8DED6),
    onSurfaceVariant = Color(0xFF9A8578),
    surfaceVariant = Color(0xFF2C221F),
    surfaceContainer = Color(0xFF2C221F),
    surfaceContainerHigh = Color(0xFF1A1210),
    surfaceContainerHighest = Color(0xFF423530),
    surfaceDim = Color(0xFF2C221F),
    outline = Color(0xFF423530),
    primary = Color(0xFFE8DED6),
    onPrimary = Color(0xFF1A1210),
    primaryContainer = Color(0xFF423530),
    onPrimaryContainer = Color(0xFFE8DED6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    connectDisabledOuter = Color(0xFF322824),
    connectDisabledInner = Color(0xFF2A201E),
    connectDisabledBorder = Color(0xFF4A3C36),
    connectDisabledIcon = Color(0xFF7A6A62),
)

private fun colorSchemeFrom(extra: CoffemaniaExtraColors, dark: Boolean) = if (!dark) {
    lightColorScheme(
        primary = extra.primary,
        onPrimary = extra.onPrimary,
        primaryContainer = extra.primaryContainer,
        onPrimaryContainer = extra.onPrimaryContainer,
        secondary = CoffemaniaColors.Secondary,
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
fun CoffemaniaTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val extra = if (useDarkTheme) darkCoffemaniaExtraColors() else lightCoffemaniaExtraColors()
    val scheme = colorSchemeFrom(extra, useDarkTheme)

    CompositionLocalProvider(LocalCoffemaniaExtraColors provides extra) {
        MaterialTheme(
            colorScheme = scheme,
            typography = CoffemaniaTypography,
            content = content,
        )
    }
}
