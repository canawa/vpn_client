package online.coffemaniavpn.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

@Composable
fun ServerFlag(
    flag: String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
) {
    val countryCode = FlagUtils.emojiToCountryCode(flag)
    val width = if (countryCode != null) height * 4 / 3 else height

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(FlagUtils.flagImageUrl(flag))
            .crossfade(true)
            .build(),
        contentDescription = flag,
        modifier = modifier
            .width(width)
            .height(height)
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(6.dp), clip = false)
            .clip(RoundedCornerShape(6.dp)),
        contentScale = ContentScale.Crop,
        loading = { FlagEmojiFallback(flag, width, height) },
        error = { FlagEmojiFallback(flag, width, height) },
    )
}

@Composable
private fun FlagEmojiFallback(
    flag: String,
    width: Dp,
    height: Dp,
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = flag,
            fontSize = (height.value * 0.72f).sp,
            lineHeight = (height.value * 0.72f).sp,
            textAlign = TextAlign.Center,
        )
    }
}
