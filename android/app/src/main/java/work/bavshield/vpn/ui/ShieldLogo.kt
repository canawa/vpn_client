package work.bavshield.vpn.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import work.bavshield.vpn.R

@Composable
fun ShieldLogo(
    modifier: Modifier = Modifier,
    tint: Color = bavShieldColors().espresso,
    filled: Boolean = false,
    contentDescription: String? = stringResource(R.string.app_name),
) {
    Image(
        painter = painterResource(
            if (filled) R.drawable.ic_shield_filled else R.drawable.ic_shield,
        ),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
    )
}

@Composable
fun ShieldLogoAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    logoScale: Float = 0.62f,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bavShieldColors().cappuccino),
        contentAlignment = Alignment.Center,
    ) {
        ShieldLogo(modifier = Modifier.size(size * logoScale))
    }
}
