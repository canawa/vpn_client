package online.coffemaniavpn.client.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoffeeBrown = Color(0xFF4E342E)
private val CoffeeCream = Color(0xFFD7CCC8)
private val CoffeeAccent = Color(0xFF8D6E63)
private val CoffeeDark = Color(0xFF1B120F)

private val ColorScheme = darkColorScheme(
    primary = CoffeeCream,
    onPrimary = CoffeeDark,
    secondary = CoffeeAccent,
    background = CoffeeDark,
    surface = CoffeeBrown,
    onBackground = CoffeeCream,
    onSurface = CoffeeCream,
)

@Composable
fun CoffemaniaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        content = content,
    )
}
