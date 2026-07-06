package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun ServerListFlag(
    flag: String,
    modifier: Modifier = Modifier,
    height: Dp = 30.dp,
) {
    ServerFlag(
        flag = flag,
        modifier = modifier,
        height = height,
        lightweight = true,
    )
}

@Composable
fun ServerFlag(
    flag: String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    crossfade: Boolean = false,
    showShadow: Boolean = false,
    lightweight: Boolean = false,
) {
    val countryCode = remember(flag) { FlagUtils.emojiToCountryCode(flag) }
    val width = if (countryCode != null) height * 4 / 3 else height
    val shape = RoundedCornerShape(if (lightweight) 6.dp else 8.dp)
    val colors = nuboColors()
    val context = LocalContext.current
    val density = LocalDensity.current
    val widthPx = remember(width, density) { with(density) { width.roundToPx() } }
    val heightPx = remember(height, density) { with(density) { height.roundToPx() } }
    val cacheKey = countryCode ?: flag

    val imageRequest = remember(flag, widthPx, heightPx, cacheKey) {
        ImageRequest.Builder(context)
            .data(FlagUtils.flagImageUrl(flag, pixelWidth = widthPx))
            .size(widthPx, heightPx)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .crossfade(crossfade && !lightweight)
            .allowHardware(true)
            .build()
    }

    val containerModifier = modifier
        .width(width)
        .height(height)
        .then(
            if (showShadow && !lightweight) {
                Modifier.shadow(
                    elevation = 4.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.35f),
                    spotColor = Color.Black.copy(alpha = 0.45f),
                )
            } else {
                Modifier
            },
        )
        .clip(shape)
        .border(
            width = 1.dp,
            color = if (lightweight) colors.border.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.22f),
            shape = shape,
        )
        .background(colors.cardHigh, shape)

    AsyncImage(
        model = imageRequest,
        contentDescription = flag,
        modifier = containerModifier,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(colors.cardHigh),
        error = ColorPainter(colors.cardHigh),
    )
}

@Composable
internal fun FlagEmojiFallback(
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
