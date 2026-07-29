package ru.coffeemaniavpn.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Палитра porozoffvpn.ru (figma-colors.json):
 * #EDE0E2 фон, #0E0810 / #060408 текст и тёмный фон,
 * #7A5F65 / #A07880 приглушённый розовый, #E8213A акцент,
 * #22C55E / #F59E0B / #FFFFFF служебные.
 */
object CoffemaniaColors {
    /** Светлый розово-молочный фон сайта */
    val MilkFoam = Color(0xFFEDE0E2)

    /** Почти чёрный — заголовки, иконки, акцентный текст */
    val Espresso = Color(0xFF0E0810)

    /** Приглушённый розово-коричневый — подписи, неактивные элементы */
    val Mocha = Color(0xFF7A5F65)

    /** Карточки, tab bar (белый на светлом фоне) */
    val Cappuccino = Color(0xFFFFFFFF)

    /** Обводки, кнопки в карточках, фон активной вкладки */
    val Latte = Color(0xFFDCC8CC)

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

    /** Брендовый красный porozoff */
    val BrandRed = Color(0xFFE8213A)

    val Error = BrandRed
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFD6DC)
    val OnErrorContainer = Color(0xFF7A0014)

    /** Пинг ≤ 200 ms */
    val PingGood = Color(0xFF22C55E)

    /** Пинг 201–400 ms */
    val PingMedium = Color(0xFFF59E0B)

    /** Пинг > 400 ms или недоступен */
    val PingBad = BrandRed

    /** Неактивная кнопка подключения (нет подписки) */
    val ConnectDisabledOuter = Color(0xFFE0D4D6)
    val ConnectDisabledInner = Color(0xFFEDE0E2)
    val ConnectDisabledBorder = Color(0xFFC9B0B4)
    val ConnectDisabledIcon = Color(0xFFA07880)

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
    milkFoam = Color(0xFF060408),
    espresso = Color(0xFFEDE0E2),
    mocha = Color(0xFFA07880),
    cappuccino = Color(0xFF0E0810),
    latte = Color(0xFF2A1820),
    background = Color(0xFF060408),
    onBackground = Color(0xFFEDE0E2),
    surface = Color(0xFF060408),
    onSurface = Color(0xFFEDE0E2),
    onSurfaceVariant = Color(0xFFA07880),
    surfaceVariant = Color(0xFF0E0810),
    surfaceContainer = Color(0xFF0E0810),
    surfaceContainerHigh = Color(0xFF060408),
    surfaceContainerHighest = Color(0xFF2A1820),
    surfaceDim = Color(0xFF0E0810),
    outline = Color(0xFF2A1820),
    primary = Color(0xFFEDE0E2),
    onPrimary = Color(0xFF060408),
    primaryContainer = Color(0xFF2A1820),
    onPrimaryContainer = Color(0xFFEDE0E2),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF060408),
    errorContainer = Color(0xFF7A0014),
    onErrorContainer = Color(0xFFFFD6DC),
    connectDisabledOuter = Color(0xFF1A1216),
    connectDisabledInner = Color(0xFF120E10),
    connectDisabledBorder = Color(0xFF3D2A30),
    connectDisabledIcon = Color(0xFF7A5F65),
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
    content: @Composable () -> Unit,
) {
    val extra = darkCoffemaniaExtraColors()
    val scheme = colorSchemeFrom(extra, dark = true)

    CompositionLocalProvider(LocalCoffemaniaExtraColors provides extra) {
        MaterialTheme(
            colorScheme = scheme,
            typography = CoffemaniaTypography,
            content = content,
        )
    }
}
