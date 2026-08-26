package work.bavshield.vpn.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import work.bavshield.vpn.R
import work.bavshield.vpn.vpn.VpnStatus

enum class AppTab { Home, Servers }

@Composable
fun BavShieldSwitch(
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
            checkedThumbColor = bavShieldColors().milkFoam,
            checkedTrackColor = bavShieldColors().espresso,
            checkedBorderColor = bavShieldColors().espresso,
            uncheckedThumbColor = bavShieldColors().milkFoam,
            uncheckedTrackColor = bavShieldColors().latte,
            uncheckedBorderColor = bavShieldColors().espresso,
            disabledCheckedThumbColor = bavShieldColors().milkFoam.copy(alpha = 0.7f),
            disabledCheckedTrackColor = bavShieldColors().mocha.copy(alpha = 0.5f),
            disabledCheckedBorderColor = bavShieldColors().mocha.copy(alpha = 0.5f),
            disabledUncheckedThumbColor = bavShieldColors().cappuccino,
            disabledUncheckedTrackColor = bavShieldColors().latte.copy(alpha = 0.7f),
            disabledUncheckedBorderColor = bavShieldColors().mocha,
        ),
    )
}

@Composable
fun BavShieldTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    showSettingsButton: Boolean = true,
    onSettingsClick: () -> Unit = {},
    showRefreshButton: Boolean = false,
    isRefreshing: Boolean = false,
    refreshEnabled: Boolean = true,
    onRefreshClick: () -> Unit = {},
    transparent: Boolean = false,
    lightContent: Boolean = false,
    titleBelow: Boolean = false,
) {
    val content = if (lightContent) Color.White else bavShieldColors().espresso
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (transparent) Color.Transparent else bavShieldColors().milkFoam,
        shadowElevation = 0.dp,
    ) {
        val horizontalPadding = if (showBackButton) 12.dp else 24.dp

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = if (titleBelow) Modifier else Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(
                        space = if (showBackButton) 4.dp else 16.dp,
                    ),
                ) {
                    if (showBackButton) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = content,
                            )
                        }
                    } else if (showSettingsButton) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.cd_settings),
                                tint = content,
                            )
                        }
                    }
                    if (!titleBelow) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = if (showBackButton) {
                                MaterialTheme.typography.titleMedium
                            } else {
                                MaterialTheme.typography.headlineMedium
                            },
                            color = content,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showRefreshButton) {
                        if (isRefreshing) {
                            Box(
                                modifier = Modifier.size(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = content,
                                )
                            }
                        } else {
                            IconButton(
                                onClick = onRefreshClick,
                                enabled = refreshEnabled,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.cd_refresh),
                                    tint = content.copy(
                                        alpha = if (refreshEnabled) 1f else 0.38f,
                                    ),
                                )
                            }
                        }
                    }
                    if (showSettingsButton && showBackButton) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.cd_settings),
                                tint = content,
                            )
                        }
                    }
                }
            }
            if (titleBelow) {
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = content,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun BavShieldBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bavShieldColors().cappuccino,
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
                label = stringResource(R.string.tab_home),
                selected = selectedTab == AppTab.Home,
                onClick = { onTabSelected(AppTab.Home) },
                useShieldLogo = true,
            )
            BottomNavItem(
                label = stringResource(R.string.tab_servers),
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
    useShieldLogo: Boolean = false,
) {
    val bg = if (selected) bavShieldColors().latte else Color.Transparent
    val fg = if (selected) bavShieldColors().espresso else bavShieldColors().mocha

    Column(
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (useShieldLogo) {
            ShieldLogo(
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
fun ShieldConnectSwitch(
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
    val thumbOn = isConnected || isConnecting

    val connectOrange = BavShieldColors.ConnectOrange
    val disconnectRed = BavShieldColors.PingBad
    val trackShape = RoundedCornerShape(50)

    val trackWidth = 236.dp
    val trackHeight = 72.dp
    val thumbSize = 104.dp
    val edgeInset = 14.dp
    val travel = trackWidth - thumbSize + edgeInset * 2
    val thumbOffset by animateDpAsState(
        targetValue = if (thumbOn) travel else 0.dp,
        animationSpec = tween(durationMillis = 280),
        label = "shieldThumb",
    )

    val trackColor = when {
        isDimmed -> bavShieldColors().connectDisabledOuter
        isDisconnecting -> disconnectRed.copy(alpha = 0.72f)
        isConnecting -> connectOrange.copy(alpha = 0.85f)
        else -> connectOrange
    }
    val progressColor = when {
        isDisconnecting -> disconnectRed
        else -> bavShieldColors().mocha
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .width(trackWidth)
                .height(thumbSize)
                .clickable(
                    enabled = enabled && !isBusy,
                    role = Role.Switch,
                    onClick = onClick,
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(trackWidth)
                    .height(trackHeight)
                    .shadow(elevation = 10.dp, shape = trackShape, clip = false)
                    .clip(trackShape)
                    .background(trackColor),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset - edgeInset)
                    .size(thumbSize),
                contentAlignment = Alignment.Center,
            ) {
                ShieldLogo(
                    modifier = Modifier.size(100.dp),
                    tint = Color.White,
                    filled = true,
                    contentDescription = if (isConnected) {
                        stringResource(R.string.cd_disconnect)
                    } else {
                        stringResource(R.string.cd_connect)
                    },
                )
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = progressColor,
                        strokeWidth = 2.5.dp,
                    )
                }
            }
        }
        if (isConnected) {
            Text(
                text = formatConnectionDuration(connectionElapsedMs),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = connectOrange,
                modifier = Modifier.padding(top = 10.dp),
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
    expanded: Boolean? = null,
    drawContainer: Boolean = true,
) {
    val colors = bavShieldColors()
    val pingColor = when {
        display.pingMs != null -> BavShieldColors.pingColor(display.pingMs)
        display.pingText == "N/A" -> BavShieldColors.PingBad
        else -> colors.mocha
    }
    val body = @Composable {
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
                imageVector = if (expanded != null) {
                    Icons.Default.KeyboardArrowDown
                } else {
                    Icons.AutoMirrored.Filled.KeyboardArrowRight
                },
                contentDescription = stringResource(R.string.cd_select_server),
                tint = colors.espresso,
                modifier = Modifier
                    .then(if (emphasized) Modifier.size(28.dp) else Modifier)
                    .graphicsLayer { rotationZ = if (expanded == true) 180f else 0f },
            )
        }
    }
    if (!drawContainer) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
        ) {
            body()
        }
        return
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
        body()
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
    val bg = bavShieldColors().cappuccino
    val borderColor = if (selected) bavShieldColors().espresso else bavShieldColors().latte
    val pingColor = when {
        display.pingMs != null -> BavShieldColors.pingColor(display.pingMs)
        display.pingText == "N/A" -> BavShieldColors.PingBad
        else -> bavShieldColors().mocha
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
                    color = bavShieldColors().espresso,
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
                        color = bavShieldColors().espresso,
                        modifier = Modifier
                            .background(bavShieldColors().latte, CircleShape)
                            .padding(horizontal = 8.dp, vertical = 1.dp),
                    )
                    if (display.subtitle.isNotBlank()) {
                        Text(
                            text = display.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.sp,
                            ),
                            color = bavShieldColors().mocha,
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
        color = bavShieldColors().mocha,
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
                text = stringResource(R.string.subscription_expired),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = stringResource(R.string.subscription_expired_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            SubscriptionActionButton(
                text = stringResource(R.string.subscription_renew_telegram),
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = onRenewTelegramClick,
                modifier = Modifier.fillMaxWidth(),
            )
            SubscriptionActionButton(
                text = stringResource(R.string.subscription_renew_website),
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
        color = bavShieldColors().cappuccino,
        border = androidx.compose.foundation.BorderStroke(1.dp, bavShieldColors().latte),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.subscription_add),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = bavShieldColors().espresso,
            )
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SubscriptionActionButton(
                    text = stringResource(R.string.subscription_paste_link),
                    icon = Icons.Default.ContentPaste,
                    onClick = onPasteLinkClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                SubscriptionActionButton(
                    text = stringResource(R.string.subscription_buy_website),
                    icon = Icons.Default.ShoppingCart,
                    onClick = onBuyOnWebsiteClick,
                    modifier = Modifier.fillMaxWidth(),
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
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable(enabled = !isLoading, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bavShieldColors().milkFoam,
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
                    color = bavShieldColors().espresso,
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = bavShieldColors().espresso,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = bavShieldColors().espresso,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun statusHeadline(vpnStatus: VpnStatus): String = when (vpnStatus) {
    VpnStatus.Stopped -> stringResource(R.string.status_disconnected)
    VpnStatus.Starting -> stringResource(R.string.status_connecting)
    VpnStatus.Started -> stringResource(R.string.status_connected)
    VpnStatus.Stopping -> stringResource(R.string.status_disconnecting)
}
