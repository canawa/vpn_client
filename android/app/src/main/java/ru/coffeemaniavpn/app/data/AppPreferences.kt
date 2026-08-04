package ru.coffeemaniavpn.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.coffeemaniavpn.app.util.AppLog

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "coffemania_vpn")

class AppPreferences(private val context: Context) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val subscriptionUrl: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_SUBSCRIPTION_URL].orEmpty() }
        .flowOn(Dispatchers.IO)

    val selectedNodeId: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[KEY_SELECTED_NODE_ID] }
        .flowOn(Dispatchers.IO)

    val nodes: Flow<List<ProxyNode>> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_NODES].orEmpty()
            if (raw.isBlank()) {
                emptyList()
            } else {
                runCatching {
                    json.decodeFromString<List<ProxyNode>>(raw)
                }.onFailure {
                    AppLog.e("Failed to decode saved nodes, rawLen=${raw.length}", it)
                }.getOrDefault(emptyList())
            }
        }
        .flowOn(Dispatchers.IO)

    val subscriptionInfo: Flow<SubscriptionInfo?> = context.dataStore.data
        .map { prefs ->
            val raw = prefs[KEY_SUBSCRIPTION_INFO].orEmpty()
            if (raw.isBlank()) {
                null
            } else {
                runCatching {
                    json.decodeFromString<SubscriptionInfo>(raw)
                }.onFailure {
                    AppLog.e("Failed to decode subscription info", it)
                }.getOrNull()
            }
        }
        .flowOn(Dispatchers.IO)

    suspend fun saveSubscription(
        url: String,
        nodes: List<ProxyNode>,
        selectedNodeId: String?,
        info: SubscriptionInfo?,
    ) {
        AppLog.i(
            "saveSubscription urlLen=${url.length} nodes=${nodes.size} selected=$selectedNodeId info=$info",
        )
        context.dataStore.edit { prefs ->
            prefs[KEY_SUBSCRIPTION_URL] = url.trim()
            prefs[KEY_NODES] = json.encodeToString(nodes)
            prefs[KEY_SUB_LAST_AUTO_REFRESH_MS] = System.currentTimeMillis()
            if (info != null) {
                prefs[KEY_SUBSCRIPTION_INFO] = json.encodeToString(info)
            } else {
                prefs.remove(KEY_SUBSCRIPTION_INFO)
            }
            if (selectedNodeId != null) {
                prefs[KEY_SELECTED_NODE_ID] = selectedNodeId
            }
        }
    }

    suspend fun setSelectedNodeId(nodeId: String) {
        AppLog.i("setSelectedNodeId $nodeId")
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_NODE_ID] = nodeId
        }
    }

    suspend fun clearSubscription() {
        AppLog.w("clearSubscription called")
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SUBSCRIPTION_URL)
            prefs.remove(KEY_NODES)
            prefs.remove(KEY_SELECTED_NODE_ID)
            prefs.remove(KEY_SUBSCRIPTION_INFO)
            prefs.remove(KEY_SUB_LAST_AUTO_REFRESH_MS)
        }
    }

    suspend fun clearNodes() {
        AppLog.w("clearNodes called")
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_NODES)
            prefs.remove(KEY_SELECTED_NODE_ID)
            prefs.remove(KEY_SUBSCRIPTION_INFO)
        }
    }

    val routingProfile: Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[KEY_ROUTING_PROFILE] }
        .flowOn(Dispatchers.IO)

    val routingEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_ROUTING_ENABLED] ?: false }
        .flowOn(Dispatchers.IO)

    suspend fun saveRoutingProfile(json: String, enable: Boolean) {
        AppLog.i("saveRoutingProfile enable=$enable jsonLen=${json.length}")
        context.dataStore.edit { prefs ->
            prefs[KEY_ROUTING_PROFILE] = json
            if (enable) {
                prefs[KEY_ROUTING_ENABLED] = true
            }
        }
        if (enable) {
            RoutingProfileStore.updateActive(json)
        }
    }

    suspend fun setRoutingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ROUTING_ENABLED] = enabled
        }
        val prefs = context.dataStore.data.first()
        RoutingProfileStore.updateActive(
            prefs[KEY_ROUTING_PROFILE].takeIf { enabled && !it.isNullOrBlank() },
        )
    }

    suspend fun loadActiveRoutingIntoMemory() {
        val prefs = context.dataStore.data.first()
        val enabled = prefs[KEY_ROUTING_ENABLED] ?: false
        val json = prefs[KEY_ROUTING_PROFILE]
        RoutingProfileStore.updateActive(json.takeIf { enabled && !it.isNullOrBlank() })
    }

    val appThemeMode: Flow<AppThemeMode> = context.dataStore.data
        .map { prefs -> AppThemeMode.fromStored(prefs[KEY_APP_THEME_MODE]) }
        .flowOn(Dispatchers.IO)

    suspend fun setAppThemeMode(mode: AppThemeMode) {
        AppLog.i("setAppThemeMode ${mode.label}")
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_THEME_MODE] = mode.name
        }
    }

    val subscriptionAutoUpdateInterval: Flow<SubscriptionAutoUpdateInterval> = context.dataStore.data
        .map { prefs ->
            SubscriptionAutoUpdateInterval.fromStoredHours(prefs[KEY_SUB_AUTO_UPDATE_HOURS])
        }
        .flowOn(Dispatchers.IO)

    val subscriptionLastAutoRefreshAt: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[KEY_SUB_LAST_AUTO_REFRESH_MS] ?: 0L }
        .flowOn(Dispatchers.IO)

    suspend fun setSubscriptionAutoUpdateInterval(interval: SubscriptionAutoUpdateInterval) {
        AppLog.i("setSubscriptionAutoUpdateInterval ${interval.label}")
        context.dataStore.edit { prefs ->
            prefs[KEY_SUB_AUTO_UPDATE_HOURS] = interval.hours
        }
    }

    suspend fun setSubscriptionLastAutoRefreshAt(epochMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SUB_LAST_AUTO_REFRESH_MS] = epochMs
        }
    }

    val connectionSettings: Flow<ConnectionSettingsState> = context.dataStore.data
        .map { prefs -> prefs.toConnectionSettings() }
        .flowOn(Dispatchers.IO)

    suspend fun saveConnectionSettings(settings: ConnectionSettingsState) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SPLIT_SITES_ENABLED] = settings.sitesEnabled
            prefs[KEY_SPLIT_SITES_MODE] = settings.sitesMode.name
            prefs[KEY_SPLIT_SITES_DOMAINS] = settings.siteDomains.joinToString("\n")
            prefs[KEY_CUSTOM_RULES] = json.encodeToString(settings.customRules)
            prefs[KEY_SPLIT_APPS_ENABLED] = settings.appsEnabled
            prefs[KEY_SPLIT_APPS_MODE] = settings.appsMode.name
            prefs[KEY_SPLIT_APP_PACKAGES] = json.encodeToString(settings.appPackages.toList())
            prefs[KEY_KILL_SWITCH_ENABLED] = settings.killSwitchEnabled
        }
        ConnectionSettingsStore.update(settings)
    }

    suspend fun loadConnectionSettingsIntoMemory() {
        val settings = context.dataStore.data.first().toConnectionSettings()
        ConnectionSettingsStore.update(settings)
    }

    val favoriteNodeIds: Flow<Set<String>> = context.dataStore.data
        .map { prefs ->
            runCatching {
                json.decodeFromString<List<String>>(prefs[KEY_FAVORITE_NODE_IDS].orEmpty().ifBlank { "[]" })
            }.getOrDefault(emptyList()).toSet()
        }
        .flowOn(Dispatchers.IO)

    suspend fun setFavoriteNodeIds(ids: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FAVORITE_NODE_IDS] = json.encodeToString(ids.toList())
        }
    }

    suspend fun toggleFavoriteNode(nodeId: String) {
        val current = favoriteNodeIds.first()
        setFavoriteNodeIds(
            if (nodeId in current) current - nodeId else current + nodeId,
        )
    }

    val appLanguage: Flow<AppLanguage> = context.dataStore.data
        .map { prefs -> AppLanguage.fromStored(prefs[KEY_APP_LANGUAGE]) }
        .flowOn(Dispatchers.IO)

    suspend fun setAppLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LANGUAGE] = language.name
        }
    }

    val trafficRoutingMode: Flow<TrafficRoutingMode> = context.dataStore.data
        .map { prefs -> TrafficRoutingMode.fromStored(prefs[KEY_TRAFFIC_ROUTING_MODE]) }
        .flowOn(Dispatchers.IO)

    suspend fun setTrafficRoutingMode(mode: TrafficRoutingMode) {
        TrafficRoutingStore.update(mode)
        context.dataStore.edit { prefs ->
            prefs[KEY_TRAFFIC_ROUTING_MODE] = mode.name
        }
    }

    suspend fun loadTrafficRoutingModeIntoMemory() {
        val mode = context.dataStore.data.first().let { prefs ->
            TrafficRoutingMode.fromStored(prefs[KEY_TRAFFIC_ROUTING_MODE])
        }
        TrafficRoutingStore.update(mode)
    }

    private fun Preferences.toConnectionSettings(): ConnectionSettingsState {
        val domains = this[KEY_SPLIT_SITES_DOMAINS].orEmpty()
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val packages = runCatching {
            json.decodeFromString<List<String>>(this[KEY_SPLIT_APP_PACKAGES].orEmpty().ifBlank { "[]" })
        }.getOrDefault(emptyList()).toSet()
        val sitesMode = runCatching {
            SplitTunnelSitesMode.valueOf(this[KEY_SPLIT_SITES_MODE] ?: SplitTunnelSitesMode.ProxyOnly.name)
        }.getOrDefault(SplitTunnelSitesMode.ProxyOnly)
        val appsMode = runCatching {
            SplitTunnelAppsMode.valueOf(this[KEY_SPLIT_APPS_MODE] ?: SplitTunnelAppsMode.IncludeOnly.name)
        }.getOrDefault(SplitTunnelAppsMode.IncludeOnly)

        val customRules = runCatching {
            json.decodeFromString<List<RoutingRule>>(this[KEY_CUSTOM_RULES].orEmpty().ifBlank { "[]" })
        }.getOrDefault(emptyList()).ifEmpty {
            RoutingRuleMigration.fromLegacyDomains(domains, sitesMode)
        }

        return ConnectionSettingsState(
            sitesEnabled = this[KEY_SPLIT_SITES_ENABLED] ?: false,
            sitesMode = sitesMode,
            siteDomains = domains,
            customRules = customRules,
            appsEnabled = this[KEY_SPLIT_APPS_ENABLED] ?: false,
            appsMode = appsMode,
            appPackages = packages,
            killSwitchEnabled = this[KEY_KILL_SWITCH_ENABLED] ?: false,
        )
    }

    companion object {
        private val KEY_SUBSCRIPTION_URL = stringPreferencesKey("subscription_url")
        private val KEY_NODES = stringPreferencesKey("nodes")
        private val KEY_SELECTED_NODE_ID = stringPreferencesKey("selected_node_id")
        private val KEY_SUBSCRIPTION_INFO = stringPreferencesKey("subscription_info")
        private val KEY_ROUTING_PROFILE = stringPreferencesKey("routing_profile")
        private val KEY_ROUTING_ENABLED = booleanPreferencesKey("routing_enabled")
        private val KEY_SPLIT_SITES_ENABLED = booleanPreferencesKey("split_sites_enabled")
        private val KEY_SPLIT_SITES_MODE = stringPreferencesKey("split_sites_mode")
        private val KEY_SPLIT_SITES_DOMAINS = stringPreferencesKey("split_sites_domains")
        private val KEY_CUSTOM_RULES = stringPreferencesKey("custom_rules")
        private val KEY_SPLIT_APPS_ENABLED = booleanPreferencesKey("split_apps_enabled")
        private val KEY_SPLIT_APPS_MODE = stringPreferencesKey("split_apps_mode")
        private val KEY_SPLIT_APP_PACKAGES = stringPreferencesKey("split_app_packages")
        private val KEY_KILL_SWITCH_ENABLED = booleanPreferencesKey("kill_switch_enabled")
        private val KEY_APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        private val KEY_SUB_AUTO_UPDATE_HOURS = intPreferencesKey("sub_auto_update_hours")
        private val KEY_SUB_LAST_AUTO_REFRESH_MS = longPreferencesKey("sub_last_auto_refresh_ms")
        private val KEY_FAVORITE_NODE_IDS = stringPreferencesKey("favorite_node_ids")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_TRAFFIC_ROUTING_MODE = stringPreferencesKey("traffic_routing_mode")
    }
}
