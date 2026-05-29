package online.coffemaniavpn.client.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ServerFlagAvatar(
    flag: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    background: Color = CoffemaniaColors.SurfaceContainerLowest,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .border(1.dp, CoffemaniaColors.OutlineVariant.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = flag,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = (size.value * 0.45f).sp,
                lineHeight = (size.value * 0.45f).sp,
            ),
        )
    }
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
