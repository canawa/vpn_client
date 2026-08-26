package work.bavshield.vpn.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import work.bavshield.vpn.R
import work.bavshield.vpn.data.PingState
import work.bavshield.vpn.data.ProxyNode
import work.bavshield.vpn.vpn.VpnStatus

@Composable
fun HomeScreen(
    state: MainUiState,
    selectedDisplay: ServerDisplay?,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    onOpenServers: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onSiteClick: () -> Unit,
    onTelegramBotClick: () -> Unit,
    onPaySubscriptionClick: () -> Unit,
    onPayDevicesClick: () -> Unit,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources
    val isConnected = state.vpnStatus == VpnStatus.Started
    val hasSubscription = state.subscriptionUrl.isNotBlank() && state.nodes.isNotEmpty()
    val subscriptionExpired = state.subscriptionInfo?.isExpired() == true
    val canConnect = hasSubscription && !subscriptionExpired
    val connectEnabled = when {
        subscriptionExpired -> isConnected
        else -> isConnected || canConnect
    }
    val expireText = state.subscriptionInfo?.expireLabel(resources)
    val trafficText = state.subscriptionInfo
        ?.takeIf { it.total > 0 || it.used > 0 }
        ?.trafficLabel()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = 8.dp),
        ) {
            state.startupCrash?.let {
                ErrorBanner(text = stringResource(R.string.last_crash, it))
                Spacer(modifier = Modifier.height(12.dp))
            }
            state.error?.let {
                ErrorBanner(text = it)
                Spacer(modifier = Modifier.height(12.dp))
            }
            state.message?.takeIf { hasSubscription }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9AA0B8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
            }
        }

        val switchOffsetY = maxHeight * -0.28f
        ShieldConnectSwitch(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = switchOffsetY),
            vpnStatus = state.vpnStatus,
            connectionElapsedMs = state.connectionElapsedMs,
            enabled = connectEnabled,
            onClick = {
                if (isConnected) onDisconnectClick() else onConnectClick()
            },
        )

        val belowSwitch = maxHeight / 2 + switchOffsetY + 42.dp + 16.dp + if (isConnected) 28.dp else 0.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = belowSwitch, bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (subscriptionExpired) {
                    stringResource(R.string.subscription_expired)
                } else {
                    statusHeadline(state.vpnStatus)
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (subscriptionExpired) {
                    MaterialTheme.colorScheme.error
                } else {
                    Color(0xFF9AA0B8)
                },
            )

            if (!subscriptionExpired) {
                Spacer(modifier = Modifier.height(20.dp))
                if (selectedDisplay != null || hasSubscription) {
                    ServerCountryDropdown(
                        nodes = state.nodes,
                        selectedNodeId = state.selectedNodeId,
                        nodePings = state.nodePings,
                        selectedDisplay = selectedDisplay,
                        vpnStatus = state.vpnStatus,
                        onSelectNode = onSelectNode,
                        onConnectToNode = onConnectToNode,
                        onOpenAllServers = onOpenServers,
                    )
                }
            }

            if (hasSubscription && subscriptionExpired) {
                Spacer(modifier = Modifier.height(20.dp))
                SubscriptionExpiredCard(
                    onRenewTelegramClick = onTelegramBotClick,
                    onRenewWebsiteClick = onSiteClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            SubscriptionStatusBanner(
                hasSubscription = hasSubscription,
                expired = subscriptionExpired,
                expireText = expireText,
                trafficText = trafficText,
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeQuickActions(
                onSiteClick = onSiteClick,
                onBotClick = onTelegramBotClick,
                onPaySubscriptionClick = onPaySubscriptionClick,
                onPayDevicesClick = onPayDevicesClick,
                onSupportClick = onSupportClick,
            )

            if (!hasSubscription) {
                Spacer(modifier = Modifier.height(20.dp))
                SubscriptionCard(
                    onPasteLinkClick = onPasteLinkClick,
                    onBuyOnWebsiteClick = onSiteClick,
                )
                state.message?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF9AA0B8),
                        modifier = Modifier.padding(top = 12.dp, start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerCountryDropdown(
    nodes: List<ProxyNode>,
    selectedNodeId: String?,
    nodePings: Map<String, PingState>,
    selectedDisplay: ServerDisplay?,
    vpnStatus: VpnStatus,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    onOpenAllServers: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var expanded by remember { mutableStateOf(false) }
    var menuWidth by remember { mutableStateOf(0.dp) }
    val autoselect = stringResource(R.string.server_autoselect)
    val rows = remember(nodes, nodePings, autoselect) {
        nodes.map { node ->
            node to ServerDisplayMapper.map(context, node, nodePings[node.id])
        }.sortedWith(
            compareBy(
                { it.second.title != autoselect },
                { it.second.flag },
                { it.second.title.lowercase() },
            ),
        )
    }
    val pickingEnabled = vpnStatus != VpnStatus.Starting && vpnStatus != VpnStatus.Stopping

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                menuWidth = with(density) { coordinates.size.width.toDp() }
            },
    ) {
        if (selectedDisplay != null) {
            SelectedServerCard(
                display = selectedDisplay,
                onClick = { if (pickingEnabled) expanded = !expanded },
                emphasized = true,
                expanded = expanded,
            )
        } else {
            EmptyServerHint(
                onClick = {
                    if (pickingEnabled) {
                        if (nodes.isEmpty()) onOpenAllServers() else expanded = true
                    }
                },
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(menuWidth),
        ) {
            rows.forEach { (node, display) ->
                val pingColor = when {
                    display.pingMs != null -> BavShieldColors.pingColor(display.pingMs)
                    display.pingText == "N/A" -> BavShieldColors.PingBad
                    else -> bavShieldColors().mocha
                }
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ServerListFlag(flag = display.flag, height = 22.dp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = display.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (node.id == selectedNodeId) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    },
                                    color = bavShieldColors().espresso,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (display.subtitle.isNotBlank()) {
                                    Text(
                                        text = display.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = bavShieldColors().mocha,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Text(
                                text = display.pingText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = pingColor,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        if (vpnStatus == VpnStatus.Started) {
                            onConnectToNode(node.id)
                        } else {
                            onSelectNode(node.id)
                        }
                    },
                )
            }
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.servers_see_all),
                        fontWeight = FontWeight.Bold,
                        color = bavShieldColors().espresso,
                    )
                },
                onClick = {
                    expanded = false
                    onOpenAllServers()
                },
            )
        }
    }
}

@Composable
private fun SubscriptionStatusBanner(
    hasSubscription: Boolean,
    expired: Boolean,
    expireText: String?,
    trafficText: String?,
) {
    val colors = bavShieldColors()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (expired) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        } else {
            colors.cappuccino
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (expired) MaterialTheme.colorScheme.error else colors.latte,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.subscription_label),
                style = MaterialTheme.typography.labelSmall,
                color = if (expired) MaterialTheme.colorScheme.error else colors.mocha,
            )
            Text(
                text = when {
                    !hasSubscription -> stringResource(R.string.subscription_status_none)
                    expireText != null -> expireText
                    else -> stringResource(R.string.subscription_unlimited)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (expired) {
                    MaterialTheme.colorScheme.error
                } else {
                    colors.espresso
                },
            )
            if (hasSubscription && !expired && !trafficText.isNullOrBlank()) {
                Text(
                    text = trafficText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mocha,
                )
            }
        }
    }
}

@Composable
private fun HomeQuickActions(
    onSiteClick: () -> Unit,
    onBotClick: () -> Unit,
    onPaySubscriptionClick: () -> Unit,
    onPayDevicesClick: () -> Unit,
    onSupportClick: () -> Unit,
) {
    var payMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeMiniButton(
            label = stringResource(R.string.home_action_site),
            icon = Icons.Default.Language,
            onClick = onSiteClick,
            modifier = Modifier.weight(1f),
        )
        HomeMiniButton(
            label = stringResource(R.string.home_action_bot),
            icon = Icons.AutoMirrored.Filled.Send,
            onClick = onBotClick,
            modifier = Modifier.weight(1f),
        )
        Box(modifier = Modifier.weight(1f)) {
            HomeMiniButton(
                label = stringResource(R.string.home_action_pay),
                icon = Icons.Default.Payments,
                onClick = { payMenu = true },
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(
                expanded = payMenu,
                onDismissRequest = { payMenu = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pay_subscription)) },
                    onClick = {
                        payMenu = false
                        onPaySubscriptionClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.pay_devices)) },
                    onClick = {
                        payMenu = false
                        onPayDevicesClick()
                    },
                )
            }
        }
        HomeMiniButton(
            label = stringResource(R.string.home_action_support),
            icon = Icons.Default.HeadsetMic,
            onClick = onSupportClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HomeMiniButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cappuccino)
            .border(1.dp, colors.latte, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.espresso,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyServerHint(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (onClick != null) {
            stringResource(R.string.hint_tap_select_server)
        } else {
            stringResource(R.string.hint_add_subscription_to_select)
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = bavShieldColors().espresso,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bavShieldColors().cappuccino)
                        .clickable(onClick = onClick)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                } else {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bavShieldColors().cappuccino)
                        .padding(16.dp)
                },
            ),
    )
}

@Composable
private fun ErrorBanner(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp),
    )
}
