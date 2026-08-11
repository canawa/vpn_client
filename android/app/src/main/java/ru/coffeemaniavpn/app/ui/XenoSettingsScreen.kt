package ru.coffeemaniavpn.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.BuildConfig
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.AppLanguage
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.RoutingRuleTarget
import ru.coffeemaniavpn.app.data.SubscriptionInfo
import ru.coffeemaniavpn.app.data.SubscriptionUrlValidator
import ru.coffeemaniavpn.app.data.TrafficRoutingMode
import ru.coffeemaniavpn.app.data.formatTrafficBytes

private enum class XenoSettingsPage { Main, Routing, Language }

@Composable
fun XenoSettingsScreen(
    state: MainUiState,
    onUpdateConnectionSettings: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onAddCustomRule: (String, RoutingRuleTarget) -> Unit,
    onRemoveCustomRule: (String) -> Unit,
    onReplaceSubscription: () -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf(XenoSettingsPage.Main) }

    when (page) {
        XenoSettingsPage.Main -> XenoSettingsMain(
            state = state,
            modifier = modifier,
            onOpenRouting = { page = XenoSettingsPage.Routing },
            onOpenLanguage = { page = XenoSettingsPage.Language },
            onUpdateConnectionSettings = onUpdateConnectionSettings,
            onTrafficRoutingModeChange = onTrafficRoutingModeChange,
            onReplaceSubscription = onReplaceSubscription,
        )
        XenoSettingsPage.Routing -> XenoRoutingScreen(
            state = state,
            modifier = modifier,
            onBack = { page = XenoSettingsPage.Main },
            onUpdateConnectionSettings = onUpdateConnectionSettings,
            onAddCustomRule = onAddCustomRule,
            onRemoveCustomRule = onRemoveCustomRule,
            onTrafficRoutingModeChange = onTrafficRoutingModeChange,
        )
        XenoSettingsPage.Language -> XenoSettingsLanguagePage(
            language = state.appLanguage,
            modifier = modifier,
            onBack = { page = XenoSettingsPage.Main },
            onLanguageChange = {
                onLanguageChange(it)
                page = XenoSettingsPage.Main
            },
        )
    }
}

@Composable
private fun XenoSettingsMain(
    state: MainUiState,
    onOpenRouting: () -> Unit,
    onOpenLanguage: () -> Unit,
    onUpdateConnectionSettings: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onReplaceSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    var showReplaceConfirm by remember { mutableStateOf(false) }
    val activeRules = state.connectionSettings.customRules.count { it.isEnabled }
    val smartRoutingOn = state.trafficRoutingMode == TrafficRoutingMode.CUSTOM
    val cabinetUrl = SubscriptionUrlValidator.websiteUrl("settings_cabinet")
    val botUrl = SubscriptionUrlValidator.telegramBotUrl("settings_bot")
    val supportUrl = state.subscriptionInfo?.supportURL?.takeIf { it.isNotBlank() }
        ?: botUrl

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        XenoScreenHeader(title = stringResource(R.string.xeno_settings_title))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XenoProfileCard(info = state.subscriptionInfo)

            XenoSettingsSubscriptionCard(info = state.subscriptionInfo)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                XenoQuickLinkButton(
                    label = stringResource(R.string.xeno_web_cabinet),
                    modifier = Modifier.weight(1f),
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cabinetUrl))) },
                )
                XenoQuickLinkButton(
                    label = stringResource(R.string.xeno_tg_bot),
                    modifier = Modifier.weight(1f),
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(botUrl))) },
                )
            }

            XenoSettingsSection(title = stringResource(R.string.xeno_settings_connection)) {
                XenoSettingsToggleRow(
                    title = stringResource(R.string.xeno_smart_routing),
                    subtitle = stringResource(R.string.xeno_smart_routing_hint, activeRules),
                    checked = smartRoutingOn,
                    onCheckedChange = { enabled ->
                        onTrafficRoutingModeChange(
                            if (enabled) TrafficRoutingMode.CUSTOM else TrafficRoutingMode.GLOBAL,
                        )
                    },
                    onClick = onOpenRouting,
                )
                XenoSettingsDivider()
                XenoSettingsValueRow(
                    title = stringResource(R.string.xeno_dns),
                    value = stringResource(R.string.xeno_dns_internal),
                    onClick = null,
                )
                XenoSettingsDivider()
                XenoSettingsToggleRow(
                    title = stringResource(R.string.clev_kill_switch),
                    subtitle = null,
                    checked = state.connectionSettings.killSwitchEnabled,
                    onCheckedChange = { enabled ->
                        onUpdateConnectionSettings { it.copy(killSwitchEnabled = enabled) }
                    },
                    onClick = null,
                )
            }

            XenoSettingsSection(title = stringResource(R.string.xeno_settings_application)) {
                XenoSettingsValueRow(
                    title = stringResource(R.string.xeno_need_help),
                    value = stringResource(R.string.xeno_support_tg),
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl))) },
                )
                XenoSettingsDivider()
                XenoSettingsValueRow(
                    title = stringResource(R.string.clev_language),
                    value = when (state.appLanguage) {
                        AppLanguage.RU -> stringResource(R.string.clev_lang_ru)
                        AppLanguage.EN -> stringResource(R.string.clev_lang_en)
                    },
                    onClick = onOpenLanguage,
                )
            }

            XenoSettingsSection(title = stringResource(R.string.xeno_settings_about)) {
                XenoSettingsValueRow(
                    title = stringResource(R.string.xeno_version),
                    value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = null,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                XenoFooterButton(
                    label = if (copied) {
                        stringResource(R.string.settings_copied)
                    } else {
                        stringResource(R.string.xeno_copy_link)
                    },
                    destructive = false,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val url = state.subscriptionUrl.trim()
                        if (url.isNotBlank()) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("subscription", url))
                            copied = true
                        }
                    },
                )
                XenoFooterButton(
                    label = stringResource(R.string.xeno_add_new_config),
                    destructive = true,
                    modifier = Modifier.weight(1f),
                    onClick = { showReplaceConfirm = true },
                )
            }
        }
    }

    if (showReplaceConfirm) {
        AlertDialog(
            onDismissRequest = { showReplaceConfirm = false },
            title = { Text(stringResource(R.string.xeno_replace_config_title)) },
            text = { Text(stringResource(R.string.xeno_replace_config_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReplaceConfirm = false
                        onReplaceSubscription()
                    },
                ) {
                    Text(stringResource(R.string.clev_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirm = false }) {
                    Text(stringResource(R.string.clev_cancel))
                }
            },
        )
    }
}

@Composable
private fun XenoProfileCard(info: SubscriptionInfo?) {
    val colors = coffemaniaColors()
    val title = info?.title?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.clev_subscription_default_title)
    val initials = profileInitials(title)
    val statusLine = when {
        info == null -> stringResource(R.string.xeno_profile_inactive)
        info.isExpired() -> stringResource(R.string.clev_subscription_expired)
        else -> stringResource(R.string.xeno_profile_active)
    }

    XenoCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF12352B))
                    .border(1.dp, colors.primary.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = colors.espresso,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusLine,
                    color = colors.mocha,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun XenoSettingsSubscriptionCard(info: SubscriptionInfo?) {
    val colors = coffemaniaColors()
    XenoCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SUBSCRIPTION",
                    color = colors.mocha,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp,
                )
                if (info != null && !info.isExpired()) {
                    XenoStatusBadge(text = "ACTIVE")
                }
            }

            if (info == null) {
                Text(
                    text = stringResource(R.string.xeno_sub_not_imported),
                    color = colors.espresso,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    val left = if (info.isDisplayUnlimitedTraffic()) {
                        stringResource(R.string.subscription_traffic_unlimited)
                    } else if (info.total > 0) {
                        val remaining = (info.total - info.used).coerceAtLeast(0)
                        "${formatTrafficBytes(remaining)} left"
                    } else {
                        formatTrafficBytes(info.used)
                    }
                    Text(
                        text = left,
                        color = colors.espresso,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        info.expireCalendarLabel()?.let {
                            Text(
                                text = stringResource(R.string.xeno_expires_prefix, it),
                                color = colors.mocha,
                                fontSize = 11.sp,
                            )
                        }
                        info.expireLabel()?.let {
                            Text(
                                text = it,
                                color = colors.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                if (info.total > 0 && !info.isDisplayUnlimitedTraffic()) {
                    LinearProgressIndicator(
                        progress = { 1f - info.usageFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = colors.primary,
                        trackColor = colors.latte,
                        strokeCap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun XenoSettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = coffemaniaColors()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            color = colors.mocha,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
        XenoCard(content = content)
    }
}

@Composable
private fun XenoSettingsDivider() {
    HorizontalDivider(color = coffemaniaColors().latte, thickness = 1.dp)
}

@Composable
private fun XenoSettingsToggleRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        XenoSettingsIconPlaceholder()
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = coffemaniaColors().espresso,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            subtitle?.let {
                Text(text = it, color = coffemaniaColors().mocha, fontSize = 12.sp)
            }
        }
        ClevAnimatedSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun XenoSettingsValueRow(
    title: String,
    value: String,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        XenoSettingsIconPlaceholder()
        Text(
            text = title,
            color = coffemaniaColors().espresso,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = coffemaniaColors().mocha,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun XenoSettingsIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(coffemaniaColors().surfaceVariant),
    )
}

@Composable
private fun XenoQuickLinkButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    XenoCard(modifier = modifier, onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surfaceVariant),
            )
            Text(
                text = label,
                color = colors.espresso,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun XenoFooterButton(
    label: String,
    destructive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val tint = if (destructive) Color(0xFFFF6B6B) else colors.primary
    val bg = if (destructive) Color(0xFF2A1414) else colors.cappuccino
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, colors.latte, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(tint.copy(alpha = 0.15f)),
            )
            Text(
                text = label,
                color = colors.espresso,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun XenoSettingsLanguagePage(
    language: AppLanguage,
    onBack: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam)
            .verticalScroll(rememberScrollState()),
    ) {
        XenoSubpageHeader(
            title = stringResource(R.string.clev_language),
            onBack = onBack,
        )
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            XenoCard {
                AppLanguage.selectable.forEachIndexed { index, lang ->
                    if (index > 0) XenoSettingsDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageChange(lang) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = when (lang) {
                                AppLanguage.RU -> stringResource(R.string.clev_lang_ru)
                                AppLanguage.EN -> stringResource(R.string.clev_lang_en)
                            },
                            color = colors.espresso,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        if (language == lang) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun XenoSubpageHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = coffemaniaColors().espresso,
            )
        }
        Text(
            text = title,
            color = coffemaniaColors().espresso,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

private fun profileInitials(title: String): String {
    val parts = title.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        title.length >= 2 -> title.take(2).uppercase()
        title.isNotEmpty() -> title.first().uppercaseChar().toString()
        else -> "X"
    }
}
