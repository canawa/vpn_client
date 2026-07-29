package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size

@Composable
fun ServerListFlag(
    flag: String,
    modifier: Modifier = Modifier,
    height: Dp = 34.dp,
) {
    val countryCode = remember(flag) { FlagUtils.resolveCountryCode(flag) }
    val assetPath = remember(flag) { FlagUtils.flagAssetPath(flag) }
    val width = height * 3 / 2
    val shape = RoundedCornerShape(6.dp)
    val colors = coffemaniaColors()
    val density = LocalDensity.current
    val pixelWidth = with(density) { width.roundToPx().coerceAtLeast(1) }
    val pixelHeight = with(density) { height.roundToPx().coerceAtLeast(1) }
    val context = LocalContext.current

    val imageModifier = modifier
        .width(width)
        .height(height)
        .clip(shape)
        .border(1.dp, colors.latte, shape)
        .background(colors.cappuccino)

    if (assetPath == null) {
        FlagFallback(
            countryCode = null,
            modifier = imageModifier,
            height = height,
        )
        return
    }

    // Лёгкий AsyncImage без Subcompose — иначе список лагает при быстром скролле.
    val request = remember(assetPath, pixelWidth, pixelHeight, countryCode) {
        ImageRequest.Builder(context)
            .data(assetPath)
            .size(Size(pixelWidth, pixelHeight))
            .memoryCacheKey("flag-list-$countryCode-${pixelWidth}x$pixelHeight")
            .diskCachePolicy(CachePolicy.DISABLED)
            .allowHardware(true)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = countryCode?.uppercase() ?: "Флаг",
        modifier = imageModifier,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(colors.latte),
        error = ColorPainter(colors.latte),
    )
}

@Composable
fun ServerFlag(
    flag: String,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    crossfade: Boolean = true,
    showShadow: Boolean = false,
    showBorder: Boolean = true,
    cornerRadius: Dp = 8.dp,
) {
    val countryCode = remember(flag) { FlagUtils.resolveCountryCode(flag) }
    val assetPath = remember(flag) { FlagUtils.flagAssetPath(flag) }
    val width = height * 3 / 2
    val shape = RoundedCornerShape(cornerRadius)
    val colors = coffemaniaColors()
    val density = LocalDensity.current
    val pixelWidth = with(density) { width.roundToPx().coerceAtLeast(1) }
    val pixelHeight = with(density) { height.roundToPx().coerceAtLeast(1) }
    val context = LocalContext.current

    val imageModifier = modifier
        .width(width)
        .height(height)
        .then(
            if (showShadow) {
                Modifier.shadow(elevation = 4.dp, shape = shape, clip = false)
            } else {
                Modifier
            },
        )
        .clip(shape)
        .then(
            if (showBorder) {
                Modifier.border(1.dp, colors.latte, shape)
            } else {
                Modifier
            },
        )
        .background(colors.cappuccino)

    if (assetPath == null) {
        FlagFallback(
            countryCode = null,
            modifier = imageModifier,
            height = height,
        )
        return
    }

    val request = remember(assetPath, pixelWidth, pixelHeight, countryCode, crossfade) {
        ImageRequest.Builder(context)
            .data(assetPath)
            .size(Size(pixelWidth, pixelHeight))
            .crossfade(if (crossfade) 160 else 0)
            .memoryCacheKey("flag-asset-$countryCode-${pixelWidth}x$pixelHeight")
            .diskCachePolicy(CachePolicy.DISABLED)
            .allowHardware(true)
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = countryCode?.uppercase() ?: "Флаг",
        modifier = imageModifier,
        contentScale = ContentScale.Crop,
        loading = {
            FlagFallback(
                countryCode = countryCode,
                modifier = Modifier
                    .width(width)
                    .height(height),
                height = height,
                muted = true,
            )
        },
        error = {
            FlagFallback(
                countryCode = countryCode,
                modifier = Modifier
                    .width(width)
                    .height(height),
                height = height,
            )
        },
    )
}

@Composable
private fun FlagFallback(
    countryCode: String?,
    modifier: Modifier = Modifier,
    height: Dp,
    muted: Boolean = false,
) {
    val colors = coffemaniaColors()
    Box(
        modifier = modifier
            .background(if (muted) colors.latte.copy(alpha = 0.7f) else colors.latte),
        contentAlignment = Alignment.Center,
    ) {
        if (!countryCode.isNullOrBlank()) {
            Text(
                text = countryCode.uppercase(),
                color = colors.espresso.copy(alpha = if (muted) 0.45f else 0.85f),
                fontWeight = FontWeight.Bold,
                fontSize = (height.value * 0.34f).sp,
                letterSpacing = 0.6.sp,
                maxLines = 1,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = colors.mocha.copy(alpha = if (muted) 0.55f else 1f),
                modifier = Modifier.size(height * 0.55f),
            )
        }
    }
}
