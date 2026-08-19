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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.LoadBalancer
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.data.formatTrafficRate
import ru.coffeemaniavpn.app.vpn.VpnAutoReconnect
import ru.coffeemaniavpn.app.vpn.VpnStatus

@Composable
fun HomeScreen(
    state: MainUiState,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onPasteLinkClick: () -> Unit,
    onImportUrl: (String) -> Unit,
    onOpenServers: () -> Unit,
    onOpenWebsite: () -> Unit,
    onAcceptClipboard: () -> Unit,
    onDismissClipboard: () -> Unit,
    onDismissForeignPrompt: () -> Unit,
    onBuyTelegram: () -> Unit,
    onBuyWebsite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSubscription = state.subscriptionUrl.isNotBlank()
    if (!hasSubscription) {
        XenoActivationFlow(
            modifier = modifier,
            isLoading = state.isLoading,
            error = state.error,
            clipboardUrl = state.clipboardSubscriptionUrl,
            showForeignPrompt = state.showForeignSubscriptionPrompt,
            onPasteLinkClick = onPasteLinkClick,
            onAcceptClipboard = onAcceptClipboard,
            onDismissClipboard = onDismissClipboard,
            onDismissForeignPrompt = onDismissForeignPrompt,
            onBuyTelegram = onBuyTelegram,
            onBuyWebsite = onBuyWebsite,
            onImportUrl = onImportUrl,
        )
        return
    }

    val colors = coffemaniaColors()
    val isConnected = state.vpnStatus == VpnStatus.Started
    val subscriptionExpired = state.subscriptionInfo?.isExpired() == true
    val connectEnabled = when {
        subscriptionExpired -> isConnected
        else -> true
    }
    val serverDisplay = selectedServerDisplay(state)
    val flagCode = FlagUtils.resolveCountryCodeOrDefault(serverDisplay.flag)

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF0A0D0C))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                XenoLogoMark(inCapsule = true, modifier = Modifier.clickable(onClick = onOpenWebsite))
                XenoGridIconButton(onClick = onOpenServers)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                XenoConnectButton(
                    vpnStatus = state.vpnStatus,
                    enabled = connectEnabled,
                    onClick = {
                        if (isConnected) onDisconnectClick() else onConnectClick()
                    },
                    size = 188.dp,
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = when {
                        isConnected -> stringResource(R.string.xeno_status_connected)
                        state.vpnStatus == VpnStatus.Starting -> stringResource(R.string.xeno_connecting)
                        else -> stringResource(R.string.xeno_status_disconnected)
                    }.uppercase(),
                    color = if (isConnected) Color(0xFF00D4A8) else Color(0xFF6B7672),
                    fontFamily = OswaldFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    letterSpacing = 1.92.sp, // 6% of 32
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        isConnected && state.connectionElapsedMs > 0 ->
                            formatSessionXeno(state.connectionElapsedMs)
                        isConnected -> stringResource(R.string.xeno_tap_disconnect)
                        else -> stringResource(R.string.xeno_tap_connect)
                    },
                    color = if (isConnected) Color.White else Color(0xFF566460),
                    fontFamily = if (isConnected) JetBrainsMonoFamily else InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = if (isConnected) 16.sp else 12.sp,
                    lineHeight = if (isConnected) 16.sp else 12.sp,
                    letterSpacing = if (isConnected) 1.sp else 0.sp,
                    textAlign = TextAlign.Center,
                    modifier = if (isConnected) {
                        Modifier
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(15.dp)
                    },
                )

                Spacer(modifier = Modifier.height(14.dp))
                XenoSquareDashes(
                    active = isConnected,
                    count = if (isConnected) 10 else 9,
                )

                state.error?.takeIf { it.isNotBlank() }?.let { err ->
                    Text(
                        text = err,
                        color = colors.error,
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .padding(horizontal = 12.dp),
                    )
                }
            }

            XenoServerCard(
                flagCode = flagCode,
                title = serverDisplay.title,
                subtitle = serverDisplay.subtitle.ifBlank { serverDisplay.title },
                onClick = onOpenServers,
                pingMs = serverDisplay.pingMs,
                connected = isConnected,
            )
            Spacer(modifier = Modifier.height(10.dp))
            XenoSubscriptionCard(
                info = state.subscriptionInfo,
                hasSubscription = hasSubscription,
            )
            if (isConnected) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = "↓ ${formatTrafficRate(state.downlinkBytesPerSec)}",
                        color = Color.White,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "↑ ${formatTrafficRate(state.uplinkBytesPerSec)}",
                        color = Color.White,
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 13.sp,
                    )
                }
            } else if (hasSubscription && !subscriptionExpired) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    // Figma: h34, r12; width grows for longer labels
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .wrapContentWidth()
                            .widthIn(min = 155.dp, max = 280.dp)
                            .clip(shape)
                            .background(Color(0xFF141B18))
                            .border(1.dp, Color(0xFF222B28), shape)
                            .clickable(onClick = onConnectClick)
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.xeno_test_connection),
                            color = Color(0xFF00D4A8),
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 11.sp,
                            lineHeight = 11.sp,
                            letterSpacing = 0.4.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun selectedServerDisplay(state: MainUiState): ServerDisplay {
    val selectedId = state.selectedNodeId
    return when {
        selectedId == LoadBalancer.AUTO_NODE_ID -> autoBalancerDisplay(state)
        else -> {
            val tunnelActive =
                state.vpnStatus == VpnStatus.Starting || state.vpnStatus == VpnStatus.Started
            val connected = VpnAutoReconnect.connectedNode()
                ?.takeIf { node -> state.nodes.any { it.id == node.id } }
            val node = when {
                tunnelActive && connected != null -> connected
                else -> state.nodes.find { it.id == selectedId } ?: state.nodes.firstOrNull()
            }
            if (node == null) {
                ServerDisplay(
                    flag = FlagUtils.DEFAULT_FLAG_EMOJI,
                    title = stringResource(R.string.xeno_no_server),
                    subtitle = stringResource(R.string.xeno_import_for_servers),
                    protocolLabel = "",
                    pingText = "",
                    pingMs = null,
                )
            } else {
                mappedNodeDisplay(node, state)
            }
        }
    }
}

@Composable
private fun autoBalancerDisplay(state: MainUiState): ServerDisplay {
    val connecting = state.vpnStatus == VpnStatus.Starting || state.vpnStatus == VpnStatus.Started
    val picked = if (connecting) resolvedAutoNode(state) else null
    if (picked == null) {
        return ServerDisplay(
            flag = FlagUtils.DEFAULT_FLAG_EMOJI,
            title = stringResource(R.string.xeno_auto),
            subtitle = stringResource(R.string.xeno_servers_auto_subtitle),
            protocolLabel = "",
            pingText = "",
            pingMs = null,
        )
    }
    val mapped = ServerDisplayMapper.map(picked, state.nodePings[picked.id])
    return ServerDisplay(
        flag = mapped.flag,
        title = stringResource(R.string.xeno_auto),
        subtitle = nodeChoiceLabel(mapped),
        protocolLabel = mapped.protocolLabel,
        pingText = mapped.pingText,
        pingMs = mapped.pingMs,
    )
}

private fun resolvedAutoNode(state: MainUiState): ProxyNode? {
    val connected = VpnAutoReconnect.connectedNode()
        ?.takeIf { node -> state.nodes.any { it.id == node.id } }
        ?.takeUnless { LoadBalancer.isOnWifi() && LoadBalancer.isLteServer(it) }
    return connected ?: LoadBalancer.pickBestNormal(state.nodes, state.nodePings)
}

private fun mappedNodeDisplay(node: ProxyNode, state: MainUiState): ServerDisplay {
    return ServerDisplayMapper.map(node, state.nodePings[node.id]).let { d ->
        val detail = buildString {
            if (d.protocolLabel.isNotBlank()) append(d.protocolLabel)
            if (d.subtitle.isNotBlank()) {
                if (isNotEmpty()) append(" | ")
                append(d.subtitle)
            }
            if (isEmpty()) {
                d.group?.takeIf { it.isNotBlank() }?.let { append(it) }
            }
        }
        d.copy(subtitle = detail)
    }
}

private fun nodeChoiceLabel(display: ServerDisplay): String {
    val extra = display.subtitle.trim()
    return if (extra.isNotBlank() && extra != display.title) {
        "${display.title} · $extra"
    } else {
        display.title
    }
}

private fun formatSessionXeno(elapsedMs: Long): String {
    val totalSec = (elapsedMs / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%02d : %02d : %02d".format(h, m, s)
}

fun openExternalUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)),
        )
    }
}
