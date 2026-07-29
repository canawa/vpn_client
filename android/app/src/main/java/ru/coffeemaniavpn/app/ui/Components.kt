package ru.coffeemaniavpn.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.SubscriptionInfo
import ru.coffeemaniavpn.app.vpn.VpnStatus

enum class AppTab { Home, Servers }

@Composable
fun CoffemaniaSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = coffemaniaColors().milkFoam,
            checkedTrackColor = coffemaniaColors().espresso,
            checkedBorderColor = coffemaniaColors().espresso,
            uncheckedThumbColor = coffemaniaColors().milkFoam,
            uncheckedTrackColor = coffemaniaColors().latte,
            uncheckedBorderColor = coffemaniaColors().espresso,
            disabledCheckedThumbColor = coffemaniaColors().milkFoam.copy(alpha = 0.7f),
            disabledCheckedTrackColor = coffemaniaColors().mocha.copy(alpha = 0.5f),
            disabledCheckedBorderColor = coffemaniaColors().mocha.copy(alpha = 0.5f),
            disabledUncheckedThumbColor = coffemaniaColors().cappuccino,
            disabledUncheckedTrackColor = coffemaniaColors().latte.copy(alpha = 0.7f),
            disabledUncheckedBorderColor = coffemaniaColors().mocha,
        ),
    )
}

@Composable
fun CoffemaniaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    showSettingsButton: Boolean = true,
    onSettingsClick: () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = coffemaniaColors().milkFoam,
        shadowElevation = 0.dp,
    ) {
        val horizontalPadding = if (showBackButton) 12.dp else 24.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = if (showBackButton) 4.dp else 16.dp,
                ),
            ) {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = coffemaniaColors().espresso,
                        )
                    }
                } else {
                    CoffeeLogo(modifier = Modifier.size(28.dp))
                }
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = if (showBackButton) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    color = coffemaniaColors().espresso,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showSettingsButton) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = coffemaniaColors().espresso,
                    )
                }
            }
        }
    }
}

@Composable
fun CoffemaniaBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = coffemaniaColors().cappuccino,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BottomNavItem(
                label = "Главная",
                icon = Icons.Default.Home,
                selected = selectedTab == AppTab.Home,
                onClick = { onTabSelected(AppTab.Home) },
            )
            BottomNavItem(
                label = "Серверы",
                icon = Icons.Default.Language,
                selected = selectedTab == AppTab.Servers,
                onClick = { onTabSelected(AppTab.Servers) },
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Home,
) {
    val bg = if (selected) coffemaniaColors().latte else Color.Transparent
    val fg = if (selected) coffemaniaColors().espresso else coffemaniaColors().mocha

    Column(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = fg,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}

@Composable
fun BrewConnectButton(
    vpnStatus: VpnStatus,
    connectionElapsedMs: Long,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = vpnStatus == VpnStatus.Started
    val isConnecting = vpnStatus == VpnStatus.Starting
    val isDisconnecting = vpnStatus == VpnStatus.Stopping
    val isBusy = isConnecting || isDisconnecting
    val isDimmed = !enabled && !isConnected && !isBusy

    // Выключено (off) — зелёный; включено (on) — красный, как на референсе.
    val palette = when {
        isDimmed -> NeonPowerPalette.Dimmed
        isConnected || isDisconnecting -> NeonPowerPalette.OnRed
        else -> NeonPowerPalette.OffGreen
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(236.dp)
                .clickable(enabled = enabled && !isBusy, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isBusy) {
                ConnectPulseRings(color = palette.ring)
            }
            NeonPowerButtonFace(
                palette = palette,
                isBusy = isBusy,
                contentDescription = if (isConnected) "Отключить" else "Подключить",
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isConnected) {
            Text(
                text = formatConnectionDuration(connectionElapsedMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = NeonPowerPalette.OnRed.ring,
            )
        }
    }
}

private data class NeonPowerPalette(
    val glow: Color,
    val ring: Color,
    val faceCenter: Color,
    val faceEdge: Color,
    val faceHighlight: Color,
    val icon: Color,
) {
    companion object {
        val OnRed = NeonPowerPalette(
            glow = Color(0xFFFF2B3E),
            ring = Color(0xFFFF3B4D),
            faceCenter = Color(0xFF7A121C),
            faceEdge = Color(0xFF2A060A),
            faceHighlight = Color(0xFFA51C2A),
            icon = Color(0xFFFFFFFF),
        )
        val OffGreen = NeonPowerPalette(
            glow = Color(0xFF22C55E),
            ring = Color(0xFF4ADE80),
            faceCenter = Color(0xFF0F5A32),
            faceEdge = Color(0xFF062014),
            faceHighlight = Color(0xFF168A4A),
            icon = Color(0xFFFFFFFF),
        )
        val Dimmed = NeonPowerPalette(
            glow = Color(0xFF5A5A5A),
            ring = Color(0xFF6E6E6E),
            faceCenter = Color(0xFF2A2A2A),
            faceEdge = Color(0xFF121212),
            faceHighlight = Color(0xFF3A3A3A),
            icon = Color(0xFFB0B0B0),
        )
    }
}

@Composable
private fun NeonPowerButtonFace(
    palette: NeonPowerPalette,
    isBusy: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = center
            val outerR = size.minDimension / 2f

            // Soft outer neon halo (несколько слоёв вместо blur)
            listOf(
                1.00f to 0.06f,
                0.92f to 0.10f,
                0.84f to 0.16f,
                0.78f to 0.22f,
            ).forEach { (scale, alpha) ->
                drawCircle(
                    color = palette.glow.copy(alpha = alpha),
                    radius = outerR * scale,
                    center = c,
                )
            }

            // Dark recessed body
            val faceR = outerR * 0.72f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to palette.faceHighlight,
                        0.45f to palette.faceCenter,
                        1.0f to palette.faceEdge,
                    ),
                    center = c - Offset(0f, faceR * 0.12f),
                    radius = faceR * 1.15f,
                ),
                radius = faceR,
                center = c,
            )

            // Inner bevel ring
            drawCircle(
                color = Color.Black.copy(alpha = 0.45f),
                radius = faceR * 0.97f,
                center = c,
                style = Stroke(width = faceR * 0.045f),
            )
            drawCircle(
                color = palette.faceHighlight.copy(alpha = 0.35f),
                radius = faceR * 0.93f,
                center = c,
                style = Stroke(width = faceR * 0.02f),
            )

            // Bright neon outer ring
            val ringR = outerR * 0.78f
            drawCircle(
                color = palette.ring.copy(alpha = 0.35f),
                radius = ringR,
                center = c,
                style = Stroke(width = 10.dp.toPx()),
            )
            drawCircle(
                color = palette.ring,
                radius = ringR,
                center = c,
                style = Stroke(width = 3.5.dp.toPx()),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.35f),
                radius = ringR,
                center = c,
                style = Stroke(width = 1.2.dp.toPx()),
            )
        }

        if (isBusy) {
            CircularProgressIndicator(
                modifier = Modifier.size(52.dp),
                color = palette.icon,
                trackColor = palette.icon.copy(alpha = 0.15f),
                strokeWidth = 3.dp,
            )
        } else {
            // Soft glow behind power glyph
            Canvas(modifier = Modifier.size(110.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.28f),
                            Color.White.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                    radius = size.minDimension / 2f,
                )
            }
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = contentDescription,
                tint = palette.icon,
                modifier = Modifier.size(78.dp),
            )
        }
    }
}

@Composable
private fun ConnectPulseRings(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "connectPulse")
    val pulse1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse1",
    )
    val pulse2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400, delayMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse2",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = center
        val baseRadius = size.minDimension / 2f * 0.78f
        val strokeWidth = 3.dp.toPx()

        listOf(pulse1, pulse2).forEach { progress ->
            val radius = baseRadius * (1f + progress * 0.28f)
            val alpha = (1f - progress) * 0.55f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

private fun formatConnectionDuration(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1_000).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Composable
fun SelectedServerCard(
    display: ServerDisplay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val colors = coffemaniaColors()
    val pingColor = when {
        display.pingMs != null -> CoffemaniaColors.pingColor(display.pingMs)
        display.pingText == "N/A" -> CoffemaniaColors.PingBad
        else -> colors.mocha
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (emphasized) 14.dp else 12.dp),
        color = if (emphasized) colors.surfaceContainerHighest else colors.cappuccino,
        border = androidx.compose.foundation.BorderStroke(
            width = if (emphasized) 2.dp else 1.dp,
            color = if (emphasized) colors.espresso.copy(alpha = 0.55f) else colors.latte,
        ),
        shadowElevation = if (emphasized) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (emphasized) 18.dp else 16.dp,
                vertical = if (emphasized) 18.dp else 16.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (emphasized) 14.dp else 16.dp),
                modifier = Modifier.weight(1f),
            ) {
                ServerFlag(
                    flag = display.flag,
                    height = if (emphasized) 56.dp else 48.dp,
                    crossfade = true,
                    showShadow = true,
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = display.title,
                        style = if (emphasized) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                        fontWeight = FontWeight.Bold,
                        color = colors.espresso,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (display.subtitle.isNotBlank()) {
                            Text(
                                text = display.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (emphasized) FontWeight.Medium else FontWeight.Normal,
                                color = if (emphasized) {
                                    colors.espresso.copy(alpha = 0.72f)
                                } else {
                                    colors.mocha
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                        if (emphasized && display.pingText.isNotBlank() && display.pingText != "—") {
                            Text(
                                text = display.pingText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = pingColor,
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Выбрать сервер",
                tint = colors.espresso,
                modifier = if (emphasized) Modifier.size(28.dp) else Modifier,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerListCard(
    display: ServerDisplay,
    selected: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = coffemaniaColors().cappuccino
    val borderColor = if (selected) coffemaniaColors().espresso else coffemaniaColors().latte
    val pingColor = when {
        display.pingMs != null -> CoffemaniaColors.pingColor(display.pingMs)
        display.pingText == "N/A" -> CoffemaniaColors.PingBad
        else -> coffemaniaColors().mocha
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { clip = true }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            ServerListFlag(flag = display.flag, height = 32.dp)
            Column {
                Text(
                    text = display.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = coffemaniaColors().espresso,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(
                        text = display.protocolLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = coffemaniaColors().espresso,
                        modifier = Modifier
                            .background(coffemaniaColors().latte, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                    if (display.subtitle.isNotBlank()) {
                        Text(
                            text = display.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.sp,
                            ),
                            color = coffemaniaColors().mocha,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        Text(
            text = display.pingText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = pingColor,
        )
    }
}

@Composable
private fun ProtocolBadge(text: String, bg: Color, fg: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = Modifier
            .background(bg, CircleShape)
            .padding(horizontal = 8.dp, vertical = 1.dp),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = coffemaniaColors().mocha,
        modifier = modifier.padding(start = 8.dp, bottom = 8.dp),
    )
}

@Composable
fun SubscriptionExpiredCard(
    onRenewTelegramClick: () -> Unit,
    onRenewWebsiteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Подписка истекла",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = "Продлите подписку, чтобы снова пользоваться VPN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            SubscriptionActionButton(
                text = "Продлить в телеграмме",
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = onRenewTelegramClick,
                modifier = Modifier.fillMaxWidth(),
            )
            SubscriptionActionButton(
                text = "Продлить на сайте",
                icon = Icons.Default.ShoppingCart,
                onClick = onRenewWebsiteClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun SubscriptionCard(
    onPasteLinkClick: () -> Unit,
    onBuyOnWebsiteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = coffemaniaColors().cappuccino,
        border = androidx.compose.foundation.BorderStroke(1.dp, coffemaniaColors().latte),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Добавить подписку",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = coffemaniaColors().espresso,
            )
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SubscriptionActionButton(
                    text = "Вставить ссылку",
                    icon = Icons.Default.ContentPaste,
                    onClick = onPasteLinkClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                SubscriptionActionButton(
                    text = "Купить на сайте",
                    icon = Icons.Default.ShoppingCart,
                    onClick = onBuyOnWebsiteClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun PromoBanner(
    @androidx.annotation.DrawableRes imageRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
        color = Color.Transparent,
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.FillWidth,
        )
    }
}

@Composable
fun WebsiteBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PromoBanner(
        imageRes = R.drawable.banner_go_web,
        contentDescription = "Управляйте ключами на сайте porozoffvpn.ru",
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun TelegramChannelBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PromoBanner(
        imageRes = R.drawable.banner_got_tg,
        contentDescription = "Перейти в Telegram-канал",
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun SubscriptionActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(enabled = !isLoading, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = coffemaniaColors().milkFoam,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = coffemaniaColors().espresso,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = coffemaniaColors().espresso,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = coffemaniaColors().espresso,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SubscriptionStatusBar(
    nodeCount: Int,
    subscriptionInfo: SubscriptionInfo?,
    isRefreshing: Boolean,
    isPinging: Boolean,
    canRefresh: Boolean,
    canPing: Boolean,
    onRefreshConfig: () -> Unit,
    onRefreshPing: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = coffemaniaColors().cappuccino,
        border = androidx.compose.foundation.BorderStroke(1.dp, coffemaniaColors().latte),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            subscriptionInfo?.let { info ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = coffemaniaColors().espresso,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    info.expireLabel()?.let { expireText ->
                        val expired = info.isExpired()
                        Text(
                            text = expireText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (expired) {
                                MaterialTheme.colorScheme.error
                            } else {
                                coffemaniaColors().mocha
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(coffemaniaColors().latte),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = nodeCount.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = coffemaniaColors().espresso,
                        )
                    }
                    Text(
                        text = "Серверов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = coffemaniaColors().mocha,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ConfigRefreshButton(
                        isRefreshing = isRefreshing,
                        enabled = canRefresh,
                        onClick = onRefreshConfig,
                    )
                    PingTestButton(
                        isPinging = isPinging,
                        enabled = canPing,
                        onClick = onRefreshPing,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (subscriptionInfo != null && subscriptionInfo.isUnlimitedTraffic) {
                    Text(
                        text = "∞",
                        style = MaterialTheme.typography.titleMedium,
                        color = coffemaniaColors().espresso,
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(16.dp)
                            .background(coffemaniaColors().latte),
                    )
                }

                TrafficProgressBar(
                    subscriptionInfo = subscriptionInfo,
                    modifier = Modifier.weight(1f),
                )

                Text(
                    text = subscriptionInfo?.trafficLabel() ?: "— / —",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = coffemaniaColors().mocha,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TrafficProgressBar(
    subscriptionInfo: SubscriptionInfo?,
    modifier: Modifier = Modifier,
) {
    val trackColor = coffemaniaColors().latte
    val progressColor = coffemaniaColors().espresso

    if (subscriptionInfo == null) {
        Box(
            modifier = modifier
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(trackColor),
        )
        return
    }

    if (subscriptionInfo.isUnlimitedTraffic) {
        Box(
            modifier = modifier
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(trackColor),
        ) {
            if (subscriptionInfo.used > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.08f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressColor),
                )
            }
        }
        return
    }

    Box(
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(subscriptionInfo.usageFraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(3.dp))
                .background(progressColor),
        )
    }
}

@Composable
fun ConfigRefreshButton(
    isRefreshing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !isRefreshing,
        modifier = modifier.size(48.dp),
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = coffemaniaColors().espresso,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Обновить конфиг",
                tint = coffemaniaColors().espresso,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
fun PingTestButton(
    isPinging: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled && !isPinging,
        modifier = modifier.size(48.dp),
    ) {
        if (isPinging) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = coffemaniaColors().espresso,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Проверить пинг",
                tint = coffemaniaColors().espresso,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

fun statusHeadline(vpnStatus: VpnStatus): String = when (vpnStatus) {
    VpnStatus.Stopped -> "Отключено"
    VpnStatus.Starting -> "Подключение…"
    VpnStatus.Started -> "Подключено"
    VpnStatus.Stopping -> "Отключение…"
}
