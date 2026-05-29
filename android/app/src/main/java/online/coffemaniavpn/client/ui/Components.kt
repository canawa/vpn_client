package online.coffemaniavpn.client.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import online.coffemaniavpn.client.vpn.VpnStatus

enum class AppTab { Home, Servers }

@Composable
fun CoffemaniaTopBar(
    title: String,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CoffeeLogo(modifier = Modifier.size(28.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Настройки",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
        color = CoffemaniaColors.SurfaceContainer,
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
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val fg = if (selected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (useCoffeeLogo) {
            CoffeeLogo(modifier = Modifier.size(24.dp))
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
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = vpnStatus == VpnStatus.Started
    val isBusy = vpnStatus == VpnStatus.Starting || vpnStatus == VpnStatus.Stopping

    val outerBorder = if (isConnected) MaterialTheme.colorScheme.primary else CoffemaniaColors.OutlineVariant
    val innerBg = if (isConnected) CoffemaniaColors.SurfaceContainerHigh else CoffemaniaColors.SurfaceContainerLow

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(192.dp)
                .clip(CircleShape)
                .border(2.dp, outerBorder, CircleShape)
                .clickable(enabled = enabled && !isBusy, onClick = onClick)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(innerBg),
                contentAlignment = Alignment.Center,
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 3.dp,
                    )
                } else {
                    CoffeeLogo(modifier = Modifier.size(88.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = when {
                isConnected -> "Отключить"
                isBusy -> statusLabel(vpnStatus)
                else -> "Подключить"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        color = CoffemaniaColors.SurfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            CoffemaniaColors.OutlineVariant.copy(alpha = 0.3f),
        ),
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
                ServerFlagAvatar(flag = display.flag, size = 40.dp)
                Column {
                    Text(
                        text = display.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = display.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SignalCellularAlt,
                    contentDescription = null,
                    tint = CoffemaniaColors.OutlineVariant,
                    modifier = Modifier.size(16.dp),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun ServerListCard(
    display: ServerDisplay,
    selected: Boolean,
    showFastestBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) CoffemaniaColors.SurfaceVariant else CoffemaniaColors.SurfaceContainer
    val borderColor = if (selected) CoffemaniaColors.PrimaryFixedDim else CoffemaniaColors.OutlineVariant.copy(0.3f)

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
                ServerFlagAvatar(
                    flag = display.flag,
                    size = 40.dp,
                    background = if (selected) CoffemaniaColors.SurfaceContainerLow else CoffemaniaColors.SurfaceContainerLowest,
                )
                Column {
                    Text(
                        text = display.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        if (showFastestBadge) {
                            ProtocolBadge(
                                text = "FASTEST",
                                bg = MaterialTheme.colorScheme.primary,
                                fg = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            ProtocolBadge(
                                text = display.protocolLabel,
                                bg = MaterialTheme.colorScheme.secondaryContainer,
                                fg = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Text(
                            text = display.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${display.pingMs} ms",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PingSparkline(
                    seed = display.pingMs,
                    color = if (selected) MaterialTheme.colorScheme.secondary else CoffemaniaColors.Outline,
                    modifier = Modifier.padding(top = 4.dp),
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
fun PingSparkline(
    seed: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(width = 32.dp, height = 12.dp)) {
        val w = size.width
        val h = size.height
        val y1 = h * (0.3f + (seed % 5) * 0.08f)
        val y2 = h * (0.5f + (seed % 3) * 0.1f)
        val path = Path().apply {
            moveTo(0f, h * 0.8f)
            cubicTo(w * 0.15f, h * 0.8f, w * 0.25f, y1, w * 0.5f, y1)
            cubicTo(w * 0.75f, y1, w * 0.85f, y2, w, y2)
        }
        drawPath(path, color, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 8.dp, bottom = 8.dp),
    )
}

@Composable
fun SubscriptionCard(
    onTelegramClick: () -> Unit,
    onPasteLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CoffemaniaColors.SurfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            CoffemaniaColors.OutlineVariant.copy(0.5f),
        ),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CoffeeLogo(modifier = Modifier.size(22.dp))
                Text(
                    text = "Добавить подписку",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SubscriptionActionButton(
                    text = "Через Telegram",
                    icon = Icons.Default.Send,
                    onClick = onTelegramClick,
                    modifier = Modifier.weight(1f),
                )
                SubscriptionActionButton(
                    text = "Вставить ссылку",
                    icon = Icons.Default.ContentPaste,
                    onClick = onPasteLinkClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SubscriptionActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = CoffemaniaColors.SurfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SortBar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CoffemaniaColors.SurfaceContainer,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            CoffemaniaColors.OutlineVariant.copy(0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = null,
                    tint = CoffemaniaColors.Outline,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Сортировка:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "По пингу",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
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

private fun statusLabel(vpnStatus: VpnStatus): String = when (vpnStatus) {
    VpnStatus.Starting -> "Подключение…"
    VpnStatus.Stopping -> "Отключение…"
    else -> "Подключить"
}
