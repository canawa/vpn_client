package work.bavshield.vpn.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onPasteLinkClick: () -> Unit,
    onSiteClick: () -> Unit,
    onTelegramBotClick: () -> Unit,
    onTelegramChannelClick: () -> Unit,
    onSupportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    val isConnected = state.vpnStatus == VpnStatus.Started
    val hasSubscription = state.subscriptionUrl.isNotBlank() && state.nodes.isNotEmpty()
    val subscriptionExpired = state.subscriptionInfo?.isExpired() == true
    val canConnect = hasSubscription && !subscriptionExpired
    val connectEnabled = when {
        subscriptionExpired -> isConnected
        else -> isConnected || canConnect
    }
    var showServers by remember { mutableStateOf(false) }
    var channelMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = showServers || channelMenu) {
        channelMenu = false
        showServers = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            CyberSettingsButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Image(
                painter = painterResource(R.drawable.bav_logo),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.78f)
                    .height(40.dp),
            )
        }

        state.error?.let {
            Spacer(modifier = Modifier.height(10.dp))
            ErrorBanner(text = it)
        }

        Spacer(modifier = Modifier.height(10.dp))

        ShieldConnectButton(
            vpnStatus = state.vpnStatus,
            enabled = connectEnabled,
            onClick = {
                if (isConnected) onDisconnectClick() else onConnectClick()
            },
        )

        Text(
            text = connectionHint(state.vpnStatus, selectedDisplay),
            fontSize = 13.sp,
            color = Color(0xFF9AA39E),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 2.dp),
        )

        Spacer(modifier = Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CyberMenuButton(
                label = stringResource(R.string.home_action_tech_support),
                icon = Icons.Default.HeadsetMic,
                onClick = onSupportClick,
                modifier = Modifier.weight(1f),
            )
            CyberMenuButton(
                label = stringResource(R.string.home_action_site),
                icon = Icons.Default.Language,
                onClick = onSiteClick,
                modifier = Modifier.weight(1f),
            )
            Box(modifier = Modifier.weight(1f)) {
                CyberMenuButton(
                    label = stringResource(R.string.home_action_channel_bot),
                    iconPainter = painterResource(R.drawable.ic_telegram),
                    iconTint = Color.Unspecified,
                    onClick = { channelMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = channelMenu,
                    onDismissRequest = { channelMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_open_channel)) },
                        onClick = {
                            channelMenu = false
                            onTelegramChannelClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.home_open_bot)) },
                        onClick = {
                            channelMenu = false
                            onTelegramBotClick()
                        },
                    )
                }
            }
            CyberMenuButton(
                label = stringResource(R.string.home_action_subscription),
                iconPainter = painterResource(R.drawable.ic_subscription),
                onClick = onSubscriptionClick,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        CyberServersToggle(
            expanded = showServers,
            onClick = { showServers = !showServers },
        )

        AnimatedVisibility(
            visible = showServers,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            CyberServerList(
                nodes = state.nodes,
                selectedNodeId = state.selectedNodeId,
                nodePings = state.nodePings,
                vpnStatus = state.vpnStatus,
                onSelectNode = onSelectNode,
                onConnectToNode = onConnectToNode,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (hasSubscription && subscriptionExpired) {
            Spacer(modifier = Modifier.height(16.dp))
            SubscriptionExpiredCard(
                onRenewTelegramClick = onTelegramBotClick,
                onRenewWebsiteClick = onSiteClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (!hasSubscription) {
            Spacer(modifier = Modifier.height(16.dp))
            SubscriptionCard(
                onPasteLinkClick = onPasteLinkClick,
                onBuyOnWebsiteClick = onSiteClick,
            )
        }
    }
}

@Composable
private fun connectionHint(status: VpnStatus, selectedDisplay: ServerDisplay?): String {
    val connectedName = selectedDisplay?.title
    return when (status) {
        VpnStatus.Started -> if (!connectedName.isNullOrBlank()) {
            stringResource(R.string.home_secure_on_server, connectedName)
        } else {
            stringResource(R.string.home_secure_on)
        }
        VpnStatus.Starting -> stringResource(R.string.status_connecting)
        VpnStatus.Stopping -> stringResource(R.string.status_disconnecting)
        VpnStatus.Stopped -> stringResource(R.string.home_tap_shield)
    }
}

@Composable
private fun ShieldConnectButton(
    vpnStatus: VpnStatus,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val isConnected = vpnStatus == VpnStatus.Started
    val isConnecting = vpnStatus == VpnStatus.Starting
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shieldPainter = painterResource(R.drawable.bav_shield)
    val glowPainter = painterResource(R.drawable.bav_shield_glow)

    val infinite = rememberInfiniteTransition(label = "shieldAnim")
    val pulse by infinite.animateFloat(
        initialValue = 0.90f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shieldPulse",
    )

    val glowTarget = when (vpnStatus) {
        VpnStatus.Started -> 1f
        VpnStatus.Starting -> 0.55f + 0.40f * pulse
        VpnStatus.Stopping -> 0.35f
        VpnStatus.Stopped -> 0f
    }
    val glowAlpha by animateFloatAsState(
        targetValue = glowTarget,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "shieldGlow",
    )
    val baseScale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.97f
            isConnecting -> 0.994f + 0.01f * pulse
            isConnected -> 1.01f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "shieldScale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .aspectRatio(1200f / 1411f)
                .graphicsLayer {
                    scaleX = baseScale
                    scaleY = baseScale
                    clip = false
                },
            contentAlignment = Alignment.Center,
        ) {
            // Always in tree so fade in/out stays continuous
            Image(
                painter = glowPainter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = if (isConnecting) 1f + 0.012f * pulse else 1.01f
                        scaleX = s
                        scaleY = s
                        alpha = glowAlpha
                        clip = false
                    },
            )
            if (glowAlpha > 0.01f) {
                // Solid body cover — blocks bloom under the metal
                Image(
                    painter = shieldPainter,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(Color(0xFF050807), BlendMode.SrcIn),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Image(
                painter = shieldPainter,
                contentDescription = stringResource(
                    if (isConnected) R.string.cd_disconnect else R.string.cd_connect,
                ),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        enabled = enabled,
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    ),
            )
        }
    }
}

@Composable
private fun CyberMenuButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CyberMenuButton(
        label = label,
        iconPainter = null,
        imageVector = icon,
        iconTint = bavShieldColors().espresso,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun CyberMenuButton(
    label: String,
    iconPainter: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.Unspecified,
) {
    CyberMenuButton(
        label = label,
        iconPainter = iconPainter,
        imageVector = null,
        iconTint = iconTint,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun CyberMenuButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconPainter: Painter? = null,
    imageVector: ImageVector? = null,
    iconTint: Color = bavShieldColors().espresso,
) {
    val colors = bavShieldColors()
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(Color(0xFF070B09))
            .border(1.dp, colors.espresso.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when {
            iconPainter != null -> Icon(
                painter = iconPainter,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            imageVector != null -> Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFD6DDD8),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CyberServersToggle(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val colors = bavShieldColors()
    val shape = RoundedCornerShape(14.dp)
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label = "serversArrow",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(Color(0xFF070B09))
            .border(1.dp, colors.espresso.copy(alpha = 0.55f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Dns,
            contentDescription = null,
            tint = colors.espresso,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = stringResource(R.string.home_servers),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        )
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.espresso,
            modifier = Modifier
                .size(22.dp)
                .rotate(arrowRotation),
        )
    }
}

@Composable
private fun CyberServerList(
    nodes: List<ProxyNode>,
    selectedNodeId: String?,
    nodePings: Map<String, PingState>,
    vpnStatus: VpnStatus,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = bavShieldColors()
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
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .border(1.dp, colors.espresso.copy(alpha = 0.18f), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (rows.isEmpty()) {
            Text(
                text = stringResource(R.string.servers_empty),
                style = MaterialTheme.typography.bodySmall,
                color = colors.mocha,
                modifier = Modifier.padding(12.dp),
            )
        } else {
            rows.forEach { (node, display) ->
                val selected = node.id == selectedNodeId
                val itemShape = RoundedCornerShape(10.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(itemShape)
                        .background(Color(0xFF0B120E))
                        .border(
                            1.dp,
                            if (selected) colors.espresso else colors.espresso.copy(alpha = 0.13f),
                            itemShape,
                        )
                        .clickable(enabled = pickingEnabled) {
                            if (vpnStatus == VpnStatus.Started) {
                                onConnectToNode(node.id)
                            } else {
                                onSelectNode(node.id)
                            }
                        }
                        .padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ServerListFlag(flag = display.flag, height = 22.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = display.title,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.espresso,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (display.subtitle.isNotBlank()) {
                            Text(
                                text = display.subtitle,
                                fontSize = 9.sp,
                                color = colors.mocha,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = display.pingText.ifBlank { "—" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = display.pingMs?.let { BavShieldColors.pingColor(it) }
                            ?: colors.espresso,
                    )
                }
            }
        }
    }
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
