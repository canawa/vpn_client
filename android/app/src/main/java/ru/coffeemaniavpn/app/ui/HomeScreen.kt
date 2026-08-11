package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.LoadBalancer
import ru.coffeemaniavpn.app.data.formatTrafficRate
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
    val glow = when {
        isConnected -> ConnectUiStatus.On
        state.vpnStatus == VpnStatus.Starting || state.vpnStatus == VpnStatus.Stopping ->
            ConnectUiStatus.Busy
        else -> ConnectUiStatus.Off
    }

    val serverDisplay = selectedServerDisplay(state)
    val flagCode = FlagUtils.resolveCountryCodeOrDefault(serverDisplay.flag)

    Box(modifier = modifier.fillMaxSize().background(colors.milkFoam)) {
        StatusGlow(status = glow)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                XenoLogoMark(inCapsule = true, modifier = Modifier.clickable(onClick = onOpenWebsite))
                XenoGridIconButton(onClick = onOpenWebsite)
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
                    size = 168.dp,
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = when {
                        isConnected -> stringResource(R.string.xeno_status_connected)
                        state.vpnStatus == VpnStatus.Starting -> stringResource(R.string.clev_connecting)
                        else -> stringResource(R.string.xeno_status_disconnected)
                    }.uppercase(),
                    color = if (isConnected) colors.primary else ColorGraySoft,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when {
                        isConnected && state.connectionElapsedMs > 0 ->
                            formatSessionXeno(state.connectionElapsedMs)
                        isConnected -> stringResource(R.string.xeno_tap_disconnect)
                        else -> stringResource(R.string.xeno_tap_connect)
                    },
                    color = if (isConnected) colors.espresso else colors.mocha,
                    fontSize = if (isConnected) 16.sp else 13.sp,
                    fontFamily = if (isConnected) FontFamily.Monospace else FontFamily.Default,
                    textAlign = TextAlign.Center,
                    letterSpacing = if (isConnected) 1.sp else 0.sp,
                )

                Spacer(modifier = Modifier.height(14.dp))
                XenoSquareDashes(active = isConnected)

                state.error?.takeIf { it.isNotBlank() }?.let { err ->
                    Text(
                        text = err,
                        color = colors.error,
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Text(
                        text = "↓ ${formatTrafficRate(state.downlinkBytesPerSec)}",
                        color = colors.espresso,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "↑ ${formatTrafficRate(state.uplinkBytesPerSec)}",
                        color = colors.espresso,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    )
                }
            } else if (hasSubscription && !subscriptionExpired) {
                Spacer(modifier = Modifier.height(12.dp))
                XenoOutlineButton(
                    text = stringResource(R.string.xeno_test_connection),
                    onClick = onConnectClick,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private val ColorGraySoft = androidx.compose.ui.graphics.Color(0xFFB0B0B0)

@Composable
private fun selectedServerDisplay(state: MainUiState): ServerDisplay {
    val selectedId = state.selectedNodeId
    return when {
        selectedId == LoadBalancer.AUTO_NODE_ID -> ServerDisplay(
            flag = FlagUtils.DEFAULT_FLAG_EMOJI,
            title = stringResource(R.string.xeno_auto),
            subtitle = stringResource(R.string.xeno_auto_subtitle),
            protocolLabel = "",
            pingText = "",
            pingMs = null,
        )
        else -> {
            val node = state.nodes.find { it.id == selectedId } ?: state.nodes.firstOrNull()
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
                ServerDisplayMapper.map(node, state.nodePings[node.id]).let { d ->
                    val subtitle = d.group?.takeIf { it.isNotBlank() }
                        ?: d.subtitle.ifBlank { node.host }
                    d.copy(subtitle = subtitle)
                }
            }
        }
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
