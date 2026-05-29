package online.coffemaniavpn.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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
}

data class CoffemaniaExtraColors(
    val milkFoam: Color = CoffemaniaColors.MilkFoam,
    val espresso: Color = CoffemaniaColors.Espresso,
    val mocha: Color = CoffemaniaColors.Mocha,
    val cappuccino: Color = CoffemaniaColors.Cappuccino,
    val latte: Color = CoffemaniaColors.Latte,
)

val LocalCoffemaniaExtraColors = staticCompositionLocalOf { CoffemaniaExtraColors() }

private val LightScheme = lightColorScheme(
    primary = CoffemaniaColors.Primary,
    onPrimary = CoffemaniaColors.OnPrimary,
    primaryContainer = CoffemaniaColors.PrimaryContainer,
    onPrimaryContainer = CoffemaniaColors.OnPrimaryContainer,
    secondary = CoffemaniaColors.Secondary,
    onSecondary = CoffemaniaColors.OnSecondary,
    secondaryContainer = CoffemaniaColors.SecondaryContainer,
    onSecondaryContainer = CoffemaniaColors.OnSecondaryContainer,
    tertiary = CoffemaniaColors.Tertiary,
    onTertiary = CoffemaniaColors.OnTertiary,
    tertiaryContainer = CoffemaniaColors.TertiaryContainer,
    onTertiaryContainer = CoffemaniaColors.OnTertiaryContainer,
    background = CoffemaniaColors.Background,
    onBackground = CoffemaniaColors.OnBackground,
    surface = CoffemaniaColors.Surface,
    onSurface = CoffemaniaColors.OnSurface,
    surfaceVariant = CoffemaniaColors.SurfaceVariant,
    onSurfaceVariant = CoffemaniaColors.OnSurfaceVariant,
    outline = CoffemaniaColors.Outline,
    outlineVariant = CoffemaniaColors.OutlineVariant,
    error = CoffemaniaColors.Error,
    onError = CoffemaniaColors.OnError,
    errorContainer = CoffemaniaColors.ErrorContainer,
    onErrorContainer = CoffemaniaColors.OnErrorContainer,
    surfaceContainer = CoffemaniaColors.SurfaceContainer,
    surfaceContainerLow = CoffemaniaColors.SurfaceContainerLow,
    surfaceContainerHigh = CoffemaniaColors.SurfaceContainerHigh,
    surfaceContainerHighest = CoffemaniaColors.SurfaceContainerHighest,
    surfaceContainerLowest = CoffemaniaColors.SurfaceContainerLowest,
    surfaceDim = CoffemaniaColors.SurfaceDim,
    surfaceBright = CoffemaniaColors.SurfaceBright,
)

@Composable
fun CoffemaniaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = CoffemaniaTypography,
        content = content,
    )
}
