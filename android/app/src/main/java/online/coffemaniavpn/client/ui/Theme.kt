package online.coffemaniavpn.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object CoffemaniaColors {
    val Background = Color(0xFFFFF8F5)
    val OnBackground = Color(0xFF26190D)
    val Primary = Color(0xFF33210D)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFF4B3621)
    val OnPrimaryContainer = Color(0xFFBD9F83)
    val PrimaryFixedDim = Color(0xFFE1C1A4)
    val Secondary = Color(0xFF725A39)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFFBDBB0)
    val OnSecondaryContainer = Color(0xFF765F3D)
    val Surface = Color(0xFFFFF8F5)
    val OnSurface = Color(0xFF26190D)
    val SurfaceVariant = Color(0xFFF8DECA)
    val OnSurfaceVariant = Color(0xFF4E453D)
    val SurfaceContainer = Color(0xFFFFEADC)
    val SurfaceContainerLow = Color(0xFFFFF1E8)
    val SurfaceContainerLowest = Color(0xFFFFFFFF)
    val SurfaceContainerHigh = Color(0xFFFEE3D0)
    val SurfaceContainerHighest = Color(0xFFF8DECA)
    val Outline = Color(0xFF80756C)
    val OutlineVariant = Color(0xFFD2C4BA)
    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
    val OnErrorContainer = Color(0xFF93000A)
}

data class CoffemaniaExtraColors(
    val surfaceContainerLow: Color = CoffemaniaColors.SurfaceContainerLow,
    val surfaceContainerLowest: Color = CoffemaniaColors.SurfaceContainerLowest,
    val surfaceContainerHigh: Color = CoffemaniaColors.SurfaceContainerHigh,
    val primaryFixedDim: Color = CoffemaniaColors.PrimaryFixedDim,
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
)

@Composable
fun CoffemaniaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = CoffemaniaTypography,
        content = content,
    )
}
