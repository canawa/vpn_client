package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.LocalTextStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.R
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.CircularProgressIndicator
import ru.coffeemaniavpn.app.data.AppLanguage
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.RoutingRule
import ru.coffeemaniavpn.app.data.RoutingRuleMatcher
import ru.coffeemaniavpn.app.data.RoutingRuleTarget
import ru.coffeemaniavpn.app.data.InstalledAppsLoader
import ru.coffeemaniavpn.app.data.SplitTunnelAppsMode
import ru.coffeemaniavpn.app.data.TrafficRoutingMode
import android.content.Intent
import android.net.Uri

enum class ClevSettingsTab {
    Apps,
    Rules,
    Subscription,
    Language,
}

private enum class AppRoutingMode {
    Off,
    BypassSelected,
    OnlySelected,
}

private fun ConnectionSettingsState.routingMode(): AppRoutingMode = when {
    !appsEnabled -> AppRoutingMode.Off
    appsMode == SplitTunnelAppsMode.ExcludeSelected -> AppRoutingMode.BypassSelected
    else -> AppRoutingMode.OnlySelected
}

private fun ConnectionSettingsState.withRoutingMode(mode: AppRoutingMode): ConnectionSettingsState = when (mode) {
    AppRoutingMode.Off -> copy(appsEnabled = false)
    AppRoutingMode.BypassSelected -> copy(appsEnabled = true, appsMode = SplitTunnelAppsMode.ExcludeSelected)
    AppRoutingMode.OnlySelected -> copy(appsEnabled = true, appsMode = SplitTunnelAppsMode.IncludeOnly)
}

@Composable
fun ClevSettingsHost(
    state: MainUiState,
    onClose: () -> Unit,
    onSaveConnectionSettings: (ConnectionSettingsState) -> Unit,
    onUpdateConnectionSettings: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onAddCustomRule: (String, RoutingRuleTarget) -> Unit,
    onRemoveCustomRule: (String) -> Unit,
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onExportLogs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    var tab by remember { mutableStateOf(ClevSettingsTab.Apps) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.clev_settings),
                color = colors.espresso,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.cappuccino)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.espresso.copy(alpha = 0.72f),
                    modifier = Modifier.size(13.dp),
                )
            }
        }

        ClevSegmentedTabs(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 12.dp),
        )

        Box(modifier = Modifier.weight(1f)) {
            when (tab) {
                ClevSettingsTab.Apps -> ClevAppsTab(
                    settings = state.connectionSettings,
                    onSave = onSaveConnectionSettings,
                )
                ClevSettingsTab.Rules -> ClevRulesTab(
                    settings = state.connectionSettings,
                    routingMode = state.trafficRoutingMode,
                    onUpdateSettings = onUpdateConnectionSettings,
                    onAddCustomRule = onAddCustomRule,
                    onRemoveCustomRule = onRemoveCustomRule,
                    onRoutingModeChange = onTrafficRoutingModeChange,
                )
                ClevSettingsTab.Subscription -> ClevSubscriptionTab(
                    state = state,
                    onRefresh = onRefreshSubscription,
                    onDelete = onDeleteSubscription,
                    onExportLogs = onExportLogs,
                )
                ClevSettingsTab.Language -> ClevLanguageTab(
                    language = state.appLanguage,
                    onChange = onLanguageChange,
                )
            }
        }
    }
}

@Composable
private fun ClevSegmentedTabs(
    selected: ClevSettingsTab,
    onSelect: (ClevSettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val tabs = listOf(
        ClevSettingsTab.Apps to stringResource(R.string.clev_apps),
        ClevSettingsTab.Rules to stringResource(R.string.clev_rules),
        ClevSettingsTab.Subscription to stringResource(R.string.clev_subscription),
        ClevSettingsTab.Language to stringResource(R.string.clev_language),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, (value, label) ->
            if (index > 0) {
                val prevSelected = selected == tabs[index - 1].first
                val currSelected = selected == value
                if (!prevSelected && !currSelected) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .width(1.dp)
                            .height(18.dp)
                            .background(colors.latte.copy(alpha = 0.55f)),
                    )
                }
            }
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(colors.yellow)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = ClevFontFamily,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = (-0.2).sp,
                    color = if (isSelected) Color.Black else colors.espresso,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ClevThreeWaySegment(
    options: List<Pair<AppRoutingMode, String>>,
    selected: AppRoutingMode,
    onSelect: (AppRoutingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceVariant)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, (mode, label) ->
            if (index > 0) {
                val prevSelected = selected == options[index - 1].first
                val currSelected = selected == mode
                if (!prevSelected && !currSelected) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.dp)
                            .width(1.dp)
                            .height(20.dp)
                            .background(colors.latte.copy(alpha = 0.55f)),
                    )
                }
            }
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(colors.yellow)
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontFamily = ClevFontFamily,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = (-0.25).sp,
                    color = if (isSelected) Color.Black else colors.espresso,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ClevAppsTab(
    settings: ConnectionSettingsState,
    onSave: (ConnectionSettingsState) -> Unit,
) {
    val colors = coffemaniaColors()
    val context = LocalContext.current
    val routingMode = settings.routingMode()

    var apps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var selected by remember(settings.appPackages) { mutableStateOf(settings.appPackages) }

    LaunchedEffect(Unit) {
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
        onSave(settings.copy(appPackages = packages))
    }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.clev_app_routing),
                color = colors.espresso,
                fontFamily = ClevFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )

            ClevThreeWaySegment(
                options = listOf(
                    AppRoutingMode.Off to stringResource(R.string.clev_app_mode_off),
                    AppRoutingMode.BypassSelected to stringResource(R.string.clev_app_mode_bypass),
                    AppRoutingMode.OnlySelected to stringResource(R.string.clev_app_mode_only),
                ),
                selected = routingMode,
                onSelect = { mode -> onSave(settings.withRoutingMode(mode)) },
            )

            Text(
                text = when (routingMode) {
                    AppRoutingMode.Off -> stringResource(R.string.clev_app_hint_off)
                    AppRoutingMode.BypassSelected -> stringResource(R.string.clev_app_hint_bypass)
                    AppRoutingMode.OnlySelected -> stringResource(R.string.clev_app_hint_only)
                },
                color = colors.mocha,
                fontFamily = ClevFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        when (routingMode) {
            AppRoutingMode.Off -> Unit
            else -> {
                ClevSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = stringResource(R.string.clev_search_apps),
                    modifier = Modifier.padding(horizontal = 20.dp),
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.clev_apps_loading), color = colors.mocha, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 16.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            val isOn = app.packageName in selected
                            ClevAppRow(
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
    }
}

@Composable
private fun ClevAppRow(
    label: String,
    packageName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(colors.cappuccino)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ClevAppIcon(packageName = packageName)
        Text(
            text = label,
            color = colors.espresso,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        ClevSelectionIndicator(selected = selected)
    }
}

@Composable
private fun ClevAppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap(width = 96, height = 96)
                .asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(26.dp),
        )
    } else {
        Box(modifier = modifier.size(26.dp))
    }
}

@Composable
private fun ClevSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, colors.latte, RoundedCornerShape(8.dp)),
        placeholder = {
            Text(placeholder, color = colors.mocha, fontSize = 14.sp)
        },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.mocha, modifier = Modifier.size(18.dp))
        },
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = colors.espresso,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceVariant,
            unfocusedContainerColor = colors.surfaceVariant,
            disabledContainerColor = colors.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = colors.yellow,
        ),
    )
}

@Composable
private fun ClevRulesTab(
    settings: ConnectionSettingsState,
    routingMode: TrafficRoutingMode,
    onUpdateSettings: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onAddCustomRule: (String, RoutingRuleTarget) -> Unit,
    onRemoveCustomRule: (String) -> Unit,
    onRoutingModeChange: (TrafficRoutingMode) -> Unit,
) {
    val colors = coffemaniaColors()
    var newValue by remember { mutableStateOf("") }
    var newTarget by remember { mutableStateOf(RoutingRuleTarget.Direct) }

    fun addRule() {
        val trimmed = newValue.trim()
        if (trimmed.isEmpty()) return
        onAddCustomRule(trimmed, newTarget)
        newValue = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.clev_traffic_mode),
            color = colors.espresso,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        TrafficRoutingMode.selectable.forEach { mode ->
            ClevRadioRow(
                label = mode.displayLabel(),
                selected = routingMode == mode,
                onClick = {
                    onRoutingModeChange(mode)
                    onUpdateSettings { current ->
                        when (mode) {
                            TrafficRoutingMode.GLOBAL -> current.copy(sitesEnabled = false)
                            TrafficRoutingMode.CUSTOM -> current.copy(sitesEnabled = true)
                        }
                    }
                },
            )
        }

        HorizontalDivider(color = colors.latte)

        Text(
            text = stringResource(R.string.clev_security),
            color = colors.espresso,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            ) {
                Text(
                    text = stringResource(R.string.clev_kill_switch),
                    color = colors.espresso,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    text = stringResource(R.string.clev_kill_switch_hint),
                    color = colors.mocha,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            CoffemaniaSwitch(
                checked = settings.killSwitchEnabled,
                onCheckedChange = { enabled ->
                    onUpdateSettings { it.copy(killSwitchEnabled = enabled) }
                },
            )
        }

        HorizontalDivider(color = colors.latte)

        Text(
            text = stringResource(R.string.clev_my_rules),
            color = colors.espresso,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = newValue,
                onValueChange = { newValue = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.clev_rule_domain_placeholder),
                        color = colors.mocha,
                        fontSize = 14.sp,
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, colors.latte, RoundedCornerShape(8.dp)),
                textStyle = LocalTextStyle.current.copy(
                    color = colors.espresso,
                    fontSize = 14.sp,
                ),
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceVariant,
                    unfocusedContainerColor = colors.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = colors.yellow,
                ),
            )
            IconButton(
                onClick = { addRule() },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = stringResource(R.string.clev_rule_add),
                    tint = colors.yellow,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ClevRuleTargetChip(
                label = stringResource(R.string.clev_rule_bypass),
                selected = newTarget == RoutingRuleTarget.Direct,
                selectedColor = colors.orange,
                onClick = { newTarget = RoutingRuleTarget.Direct },
                modifier = Modifier.weight(1f),
            )
            ClevRuleTargetChip(
                label = stringResource(R.string.clev_rule_via_vpn),
                selected = newTarget == RoutingRuleTarget.Proxy,
                selectedColor = colors.green,
                onClick = { newTarget = RoutingRuleTarget.Proxy },
                modifier = Modifier.weight(1f),
            )
        }

        if (settings.customRules.isEmpty()) {
            Text(
                text = stringResource(R.string.clev_rules_empty),
                color = colors.mocha,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        } else {
            settings.customRules.forEach { rule ->
                ClevRuleRow(
                    rule = rule,
                    onToggle = { enabled ->
                        onUpdateSettings { current ->
                            current.copy(
                                customRules = current.customRules.map {
                                    if (it.id == rule.id) it.copy(isEnabled = enabled) else it
                                },
                            )
                        }
                    },
                    onDelete = { onRemoveCustomRule(rule.id) },
                )
            }
        }
    }
}

@Composable
private fun ClevRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) colors.yellow else colors.latte,
                        shape = CircleShape,
                    ),
            )
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.yellow),
                )
            }
        }
        Text(
            text = label,
            color = colors.espresso,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ClevRuleTargetChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) selectedColor.copy(alpha = 0.14f) else colors.surfaceVariant)
            .border(
                width = 1.dp,
                color = if (selected) selectedColor else colors.latte,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) selectedColor else colors.espresso,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ClevRuleRow(
    rule: RoutingRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = coffemaniaColors()
    val contentAlpha = if (rule.isEnabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(colors.cappuccino)
            .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (rule.matcher == RoutingRuleMatcher.DomainSuffix) {
                Icons.Default.Language
            } else {
                Icons.Default.Numbers
            },
            contentDescription = null,
            tint = colors.mocha,
            modifier = Modifier
                .size(18.dp)
                .alpha(contentAlpha),
        )
        Text(
            text = rule.value,
            color = colors.espresso,
            fontFamily = ClevFontFamily,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .alpha(contentAlpha),
        )
        Text(
            text = stringResource(
                if (rule.target == RoutingRuleTarget.Direct) {
                    R.string.clev_rule_bypass
                } else {
                    R.string.clev_rule_via_vpn
                },
            ),
            color = if (rule.target == RoutingRuleTarget.Direct) colors.orange else colors.green,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alpha(contentAlpha),
        )
        Spacer(modifier = Modifier.width(8.dp))
        ClevRuleActionGroup(
            checked = rule.isEnabled,
            onCheckedChange = onToggle,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun ClevSubscriptionTab(
    state: MainUiState,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onExportLogs: () -> Unit,
) {
    val colors = coffemaniaColors()
    val context = LocalContext.current
    val info = state.subscriptionInfo
    val supportUrl = info?.supportURL?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.clev_support_url)
    val supportBlue = Color(0xFF0A84FF)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.clev_subscription),
                color = colors.espresso,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cappuccino)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(1.dp, colors.mocha.copy(alpha = 0.65f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = colors.espresso,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    Text(
                        text = if (info?.hasTitle == true) {
                            info.title
                        } else {
                            stringResource(R.string.clev_subscription_default_title)
                        },
                        color = colors.espresso,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                SubscriptionStatsPill(
                    used = info?.used,
                    expireLabel = info?.expireCalendarLabel()
                        ?: info?.expireLabel()?.takeIf { info.expire > 0 },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SubscriptionActionButton(
                        modifier = Modifier.weight(1f),
                        backgroundColor = colors.milkFoam,
                        icon = {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 1.75.dp,
                                    color = colors.yellow,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = colors.yellow,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        text = stringResource(R.string.clev_refresh_subscription),
                        onClick = onRefresh,
                        enabled = !state.isLoading,
                    )
                    SubscriptionActionButton(
                        modifier = Modifier.weight(1f),
                        backgroundColor = colors.milkFoam,
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = colors.yellow,
                                modifier = Modifier.size(14.dp),
                            )
                        },
                        text = stringResource(R.string.clev_delete_key_logout),
                        onClick = onDelete,
                    )
                }
            }

            state.error?.let { error ->
                Text(
                    text = error,
                    color = colors.error,
                    fontSize = 12.sp,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onExportLogs)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = colors.mocha,
                    modifier = Modifier.size(16.dp),
                )
                Column {
                    Text(
                        text = stringResource(R.string.clev_export_logs),
                        color = colors.espresso,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.clev_export_logs_hint),
                        color = colors.mocha,
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl)))
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = supportBlue,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(R.string.clev_support),
                    color = supportBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            ClevLogoFull(
                modifier = Modifier.alpha(0.5f),
                logoHeight = 16.dp,
            )
        }
    }
}

@Composable
private fun SubscriptionTrafficArrows(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-1).dp),
    ) {
        Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp),
        )
        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp),
        )
    }
}

@Composable
private fun SubscriptionStatsPill(
    used: Long?,
    expireLabel: String?,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(colors.milkFoam)
            .border(1.dp, colors.latte.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.weight(1f),
        ) {
            SubscriptionTrafficArrows(tint = colors.mocha)
            Text(
                text = if (used != null) {
                    formatSubscriptionTraffic(used, 0)
                } else {
                    "—"
                },
                color = colors.mocha,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (expireLabel != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = colors.mocha,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = expireLabel,
                    color = colors.mocha,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SubscriptionActionButton(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    enabled: Boolean = true,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = text,
            color = colors.yellow,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TrafficRoutingMode.displayLabel(): String = stringResource(
    when (this) {
        TrafficRoutingMode.GLOBAL -> R.string.clev_traffic_mode_global
        TrafficRoutingMode.CUSTOM -> R.string.clev_traffic_mode_custom
    },
)

@Composable
private fun ClevLanguageTab(
    language: AppLanguage,
    onChange: (AppLanguage) -> Unit,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.clev_language),
            color = colors.espresso,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(colors.cappuccino),
        ) {
            AppLanguage.selectable.forEachIndexed { index, lang ->
                if (index > 0) {
                    HorizontalDivider(color = colors.latte)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChange(lang) }
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = when (lang) {
                            AppLanguage.RU -> stringResource(R.string.clev_lang_ru)
                            AppLanguage.EN -> stringResource(R.string.clev_lang_en)
                        },
                        color = colors.espresso,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f),
                    )
                    if (language == lang) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = colors.yellow,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
