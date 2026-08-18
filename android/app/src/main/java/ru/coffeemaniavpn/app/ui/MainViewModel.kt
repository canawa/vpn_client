package ru.coffeemaniavpn.app.ui

import android.app.Application
import androidx.annotation.StringRes
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.AppLanguage
import ru.coffeemaniavpn.app.data.AppPreferences
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.HomeFilterOrder
import ru.coffeemaniavpn.app.data.PingState
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.data.RoutingRule
import ru.coffeemaniavpn.app.data.RoutingRuleInput
import ru.coffeemaniavpn.app.data.RoutingRuleMatcher
import ru.coffeemaniavpn.app.data.RoutingRuleTarget
import ru.coffeemaniavpn.app.data.ServerPinger
import ru.coffeemaniavpn.app.data.SubscriptionAutoUpdateInterval
import ru.coffeemaniavpn.app.data.SubscriptionInfo
import ru.coffeemaniavpn.app.data.SubscriptionParser
import ru.coffeemaniavpn.app.data.SubscriptionRepository
import ru.coffeemaniavpn.app.data.LoadBalancer
import ru.coffeemaniavpn.app.data.SubscriptionUrlValidator
import ru.coffeemaniavpn.app.data.TrafficRoutingMode
import org.json.JSONObject
import ru.coffeemaniavpn.app.deeplink.DeepLinkAction
import ru.coffeemaniavpn.app.deeplink.DeepLinkEffect
import ru.coffeemaniavpn.app.deeplink.DeepLinkParser
import ru.coffeemaniavpn.app.ktx.readClipboardText
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.KillSwitchVpnService
import ru.coffeemaniavpn.app.vpn.VpnAutoReconnect
import ru.coffeemaniavpn.app.vpn.VpnManager
import ru.coffeemaniavpn.app.vpn.VpnStatus

data class MainUiState(
    val subscriptionUrl: String = "",
    val nodes: List<ProxyNode> = emptyList(),
    val selectedNodeId: String? = null,
    val vpnStatus: VpnStatus = VpnStatus.Stopped,
    val connectionElapsedMs: Long = 0L,
    val downlinkBytesPerSec: Long = 0L,
    val uplinkBytesPerSec: Long = 0L,
    val isLoading: Boolean = false,
    val isPinging: Boolean = false,
    val nodePings: Map<String, PingState> = emptyMap(),
    val subscriptionInfo: SubscriptionInfo? = null,
    val subscriptionLastUpdatedAtMs: Long = 0L,
    val message: String? = null,
    val error: String? = null,
    val startupCrash: String? = null,
    val logsPreview: String? = null,
    val connectionSettings: ConnectionSettingsState = ConnectionSettingsState(),
    val subscriptionAutoUpdateInterval: SubscriptionAutoUpdateInterval =
        SubscriptionAutoUpdateInterval.DEFAULT,
    val favoriteNodeIds: Set<String> = emptySet(),
    val homeFilterOrder: List<String> = HomeFilterOrder.DEFAULT,
    val appLanguage: AppLanguage = AppLanguage.DEFAULT,
    val trafficRoutingMode: TrafficRoutingMode = TrafficRoutingMode.DEFAULT,
    val clipboardSubscriptionUrl: String? = null,
    val showForeignSubscriptionPrompt: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val LOCAL_IMPORT_URL = "deeplink://imported"
        /** Happ-like: pull a fresh node list when the app comes back, not once every 6 hours. */
        private const val RESUME_SUBSCRIPTION_REFRESH_MIN_MS = 5 * 60 * 1000L
    }

    private fun appStr(@StringRes resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    private fun invalidSubscriptionLink(): String =
        appStr(R.string.msg_invalid_subscription_link)

    private val preferences = AppPreferences(application)
    private val repository = SubscriptionRepository(application)

    private val subscriptionUrlInput = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private val nodePings = MutableStateFlow<Map<String, PingState>>(emptyMap())
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val startupCrash = MutableStateFlow<String?>(null)
    private val clipboardSubscriptionUrl = MutableStateFlow<String?>(null)
    private val showForeignSubscriptionPrompt = MutableStateFlow(false)
    private var subscriptionAutoUpdateJob: Job? = null
    private var pendingConnectNodeId: String? = null

    private val _connectRequests = MutableSharedFlow<ProxyNode>(extraBufferCapacity = 1)
    val connectRequests: SharedFlow<ProxyNode> = _connectRequests.asSharedFlow()

    val uiState: StateFlow<MainUiState> = combine(
        combine(
            preferences.subscriptionUrl,
            preferences.nodes.catch { e ->
                AppLog.e("nodes flow failed, clearing saved nodes", e)
                viewModelScope.launch { preferences.clearNodes() }
                emit(emptyList())
            },
            preferences.selectedNodeId,
            preferences.subscriptionInfo,
        ) { savedUrl, nodes, selectedNodeId, subscriptionInfo ->
            AppLog.i("prefs loaded urlLen=${savedUrl.length} nodes=${nodes.size}")
            SavedData(savedUrl, nodes, selectedNodeId, subscriptionInfo)
        },
        combine(
            VpnManager.status,
            VpnManager.lastError,
            VpnManager.connectionElapsedMs,
            VpnManager.trafficRates,
            subscriptionUrlInput,
        ) { vpnStatus, vpnError, elapsedMs, traffic, inputUrl ->
            VpnUiState(vpnStatus, vpnError, elapsedMs, traffic.downlinkBytesPerSec, traffic.uplinkBytesPerSec, inputUrl)
        },
        combine(isLoading, nodePings, message, error) { loading, pings, info, localError ->
            LocalUiState(
                isLoading = loading,
                isPinging = pings.values.any { it is PingState.Loading },
                nodePings = pings,
                message = info,
                error = localError,
            )
        },
        combine(
            preferences.connectionSettings,
            preferences.subscriptionAutoUpdateInterval,
            preferences.subscriptionLastAutoRefreshAt,
            preferences.favoriteNodeIds,
            combine(
                preferences.appLanguage,
                preferences.trafficRoutingMode,
                preferences.homeFilterOrder,
            ) { language, routingMode, filterOrder ->
                Triple(language, routingMode, filterOrder)
            },
        ) { connectionSettings, autoUpdateInterval, lastUpdatedAt, favorites, langRouting ->
            SettingsUiState(
                connectionSettings = connectionSettings,
                subscriptionAutoUpdateInterval = autoUpdateInterval,
                subscriptionLastUpdatedAtMs = lastUpdatedAt,
                favoriteNodeIds = favorites,
                appLanguage = langRouting.first,
                trafficRoutingMode = langRouting.second,
                homeFilterOrder = langRouting.third,
            )
        },
        combine(startupCrash, clipboardSubscriptionUrl, showForeignSubscriptionPrompt) { crash, clip, foreign ->
            Triple(crash, clip, foreign)
        },
    ) { savedData, vpnData, localData, settingsData, extras ->
        val (savedUrl, rawNodes, rawSelectedNodeId, subscriptionInfo) = savedData
        val (vpnStatus, vpnError, connectionElapsedMs, downlinkBps, uplinkBps, _) = vpnData
        val (loading, pinging, pings, info, localError) = localData
        val (crash, clipUrl, foreignPrompt) = extras
        val nodes = LoadBalancer.connectableNodes(rawNodes)
        val selectedNodeId = when {
            rawSelectedNodeId == LoadBalancer.AUTO_NODE_ID -> LoadBalancer.AUTO_NODE_ID
            rawSelectedNodeId != null && nodes.any { it.id == rawSelectedNodeId } -> rawSelectedNodeId
            nodes.isNotEmpty() -> LoadBalancer.AUTO_NODE_ID
            else -> null
        }

        MainUiState(
            // Only persisted URL unlocks Home/connect — draft input must not open the app.
            subscriptionUrl = savedUrl.trim(),
            nodes = nodes,
            selectedNodeId = selectedNodeId,
            vpnStatus = vpnStatus,
            connectionElapsedMs = connectionElapsedMs,
            downlinkBytesPerSec = downlinkBps,
            uplinkBytesPerSec = uplinkBps,
            isLoading = loading,
            isPinging = pinging,
            nodePings = pings,
            subscriptionInfo = subscriptionInfo,
            subscriptionLastUpdatedAtMs = settingsData.subscriptionLastUpdatedAtMs,
            message = info,
            error = localError ?: vpnError,
            startupCrash = crash,
            connectionSettings = settingsData.connectionSettings,
            subscriptionAutoUpdateInterval = settingsData.subscriptionAutoUpdateInterval,
            favoriteNodeIds = settingsData.favoriteNodeIds,
            homeFilterOrder = settingsData.homeFilterOrder,
            appLanguage = settingsData.appLanguage,
            trafficRoutingMode = settingsData.trafficRoutingMode,
            clipboardSubscriptionUrl = clipUrl,
            showForeignSubscriptionPrompt = foreignPrompt,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainUiState(),
    )

    private var lastAutoPingedNodeIds: Set<String> = emptySet()

    init {
        AppLog.i("MainViewModel init")
        viewModelScope.launch(Dispatchers.IO) {
            startupCrash.value = AppLog.readLastCrash()
            preferences.loadActiveRoutingIntoMemory()
            preferences.loadConnectionSettingsIntoMemory()
        }
        viewModelScope.launch {
            preferences.subscriptionUrl.collect { saved ->
                if (saved.isNotBlank() && subscriptionUrlInput.value.isBlank()) {
                    subscriptionUrlInput.value = saved
                    AppLog.i("restored subscription url from prefs urlLen=${saved.length}")
                }
            }
        }
        viewModelScope.launch {
            combine(preferences.subscriptionUrl, preferences.nodes) { savedUrl, nodes ->
                savedUrl.trim() to nodes
            }.collect { (savedUrl, nodes) ->
                if (nodes.isNotEmpty() && savedUrl.isBlank()) {
                    AppLog.w("orphaned nodes without saved subscription url, clearing")
                    preferences.clearNodes()
                }
            }
        }
        viewModelScope.launch {
            preferences.nodes.collect { nodes ->
                val ids = nodes.map { it.id }.toSet()
                if (nodes.isEmpty()) {
                    lastAutoPingedNodeIds = emptySet()
                    return@collect
                }
                if (ids == lastAutoPingedNodeIds) return@collect
                lastAutoPingedNodeIds = ids
                AppLog.i("auto-ping nodes=${nodes.size}")
                pingAllNodes(nodes)
            }
        }
        viewModelScope.launch {
            preferences.subscriptionAutoUpdateInterval.collect { interval ->
                restartSubscriptionAutoUpdate(interval)
            }
        }
        viewModelScope.launch {
            maybeRefreshStaleNodesOnStartup()
        }
        viewModelScope.launch {
            VpnManager.status.collect { status ->
                if (status != VpnStatus.Stopped) return@collect
                val nodeId = pendingConnectNodeId ?: return@collect
                pendingConnectNodeId = null
                val node = if (nodeId == LoadBalancer.AUTO_NODE_ID) {
                    resolveConnectNode()
                } else {
                    uiState.value.nodes.find { it.id == nodeId }
                } ?: return@collect
                _connectRequests.emit(node)
            }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            restoreSubscriptionSession()
            VpnAutoReconnect.tryReconnectOnResume()
            maybeAutoRefreshSubscriptionOnResume()
            checkClipboardForSubscription()
        }
    }

    fun checkClipboardForSubscription() {
        if (uiState.value.subscriptionUrl.isNotBlank()) return
        val text = getApplication<Application>().readClipboardText()?.trim().orEmpty()
        if (text.isBlank() || !SubscriptionUrlValidator.looksLikeSubscriptionUrl(text)) return
        clipboardSubscriptionUrl.value = text
    }

    fun acceptClipboardSubscription() {
        val url = clipboardSubscriptionUrl.value ?: return
        clipboardSubscriptionUrl.value = null
        importSubscriptionUrl(url)
    }

    fun dismissClipboardSubscription() {
        clipboardSubscriptionUrl.value = null
    }

    fun dismissForeignSubscriptionPrompt() {
        showForeignSubscriptionPrompt.value = false
    }

    fun importSubscriptionUrl(url: String) = addSubscriptionUrl(url)

    private fun addSubscriptionUrl(url: String) {
        if (!SubscriptionUrlValidator.isOurSubscription(url)) {
            showForeignSubscriptionPrompt.value = true
            error.value = appStr(R.string.msg_subscription_not_ours)
            return
        }
        message.value = appStr(R.string.msg_link_pasted)
        refreshConfig(showUrlRequiredError = false, urlOverride = url.trim())
    }

    fun setSubscriptionAutoUpdateInterval(interval: SubscriptionAutoUpdateInterval) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setSubscriptionAutoUpdateInterval(interval)
        }
    }

    private suspend fun restoreSubscriptionSession() {
        val savedUrl = preferences.subscriptionUrl.first().trim()
        if (savedUrl.isNotBlank() && subscriptionUrlInput.value.isBlank()) {
            subscriptionUrlInput.value = savedUrl
            AppLog.i("onAppResumed restored subscription urlLen=${savedUrl.length}")
        }
    }

    fun canConnect(): Boolean = prepareConnect(showErrors = false)

    fun prepareConnect(showErrors: Boolean = true): Boolean {
        val state = uiState.value
        return when {
            state.nodes.isEmpty() -> {
                if (showErrors) error.value = appStr(R.string.msg_wait_servers_loading)
                false
            }
            state.subscriptionUrl.isBlank() -> {
                if (showErrors) error.value = invalidSubscriptionLink()
                false
            }
            state.subscriptionInfo?.isExpired() == true -> {
                if (showErrors) error.value = appStr(R.string.msg_subscription_expired)
                false
            }
            else -> true
        }
    }

    fun pingAllNodes() {
        pingAllNodes(uiState.value.nodes)
    }

    fun pingNode(nodeId: String) {
        val node = uiState.value.nodes.find { it.id == nodeId } ?: return
        viewModelScope.launch {
            nodePings.value = nodePings.value + (nodeId to PingState.Loading)
            try {
                withContext(Dispatchers.IO) {
                    ServerPinger.pingAll(listOf(node)) { id, state ->
                        nodePings.value = nodePings.value + (id to state)
                    }
                }
            } catch (_: CancellationException) {
                // ignored
            } finally {
                if (nodePings.value[nodeId] is PingState.Loading) {
                    nodePings.value = nodePings.value + (nodeId to PingState.Unreachable)
                }
            }
        }
    }

    private var pingJob: Job? = null
    private var pingGeneration = 0

    private fun pingAllNodes(nodes: List<ProxyNode>) {
        if (nodes.isEmpty()) return

        pingJob?.cancel()
        val generation = ++pingGeneration
        pingJob = viewModelScope.launch {
            nodePings.value = nodes.associate { it.id to PingState.Loading }
            val watchdogs = nodes.map { node ->
                launch {
                    delay(ServerPinger.PER_NODE_TIMEOUT_MS + 500L)
                    if (generation != pingGeneration) return@launch
                    if (nodePings.value[node.id] is PingState.Loading) {
                        nodePings.value = nodePings.value + (node.id to PingState.Unreachable)
                    }
                }
            }
            try {
                withContext(Dispatchers.IO) {
                    ServerPinger.pingAll(nodes) { nodeId, state ->
                        if (generation != pingGeneration) return@pingAll
                        nodePings.value = nodePings.value + (nodeId to state)
                    }
                }
            } catch (_: CancellationException) {
                // cancelled by a newer ping run
            } finally {
                watchdogs.forEach { it.cancel() }
                if (generation == pingGeneration) {
                    val settled = nodePings.value.toMutableMap()
                    var changed = false
                    for (node in nodes) {
                        if (settled[node.id] is PingState.Loading) {
                            settled[node.id] = PingState.Unreachable
                            changed = true
                        }
                    }
                    if (changed) nodePings.value = settled
                }
            }
        }
    }

    fun onSubscriptionUrlChange(value: String) {
        subscriptionUrlInput.value = value
    }

    fun processDeepLink(uri: Uri, onEffect: (DeepLinkEffect) -> Unit) {
        val action = DeepLinkParser.parse(uri) ?: run {
            AppLog.w("processDeepLink unsupported uri=$uri")
            error.value = invalidSubscriptionLink()
            return
        }
        AppLog.i("processDeepLink action=$action")
        when (action) {
            DeepLinkAction.Open -> {
                message.value = "XENO VPN"
            }
            DeepLinkAction.Connect -> {
                if (prepareConnect(showErrors = true)) {
                    onEffect(DeepLinkEffect.RequestConnect)
                }
            }
            DeepLinkAction.Disconnect -> {
                VpnManager.disconnect()
                message.value = appStr(R.string.msg_disconnected)
            }
            DeepLinkAction.Close -> {
                VpnManager.disconnect()
                onEffect(DeepLinkEffect.FinishActivity)
            }
            is DeepLinkAction.Add -> addSubscriptionFromDeepLink(
                action.url,
                connectAfter = action.connectAfter,
                onEffect = onEffect,
            )
            is DeepLinkAction.Import -> importSubscriptionPayload(
                action.payload,
                connectAfter = false,
                onEffect = onEffect,
            )
            is DeepLinkAction.Routing -> saveRoutingFromDeepLink(action.profileJson, action.enable)
        }
    }

    private fun addSubscriptionFromDeepLink(
        url: String,
        connectAfter: Boolean,
        onEffect: (DeepLinkEffect) -> Unit,
    ) {
        message.value = if (connectAfter) {
            appStr(R.string.msg_subscription_added_connecting)
        } else {
            appStr(R.string.msg_subscription_added)
        }
        refreshConfig(showUrlRequiredError = false, urlOverride = url.trim()) { success ->
            if (success && connectAfter && prepareConnect(showErrors = true)) {
                onEffect(DeepLinkEffect.RequestConnect)
            }
        }
    }

    private fun importSubscriptionPayload(
        payload: String,
        connectAfter: Boolean,
        onEffect: (DeepLinkEffect) -> Unit,
    ) {
        val trimmed = payload.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            addSubscriptionFromDeepLink(trimmed, connectAfter, onEffect)
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val nodes = withContext(Dispatchers.IO) {
                    SubscriptionParser.parse(trimmed)
                }
                if (nodes.isEmpty()) error(invalidSubscriptionLink())
                val connectable = LoadBalancer.connectableNodes(nodes)
                if (connectable.isEmpty()) error(invalidSubscriptionLink())
                preferences.saveSubscription(
                    LOCAL_IMPORT_URL,
                    connectable,
                    LoadBalancer.AUTO_NODE_ID,
                    null,
                )
                subscriptionUrlInput.value = LOCAL_IMPORT_URL
                message.value = appStr(R.string.msg_servers_imported, connectable.size)
                AppLog.i("importSubscriptionPayload ok nodes=${connectable.size}")
                if (connectAfter && prepareConnect(showErrors = true)) {
                    onEffect(DeepLinkEffect.RequestConnect)
                }
            } catch (e: Exception) {
                AppLog.e("importSubscriptionPayload failed", e)
                error.value = invalidSubscriptionLink()
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun saveRoutingFromDeepLink(profileJson: String, enable: Boolean) {
        viewModelScope.launch {
            try {
                val name = JSONObject(profileJson).optString("Name")
                    .ifBlank { appStr(R.string.msg_default_profile_name) }
                preferences.saveRoutingProfile(profileJson, enable)
                message.value = if (enable) {
                    appStr(R.string.msg_routing_enabled, name)
                } else {
                    appStr(R.string.msg_routing_saved, name)
                }
            } catch (e: Exception) {
                AppLog.e("saveRoutingFromDeepLink failed", e)
                error.value = appStr(R.string.msg_invalid_routing_profile)
            }
        }
    }

    fun pasteSubscriptionFromClipboard() {
        val text = getApplication<Application>().readClipboardText()
        if (text.isNullOrBlank()) {
            error.value = invalidSubscriptionLink()
            return
        }
        if (!SubscriptionUrlValidator.looksLikeSubscriptionUrl(text)) {
            error.value = invalidSubscriptionLink()
            return
        }
        addSubscriptionUrl(text)
        AppLog.i("pasteSubscriptionFromClipboard urlLen=${text.length}")
    }

    fun deleteSubscription() {
        viewModelScope.launch {
            VpnManager.disconnect()
            pingJob?.cancel()
            pingGeneration++
            preferences.clearSubscription()
            subscriptionUrlInput.value = ""
            nodePings.value = emptyMap()
            clearMessages()
            message.value = appStr(R.string.msg_subscription_deleted)
            AppLog.i("deleteSubscription ok")
        }
    }

    fun refreshSubscription() {
        refreshConfig(showUrlRequiredError = true)
    }

    fun refreshConfig(
        showUrlRequiredError: Boolean = false,
        urlOverride: String? = null,
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        val url = urlOverride?.trim().orEmpty()
            .ifBlank { uiState.value.subscriptionUrl.trim() }
            .ifBlank { subscriptionUrlInput.value.trim() }
        if (url.isBlank() || url == LOCAL_IMPORT_URL) {
            if (url == LOCAL_IMPORT_URL && uiState.value.nodes.isNotEmpty()) {
                onComplete?.invoke(true)
                return
            }
            if (showUrlRequiredError) {
                error.value = invalidSubscriptionLink()
            }
            onComplete?.invoke(false)
            return
        }
        if (!isHttpSubscriptionUrl(url)) {
            error.value = invalidSubscriptionLink()
            onComplete?.invoke(false)
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            message.value = null
            val success = runCatching {
                fetchAndSaveSubscription(url)
            }.onSuccess { nodeCount ->
                subscriptionUrlInput.value = url
                message.value = appStr(R.string.msg_config_updated, nodeCount)
                AppLog.i("refreshConfig ok, nodes=$nodeCount")
            }.onFailure { e ->
                AppLog.e("refreshConfig failed", e)
                error.value = subscriptionErrorMessage(e)
            }.isSuccess
            isLoading.value = false
            onComplete?.invoke(success)
        }
    }

    private suspend fun fetchAndSaveSubscription(url: String): Int {
        AppLog.i("fetchAndSaveSubscription start urlLen=${url.length}")
        val result = withContext(Dispatchers.IO) {
            repository.fetchSubscription(url)
        }
        val selected = uiState.value.selectedNodeId?.takeIf { id ->
            id == LoadBalancer.AUTO_NODE_ID || result.nodes.any { it.id == id && !LoadBalancer.isRemoteAutoNode(it) }
        } ?: LoadBalancer.AUTO_NODE_ID
        val nodes = LoadBalancer.connectableNodes(result.nodes)
        // Same host after a panel restore must still re-ping — DataStore may emit the same ids.
        lastAutoPingedNodeIds = emptySet()
        preferences.saveSubscription(url, nodes, selected, result.info)
        AppLog.i(
            "fetchAndSaveSubscription saved nodes=${nodes.size} " +
                nodes.joinToString { it.name }.take(400),
        )
        return nodes.size
    }

    private suspend fun autoRefreshSubscriptionSilent() {
        val url = preferences.subscriptionUrl.first().trim()
        if (url.isBlank() || url == LOCAL_IMPORT_URL) return
        runCatching {
            fetchAndSaveSubscription(url)
        }.onSuccess { count ->
            AppLog.i("autoRefreshSubscription ok nodes=$count")
        }.onFailure { e ->
            AppLog.w("autoRefreshSubscription failed", e)
        }
    }

    private suspend fun maybeAutoRefreshSubscriptionOnResume() {
        if (maybeRefreshStaleNodes()) return
        val interval = preferences.subscriptionAutoUpdateInterval.first()
        if (interval == SubscriptionAutoUpdateInterval.OFF) return
        val url = preferences.subscriptionUrl.first().trim()
        if (url.isBlank() || url == LOCAL_IMPORT_URL) return
        val last = preferences.subscriptionLastAutoRefreshAt.first()
        val elapsed = System.currentTimeMillis() - last
        val thresholdMs = minOf(interval.durationMs, RESUME_SUBSCRIPTION_REFRESH_MIN_MS)
        if (last == 0L || elapsed >= thresholdMs) {
            AppLog.i(
                "autoRefreshSubscription on resume elapsed=${elapsed}ms " +
                    "threshold=${thresholdMs}ms interval=${interval.label}",
            )
            autoRefreshSubscriptionSilent()
        }
    }

    private suspend fun maybeRefreshStaleNodesOnStartup() {
        maybeRefreshStaleNodes()
    }

    /** Re-fetch subscription when cached nodes are missing or predate grpc/rawOutbound support. */
    private suspend fun maybeRefreshStaleNodes(): Boolean {
        val url = preferences.subscriptionUrl.first().trim()
        if (url.isBlank() || url == LOCAL_IMPORT_URL) return false
        val nodes = preferences.nodes.first()
        if (nodes.isEmpty()) {
            AppLog.i("refreshStaleNodes: saved url but empty nodes, refetching")
            isLoading.value = true
            try {
                autoRefreshSubscriptionSilent()
            } finally {
                isLoading.value = false
            }
            return true
        }

        val cacheVersion = preferences.nodesCacheVersion()
        val staleCache = cacheVersion < AppPreferences.NODES_CACHE_VERSION
        val missingRaw = nodes.any { it.rawOutboundJson.isNullOrBlank() }
        val wrongGrpcTransport = nodes.any { node ->
            node.port == 7443 && node.host.endsWith("lavivas.org") && !node.isGrpc
        }
        if (!staleCache && !missingRaw && !wrongGrpcTransport) return false

        AppLog.i(
            "refreshStaleNodes cacheVersion=$cacheVersion need=${AppPreferences.NODES_CACHE_VERSION} " +
                "missingRaw=$missingRaw wrongGrpc=$wrongGrpcTransport nodes=${nodes.size}",
        )
        autoRefreshSubscriptionSilent()
        return true
    }

    private fun restartSubscriptionAutoUpdate(interval: SubscriptionAutoUpdateInterval) {
        subscriptionAutoUpdateJob?.cancel()
        if (interval == SubscriptionAutoUpdateInterval.OFF) return
        subscriptionAutoUpdateJob = viewModelScope.launch {
            AppLog.i("subscriptionAutoUpdate started interval=${interval.label}")
            delay(interval.durationMs)
            while (isActive) {
                autoRefreshSubscriptionSilent()
                delay(interval.durationMs)
            }
        }
    }

    fun selectNode(nodeId: String) {
        applyNodeSelection(nodeId, connectIfStopped = false)
    }

    fun toggleFavorite(nodeId: String) {
        viewModelScope.launch {
            preferences.toggleFavoriteNode(nodeId)
        }
    }

    fun setHomeFilterOrder(order: List<String>) {
        viewModelScope.launch {
            preferences.setHomeFilterOrder(order)
        }
    }

    fun persistAppLanguage(language: AppLanguage) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setAppLanguage(language)
        }
    }

    fun setTrafficRoutingMode(mode: TrafficRoutingMode) {
        viewModelScope.launch {
            preferences.setTrafficRoutingMode(mode)
            reconnectVpnToApplyConfig()
        }
    }

    fun requestConnectToNode(nodeId: String) {
        clearMessages()
        applyNodeSelection(nodeId, connectIfStopped = true)
    }

    private fun applyNodeSelection(nodeId: String, connectIfStopped: Boolean) {
        val target = resolveNodeForSelection(nodeId)
        val status = VpnManager.status.value
        val connectedId = VpnAutoReconnect.connectedNode()?.id
        val switchingTunnel =
            status != VpnStatus.Stopped && target != null && connectedId != target.id
        val startingFresh = status == VpnStatus.Stopped && connectIfStopped
        if ((switchingTunnel || startingFresh) && !prepareConnect()) return

        viewModelScope.launch {
            preferences.setSelectedNodeId(nodeId)
            if (target == null) return@launch
            if (startingFresh || switchingTunnel) {
                emitConnectRequest(target, nodeId)
            }
        }
    }

    private fun resolveNodeForSelection(nodeId: String): ProxyNode? {
        val state = uiState.value
        if (nodeId == LoadBalancer.AUTO_NODE_ID) {
            return LoadBalancer.pickBest(state.nodes, state.nodePings)
                ?: state.nodes.firstOrNull()
        }
        return state.nodes.find { it.id == nodeId }
    }

    private fun emitConnectRequest(node: ProxyNode, nodeId: String = node.id) {
        when (VpnManager.status.value) {
            VpnStatus.Stopped -> {
                viewModelScope.launch { _connectRequests.emit(node) }
            }
            VpnStatus.Started -> {
                if (VpnAutoReconnect.connectedNode()?.id == node.id) return
                pendingConnectNodeId = nodeId
                VpnManager.disconnect()
            }
            VpnStatus.Starting, VpnStatus.Stopping -> {
                pendingConnectNodeId = nodeId
                VpnManager.disconnect()
            }
        }
    }

    fun clearMessages() {
        message.value = null
        error.value = null
        startupCrash.value = null
        VpnManager.setError(null)
    }

    private fun isHttpSubscriptionUrl(url: String): Boolean =
        url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true)

    private fun subscriptionErrorMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        val lower = message.lowercase()
        if (error is IllegalArgumentException ||
            "expected url" in lower ||
            "invalid url" in lower ||
            "no scheme" in lower ||
            "malformed" in lower ||
            "must be http" in lower ||
            "неверный" in lower && "ссылк" in lower
        ) {
            return invalidSubscriptionLink()
        }
        return message.ifBlank { appStr(R.string.msg_config_update_failed) }
    }

    fun saveConnectionSettings(settings: ConnectionSettingsState) {
        viewModelScope.launch(Dispatchers.IO) {
            persistConnectionSettings(settings)
        }
    }

    fun updateConnectionSettings(transform: (ConnectionSettingsState) -> ConnectionSettingsState) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = preferences.connectionSettings.first()
            persistConnectionSettings(transform(current))
        }
    }

    fun addCustomRule(rawValue: String, target: RoutingRuleTarget) {
        addCustomRules(listOf(rawValue), target)
    }

    fun addCustomRules(rawValues: List<String>, target: RoutingRuleTarget) {
        if (rawValues.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val current = preferences.connectionSettings.first()
            val existing = current.customRules.toMutableList()
            var changed = false

            rawValues.forEach { raw ->
                val kind = RoutingRuleInput.classifyLine(raw)
                val rule = RoutingRuleInput.toStoredRule(raw, kind, target) ?: return@forEach
                val duplicate = existing.any {
                    it.matcher == rule.matcher &&
                        it.value.equals(rule.value, ignoreCase = rule.matcher == RoutingRuleMatcher.DomainSuffix)
                }
                if (!duplicate) {
                    existing += rule
                    changed = true
                }
            }

            if (!changed) return@launch

            // Mode must be CUSTOM before reconnect, otherwise rules are ignored in config build.
            if (preferences.trafficRoutingMode.first() != TrafficRoutingMode.CUSTOM) {
                preferences.setTrafficRoutingMode(TrafficRoutingMode.CUSTOM)
            }

            persistConnectionSettings(
                current.copy(
                    customRules = existing,
                    sitesEnabled = true,
                ),
            )
        }
    }

    fun removeCustomRule(ruleId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = preferences.connectionSettings.first()
            persistConnectionSettings(
                current.copy(customRules = current.customRules.filter { it.id != ruleId }),
            )
        }
    }

    private suspend fun persistConnectionSettings(settings: ConnectionSettingsState) {
        preferences.saveConnectionSettings(settings)
        if (!settings.killSwitchEnabled) {
            KillSwitchVpnService.release(getApplication())
        }
        reconnectVpnToApplyConfig()
    }

    /** Drop VPN and connect again so Xray rebuilds config with latest routing. */
    private fun reconnectVpnToApplyConfig() {
        val status = VpnManager.status.value
        val shouldReconnect = status == VpnStatus.Started || status == VpnStatus.Starting
        if (!shouldReconnect) return
        val nodeId = uiState.value.selectedNodeId ?: selectedNode()?.id ?: return
        AppLog.i("reconnectVpnToApplyConfig status=$status nodeId=$nodeId")
        pendingConnectNodeId = nodeId
        VpnManager.disconnect(userInitiated = true)
    }

    fun selectedNode(): ProxyNode? = resolveConnectNode()

    fun resolveConnectNode(): ProxyNode? {
        val state = uiState.value
        if (state.selectedNodeId == LoadBalancer.AUTO_NODE_ID) {
            return LoadBalancer.pickBest(state.nodes, state.nodePings)
        }
        return state.nodes.find { it.id == state.selectedNodeId }
    }

    fun selectAutoBalancer() {
        applyNodeSelection(LoadBalancer.AUTO_NODE_ID, connectIfStopped = false)
    }

    fun refreshServersAndPing() {
        refreshConfig(showUrlRequiredError = false) { success ->
            if (success) pingAllNodes()
        }
    }

    private data class VpnUiState(
        val vpnStatus: VpnStatus,
        val vpnError: String?,
        val connectionElapsedMs: Long,
        val downlinkBytesPerSec: Long,
        val uplinkBytesPerSec: Long,
        val inputUrl: String,
    )

    private data class SavedData(
        val subscriptionUrl: String,
        val nodes: List<ProxyNode>,
        val selectedNodeId: String?,
        val subscriptionInfo: SubscriptionInfo?,
    )

    private data class LocalUiState(
        val isLoading: Boolean,
        val isPinging: Boolean,
        val nodePings: Map<String, PingState>,
        val message: String?,
        val error: String?,
    )

    private data class SettingsUiState(
        val connectionSettings: ConnectionSettingsState,
        val subscriptionAutoUpdateInterval: SubscriptionAutoUpdateInterval,
        val subscriptionLastUpdatedAtMs: Long,
        val favoriteNodeIds: Set<String> = emptySet(),
        val homeFilterOrder: List<String> = HomeFilterOrder.DEFAULT,
        val appLanguage: AppLanguage = AppLanguage.DEFAULT,
        val trafficRoutingMode: TrafficRoutingMode = TrafficRoutingMode.DEFAULT,
    )
}
