package online.coffemaniavpn.client.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.coffemaniavpn.client.R
import online.coffemaniavpn.client.data.SubscriptionInfo
import online.coffemaniavpn.client.vpn.VpnStatus

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
    val isBusy = vpnStatus == VpnStatus.Starting || vpnStatus == VpnStatus.Stopping
    val isDimmed = !enabled && !isConnected && !isBusy

    val outerBg = if (isDimmed) coffemaniaColors().connectDisabledOuter else coffemaniaColors().cappuccino
    val outerBorder = if (isDimmed) coffemaniaColors().connectDisabledBorder else coffemaniaColors().latte
    val innerBg = if (isDimmed) coffemaniaColors().connectDisabledInner else coffemaniaColors().milkFoam
    val innerBorder = if (isDimmed) coffemaniaColors().connectDisabledBorder else coffemaniaColors().latte
    val logoTint = if (isDimmed) coffemaniaColors().connectDisabledIcon else coffemaniaColors().espresso

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(192.dp)
                .clip(CircleShape)
                .background(outerBg)
                .border(2.dp, outerBorder, CircleShape)
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
                        color = coffemaniaColors().espresso,
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
        Spacer(modifier = Modifier.height(48.dp))
        if (isConnected) {
            Text(
                text = formatConnectionDuration(connectionElapsedMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = coffemaniaColors().mocha,
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
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = coffemaniaColors().cappuccino,
        border = androidx.compose.foundation.BorderStroke(1.dp, coffemaniaColors().latte),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                ServerFlag(flag = display.flag, height = 48.dp)
                Column {
                    Text(
                        text = display.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = coffemaniaColors().espresso,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (display.subtitle.isNotBlank()) {
                        Text(
                            text = display.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = coffemaniaColors().mocha,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = coffemaniaColors().espresso,
            )
        }
    }
}

@Composable
fun ServerListCard(
    display: ServerDisplay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = coffemaniaColors().cappuccino
    val borderColor = if (selected) coffemaniaColors().espresso else coffemaniaColors().latte

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f),
            ) {
                ServerFlag(flag = display.flag, height = 48.dp)
                Column {
                    Text(
                        text = display.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = coffemaniaColors().espresso,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        ProtocolBadge(
                            text = display.protocolLabel,
                            bg = coffemaniaColors().latte,
                            fg = coffemaniaColors().espresso,
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
            Column(horizontalAlignment = Alignment.End) {
                val pingColor = when {
                    display.pingMs != null -> CoffemaniaColors.pingColor(display.pingMs)
                    display.pingText == "N/A" -> CoffemaniaColors.PingBad
                    else -> coffemaniaColors().mocha
                }
                Text(
                    text = display.pingText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = pingColor,
                )
            }
        }
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
            .padding(horizontal = 8.dp, vertical = 2.dp),
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
fun SubscriptionCard(
    onPasteLinkClick: () -> Unit,
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
            }
        }
    }
}

@Composable
fun TelegramChannelBanner(
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
            painter = painterResource(R.drawable.banner_telegram_channel),
            contentDescription = "Перейти в Telegram-канал",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.FillWidth,
        )
    }
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
            if (subscriptionInfo?.hasTitle == true || subscriptionInfo?.expireLabel() != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (subscriptionInfo?.hasTitle == true) {
                        Text(
                            text = subscriptionInfo.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = coffemaniaColors().espresso,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    subscriptionInfo?.expireLabel()?.let { expireText ->
                        val expired = subscriptionInfo.expire > 0 &&
                            subscriptionInfo.expire * 1_000L <= System.currentTimeMillis()
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
    val trackColor = coffemaniaColors().milkFoam
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

    LinearProgressIndicator(
        progress = { subscriptionInfo.usageFraction },
        modifier = modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp)),
        color = progressColor,
        trackColor = trackColor,
    )
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
