package online.coffemaniavpn.client.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import online.coffemaniavpn.client.R

@Composable
fun CoffeeLogo(
    modifier: Modifier = Modifier,
    contentDescription: String? = "КОФЕМАНИЯ ВПН",
) {
    Image(
        painter = painterResource(R.drawable.ic_coffee_bean),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun CoffeeLogoAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    logoScale: Float = 0.62f,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(CoffemaniaColors.SurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        CoffeeLogo(modifier = Modifier.size(size * logoScale))
    }
}
