package work.bavshield.vpn.ui

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import work.bavshield.vpn.R
import work.bavshield.vpn.data.PingState
import work.bavshield.vpn.data.ProxyNode
import work.bavshield.vpn.data.formatConnectionDuration
import work.bavshield.vpn.data.formatTrafficSpeed
import work.bavshield.vpn.vpn.VpnStatus

private object HomeMotion {
    const val FadeInMs = 380
    const val ExpandMs = 520
    const val FadeOutMs = 280
    const val ShrinkMs = 480
    const val PressMs = 140
}

private val NeonPanel = Color(0xFF0B120E)

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
    onTelegramChannelClick: () -> Unit,
    onTelegramBotClick: () -> Unit,
    onSupportClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
    onRefreshSubscription: () -> Unit,
    onPingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    val view = LocalView.current
    val isConnected = state.vpnStatus == VpnStatus.Started
    val hasSubscription = state.subscriptionUrl.isNotBlank() && state.nodes.isNotEmpty()
    val subscriptionExpired = state.subscriptionInfo?.isExpired() == true
    val canConnect = hasSubscription && !subscriptionExpired
    val connectEnabled = when {
        subscriptionExpired -> isConnected
        else -> isConnected || canConnect
    }
    var showServers by remember { mutableStateOf(false) }

    BackHandler(enabled = showServers) {
        showServers = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CyberSettingsButton(onClick = onSettingsClick)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.bav_logo),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(36.dp),
                )
            }
            CyberRefreshButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    onRefreshSubscription()
                },
                enabled = hasSubscription && !state.isLoading,
                isLoading = state.isLoading,
            )
        }

        state.error?.let { error ->
            Spacer(modifier = Modifier.height(10.dp))
            ErrorBanner(
                text = error,
                actionLabel = stringResource(R.string.home_error_support),
                onAction = onSupportClick,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        ShieldConnectButton(
            vpnStatus = state.vpnStatus,
            enabled = connectEnabled,
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                if (isConnected) onDisconnectClick() else onConnectClick()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = state.vpnStatus == VpnStatus.Started ||
                    state.vpnStatus == VpnStatus.Starting,
                enter = fadeIn(animationSpec = tween(HomeMotion.FadeInMs, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(HomeMotion.FadeOutMs, easing = FastOutSlowInEasing)),
            ) {
                ConnectionLiveStats(
                    elapsedMs = state.connectionElapsedMs,
                    downloadBytesPerSec = state.downloadBytesPerSec,
                    uploadBytesPerSec = state.uploadBytesPerSec,
                )
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = state.vpnStatus == VpnStatus.Stopped,
                enter = fadeIn(animationSpec = tween(HomeMotion.FadeInMs, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(HomeMotion.FadeOutMs, easing = FastOutSlowInEasing)),
            ) {
                Text(
                    text = stringResource(R.string.home_tap_shield),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.mocha,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

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
            CyberMenuButton(
                label = stringResource(R.string.home_action_channel_bot),
                iconPainter = painterResource(R.drawable.ic_telegram),
                onClick = onTelegramChannelClick,
                modifier = Modifier.weight(1f),
                iconSize = 28.dp,
                labelColor = Color.White,
            )
            CyberMenuButton(
                label = stringResource(R.string.home_action_subscription),
                iconPainter = painterResource(R.drawable.ic_credit_card),
                onClick = onSubscriptionClick,
                modifier = Modifier.weight(1f),
                iconSize = 28.dp,
                labelColor = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        CyberServersToggle(
            selectedDisplay = selectedDisplay,
            isPinging = state.isPinging,
            pingEnabled = state.nodes.isNotEmpty(),
            onPingClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                onPingClick()
            },
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                showServers = true
            },
        )

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

    if (showServers) {
        ServersPickerDialog(
            nodes = state.nodes,
            selectedNodeId = state.selectedNodeId,
            nodePings = state.nodePings,
            vpnStatus = state.vpnStatus,
            isPinging = state.isPinging,
            onDismiss = { showServers = false },
            onPingClick = onPingClick,
            onSelectNode = { id ->
                onSelectNode(id)
                showServers = false
            },
            onConnectToNode = { id ->
                onConnectToNode(id)
                showServers = false
            },
            onRefreshSubscription = onRefreshSubscription,
            onSupportClick = onSupportClick,
        )
    }
}

@Composable
private fun ConnectionLiveStats(
    elapsedMs: Long,
    downloadBytesPerSec: Long,
    uploadBytesPerSec: Long,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatConnectionDuration(elapsedMs),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.espresso,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "↓ ${formatTrafficSpeed(downloadBytesPerSec)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.espresso,
            )
            Text(
                text = "↑ ${formatTrafficSpeed(uploadBytesPerSec)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.mocha,
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
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    labelColor: Color = Color(0xFFD6DDD8),
) {
    CyberMenuButton(
        label = label,
        iconPainter = iconPainter,
        imageVector = null,
        iconTint = bavShieldColors().espresso,
        onClick = onClick,
        modifier = modifier,
        iconSize = iconSize,
        labelColor = labelColor,
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
    iconSize: androidx.compose.ui.unit.Dp = 24.dp,
    labelColor: Color = Color(0xFFD6DDD8),
) {
    val colors = bavShieldColors()
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(Color(0xFF070B09))
            .border(1.dp, colors.espresso.copy(alpha = 0.85f), shape)
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
                modifier = Modifier.size(iconSize),
            )
            imageVector != null -> Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(iconSize),
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CyberServersToggle(
    selectedDisplay: ServerDisplay?,
    isPinging: Boolean,
    pingEnabled: Boolean,
    onPingClick: () -> Unit,
    onClick: () -> Unit,
) {
    val colors = bavShieldColors()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(Color(0xFF070B09))
            .border(1.dp, colors.espresso.copy(alpha = 0.55f), shape)
            .padding(end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (selectedDisplay != null) {
                ServerListFlag(flag = selectedDisplay.flag, height = 22.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_servers),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.mocha,
                    )
                    Text(
                        text = selectedDisplay.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.espresso,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = selectedDisplay.pingText.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = selectedDisplay.pingMs?.let { BavShieldColors.pingColor(it) }
                        ?: colors.espresso,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Dns,
                    contentDescription = null,
                    tint = colors.espresso,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.home_select_server),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.espresso,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        CyberServerActionButton(
            loading = isPinging,
            enabled = pingEnabled && !isPinging,
            onClick = onPingClick,
            contentDescription = stringResource(R.string.cd_ping),
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = bavShieldColors().espresso.copy(
                    alpha = if (pingEnabled && !isPinging) 1f else 0.38f,
                ),
                modifier = Modifier.size(22.dp),
            )
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = colors.espresso,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(9.dp),
        )
    }
}

@Composable
private fun ServersPickerDialog(
    nodes: List<ProxyNode>,
    selectedNodeId: String?,
    nodePings: Map<String, PingState>,
    vpnStatus: VpnStatus,
    isPinging: Boolean,
    onDismiss: () -> Unit,
    onPingClick: () -> Unit,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    onRefreshSubscription: () -> Unit,
    onSupportClick: () -> Unit,
) {
    val colors = bavShieldColors()
    val view = LocalView.current
    val popupShape = RoundedCornerShape(18.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 520.dp)
                    .clip(popupShape)
                    .background(Color(0xFF0A100D))
                    .border(1.dp, colors.espresso.copy(alpha = 0.35f), popupShape)
                    .clickable(enabled = false, onClick = {})
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_servers),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.espresso,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp),
                    )
                    if (isPinging) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = colors.espresso,
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                onPingClick()
                            },
                            enabled = nodes.isNotEmpty(),
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = stringResource(R.string.cd_ping),
                                tint = colors.espresso.copy(
                                    alpha = if (nodes.isNotEmpty()) 1f else 0.38f,
                                ),
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = colors.espresso,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                CyberServerList(
                    nodes = nodes,
                    selectedNodeId = selectedNodeId,
                    nodePings = nodePings,
                    vpnStatus = vpnStatus,
                    onSelectNode = onSelectNode,
                    onConnectToNode = onConnectToNode,
                    onRefreshSubscription = {
                        onDismiss()
                        onRefreshSubscription()
                    },
                    onSupportClick = {
                        onDismiss()
                        onSupportClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                )
            }
        }
    }
}

@Composable
private fun CyberServerActionButton(
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = bavShieldColors()
    if (loading) {
        Box(
            modifier = modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = colors.espresso,
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(40.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF0D1711))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
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
    val view = LocalView.current
    val density = LocalDensity.current
    val shieldPainter = painterResource(R.drawable.bav_shield)
    val glowPainter = painterResource(R.drawable.bav_shield_glow)

    LaunchedEffect(interaction) {
        snapshotFlow { pressed }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
    }

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

    val isLit = vpnStatus == VpnStatus.Started || vpnStatus == VpnStatus.Starting
    val glowBase by animateFloatAsState(
        targetValue = if (isLit) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isLit) 1100 else 1500,
            easing = FastOutSlowInEasing,
        ),
        label = "shieldGlow",
    )
    val glowAlpha = if (isConnecting) {
        glowBase * (0.55f + 0.40f * pulse)
    } else {
        glowBase
    }

    val pressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )
    val pressDepth by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = pressSpring,
        label = "shieldPressDepth",
    )
    val idleScale = when {
        isConnecting -> 0.994f + 0.01f * pulse
        isConnected -> 1.01f
        else -> 1f
    }
    // Soft push-in: shrink + sink + slight tilt
    val baseScale = idleScale * (1f - 0.08f * pressDepth)
    val pressOffsetY = with(density) { (10.dp * pressDepth).toPx() }
    val pressDim = 1f - 0.14f * pressDepth
    val glowPressScale = 1f - 0.12f * pressDepth

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1200f / 1411f)
                .graphicsLayer {
                    scaleX = baseScale
                    scaleY = baseScale
                    translationY = pressOffsetY
                    cameraDistance = 16f * density.density
                    rotationX = 5f * pressDepth
                    clip = false
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = glowPainter,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = (if (isConnecting) 1f + 0.012f * pulse else 1.01f) * glowPressScale
                        scaleX = s
                        scaleY = s
                        alpha = glowAlpha * pressDim
                        clip = false
                    },
            )
            if (glowAlpha > 0.01f) {
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
                    .graphicsLayer {
                        alpha = pressDim
                    }
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
private fun CyberServerList(
    nodes: List<ProxyNode>,
    selectedNodeId: String?,
    nodePings: Map<String, PingState>,
    vpnStatus: VpnStatus,
    onSelectNode: (String) -> Unit,
    onConnectToNode: (String) -> Unit,
    onRefreshSubscription: () -> Unit,
    onSupportClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = bavShieldColors()
    val view = LocalView.current
    val rows = remember(nodes, nodePings) {
        ServerDisplayMapper.sortRows(
            nodes.map { node ->
                node to ServerDisplayMapper.map(context, node, nodePings[node.id])
            },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.servers_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.mocha,
                    textAlign = TextAlign.Center,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NeonActionChip(
                        label = stringResource(R.string.subscription_refresh),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onRefreshSubscription()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    NeonActionChip(
                        label = stringResource(R.string.home_error_support),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            onSupportClick()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            rows.forEach { (node, display) ->
                val selected = node.id == selectedNodeId
                val itemShape = RoundedCornerShape(10.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(itemShape)
                        .background(NeonPanel)
                        .border(
                            1.dp,
                            if (selected) colors.espresso else colors.espresso.copy(alpha = 0.13f),
                            itemShape,
                        )
                        .clickable(enabled = pickingEnabled) {
                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            if (vpnStatus == VpnStatus.Started) {
                                onConnectToNode(node.id)
                            } else {
                                onSelectNode(node.id)
                            }
                        }
                        .padding(horizontal = 11.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ServerListFlag(flag = display.flag, height = 22.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = display.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            fontWeight = FontWeight.Medium,
                            color = colors.espresso,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (display.subtitle.isNotBlank()) {
                            Text(
                                text = display.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = colors.mocha,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Text(
                        text = display.pingText.ifBlank { "—" },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = display.pingMs?.let { BavShieldColors.pingColor(it) }
                            ?: colors.espresso,
                    )
                }
            }
        }
    }
}

@Composable
private fun NeonActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    val shape = RoundedCornerShape(10.dp)
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = colors.espresso,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(shape)
            .border(1.dp, colors.espresso.copy(alpha = 0.45f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    )
}

@Composable
private fun ErrorBanner(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val colors = bavShieldColors()
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f))
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f), shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.espresso,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0A100E))
                .border(1.dp, colors.espresso.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .clickable(onClick = onAction)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}
