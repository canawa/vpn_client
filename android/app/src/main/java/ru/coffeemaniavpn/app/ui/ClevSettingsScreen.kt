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
import androidx.compose.material.icons.filled.Apps
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    onRefreshSubscription: () -> Unit,
    onDeleteSubscription: () -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
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
                .padding(top = 16.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.clev_settings),
                color = colors.espresso,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    tint = colors.mocha,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        ClevSegmentedTabs(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        HorizontalDivider(
            color = colors.latte,
            modifier = Modifier.padding(top = 8.dp),
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
                    onSave = onSaveConnectionSettings,
                    onRoutingModeChange = onTrafficRoutingModeChange,
                )
                ClevSettingsTab.Subscription -> ClevSubscriptionTab(
                    state = state,
                    onRefresh = onRefreshSubscription,
                    onDelete = onDeleteSubscription,
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
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .heightIn(min = 32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(Brush.linearGradient(listOf(colors.yellow, colors.amber)))
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(value) }
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
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
            .height(IntrinsicSize.Max)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (mode, label) ->
            val isSelected = selected == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (isSelected) {
                            Modifier.background(Brush.linearGradient(listOf(colors.yellow, colors.amber)))
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.Black else colors.espresso,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    lineHeight = 12.sp,
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
                .padding(top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.clev_app_routing),
                color = colors.espresso,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
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
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }

        when (routingMode) {
            AppRoutingMode.Off -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            tint = colors.latte,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = stringResource(R.string.clev_app_pick_mode),
                            color = colors.mocha,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
            }
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
        Icon(
            imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (selected) colors.yellow else colors.latte,
            modifier = Modifier.size(18.dp),
        )
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
    onSave: (ConnectionSettingsState) -> Unit,
    onRoutingModeChange: (TrafficRoutingMode) -> Unit,
) {
    val colors = coffemaniaColors()
    var newValue by remember { mutableStateOf("") }
    var newMatcher by remember { mutableStateOf(RoutingRuleMatcher.DomainSuffix) }
    var newTarget by remember { mutableStateOf(RoutingRuleTarget.Direct) }

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
        TrafficRoutingMode.entries.forEach { mode ->
            ClevRadioRow(
                label = mode.displayLabel(),
                selected = routingMode == mode,
                onClick = {
                    onRoutingModeChange(mode)
                    onSave(
                        when (mode) {
                            TrafficRoutingMode.GLOBAL -> settings.copy(sitesEnabled = false)
                            TrafficRoutingMode.SMART, TrafficRoutingMode.CUSTOM ->
                                settings.copy(sitesEnabled = true)
                        },
                    )
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
            Column(modifier = Modifier.weight(1f)) {
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
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            CoffemaniaSwitch(
                checked = settings.killSwitchEnabled,
                onCheckedChange = { onSave(settings.copy(killSwitchEnabled = it)) },
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
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                value = newValue,
                onValueChange = { newValue = it },
                placeholder = {
                    Text(
                        text = stringResource(
                            if (newMatcher == RoutingRuleMatcher.DomainSuffix) {
                                R.string.clev_rule_domain_placeholder
                            } else {
                                R.string.clev_rule_ip_placeholder
                            },
                        ),
                        color = colors.mocha,
                        fontSize = 13.sp,
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 160.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, colors.latte, RoundedCornerShape(6.dp)),
                textStyle = LocalTextStyle.current.copy(
                    color = colors.espresso,
                    fontSize = 13.sp,
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
            ClevRulePicker(
                label = stringResource(
                    if (newMatcher == RoutingRuleMatcher.DomainSuffix) {
                        R.string.clev_rule_domain
                    } else {
                        R.string.clev_rule_ip
                    },
                ),
                modifier = Modifier.width(96.dp),
            ) { dismiss ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clev_rule_domain)) },
                    onClick = {
                        newMatcher = RoutingRuleMatcher.DomainSuffix
                        dismiss()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clev_rule_ip)) },
                    onClick = {
                        newMatcher = RoutingRuleMatcher.IpCidr
                        dismiss()
                    },
                )
            }
            ClevRulePicker(
                label = stringResource(
                    if (newTarget == RoutingRuleTarget.Direct) {
                        R.string.clev_rule_bypass
                    } else {
                        R.string.clev_rule_via_vpn
                    },
                ),
                modifier = Modifier.width(112.dp),
            ) { dismiss ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clev_rule_bypass)) },
                    onClick = {
                        newTarget = RoutingRuleTarget.Direct
                        dismiss()
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.clev_rule_via_vpn)) },
                    onClick = {
                        newTarget = RoutingRuleTarget.Proxy
                        dismiss()
                    },
                )
            }
            IconButton(
                onClick = {
                    val trimmed = newValue.trim()
                    if (trimmed.isEmpty()) return@IconButton
                    onSave(
                        settings.copy(
                            customRules = settings.customRules + RoutingRule(
                                value = trimmed,
                                matcher = newMatcher,
                                target = newTarget,
                            ),
                        ),
                    )
                    newValue = ""
                },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = colors.yellow,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        settings.customRules.forEach { rule ->
            ClevRuleRow(
                rule = rule,
                onToggle = { enabled ->
                    onSave(
                        settings.copy(
                            customRules = settings.customRules.map {
                                if (it.id == rule.id) it.copy(isEnabled = enabled) else it
                            },
                        ),
                    )
                },
                onDelete = {
                    onSave(settings.copy(customRules = settings.customRules.filter { it.id != rule.id }))
                },
            )
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
private fun ClevRulePicker(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    val colors = coffemaniaColors()
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.latte, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = colors.espresso,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = colors.mocha,
                modifier = Modifier.size(18.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            content { expanded = false }
        }
    }
}

@Composable
private fun ClevRuleRow(
    rule: RoutingRule,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(colors.cappuccino)
            .padding(horizontal = 10.dp, vertical = 8.dp),
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
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = rule.value,
            color = colors.espresso,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
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
        )
        CoffemaniaSwitch(
            checked = rule.isEnabled,
            onCheckedChange = onToggle,
            modifier = Modifier.padding(start = 8.dp),
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ClevSubscriptionTab(
    state: MainUiState,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.clev_subscription),
                color = colors.espresso,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(11.dp))
                    .background(colors.cappuccino)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.mocha,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = if (info?.hasTitle == true) {
                            info.title
                        } else {
                            stringResource(R.string.clev_subscription_default_title)
                        },
                        color = colors.espresso,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = colors.mocha,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (info != null) {
                                formatSubscriptionTraffic(info.used, info.total)
                            } else {
                                "—"
                            },
                            color = colors.mocha,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    val expireLabel = info?.expireCalendarLabel()
                        ?: info?.expireLabel()?.takeIf { info.expire > 0 }
                    if (expireLabel != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = colors.mocha,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = expireLabel,
                                color = colors.mocha,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SubscriptionActionButton(
                        icon = {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = colors.yellow,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = colors.yellow,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                        text = stringResource(R.string.clev_refresh_subscription),
                        onClick = onRefresh,
                        enabled = !state.isLoading,
                    )
                    SubscriptionActionButton(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = colors.yellow,
                                modifier = Modifier.size(16.dp),
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
private fun SubscriptionActionButton(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = Modifier
            .widthIn(max = 160.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier.size(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = text,
            color = colors.yellow,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TrafficRoutingMode.displayLabel(): String = stringResource(
    when (this) {
        TrafficRoutingMode.GLOBAL -> R.string.clev_traffic_mode_global
        TrafficRoutingMode.SMART -> R.string.clev_traffic_mode_smart
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
