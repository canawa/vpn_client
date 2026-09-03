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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R
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
                    CoffeeLogo(modifier = Modifier.size(28.dp), tint = coffemaniaColors().espresso)
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
                selected = selectedTab == AppTab.Home,
                onClick = { onTabSelected(AppTab.Home) },
                useCoffeeLogo = true,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    useCoffeeLogo: Boolean = false,
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
        if (useCoffeeLogo) {
            CoffeeLogo(
                modifier = Modifier.size(24.dp),
                tint = fg,
            )
        } else {
            Icon(
                imageVector = icon ?: Icons.Default.Home,
                contentDescription = label,
                tint = fg,
                modifier = Modifier.size(24.dp),
            )
        }
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
    val isDisconnected = vpnStatus == VpnStatus.Stopped && !isDimmed

    val connectedGreen = CoffemaniaColors.PingGood
    val disconnectRed = CoffemaniaColors.PingBad

    val outerBg = when {
        isDimmed -> coffemaniaColors().connectDisabledOuter
        isConnected -> connectedGreen.copy(alpha = 0.18f)
        isConnecting -> connectedGreen.copy(alpha = 0.12f)
        isDisconnecting -> disconnectRed.copy(alpha = 0.12f)
        isDisconnected -> disconnectRed.copy(alpha = 0.12f)
        else -> coffemaniaColors().cappuccino
    }
    val outerBorder = when {
        isDimmed -> coffemaniaColors().connectDisabledBorder
        isConnected -> connectedGreen
        isConnecting -> connectedGreen.copy(alpha = 0.75f)
        isDisconnecting -> disconnectRed
        isDisconnected -> disconnectRed
        else -> coffemaniaColors().latte
    }
    val outerBorderWidth = if (isConnected || isDisconnected) 3.dp else 2.dp
    val innerBg = if (isDimmed) coffemaniaColors().connectDisabledInner else coffemaniaColors().milkFoam
    val innerBorder = when {
        isDimmed -> coffemaniaColors().connectDisabledBorder
        isConnected -> connectedGreen.copy(alpha = 0.45f)
        isConnecting -> connectedGreen.copy(alpha = 0.35f)
        isDisconnecting -> disconnectRed.copy(alpha = 0.35f)
        isDisconnected -> disconnectRed.copy(alpha = 0.35f)
        else -> coffemaniaColors().latte
    }
    val logoTint = if (isDimmed) coffemaniaColors().connectDisabledIcon else coffemaniaColors().espresso
    val progressColor = when {
        isConnecting -> connectedGreen
        isDisconnecting -> disconnectRed
        else -> coffemaniaColors().espresso
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isConnecting) {
                ConnectPulseRings(color = connectedGreen)
            }
            if (isDisconnecting) {
                ConnectPulseRings(color = disconnectRed)
            }

            Box(
                modifier = Modifier
                    .size(192.dp)
                    .clip(CircleShape)
                    .background(outerBg)
                    .border(outerBorderWidth, outerBorder, CircleShape)
                    .clickable(enabled = enabled && !isBusy, onClick = onClick)
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(innerBg)
                        .border(1.5.dp, innerBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = progressColor,
                            strokeWidth = 3.dp,
                        )
                    } else {
                        CoffeeLogo(
                            modifier = Modifier.size(88.dp),
                            tint = logoTint,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        if (isConnected) {
            Text(
                text = formatConnectionDuration(connectionElapsedMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = connectedGreen,
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
        val baseRadius = size.minDimension / 2f
        val strokeWidth = 3.dp.toPx()

        listOf(pulse1, pulse2).forEach { progress ->
            val radius = baseRadius * (0.82f + progress * 0.38f)
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
                                fontWeight = FontWeight.Bold,
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
                    fontWeight = FontWeight.Bold,
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
                                fontWeight = FontWeight.Bold,
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
            fontWeight = FontWeight.Bold,
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
                fontWeight = FontWeight.Bold,
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
                fontWeight = FontWeight.Bold,
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
    metrics: PromoBannerMetrics.Spec? = null,
) {
    val configuration = LocalConfiguration.current
    val painter = painterResource(imageRes)
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val spec = metrics ?: PromoBannerMetrics.compute(
            widthDp = maxWidth.value,
            heightDp = when {
                maxHeight.value.isFinite() && maxHeight.value < Float.POSITIVE_INFINITY / 2f ->
                    maxHeight.value
                else -> configuration.screenHeightDp.toFloat()
            },
        )
        val intrinsic = painter.intrinsicSize
        val aspectRatio = if (
            intrinsic.width.isFinite() &&
            intrinsic.height.isFinite() &&
            intrinsic.width > 0f &&
            intrinsic.height > 0f
        ) {
            intrinsic.width / intrinsic.height
        } else {
            770f / 205f
        }
        val contentWidth = PromoBannerMetrics.resolveContentWidthDp(maxWidth.value, spec)
        val fitted = PromoBannerMetrics.fitInside(
            maxWidthDp = contentWidth,
            maxHeightDp = spec.maxHeightDp,
            aspectRatio = aspectRatio,
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .width(fitted.widthDp.dp)
                    .height(fitted.heightDp.dp),
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 8.dp,
                color = Color.Transparent,
            ) {
                Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.FillBounds,
                )
            }
        }
    }
}

@Composable
fun WebsiteBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    metrics: PromoBannerMetrics.Spec? = null,
) {
    PromoBanner(
        imageRes = R.drawable.banner_go_web,
        contentDescription = "Управляйте ключами на сайте coffeemaniavpn.ru",
        onClick = onClick,
        modifier = modifier,
        metrics = metrics,
    )
}

@Composable
fun TelegramChannelBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    metrics: PromoBannerMetrics.Spec? = null,
) {
    PromoBanner(
        imageRes = R.drawable.banner_got_tg,
        contentDescription = "Перейти в Telegram-канал",
        onClick = onClick,
        modifier = modifier,
        metrics = metrics,
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

fun statusHeadline(vpnStatus: VpnStatus): String = when (vpnStatus) {
    VpnStatus.Stopped -> "Отключено"
    VpnStatus.Starting -> "Подключение…"
    VpnStatus.Started -> "Подключено"
    VpnStatus.Stopping -> "Отключение…"
}
