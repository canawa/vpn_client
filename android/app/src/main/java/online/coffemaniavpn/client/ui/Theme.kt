package online.coffemaniavpn.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object CoffemaniaColors {
    // Фоны
    val Surface = Color(0xFFFEF8F5)
    val SurfaceBright = Color(0xFFFEF8F5)
    val SurfaceDim = Color(0xFFDED9D6)
    val SurfaceContainer = Color(0xFFF4EEEB)
    val SurfaceContainerLow = Color(0xFFF8F2EF)

    // Акценты и текст
    val Primary = Color(0xFF422C26)
    val OnPrimary = Color(0xFFFEF8F5)
    val OnSurface = Color(0xFF201A18)
    val OnSurfaceVariant = Color(0xFF52443D)
    val Outline = Color(0xFF85736B)

    // Дополнительные
    val SecondaryContainer = Color(0xFFFCE0D4)
    val Tertiary = Color(0xFF5C6239)

    val Background = Surface
    val OnBackground = OnSurface
    val SurfaceVariant = SurfaceDim
    val SurfaceContainerLowest = SurfaceBright
    val SurfaceContainerHigh = SurfaceContainerLow
    val SurfaceContainerHighest = SurfaceDim

    val PrimaryContainer = Color(0xFFFCE0D4)
    val OnPrimaryContainer = Primary
    val PrimaryFixedDim = Color(0xFF6B4E45)

    val Secondary = Outline
    val OnSecondary = SurfaceBright
    val OnSecondaryContainer = OnSurfaceVariant

    val TertiaryContainer = Color(0xFFE8EBD4)
    val OnTertiary = SurfaceBright
    val OnTertiaryContainer = Tertiary

    val OutlineVariant = Color(0xFFDED9D6)

    val Error = Color(0xFFBA1A1A)
    val OnError = SurfaceBright
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)
}

data class CoffemaniaExtraColors(
    val surfaceContainerLow: Color = CoffemaniaColors.SurfaceContainerLow,
    val surfaceContainerLowest: Color = CoffemaniaColors.SurfaceContainerLowest,
    val surfaceContainerHigh: Color = CoffemaniaColors.SurfaceContainerHigh,
    val primaryFixedDim: Color = CoffemaniaColors.PrimaryFixedDim,
    val surfaceDim: Color = CoffemaniaColors.SurfaceDim,
    val surfaceBright: Color = CoffemaniaColors.SurfaceBright,
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
