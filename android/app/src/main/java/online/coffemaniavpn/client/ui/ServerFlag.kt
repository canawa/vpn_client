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
fun ServerListFlag(
    flag: String,
    modifier: Modifier = Modifier,
    height: Dp = 32.dp,
) {
    val countryCode = FlagUtils.emojiToCountryCode(flag)
    val width = if (countryCode != null) height * 4 / 3 else height

    Box(
        modifier = modifier
            .width(width)
            .height(height),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = flag,
            fontSize = (height.value * 0.78f).sp,
            lineHeight = (height.value * 0.78f).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun ServerFlag(
    flag: String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    crossfade: Boolean = false,
    showShadow: Boolean = false,
) {
    val countryCode = FlagUtils.emojiToCountryCode(flag)
    val width = if (countryCode != null) height * 4 / 3 else height
    val shape = RoundedCornerShape(6.dp)
    val imageModifier = modifier
        .width(width)
        .height(height)
        .then(
            if (showShadow) {
                Modifier.shadow(elevation = 2.dp, shape = shape, clip = false)
            } else {
                Modifier
            },
        )
        .clip(shape)

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(FlagUtils.flagImageUrl(flag))
            .crossfade(crossfade)
            .memoryCacheKey(countryCode ?: flag)
            .diskCacheKey(countryCode ?: flag)
            .build(),
        contentDescription = flag,
        modifier = imageModifier,
        contentScale = ContentScale.Crop,
        loading = { FlagEmojiFallback(flag = flag, width = width, height = height) },
        error = { FlagEmojiFallback(flag = flag, width = width, height = height) },
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
