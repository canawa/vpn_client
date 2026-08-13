package ru.coffeemaniavpn.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.AltRoute
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

private enum class XenoSettingsPage { Main, Routing, Language }

private val XenoBg = Color(0xFF0A0D0C)
private val XenoPlate = Color(0xFF141B18)
private val XenoStroke = Color(0xFF222B28)
private val XenoTeal = Color(0xFF00D4A8)
private val XenoMuted = Color(0xFF6B7672)
private val XenoText = Color(0xFFF2F5F4)
private val XenoIconWell = Color(0xFF1A2420)
private val XenoDestructiveTint = Color(0xFFFF6B6B)
private val XenoDestructiveBg = Color(0xFF2A1414)

@Composable
fun XenoSettingsScreen(
    state: MainUiState,
    onUpdateConnectionSettings: ((ConnectionSettingsState) -> ConnectionSettingsState) -> Unit,
    onAddCustomRule: (String, RoutingRuleTarget) -> Unit,
    onAddCustomRules: (List<String>, RoutingRuleTarget) -> Unit,
    onRemoveCustomRule: (String) -> Unit,
    onReplaceSubscription: () -> Unit,
    onTrafficRoutingModeChange: (TrafficRoutingMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf(XenoSettingsPage.Main) }

    BackHandler(enabled = page != XenoSettingsPage.Main) {
        page = XenoSettingsPage.Main
    }

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
            onAddCustomRules = onAddCustomRules,
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
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    var showReplaceConfirm by remember { mutableStateOf(false) }
    val activeRules = state.connectionSettings.customRules.count { it.isEnabled }
    val smartRoutingOn = state.trafficRoutingMode == TrafficRoutingMode.CUSTOM
    val cabinetUrl = SubscriptionUrlValidator.websiteUrl("settings_cabinet")
    val botUrl = SubscriptionUrlValidator.telegramBotUrl("settings_bot")
    val supportUrl = state.subscriptionInfo?.supportURL?.takeIf { it.isNotBlank() }
        ?: botUrl
    val hasSubscription = state.subscriptionUrl.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XenoBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
    ) {
        XenoScreenHeader(title = stringResource(R.string.xeno_settings_title))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XenoProfileCard(info = state.subscriptionInfo)

            XenoSubscriptionCard(
                info = state.subscriptionInfo,
                hasSubscription = hasSubscription,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                XenoQuickLinkButton(
                    label = stringResource(R.string.xeno_web_cabinet),
                    icon = Icons.Outlined.Public,
                    modifier = Modifier.weight(1f),
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cabinetUrl))) },
                )
                XenoQuickLinkButton(
                    label = stringResource(R.string.xeno_tg_bot),
                    icon = Icons.AutoMirrored.Outlined.Send,
                    modifier = Modifier.weight(1f),
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(botUrl))) },
                )
            }

            XenoSettingsSection(title = stringResource(R.string.xeno_settings_connection)) {
                XenoSettingsNavToggleRow(
                    title = stringResource(R.string.xeno_smart_routing),
                    subtitle = stringResource(R.string.xeno_smart_routing_hint, activeRules),
                    icon = Icons.AutoMirrored.Outlined.AltRoute,
                    checked = smartRoutingOn,
                    onCheckedChange = { enabled ->
                        onTrafficRoutingModeChange(
                            if (enabled) TrafficRoutingMode.CUSTOM else TrafficRoutingMode.GLOBAL,
                        )
                    },
                    onOpen = onOpenRouting,
                )
                XenoSettingsDivider()
                XenoSettingsValueRow(
                    title = stringResource(R.string.xeno_dns),
                    value = stringResource(R.string.xeno_dns_internal),
                    icon = Icons.Outlined.Dns,
                    onClick = null,
                )
                XenoSettingsDivider()
                XenoSettingsToggleRow(
                    title = stringResource(R.string.clev_kill_switch),
                    subtitle = null,
                    icon = Icons.Outlined.Shield,
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
                    icon = Icons.Outlined.SupportAgent,
                    trailingIcon = Icons.AutoMirrored.Outlined.Send,
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(supportUrl))) },
                )
                XenoSettingsDivider()
                XenoSettingsValueRow(
                    title = stringResource(R.string.clev_language),
                    value = when (state.appLanguage) {
                        AppLanguage.RU -> stringResource(R.string.clev_lang_ru)
                        AppLanguage.EN -> stringResource(R.string.clev_lang_en)
                    },
                    icon = Icons.Outlined.Language,
                    onClick = onOpenLanguage,
                )
            }

            XenoSettingsSection(title = stringResource(R.string.xeno_settings_about)) {
                XenoSettingsValueRow(
                    title = stringResource(R.string.xeno_version),
                    value = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    icon = Icons.Outlined.Info,
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
                    icon = Icons.Outlined.ContentCopy,
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
                    icon = Icons.Outlined.Add,
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
    val title = info?.title?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.clev_subscription_default_title)
    val initials = profileInitials(title)
    val statusLine = when {
        info == null -> stringResource(R.string.xeno_profile_inactive)
        info.isExpired() -> stringResource(R.string.clev_subscription_expired)
        else -> stringResource(R.string.xeno_profile_active)
    }
    // Figma: 355×71, r16, bg #121A17, border #222B28
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(71.dp)
            .clip(shape)
            .background(Color(0xFF121A17))
            .border(1.dp, Color(0xFF222B28), shape)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF12352B))
                    .border(1.dp, XenoTeal.copy(alpha = 0.55f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    color = XenoTeal,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = XenoText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = statusLine,
                    color = XenoMuted,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun XenoSettingsSection(
    title: String,
    cardHeight: Dp? = null,
    content: @Composable () -> Unit,
) {
    // Figma Connection: 355×171, r16, bg #121A17, border #222B28
    val shape = RoundedCornerShape(16.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            color = XenoMuted,
            fontFamily = InterFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (cardHeight != null) Modifier.height(cardHeight) else Modifier)
                .clip(shape)
                .background(Color(0xFF121A17))
                .border(1.dp, Color(0xFF222B28), shape)
                .padding(horizontal = 14.dp, vertical = if (cardHeight != null) 6.dp else 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (cardHeight != null) Modifier.fillMaxHeight() else Modifier),
                verticalArrangement = if (cardHeight != null) {
                    Arrangement.SpaceEvenly
                } else {
                    Arrangement.Top
                },
            ) {
                content()
            }
        }
    }
}

@Composable
private fun XenoSettingsDivider() {
    HorizontalDivider(color = XenoStroke, thickness = 1.dp)
}

@Composable
private fun XenoSettingsToggleRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        XenoSettingsRowIcon(icon)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = XenoText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            subtitle?.let {
                Text(
                    text = it,
                    color = XenoMuted,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                )
            }
        }
        ClevAnimatedSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

/** Left side opens the page; switch only toggles — avoids nested clickable conflicts. */
@Composable
private fun XenoSettingsNavToggleRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            XenoSettingsRowIcon(icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = XenoText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        color = XenoMuted,
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = XenoMuted,
                modifier = Modifier.size(20.dp),
            )
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
    icon: ImageVector,
    onClick: (() -> Unit)?,
    value: String? = null,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        XenoSettingsRowIcon(icon)
        Text(
            text = title,
            color = XenoText,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        when {
            trailingIcon != null -> {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(XenoIconWell)
                        .border(1.dp, Color(0xFF222B28), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = XenoTeal,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            !value.isNullOrBlank() -> {
                Text(
                    text = value,
                    color = XenoMuted,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun XenoSettingsRowIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(XenoIconWell)
            .border(1.dp, XenoStroke, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = XenoTeal,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun XenoQuickLinkButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Figma: 173×55, r16, bg #121A17, border #222B28
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(55.dp)
            .clip(shape)
            .background(Color(0xFF121A17))
            .border(1.dp, Color(0xFF222B28), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(XenoIconWell)
                    .border(1.dp, Color(0xFF222B28), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = XenoTeal,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = label,
                color = XenoText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun XenoFooterButton(
    label: String,
    icon: ImageVector,
    destructive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Figma Copy link: 174×61, r16, bg #121A17, border #222B28
    // Label: 90×34, Inter Regular 14, #F2F5F4, lh 100%
    val tint = if (destructive) XenoDestructiveTint else XenoTeal
    val bg = if (destructive) XenoDestructiveBg else Color(0xFF121A17)
    val border = if (destructive) Color(0xFF3A1C1C) else Color(0xFF222B28)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(61.dp)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
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
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = label,
                color = Color(0xFFF2F5F4),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(XenoBg)
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
                            .height(48.dp)
                            .clickable { onLanguageChange(lang) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = when (lang) {
                                AppLanguage.RU -> stringResource(R.string.clev_lang_ru)
                                AppLanguage.EN -> stringResource(R.string.clev_lang_en)
                            },
                            color = XenoText,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier.size(18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (language == lang) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = XenoTeal,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
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
                tint = XenoText,
            )
        }
        Text(
            text = title,
            color = XenoText,
            fontFamily = InterFontFamily,
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
