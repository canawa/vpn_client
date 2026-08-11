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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.RoutingRuleTarget
import ru.coffeemaniavpn.app.data.SplitTunnelAppsMode
import ru.coffeemaniavpn.app.data.TrafficRoutingMode

private enum class XenoRoutingTab { Apps, Domains }

@Composable
fun XenoRoutingScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onUpdateConnectionSettings: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onAddCustomRule: (String, RoutingRuleTarget) -> Unit,
    onRemoveCustomRule: (String) -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onReconnectNow: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    var tab by remember { mutableStateOf(XenoRoutingTab.Apps) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.espresso)
            }
            Text(
                text = stringResource(R.string.xeno_routing_title),
                color = colors.espresso,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, null, tint = colors.mocha)
            }
        }

        XenoSegmentedTabs(
            left = stringResource(R.string.xeno_routing_apps),
            right = stringResource(R.string.xeno_routing_domains),
            leftSelected = tab == XenoRoutingTab.Apps,
            onLeft = { tab = XenoRoutingTab.Apps },
            onRight = { tab = XenoRoutingTab.Domains },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        when (tab) {
            XenoRoutingTab.Apps -> XenoRoutingAppsPane(
                settings = state.connectionSettings,
                onUpdate = onUpdateConnectionSettings,
                onReconnectNow = onReconnectNow,
            )
            XenoRoutingTab.Domains -> XenoRoutingDomainsPane(
                state = state,
                onUpdate = onUpdateConnectionSettings,
                onAddCustomRule = onAddCustomRule,
                onRemoveCustomRule = onRemoveCustomRule,
                onTrafficRoutingModeChange = onTrafficRoutingModeChange,
            )
        }
    }
}

@Composable
private fun XenoSegmentedTabs(
    left: String,
    right: String,
    leftSelected: Boolean,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.cappuccino)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (leftSelected) colors.primary else Color.Transparent)
                .clickable(onClick = onLeft)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                left,
                color = if (leftSelected) Color.Black else colors.mocha,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (!leftSelected) colors.primary else Color.Transparent)
                .clickable(onClick = onRight)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                right,
                color = if (!leftSelected) Color.Black else colors.mocha,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun XenoRoutingAppsPane(
    settings: ConnectionSettingsState,
    onUpdate: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onReconnectNow: (() -> Unit)?,
) {
    val colors = coffemaniaColors()
    val bypass = settings.appsMode == SplitTunnelAppsMode.ExcludeSelected
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        XenoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.xeno_split_tunneling), color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(stringResource(R.string.xeno_split_tunneling_hint), color = colors.mocha, fontSize = 12.sp)
                }
                ClevAnimatedSwitch(
                    checked = settings.appsEnabled,
                    onCheckedChange = { enabled -> onUpdate { it.copy(appsEnabled = enabled) } },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            XenoModeCard(
                title = stringResource(R.string.xeno_bypass),
                subtitle = stringResource(R.string.xeno_bypass_hint),
                selected = bypass,
                modifier = Modifier.weight(1f),
                onClick = {
                    onUpdate {
                        it.copy(appsEnabled = true, appsMode = SplitTunnelAppsMode.ExcludeSelected)
                    }
                },
            )
            XenoModeCard(
                title = stringResource(R.string.xeno_tunnel_only),
                subtitle = stringResource(R.string.xeno_tunnel_only_hint),
                selected = !bypass,
                modifier = Modifier.weight(1f),
                onClick = {
                    onUpdate {
                        it.copy(appsEnabled = true, appsMode = SplitTunnelAppsMode.IncludeOnly)
                    }
                },
            )
        }

        if (onReconnectNow != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cappuccino)
                    .border(1.dp, colors.latte, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 28.dp)
                        .background(colors.primary, RoundedCornerShape(2.dp)),
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    stringResource(R.string.xeno_reconnect_hint),
                    color = colors.mocha,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.xeno_reconnect_now),
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onReconnectNow),
                )
            }
        }

        // App picker is heavy; keep existing ClevAppsTab body without its own header.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
        ) {
            ClevAppsTab(
                settings = settings,
                onSave = { saved -> onUpdate { _ -> saved } },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun XenoModeCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cappuccino)
            .border(1.dp, if (selected) colors.primary else colors.latte, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                title,
                color = if (selected) colors.primary else colors.espresso,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(subtitle, color = colors.mocha, fontSize = 11.sp, lineHeight = 14.sp)
        }
    }
}

@Composable
private fun XenoRoutingDomainsPane(
    state: MainUiState,
    onUpdate: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onAddCustomRule: (String, RoutingRuleTarget) -> Unit,
    onRemoveCustomRule: (String) -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
) {
    val colors = coffemaniaColors()
    val settings = state.connectionSettings
    val rules = settings.customRules
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        XenoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.xeno_domain_rules), color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text(stringResource(R.string.xeno_domain_rules_hint), color = colors.mocha, fontSize = 12.sp)
                }
                ClevAnimatedSwitch(
                    checked = settings.sitesEnabled || state.trafficRoutingMode == TrafficRoutingMode.CUSTOM,
                    onCheckedChange = { enabled ->
                        onTrafficRoutingModeChange(
                            if (enabled) TrafficRoutingMode.CUSTOM else TrafficRoutingMode.GLOBAL,
                        )
                        onUpdate { it.copy(sitesEnabled = enabled) }
                    },
                )
            }
        }

        rules.forEach { rule ->
            XenoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = rule.value,
                        color = colors.espresso,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    val (label, tint) = when (rule.target) {
                        RoutingRuleTarget.Proxy -> "Туннель" to colors.primary
                        RoutingRuleTarget.Direct -> "Напрямую" to colors.mocha
                    }
                    Text(
                        text = label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tint.copy(alpha = 0.15f))
                            .clickable { onRemoveCustomRule(rule.id) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        XenoDashedButton(
            text = stringResource(R.string.xeno_add_rule),
            leadingAccent = "+",
            onClick = {
                // simple inline add via draft dialog-less: focus ClevRulesTab style
            },
        )

        // Compact add field matching product capability
        XenoCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = colors.espresso, fontSize = 14.sp),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (draft.isBlank()) Text("*.example.com", color = colors.mocha, fontSize = 14.sp)
                        inner()
                    },
                )
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            val v = draft.trim()
                            if (v.isNotEmpty()) {
                                onAddCustomRule(v, RoutingRuleTarget.Direct)
                                draft = ""
                            }
                        },
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}
