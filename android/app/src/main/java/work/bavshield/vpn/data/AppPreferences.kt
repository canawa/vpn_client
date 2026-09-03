package work.bavshield.vpn.data

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
import work.bavshield.vpn.util.AppLog

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bavshield_vpn")

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

    suspend fun saveSubscriptionUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        AppLog.i("saveSubscriptionUrl urlLen=${trimmed.length}")
        context.dataStore.edit { prefs ->
            prefs[KEY_SUBSCRIPTION_URL] = trimmed
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

    val subscriptionAutoUpdateInterval: Flow<SubscriptionAutoUpdateInterval> = context.dataStore.data
        .map { prefs ->
            SubscriptionAutoUpdateInterval.fromStoredHours(prefs[KEY_SUB_AUTO_UPDATE_HOURS])
        }
        .flowOn(Dispatchers.IO)

    val subscriptionLastAutoRefreshAt: Flow<Long> = context.dataStore.data
        .map { prefs -> prefs[KEY_SUB_LAST_AUTO_REFRESH_MS] ?: 0L }
        .flowOn(Dispatchers.IO)

    suspend fun setSubscriptionAutoUpdateInterval(interval: SubscriptionAutoUpdateInterval) {
        AppLog.i("setSubscriptionAutoUpdateInterval ${interval.logLabel}")
        context.dataStore.edit { prefs ->
            prefs[KEY_SUB_AUTO_UPDATE_HOURS] = interval.hours
        }
    }

    suspend fun setSubscriptionLastAutoRefreshAt(epochMs: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SUB_LAST_AUTO_REFRESH_MS] = epochMs
        }
    }

    val pingAutoInterval: Flow<PingAutoInterval> = context.dataStore.data
        .map { prefs -> PingAutoInterval.fromStoredMinutes(prefs[KEY_PING_AUTO_MINUTES]) }
        .flowOn(Dispatchers.IO)

    val pingTestHosts: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[KEY_PING_TEST_HOSTS] ?: DEFAULT_PING_TEST_HOSTS }
        .flowOn(Dispatchers.IO)

    val pingMethod: Flow<PingMethod> = context.dataStore.data
        .map { prefs -> PingMethod.fromStored(prefs[KEY_PING_METHOD]) }
        .flowOn(Dispatchers.IO)

    suspend fun setPingAutoInterval(interval: PingAutoInterval) {
        AppLog.i("setPingAutoInterval ${interval.logLabel}")
        context.dataStore.edit { prefs ->
            prefs[KEY_PING_AUTO_MINUTES] = interval.minutes
        }
    }

    suspend fun setPingTestHosts(raw: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PING_TEST_HOSTS] = raw
        }
    }

    suspend fun setPingMethod(method: PingMethod) {
        AppLog.i("setPingMethod ${method.logLabel}")
        ServerPinger.method = method
        context.dataStore.edit { prefs ->
            prefs[KEY_PING_METHOD] = method.name
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

        return ConnectionSettingsState(
            sitesEnabled = this[KEY_SPLIT_SITES_ENABLED] ?: false,
            sitesMode = sitesMode,
            siteDomains = domains,
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
        private val KEY_SPLIT_APPS_ENABLED = booleanPreferencesKey("split_apps_enabled")
        private val KEY_SPLIT_APPS_MODE = stringPreferencesKey("split_apps_mode")
        private val KEY_SPLIT_APP_PACKAGES = stringPreferencesKey("split_app_packages")
        private val KEY_KILL_SWITCH_ENABLED = booleanPreferencesKey("kill_switch_enabled")
        private val KEY_SUB_AUTO_UPDATE_HOURS = intPreferencesKey("sub_auto_update_hours")
        private val KEY_SUB_LAST_AUTO_REFRESH_MS = longPreferencesKey("sub_last_auto_refresh_ms")
        private val KEY_PING_AUTO_MINUTES = intPreferencesKey("ping_auto_minutes")
        private val KEY_PING_TEST_HOSTS = stringPreferencesKey("ping_test_hosts")
        private val KEY_PING_METHOD = stringPreferencesKey("ping_method")

        const val DEFAULT_PING_TEST_HOSTS = "1.1.1.1:443\n8.8.8.8:443"
    }
}
