package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.coffeemaniavpn.app.R

@Composable
fun CoffeeLogo(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    contentDescription: String? = "POROZOFF VPN",
) {
    Image(
        painter = painterResource(R.drawable.ic_logo),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = tint?.let { ColorFilter.tint(it) },
    )
}

@Composable
fun CoffeeLogoAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    CoffeeLogo(
        modifier = modifier.size(size),
        contentDescription = "POROZOFF VPN",
    )
}
