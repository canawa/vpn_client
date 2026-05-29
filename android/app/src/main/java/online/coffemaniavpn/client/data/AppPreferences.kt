package online.coffemaniavpn.client.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import online.coffemaniavpn.client.util.AppLog

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

    suspend fun clearNodes() {
        AppLog.w("clearNodes called")
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_NODES)
            prefs.remove(KEY_SELECTED_NODE_ID)
            prefs.remove(KEY_SUBSCRIPTION_INFO)
        }
    }

    companion object {
        private val KEY_SUBSCRIPTION_URL = stringPreferencesKey("subscription_url")
        private val KEY_NODES = stringPreferencesKey("nodes")
        private val KEY_SELECTED_NODE_ID = stringPreferencesKey("selected_node_id")
        private val KEY_SUBSCRIPTION_INFO = stringPreferencesKey("subscription_info")
    }
}
