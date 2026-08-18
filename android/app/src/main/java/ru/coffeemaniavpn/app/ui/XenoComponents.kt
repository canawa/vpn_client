package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.SubscriptionInfo
import ru.coffeemaniavpn.app.data.formatTrafficBytes

enum class XenoTab { Home, Servers, Settings }

@Composable
fun XenoLogoMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    inCapsule: Boolean = false,
) {
    val useCompact = compact || inCapsule
    val square = if (useCompact) 10.dp else 12.dp
    val offset = if (useCompact) 3.dp else 4.dp
    val markSize = square + offset
    val content = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (inCapsule) 5.dp else if (useCompact) 7.dp else 8.dp),
        ) {
            if (inCapsule) {
                // Figma 24×24 backing + nested mark (outer #075243 16, inner #00D4A8 8)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0A0D0C)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFF075243)),
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF00D4A8)),
                    )
                }
            } else {
                Box(modifier = Modifier.size(markSize)) {
                    Box(
                        modifier = Modifier
                            .padding(top = offset, start = offset)
                            .size(square)
                            .background(Color(0xFF04342C)),
                    )
                    Box(
                        modifier = Modifier
                            .size(square)
                            .background(Color(0xFF00E091)),
                    )
                }
            }
            Text(
                text = "XENO",
                color = Color(0xFFF2F5F4),
                fontFamily = BytesizedFamily,
                fontWeight = FontWeight.Normal,
                fontSize = when {
                    inCapsule -> 30.sp
                    useCompact -> 18.sp
                    else -> 28.sp
                },
                lineHeight = when {
                    inCapsule -> 30.sp
                    useCompact -> 18.sp
                    else -> 28.sp
                },
                letterSpacing = if (inCapsule) 0.6.sp else 1.sp,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
    if (inCapsule) {
        // Figma: 101×40, r12, bg #141B18, border #222B28; logo inset 5×8
        Box(
            modifier = modifier
                .width(101.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141B18))
                .border(1.dp, Color(0xFF222B28), RoundedCornerShape(12.dp))
                .padding(start = 5.dp, end = 8.dp),
            contentAlignment = Alignment.CenterStart,
        ) { content() }
    } else {
        Box(modifier = modifier) { content() }
    }
}

@Composable
fun XenoGridIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141B18))
            .border(1.dp, Color(0xFF222B28), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Figma grid: 5×5 cells — TL/BR teal, TR/BL grey
        val teal = Color(0xFF00D4A8)
        val grey = Color(0xFF566460)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(teal))
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(grey))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(grey))
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(teal))
            }
        }
    }
}

@Composable
fun XenoHeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(shape)
            .background(Color(0xFF141B18))
            .border(1.dp, Color(0xFF222B28), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) Color(0xFF00D4A8) else Color(0xFF566460),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun XenoScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFFF2F5F4),
                fontFamily = OswaldFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 32.sp,
                letterSpacing = 1.2.sp,
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    color = Color(0xFF6B7672),
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun XenoBottomNav(
    selected: XenoTab,
    onSelect: (XenoTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(30.dp)
    // Figma: 177×60, r=30, border #FFFFFF1F, glass bg, shadow 0 14 30 -8 #00000073
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .width(177.dp)
                .height(60.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = shape,
                    clip = false,
                    ambientColor = Color(0x73000000),
                    spotColor = Color(0x73000000),
                )
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x8C0E1513), // rgba(14, 21, 19, 0.55)
                            Color(0x8C0E1513),
                        ),
                    ),
                )
                .background(Color(0x0DFFFFFF)) // rgba(255,255,255,0.05)
                .border(1.dp, Color(0x1FFFFFFF), shape)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            XenoNavItem(Icons.Default.Shield, selected == XenoTab.Home) { onSelect(XenoTab.Home) }
            XenoNavItem(Icons.Default.Language, selected == XenoTab.Servers) { onSelect(XenoTab.Servers) }
            XenoNavItem(Icons.Default.Tune, selected == XenoTab.Settings) { onSelect(XenoTab.Settings) }
        }
    }
}

@Composable
private fun XenoNavItem(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Figma selected (shield): 46×38, r=14, bg #00D4A829, border #00D4A880
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(38.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier
                        .background(Color(0x2900D4A8))
                        .border(1.dp, Color(0x8000D4A8), shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) Color(0xFF00D4A8) else Color(0xFF7A7F78),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun XenoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    content: @Composable () -> Unit,
) {
    // Figma: plate #141B18, border #222B28, r16
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF141B18))
            .border(1.dp, borderColor ?: Color(0xFF222B28), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
fun XenoStatusBadge(
    text: String,
    active: Boolean = true,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    // Figma: r11, pad 4/8, bg #00D4A829, border #00D4A880 — width wraps label
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .height(20.dp)
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(Color(0xFF00E091))
                } else {
                    Modifier
                        .background(
                            if (active) Color(0x2900D4A8) else Color.Transparent,
                        )
                        .border(
                            1.dp,
                            if (active) Color(0x8000D4A8) else Color(0xFF252525),
                            shape,
                        )
                },
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = when {
                filled -> Color.Black
                active -> Color(0xFF00D4A8)
                else -> Color(0xFF7A7F78)
            },
            fontFamily = JetBrainsMonoFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            letterSpacing = 0.54.sp,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun XenoCountryTile(
    code: String,
    modifier: Modifier = Modifier,
) {
    val countryCode = remember(code) { FlagUtils.resolveCountryCodeOrDefault(code) }
    val assetPath = remember(countryCode) { FlagUtils.flagAssetPath(countryCode) }
    val shape = RoundedCornerShape(10.dp)
    val context = LocalContext.current
    val density = LocalDensity.current
    val px = with(density) { 42.dp.roundToPx().coerceAtLeast(1) }
    val request = remember(assetPath, px, countryCode) {
        ImageRequest.Builder(context)
            .data(assetPath)
            .size(Size(px, px))
            .memoryCacheKey("xeno-flag-$countryCode-$px")
            .diskCachePolicy(CachePolicy.DISABLED)
            .allowHardware(true)
            .build()
    }

    Box(
        modifier = modifier
            .size(42.dp)
            .clip(shape)
            .background(Color(0xFF121A17))
            .border(1.dp, Color(0xFF222B28), shape),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = request,
            contentDescription = countryCode.uppercase(),
            modifier = Modifier
                .fillMaxSize()
                .clip(shape),
            contentScale = ContentScale.Crop,
            error = ColorPainter(Color(0xFF04342C)),
            placeholder = ColorPainter(Color(0xFF121A17)),
        )
    }
}

@Composable
fun XenoSignalBars(
    strength: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    // Figma: 4×6, 4×10, 4×14, r1, gap 3, #00D4A8 when lit
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val heights = listOf(6.dp, 10.dp, 14.dp)
        heights.forEachIndexed { index, h ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (index < strength) color else color.copy(alpha = 0.25f),
                    ),
            )
        }
    }
}

/** 3 ≤100 ms · 2 ≤200 ms · 1 ≤300 ms · иначе 1. */
fun xenoPingSignalStrength(pingMs: Int): Int = when {
    pingMs <= 100 -> 3
    pingMs <= 200 -> 2
    else -> 1
}

@Composable
fun XenoServerCard(
    flagCode: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    pingMs: Int? = null,
    connected: Boolean = false,
) {
    // Figma: 345×78, r=18, bg #121A17, border #222B28
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .clip(shape)
            .background(Color(0xFF121A17))
            .border(1.dp, Color(0xFF222B28), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XenoCountryTile(flagCode)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color(0xFFF2F5F4),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = Color(0xFF7A7F78),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.5.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (connected && pingMs != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    XenoSignalBars(
                        strength = xenoPingSignalStrength(pingMs),
                        color = Color(0xFF00D4A8),
                    )
                    Text(
                        text = "$pingMs ms",
                        color = Color(0xFF00D4A8),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(Color(0xFF7A7F78)),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.xeno_select),
                        color = Color(0xFF7A7F78),
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun XenoSubscriptionCard(
    info: SubscriptionInfo?,
    hasSubscription: Boolean,
    modifier: Modifier = Modifier,
) {
    // Figma: 345×96, r=18, bg #121A17, border #222B28
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(shape)
            .background(Color(0xFF121A17))
            .border(1.dp, Color(0xFF222B28), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.xeno_subscription_label),
                    color = Color(0xFF7A7F78),
                    fontFamily = InterFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
                if (hasSubscription && info != null && !info.isExpired()) {
                    XenoStatusBadge(text = stringResource(R.string.xeno_active_badge))
                }
            }
            if (!hasSubscription || info == null) {
                Text(
                    text = stringResource(R.string.xeno_sub_not_imported),
                    color = Color.White,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    if (info.isDisplayUnlimitedTraffic()) {
                        Text(
                            text = stringResource(R.string.subscription_traffic_unlimited),
                            color = Color.White,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        )
                    } else if (info.total > 0) {
                        val remaining = (info.total - info.used).coerceAtLeast(0)
                        val label = formatTrafficBytes(remaining)
                        val parts = label.split(' ', limit = 2)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = parts.getOrElse(0) { label },
                                color = Color.White,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                lineHeight = 28.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    R.string.xeno_traffic_left,
                                    parts.getOrNull(1) ?: "GB",
                                ),
                                color = Color(0xFF7A7F78),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 3.dp),
                            )
                        }
                    } else {
                        Text(
                            text = formatTrafficBytes(info.used),
                            color = Color.White,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        xenoShortExpireDate(info)?.let { date ->
                            Text(
                                text = stringResource(R.string.xeno_expires_prefix, date),
                                color = Color(0xFF6B7672),
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                lineHeight = 12.sp,
                                letterSpacing = 0.sp,
                                textAlign = TextAlign.Right,
                                modifier = Modifier
                                    .width(183.dp)
                                    .height(15.dp),
                            )
                        }
                        xenoDaysLeftLabel(info)?.let { remaining ->
                            Text(
                                text = remaining,
                                color = Color(0xFF00D4A8),
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                lineHeight = 11.sp,
                                letterSpacing = 0.22.sp, // 2% of 11
                                textAlign = TextAlign.Right,
                                modifier = Modifier
                                    .width(183.dp)
                                    .height(15.dp),
                            )
                        }
                    }
                }
                if (info.total > 0 && !info.isDisplayUnlimitedTraffic()) {
                    val remaining = (1f - info.usageFraction).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF222B28)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(remaining)
                                .fillMaxSize()
                                .background(Color(0xFF00D4A8)),
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

private fun xenoShortExpireDate(info: SubscriptionInfo): String? {
    if (info.expire <= 0) return null
    return java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.ENGLISH)
        .format(java.util.Date(info.expire * 1_000L))
}

@Composable
private fun xenoDaysLeftLabel(info: SubscriptionInfo): String? {
    if (info.expire <= 0) return null
    val nowSec = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
    val remainingSec = info.expire - nowSec
    if (remainingSec <= 0) return null
    val days = (remainingSec + 86_399L) / 86_400L
    return if (days == 1L) {
        stringResource(R.string.xeno_day_left)
    } else {
        stringResource(R.string.xeno_days_left, days)
    }
}

@Composable
fun XenoPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (filled) {
                    Modifier.background(if (enabled) colors.primary else colors.primary.copy(alpha = 0.4f))
                } else {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.dp, colors.primary.copy(alpha = 0.7f), shape)
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (trailing == null) {
            Text(
                text = text,
                color = if (filled) Color.Black else colors.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                fontFamily = if (!filled) FontFamily.Monospace else FontFamily.Default,
                letterSpacing = if (!filled) 1.sp else 0.sp,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = text,
                    color = if (filled) Color.Black else colors.espresso,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                trailing()
            }
        }
    }
}

@Composable
fun XenoOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Color.Transparent)
            .border(1.dp, Color(0xFF2A2A2A), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = Color(0xFF00E091),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 1.2.sp,
        )
    }
}

@Composable
fun XenoDashedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingAccent: String? = "?",
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(47.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 0.5dp часто пропадает на dpi — рисуем ≥1px, визуально как тонкий stroke
            val strokeWidth = maxOf(0.5.dp.toPx(), 1f)
            val dash = 4.dp.toPx()
            val inset = strokeWidth / 2f
            drawRoundRect(
                color = Color(0xFF6E6E6E),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(
                    size.width - strokeWidth,
                    size.height - strokeWidth,
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
                style = Stroke(
                    width = strokeWidth,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash), 0f),
                ),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            leadingAccent?.let {
                Text(
                    text = it,
                    color = Color(0xFF00E091),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
            Text(
                text = text,
                color = Color(0xFF9A9A9A),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
            )
        }
    }
}
