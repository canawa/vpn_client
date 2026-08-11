package ru.coffeemaniavpn.app.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.data.SubscriptionInfo
import ru.coffeemaniavpn.app.data.formatTrafficBytes

enum class XenoTab { Home, Servers, Settings }

@Composable
fun XenoLogoMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    inCapsule: Boolean = false,
) {
    val colors = coffemaniaColors()
    val square = if (compact) 10.dp else 12.dp
    val offset = if (compact) 3.dp else 4.dp
    val markSize = square + offset
    val content = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(markSize)) {
                // Тёмный квадрат: top 4 / left 4
                Box(
                    modifier = Modifier
                        .padding(top = offset, start = offset)
                        .size(square)
                        .background(Color(0xFF04342C)),
                )
                // Яркий квадрат чуть выше и левее (0, 0)
                Box(
                    modifier = Modifier
                        .size(square)
                        .background(Color(0xFF00E091)),
                )
            }
            Text(
                text = "XENO",
                color = colors.espresso,
                fontWeight = FontWeight.Medium,
                fontSize = if (compact) 14.sp else 22.sp,
                lineHeight = if (compact) 14.sp else 22.sp,
                letterSpacing = 2.sp,
                fontFamily = JetBrainsMonoFamily,
            )
        }
    }
    if (inCapsule) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.cappuccino)
                .border(1.dp, colors.latte, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
    val colors = coffemaniaColors()
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cappuccino)
            .border(1.dp, colors.latte, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(colors.primary))
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(colors.primary.copy(alpha = 0.45f)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(colors.primary.copy(alpha = 0.45f)))
                Box(Modifier.size(5.dp).clip(RoundedCornerShape(1.dp)).background(colors.primary.copy(alpha = 0.45f)))
            }
        }
    }
}

@Composable
fun XenoScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            XenoLogoMark(compact = true)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = colors.espresso,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                letterSpacing = 1.2.sp,
            )
            subtitle?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = it, color = colors.mocha, fontSize = 13.sp)
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
    val colors = coffemaniaColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF151515))
            .border(1.dp, colors.latte, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        XenoNavItem(Icons.Default.Shield, selected == XenoTab.Home) { onSelect(XenoTab.Home) }
        XenoNavItem(Icons.Default.Language, selected == XenoTab.Servers) { onSelect(XenoTab.Servers) }
        XenoNavItem(Icons.Default.Tune, selected == XenoTab.Settings) { onSelect(XenoTab.Settings) }
    }
}

@Composable
private fun XenoNavItem(
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = coffemaniaColors()
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(
                if (selected) {
                    Modifier
                        .background(colors.cappuccino)
                        .border(1.5.dp, colors.primary, RoundedCornerShape(14.dp))
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
            tint = if (selected) colors.primary else colors.mocha,
            modifier = Modifier.size(22.dp),
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
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cappuccino)
            .border(1.dp, borderColor ?: colors.latte, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
    ) { content() }
}

@Composable
fun XenoStatusBadge(
    text: String,
    active: Boolean = true,
    filled: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Text(
        text = text.uppercase(),
        color = if (filled) Color.Black else if (active) colors.primary else colors.mocha,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .then(
                if (filled) Modifier.background(colors.primary)
                else Modifier.border(1.dp, if (active) colors.primary else colors.latte, RoundedCornerShape(999.dp)),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
fun XenoCountryTile(
    code: String,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF102820))
            .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = code.uppercase().take(2),
            color = colors.primary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
fun XenoSignalBars(
    strength: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        val heights = listOf(6.dp, 9.dp, 12.dp, 15.dp)
        heights.forEachIndexed { index, h ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (index < strength) color else color.copy(alpha = 0.25f)),
            )
        }
    }
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
    val colors = coffemaniaColors()
    XenoCard(modifier = modifier, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XenoCountryTile(flagCode)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.espresso,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    color = colors.mocha,
                    fontSize = 12.sp,
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
                        strength = when {
                            pingMs < 50 -> 4
                            pingMs < 100 -> 3
                            pingMs < 180 -> 2
                            else -> 1
                        },
                        color = colors.primary,
                    )
                    Text(
                        text = "$pingMs ms",
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Box(Modifier.width(14.dp).height(2.dp).background(colors.mocha))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "select", color = colors.mocha, fontSize = 11.sp)
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
    val colors = coffemaniaColors()
    XenoCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SUBSCRIPTION",
                    color = colors.mocha,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
                if (hasSubscription && info != null && !info.isExpired()) {
                    XenoStatusBadge(text = "ACTIVE")
                }
            }
            if (!hasSubscription || info == null) {
                Text(
                    text = "Не импортирована",
                    color = colors.espresso,
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
                            text = "безлимит",
                            color = colors.espresso,
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
                                color = colors.espresso,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = (parts.getOrNull(1) ?: "GB") + " left",
                                color = colors.mocha,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                    } else {
                        Text(
                            text = formatTrafficBytes(info.used),
                            color = colors.espresso,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        info.expireCalendarLabel()?.let { date ->
                            Text(text = "Expires $date", color = colors.mocha, fontSize = 11.sp)
                        }
                        info.expireLabel()?.let { remaining ->
                            Text(
                                text = remaining,
                                color = colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                if (info.total > 0 && !info.isDisplayUnlimitedTraffic()) {
                    LinearProgressIndicator(
                        progress = { 1f - info.usageFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = colors.primary,
                        trackColor = colors.latte,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
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
    XenoPrimaryButton(text = text, onClick = onClick, modifier = modifier, filled = false)
}

@Composable
fun XenoDashedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingAccent: String? = "?",
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = colors.latte,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            leadingAccent?.let {
                Text(text = it, color = colors.primary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
            }
            Text(text = text, color = colors.mocha, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
