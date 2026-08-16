package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import ru.coffeemaniavpn.app.data.RoutingRuleLineKind
import ru.coffeemaniavpn.app.data.RoutingRuleInput
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.heightIn
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.InstalledAppsLoader
import ru.coffeemaniavpn.app.data.RoutingRule
import ru.coffeemaniavpn.app.data.RoutingRuleMatcher
import ru.coffeemaniavpn.app.data.RoutingRuleTarget
import ru.coffeemaniavpn.app.data.SplitTunnelAppsMode
import ru.coffeemaniavpn.app.data.TrafficRoutingMode

private enum class XenoRoutingTab { Apps, Domains }

private val XenoBg = Color(0xFF0A0D0C)
private val XenoPlate = Color(0xFF121A17)
private val XenoStroke = Color(0xFF222B28)
private val XenoTeal = Color(0xFF00D4A8)
private val XenoMuted = Color(0xFF6B7672)
private val XenoText = Color(0xFFF2F5F4)

@Composable
fun XenoRoutingScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onUpdateConnectionSettings: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onAddCustomRule: (String, RoutingRuleTarget) -> Unit,
    onAddCustomRules: (List<String>, RoutingRuleTarget) -> Unit,
    onRemoveCustomRule: (String) -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onReconnectNow: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var tab by remember { mutableStateOf(XenoRoutingTab.Apps) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XenoBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = XenoText)
            }
            Text(
                text = stringResource(R.string.xeno_routing_title),
                color = XenoText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, null, tint = XenoMuted)
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
                onAddCustomRules = onAddCustomRules,
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
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(XenoPlate)
            .border(1.dp, XenoStroke, shape)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (leftSelected) XenoTeal else Color.Transparent)
                .clickable(onClick = onLeft)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                left,
                color = if (leftSelected) Color.Black else XenoText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (!leftSelected) XenoTeal else Color.Transparent)
                .clickable(onClick = onRight)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                right,
                color = if (!leftSelected) Color.Black else XenoText,
                fontFamily = InterFontFamily,
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
    val context = LocalContext.current
    val bypass = settings.appsMode == SplitTunnelAppsMode.ExcludeSelected

    var apps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var selected by remember(settings.appPackages) { mutableStateOf(settings.appPackages) }

    LaunchedEffect(Unit) {
        // App routing is always on — no master toggle in UI.
        if (!settings.appsEnabled) {
            onUpdate { it.copy(appsEnabled = true) }
        }
        isLoading = true
        apps = withContext(Dispatchers.Default) {
            InstalledAppsLoader.load(context.packageManager, context.packageName)
        }
        isLoading = false
    }
    LaunchedEffect(settings.appPackages) {
        selected = settings.appPackages
    }

    fun persist(packages: Set<String> = selected) {
        onUpdate { it.copy(appsEnabled = true, appPackages = packages) }
    }

    val filtered = remember(apps, query, selected) {
        val base = if (query.isBlank()) {
            apps
        } else {
            apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
        base.sortedWith(
            compareByDescending<InstalledAppItem> { it.packageName in selected }
                .thenBy { it.label.lowercase() },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                XenoModeCard(
                    title = stringResource(R.string.xeno_bypass),
                    subtitle = stringResource(R.string.xeno_bypass_hint),
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    selected = bypass,
                    enabled = true,
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
                    icon = Icons.Outlined.Home,
                    selected = !bypass,
                    enabled = true,
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
                        .background(XenoPlate)
                        .border(1.dp, XenoStroke, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .background(XenoTeal, RoundedCornerShape(2.dp)),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        stringResource(R.string.xeno_reconnect_hint),
                        color = XenoMuted,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.xeno_reconnect_now),
                        color = XenoTeal,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(onClick = onReconnectNow),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        XenoAppsSearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.xeno_search_apps),
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.xeno_selected_apps_count,
                    selected.size,
                    apps.size,
                ),
                color = XenoMuted,
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            if (selected.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.xeno_clear),
                    color = XenoTeal,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable {
                        selected = emptySet()
                        persist(emptySet())
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.xeno_apps_loading),
                    color = XenoMuted,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val isOn = app.packageName in selected
                    XenoAppRow(
                        label = app.label,
                        packageName = app.packageName,
                        selected = isOn,
                        onClick = {
                            selected = if (isOn) {
                                selected - app.packageName
                            } else {
                                selected + app.packageName
                            }
                            persist(selected)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun XenoSettingsPlate(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(XenoPlate)
            .border(1.dp, XenoStroke, shape)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        content()
    }
}

@Composable
private fun XenoModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val border = when {
        selected -> XenoTeal
        else -> XenoStroke
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(XenoPlate)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) XenoTeal else XenoMuted,
                modifier = Modifier.size(20.dp),
            )
            Text(
                title,
                color = if (selected) XenoTeal else XenoText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            Text(
                subtitle,
                color = XenoMuted,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun XenoAppsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(XenoPlate)
            .border(1.dp, XenoStroke, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Outlined.Search,
            contentDescription = null,
            tint = XenoMuted,
            modifier = Modifier.size(18.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = XenoText,
                fontFamily = InterFontFamily,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(XenoTeal),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isBlank()) {
                    Text(
                        placeholder,
                        color = XenoMuted,
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                    )
                }
                inner()
            },
        )
    }
}

@Composable
private fun XenoAppRow(
    label: String,
    packageName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(XenoPlate)
            .border(1.dp, XenoStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        XenoAppIcon(packageName = packageName)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = XenoText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = packageName,
                color = XenoMuted,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        XenoCheckBox(selected = selected)
    }
}

@Composable
private fun XenoAppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }
    val shape = RoundedCornerShape(10.dp)
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier
                .size(36.dp)
                .clip(shape),
        )
    } else {
        Box(
            modifier = modifier
                .size(36.dp)
                .clip(shape)
                .background(Color(0xFF1A2420)),
        )
    }
}

@Composable
private fun XenoCheckBox(selected: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(XenoTeal)
                } else {
                    Modifier
                        .background(Color.Transparent)
                        .border(1.5.dp, XenoMuted, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun XenoRoutingDomainsPane(
    state: MainUiState,
    onUpdate: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onAddCustomRules: (List<String>, RoutingRuleTarget) -> Unit,
    onRemoveCustomRule: (String) -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
) {
    val settings = state.connectionSettings
    val rules = settings.customRules
    var filter by remember { mutableStateOf(XenoDomainFilter.All) }
    var showAddDialog by remember { mutableStateOf(false) }

    val tunnelCount = rules.count { it.target == RoutingRuleTarget.Proxy }
    val directCount = rules.count { it.target == RoutingRuleTarget.Direct }
    val blockCount = rules.count { it.target == RoutingRuleTarget.Block }

    LaunchedEffect(Unit) {
        if (state.trafficRoutingMode != TrafficRoutingMode.CUSTOM) {
            onTrafficRoutingModeChange(TrafficRoutingMode.CUSTOM)
        }
        if (!settings.sitesEnabled) {
            onUpdate { it.copy(sitesEnabled = true) }
        }
    }

    val filtered = remember(rules, filter) {
        when (filter) {
            XenoDomainFilter.All -> rules
            XenoDomainFilter.Tunnel -> rules.filter { it.target == RoutingRuleTarget.Proxy }
            XenoDomainFilter.Direct -> rules.filter { it.target == RoutingRuleTarget.Direct }
            XenoDomainFilter.Block -> rules.filter { it.target == RoutingRuleTarget.Block }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        XenoSettingsPlate {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.xeno_domain_rules),
                    color = XenoText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    stringResource(R.string.xeno_domain_rules_hint),
                    color = XenoMuted,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            XenoFilterChip(
                label = stringResource(R.string.xeno_filter_all, rules.size),
                selected = filter == XenoDomainFilter.All,
                onClick = { filter = XenoDomainFilter.All },
            )
            XenoFilterChip(
                label = stringResource(R.string.xeno_filter_tunnel, tunnelCount),
                selected = filter == XenoDomainFilter.Tunnel,
                onClick = { filter = XenoDomainFilter.Tunnel },
            )
            XenoFilterChip(
                label = stringResource(R.string.xeno_filter_direct, directCount),
                selected = filter == XenoDomainFilter.Direct,
                onClick = { filter = XenoDomainFilter.Direct },
            )
            XenoFilterChip(
                label = stringResource(R.string.xeno_filter_block, blockCount),
                selected = filter == XenoDomainFilter.Block,
                onClick = { filter = XenoDomainFilter.Block },
            )
        }

        filtered.forEach { rule ->
            XenoDomainRuleRow(
                rule = rule,
                onCycleTarget = {
                    val next = when (rule.target) {
                        RoutingRuleTarget.Direct -> RoutingRuleTarget.Proxy
                        RoutingRuleTarget.Proxy -> RoutingRuleTarget.Block
                        RoutingRuleTarget.Block -> RoutingRuleTarget.Direct
                    }
                    onUpdate { current ->
                        current.copy(
                            customRules = current.customRules.map {
                                if (it.id == rule.id) it.copy(target = next) else it
                            },
                        )
                    }
                },
                onRemove = { onRemoveCustomRule(rule.id) },
            )
        }

        XenoDashedAddRuleButton(
            label = stringResource(R.string.xeno_add_rule),
            onClick = { showAddDialog = true },
        )

        XenoSettingsPlate {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    stringResource(R.string.xeno_ready_lists),
                    color = XenoText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                XenoPresetRow(
                    title = stringResource(R.string.xeno_preset_ru_direct),
                    checked = settings.presetRuDirect,
                    onCheckedChange = { enabled ->
                        onUpdate { it.copy(presetRuDirect = enabled) }
                    },
                )
                XenoPresetRow(
                    title = stringResource(R.string.xeno_preset_ads_block),
                    checked = settings.presetAdsBlock,
                    onCheckedChange = { enabled ->
                        onUpdate { it.copy(presetAdsBlock = enabled) }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showAddDialog) {
        Dialog(
            onDismissRequest = { showAddDialog = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
            XenoAddDomainRulesScreen(
                onDismiss = { showAddDialog = false },
                onConfirm = { lines, target ->
                    onAddCustomRules(lines, target)
                    showAddDialog = false
                },
            )
        }
    }
}

private enum class XenoDomainFilter { All, Tunnel, Direct, Block }

@Composable
private fun XenoFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) XenoTeal else XenoPlate)
            .border(1.dp, if (selected) XenoTeal else XenoStroke, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else XenoMuted,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun XenoDomainRuleRow(
    rule: RoutingRule,
    onCycleTarget: () -> Unit,
    onRemove: () -> Unit,
) {
    val display = when (rule.matcher) {
        RoutingRuleMatcher.IpCidr -> rule.value
        RoutingRuleMatcher.DomainSuffix -> {
            val v = rule.value.removePrefix("*.")
            "*.$v"
        }
    }
    val (label, tint) = when (rule.target) {
        RoutingRuleTarget.Proxy -> stringResource(R.string.xeno_rule_tunnel) to XenoTeal
        RoutingRuleTarget.Direct -> stringResource(R.string.xeno_rule_direct) to XenoMuted
        RoutingRuleTarget.Block -> stringResource(R.string.xeno_rule_block) to Color(0xFFE57373)
    }
    XenoSettingsPlate {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = display,
                color = XenoText,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onRemove),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                color = tint,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint.copy(alpha = 0.15f))
                    .clickable(onClick = onCycleTarget)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun XenoDashedAddRuleButton(
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                val stroke = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                )
                drawRoundRect(
                    color = XenoStroke,
                    style = stroke,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                )
            }
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+ $label",
            color = XenoTeal,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun XenoPresetRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            color = XenoText,
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        XenoAnimatedSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun XenoAddDomainRulesScreen(
    onDismiss: () -> Unit,
    onConfirm: (List<String>, RoutingRuleTarget) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var text by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf(TextRange(0)) }
    var target by remember { mutableStateOf(RoutingRuleTarget.Direct) }

    val analysis = remember(text) { RoutingRuleInput.parseDraft(text) }
    val annotated = remember(text, analysis) {
        buildAnnotatedString {
            append(text)
            analysis.lines.forEach { line ->
                if (line.kind == RoutingRuleLineKind.Invalid && line.start < line.end) {
                    addStyle(
                        SpanStyle(background = Color(0x33E57373)),
                        line.start,
                        line.end.coerceAtMost(text.length),
                    )
                }
            }
        }
    }
    val fieldValue = TextFieldValue(annotatedString = annotated, selection = selection)
    val canSubmit = analysis.validLines.isNotEmpty()
    val errorRed = Color(0xFFE57373)
    val errorBg = Color(0xFF2A1414)
    val tealSoft = Color(0xFF12352B)

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XenoBg)
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = null, tint = XenoText)
            }
            Text(
                text = stringResource(R.string.xeno_add_rules_title),
                color = XenoText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.xeno_one_rule_per_line),
                color = XenoMuted,
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.xeno_paste),
                color = XenoTeal,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable {
                    val clip = clipboard.getText()?.text.orEmpty()
                    if (clip.isNotBlank()) {
                        text = if (text.isBlank()) clip else text.trimEnd() + "\n" + clip
                        selection = TextRange(text.length)
                    }
                },
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        BasicTextField(
            value = fieldValue,
            onValueChange = {
                text = it.text
                selection = it.selection
            },
            textStyle = TextStyle(
                color = XenoText,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            ),
            cursorBrush = SolidColor(XenoTeal),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .weight(1f, fill = false)
                .clip(RoundedCornerShape(16.dp))
                .background(XenoPlate)
                .border(1.5.dp, XenoTeal, RoundedCornerShape(16.dp))
                .padding(14.dp)
                .verticalScroll(rememberScrollState()),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            XenoStatChip(
                text = pluralStringResource(R.plurals.xeno_rules_count, analysis.validLines.size, analysis.validLines.size),
                foreground = XenoTeal,
                background = tealSoft,
            )
            XenoStatChip(
                text = pluralStringResource(R.plurals.xeno_domains_count, analysis.domainCount, analysis.domainCount),
                foreground = XenoMuted,
                background = XenoPlate,
            )
            XenoStatChip(
                text = pluralStringResource(R.plurals.xeno_subnets_count, analysis.cidrCount, analysis.cidrCount),
                foreground = XenoMuted,
                background = XenoPlate,
            )
            if (analysis.errorCount > 0) {
                XenoStatChip(
                    text = pluralStringResource(R.plurals.xeno_errors_count, analysis.errorCount, analysis.errorCount),
                    foreground = errorRed,
                    background = errorBg,
                )
            }
        }

        analysis.firstError?.let { err ->
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(errorBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .background(errorRed, RoundedCornerShape(2.dp)),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.xeno_line_invalid_skip, err.number),
                    color = errorRed,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.xeno_action_for_all),
            color = XenoMuted,
            fontFamily = InterFontFamily,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        XenoTargetSegmented(
            selected = target,
            onSelect = { target = it },
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (canSubmit) XenoTeal else XenoTeal.copy(alpha = 0.35f))
                .clickable(enabled = canSubmit) {
                    onConfirm(analysis.validLines.map { it.raw }, target)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = pluralStringResource(
                    R.plurals.xeno_add_n_rules,
                    analysis.validLines.size,
                    analysis.validLines.size,
                ),
                color = Color.Black,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun XenoStatChip(
    text: String,
    foreground: Color,
    background: Color,
) {
    Text(
        text = text,
        color = foreground,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .border(1.dp, foreground.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}

@Composable
private fun XenoTargetSegmented(
    selected: RoutingRuleTarget,
    onSelect: (RoutingRuleTarget) -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(XenoPlate)
            .border(1.dp, XenoStroke, shape)
            .padding(4.dp),
    ) {
        listOf(
            RoutingRuleTarget.Proxy to stringResource(R.string.xeno_rule_tunnel),
            RoutingRuleTarget.Direct to stringResource(R.string.xeno_rule_direct),
            RoutingRuleTarget.Block to stringResource(R.string.xeno_rule_block),
        ).forEach { (value, label) ->
            val on = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) XenoTeal else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (on) Color.Black else XenoText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
