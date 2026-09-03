package work.bavshield.vpn.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import androidx.annotation.StringRes
import work.bavshield.vpn.R
import work.bavshield.vpn.data.AppPreferences
import work.bavshield.vpn.data.ConnectionSettingsState
import work.bavshield.vpn.data.PingAutoInterval
import work.bavshield.vpn.data.PingMethod
import work.bavshield.vpn.data.PingState
import work.bavshield.vpn.data.ProxyNode
import work.bavshield.vpn.data.ServerPinger
import work.bavshield.vpn.data.SubscriptionAutoUpdateInterval
import work.bavshield.vpn.data.SubscriptionInfo
import work.bavshield.vpn.data.SubscriptionParser
import work.bavshield.vpn.data.SubscriptionRepository
import org.json.JSONObject
import work.bavshield.vpn.deeplink.DeepLinkAction
import work.bavshield.vpn.deeplink.DeepLinkEffect
import work.bavshield.vpn.deeplink.DeepLinkParser
import work.bavshield.vpn.ktx.readClipboardText
import work.bavshield.vpn.util.AppLog
import work.bavshield.vpn.vpn.KillSwitchVpnService
import work.bavshield.vpn.vpn.VpnAutoReconnect
import work.bavshield.vpn.vpn.VpnManager
import work.bavshield.vpn.vpn.VpnStatus

data class MainUiState(
    val subscriptionUrl: String = "",
    val nodes: List<ProxyNode> = emptyList(),
    val selectedNodeId: String? = null,
    val vpnStatus: VpnStatus = VpnStatus.Stopped,
    val connectionElapsedMs: Long = 0L,
    val downloadBytesPerSec: Long = 0L,
    val uploadBytesPerSec: Long = 0L,
    val isLoading: Boolean = false,
    val isPinging: Boolean = false,
    val nodePings: Map<String, PingState> = emptyMap(),
    val subscriptionInfo: SubscriptionInfo? = null,
    val message: String? = null,
    val error: String? = null,
    val startupCrash: String? = null,
    val logsPreview: String? = null,
    val connectionSettings: ConnectionSettingsState = ConnectionSettingsState(),
    val subscriptionAutoUpdateInterval: SubscriptionAutoUpdateInterval =
        SubscriptionAutoUpdateInterval.DEFAULT,
    val pingAutoInterval: PingAutoInterval = PingAutoInterval.DEFAULT,
    val pingTestHosts: String = AppPreferences.DEFAULT_PING_TEST_HOSTS,
    val pingMethod: PingMethod = PingMethod.DEFAULT,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val LOCAL_IMPORT_URL = "deeplink://imported"
    }

    private val preferences = AppPreferences(application)
    private val repository = SubscriptionRepository(application)

    private val subscriptionUrlInput = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private val isPinging = MutableStateFlow(false)
    private val nodePings = MutableStateFlow<Map<String, PingState>>(emptyMap())
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val startupCrash = MutableStateFlow<String?>(null)
    private var subscriptionAutoUpdateJob: Job? = null
    private var pingAutoJob: Job? = null
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
            VpnManager.trafficStats,
            subscriptionUrlInput,
        ) { vpnStatus, vpnError, elapsedMs, traffic, inputUrl ->
            VpnUiState(vpnStatus, vpnError, elapsedMs, traffic, inputUrl)
        },
        combine(isLoading, isPinging, nodePings, message, error) { loading, pinging, pings, info, localError ->
            LocalUiState(loading, pinging, pings, info, localError)
        },
        combine(
            preferences.connectionSettings,
            preferences.subscriptionAutoUpdateInterval,
            preferences.pingAutoInterval,
            preferences.pingTestHosts,
            preferences.pingMethod,
        ) { connectionSettings, autoUpdateInterval, pingInterval, pingHosts, pingMethod ->
            SettingsUiState(
                connectionSettings = connectionSettings,
                subscriptionAutoUpdateInterval = autoUpdateInterval,
                pingAutoInterval = pingInterval,
                pingTestHosts = pingHosts,
                pingMethod = pingMethod,
            )
        },
        startupCrash,
    ) { savedData, vpnData, localData, settingsData, crash ->
        val (connectionSettings, autoUpdateInterval, pingInterval, pingHosts, pingMethod) = settingsData
        val (savedUrl, nodes, selectedNodeId, subscriptionInfo) = savedData
        val (vpnStatus, vpnError, connectionElapsedMs, traffic, inputUrl) = vpnData
        val (loading, pinging, pings, info, localError) = localData

        MainUiState(
            subscriptionUrl = inputUrl.trim().ifBlank { savedUrl.trim() },
            nodes = nodes,
            selectedNodeId = selectedNodeId ?: nodes.firstOrNull()?.id,
            vpnStatus = vpnStatus,
            connectionElapsedMs = connectionElapsedMs,
            downloadBytesPerSec = traffic.downloadBytesPerSec,
            uploadBytesPerSec = traffic.uploadBytesPerSec,
            isLoading = loading,
            isPinging = pinging,
            nodePings = pings,
            subscriptionInfo = subscriptionInfo,
            message = info,
            error = localError ?: vpnError,
            startupCrash = crash,
            connectionSettings = connectionSettings,
            subscriptionAutoUpdateInterval = autoUpdateInterval,
            pingAutoInterval = pingInterval,
            pingTestHosts = pingHosts,
            pingMethod = pingMethod,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainUiState(),
    )

    init {
        AppLog.i("MainViewModel init")
        viewModelScope.launch(Dispatchers.IO) {
            startupCrash.value = null
            preferences.loadActiveRoutingIntoMemory()
            preferences.loadConnectionSettingsIntoMemory()
        }
        viewModelScope.launch {
            restoreSubscriptionSession()
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
            preferences.pingMethod.collect { method ->
                ServerPinger.method = method
            }
        }
        viewModelScope.launch {
            preferences.subscriptionAutoUpdateInterval.collect { interval ->
                restartSubscriptionAutoUpdate(interval)
            }
        }
        viewModelScope.launch {
            preferences.pingAutoInterval.collect { interval ->
                restartPingAutoUpdate(interval)
            }
        }
        viewModelScope.launch {
            VpnManager.status.collect { status ->
                if (status != VpnStatus.Stopped) return@collect
                val nodeId = pendingConnectNodeId ?: return@collect
                pendingConnectNodeId = null
                val node = uiState.value.nodes.find { it.id == nodeId } ?: return@collect
                _connectRequests.emit(node)
            }
        }
        viewModelScope.launch {
            preferences.nodes.collect { nodes ->
                scheduleAutoPing(nodes)
            }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            restoreSubscriptionSession()
            VpnAutoReconnect.tryReconnectOnResume()
            maybeAutoRefreshSubscriptionOnResume()
            val nodes = preferences.nodes.first()
            if (needsPing(nodes)) {
                scheduleAutoPing(nodes, force = true)
            }
        }
    }

    fun setSubscriptionAutoUpdateInterval(interval: SubscriptionAutoUpdateInterval) {
        if (interval == SubscriptionAutoUpdateInterval.OFF) return
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setSubscriptionAutoUpdateInterval(interval)
        }
    }

    fun setPingAutoInterval(interval: PingAutoInterval) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setPingAutoInterval(interval)
        }
    }

    fun setPingTestHosts(raw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setPingTestHosts(raw)
        }
    }

    fun setPingMethod(method: PingMethod) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setPingMethod(method)
        }
    }

    private suspend fun restoreSubscriptionSession() {
        val savedUrl = preferences.subscriptionUrl.first().trim()
        if (savedUrl.isBlank()) return
        if (subscriptionUrlInput.value.isBlank()) {
            subscriptionUrlInput.value = savedUrl
            AppLog.i("restored subscription url from prefs urlLen=${savedUrl.length}")
        }
        val nodes = preferences.nodes.first()
        if (nodes.isEmpty()) {
            AppLog.i("saved subscription has no nodes, refreshing")
            refreshConfig(showUrlRequiredError = false)
        }
    }

    private fun str(@StringRes id: Int, vararg formatArgs: Any): String {
        val ctx = work.bavshield.vpn.data.LocaleHelper.strings(getApplication())
        return if (formatArgs.isEmpty()) ctx.getString(id) else ctx.getString(id, *formatArgs)
    }

    fun canConnect(): Boolean = prepareConnect(showErrors = false)

    fun prepareConnect(showErrors: Boolean = true): Boolean {
        val state = uiState.value
        return when {
            state.nodes.isEmpty() -> {
                if (showErrors) error.value = str(R.string.error_wait_servers)
                false
            }
            state.subscriptionUrl.isBlank() -> {
                if (showErrors) error.value = str(R.string.error_paste_subscription)
                false
            }
            state.subscriptionInfo?.isExpired() == true -> {
                if (showErrors) error.value = str(R.string.subscription_expired)
                false
            }
            else -> true
        }
    }

    fun pingAllNodes() {
        scheduleAutoPing(uiState.value.nodes, force = true)
    }

    private var pingJob: Job? = null
    private var lastAutoPingNodeIds: Set<String>? = null

    private fun needsPing(nodes: List<ProxyNode>): Boolean {
        if (nodes.isEmpty()) return false
        val ids = nodes.map { it.id }.toSet()
        val pings = nodePings.value
        return pings.isEmpty() || ids.any { pings[it] == null }
    }

    private fun scheduleAutoPing(nodes: List<ProxyNode>, force: Boolean = false) {
        if (nodes.isEmpty()) {
            lastAutoPingNodeIds = null
            return
        }
        val ids = nodes.map { it.id }.toSet()
        if (!force && ids == lastAutoPingNodeIds && !needsPing(nodes)) return
        lastAutoPingNodeIds = ids
        pingAllNodes(nodes)
    }

    private fun pingAllNodes(nodes: List<ProxyNode>) {
        if (nodes.isEmpty()) return

        pingJob?.cancel()
        pingJob = viewModelScope.launch(Dispatchers.IO) {
            isPinging.value = true
            nodePings.value = nodes.associate { it.id to PingState.Loading }
            try {
                ServerPinger.pingAll(nodes, method = ServerPinger.method) { nodeId, state ->
                    nodePings.value = nodePings.value + (nodeId to state)
                }
            } finally {
                isPinging.value = false
            }
        }
    }

    fun onSubscriptionUrlChange(value: String) {
        subscriptionUrlInput.value = value
    }

    fun processDeepLink(uri: Uri, onEffect: (DeepLinkEffect) -> Unit) {
        val action = DeepLinkParser.parse(uri) ?: run {
            AppLog.w("processDeepLink unsupported uri=$uri")
            error.value = str(R.string.error_unsupported_link)
            return
        }
        AppLog.i("processDeepLink action=$action")
        when (action) {
            DeepLinkAction.Open -> {
                message.value = str(R.string.app_name)
            }
            DeepLinkAction.Connect -> {
                if (prepareConnect(showErrors = true)) {
                    onEffect(DeepLinkEffect.RequestConnect)
                }
            }
            DeepLinkAction.Disconnect -> {
                VpnManager.disconnect()
                message.value = str(R.string.error_disconnected)
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
        subscriptionUrlInput.value = url.trim()
        message.value = if (connectAfter) {
            str(R.string.error_subscription_added_connecting)
        } else {
            str(R.string.error_subscription_added)
        }
        refreshConfig(showUrlRequiredError = false) { success ->
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
                if (nodes.isEmpty()) error(str(R.string.error_subscription_empty))
                preferences.saveSubscription(
                    LOCAL_IMPORT_URL,
                    nodes,
                    nodes.first().id,
                    null,
                )
                subscriptionUrlInput.value = LOCAL_IMPORT_URL
                message.value = str(R.string.error_imported_servers, nodes.size)
                AppLog.i("importSubscriptionPayload ok nodes=${nodes.size}")
                if (connectAfter && prepareConnect(showErrors = true)) {
                    onEffect(DeepLinkEffect.RequestConnect)
                }
            } catch (e: Exception) {
                AppLog.e("importSubscriptionPayload failed", e)
                error.value = e.message ?: str(R.string.error_import_failed)
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun saveRoutingFromDeepLink(profileJson: String, enable: Boolean) {
        viewModelScope.launch {
            try {
                val name = JSONObject(profileJson).optString("Name")
                    .ifBlank { str(R.string.routing_profile_fallback) }
                preferences.saveRoutingProfile(profileJson, enable)
                message.value = if (enable) {
                    str(R.string.error_routing_enabled, name)
                } else {
                    str(R.string.error_routing_saved, name)
                }
            } catch (e: Exception) {
                AppLog.e("saveRoutingFromDeepLink failed", e)
                error.value = str(R.string.error_routing_invalid)
            }
        }
    }

    fun pasteSubscriptionFromClipboard() {
        val text = getApplication<Application>().readClipboardText()
        if (text.isNullOrBlank()) {
            error.value = str(R.string.error_clipboard_empty)
            return
        }
        val url = text.trim()
        subscriptionUrlInput.value = url
        message.value = str(R.string.error_link_pasted)
        AppLog.i("pasteSubscriptionFromClipboard urlLen=${url.length}")
        if (looksLikeSubscriptionUrl(url)) {
            viewModelScope.launch {
                preferences.saveSubscriptionUrl(url)
            }
        }
        refreshConfig(showUrlRequiredError = false)
    }

    private fun looksLikeSubscriptionUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("http://") || lower.startsWith("https://")
    }

    fun deleteSubscription() {
        viewModelScope.launch {
            VpnManager.disconnect()
            pingJob?.cancel()
            isPinging.value = false
            lastAutoPingNodeIds = null
            preferences.clearSubscription()
            subscriptionUrlInput.value = ""
            nodePings.value = emptyMap()
            clearMessages()
            message.value = str(R.string.error_subscription_deleted)
            AppLog.i("deleteSubscription ok")
        }
    }

    fun refreshSubscription() {
        refreshConfig(showUrlRequiredError = true)
    }

    fun refreshConfig(
        showUrlRequiredError: Boolean = false,
        onComplete: ((Boolean) -> Unit)? = null,
    ) {
        val url = uiState.value.subscriptionUrl.trim()
            .ifBlank { subscriptionUrlInput.value.trim() }
        if (url.isBlank() || url == LOCAL_IMPORT_URL) {
            if (url == LOCAL_IMPORT_URL && uiState.value.nodes.isNotEmpty()) {
                onComplete?.invoke(true)
                return
            }
            if (showUrlRequiredError) {
                error.value = str(R.string.error_paste_subscription)
            }
            onComplete?.invoke(false)
            return
        }
        if (subscriptionUrlInput.value.isBlank()) {
            subscriptionUrlInput.value = url
        }

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            message.value = null
            val success = runCatching {
                fetchAndSaveSubscription(url)
            }.onSuccess { nodeCount ->
                message.value = str(R.string.error_config_updated, nodeCount)
                AppLog.i("refreshConfig ok, nodes=$nodeCount")
            }.onFailure { e ->
                AppLog.e("refreshConfig failed", e)
                error.value = (e as? Exception)?.message ?: str(R.string.error_refresh_failed)
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
            result.nodes.any { it.id == id }
        } ?: result.nodes.first().id
        preferences.saveSubscription(url, result.nodes, selected, result.info)
        preferences.setSubscriptionLastAutoRefreshAt(System.currentTimeMillis())
        scheduleAutoPing(result.nodes, force = true)
        return result.nodes.size
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
        val interval = preferences.subscriptionAutoUpdateInterval.first()
        if (interval == SubscriptionAutoUpdateInterval.OFF) return
        val url = preferences.subscriptionUrl.first().trim()
        if (url.isBlank() || url == LOCAL_IMPORT_URL) return
        val last = preferences.subscriptionLastAutoRefreshAt.first()
        val elapsed = System.currentTimeMillis() - last
        if (last == 0L || elapsed >= interval.durationMs) {
            AppLog.i("autoRefreshSubscription on resume elapsed=${elapsed}ms interval=${interval.logLabel}")
            autoRefreshSubscriptionSilent()
        }
    }

    private fun restartSubscriptionAutoUpdate(interval: SubscriptionAutoUpdateInterval) {
        subscriptionAutoUpdateJob?.cancel()
        if (interval == SubscriptionAutoUpdateInterval.OFF) return
        subscriptionAutoUpdateJob = viewModelScope.launch {
            AppLog.i("subscriptionAutoUpdate started interval=${interval.logLabel}")
            delay(interval.durationMs)
            while (isActive) {
                autoRefreshSubscriptionSilent()
                delay(interval.durationMs)
            }
        }
    }

    private fun restartPingAutoUpdate(interval: PingAutoInterval) {
        pingAutoJob?.cancel()
        if (interval == PingAutoInterval.OFF) return
        pingAutoJob = viewModelScope.launch {
            AppLog.i("pingAutoUpdate started interval=${interval.logLabel}")
            delay(interval.durationMs)
            while (isActive) {
                val nodes = preferences.nodes.first()
                if (nodes.isNotEmpty()) {
                    pingAllNodes(nodes)
                }
                delay(interval.durationMs)
            }
        }
    }

    fun selectNode(nodeId: String) {
        viewModelScope.launch {
            preferences.setSelectedNodeId(nodeId)
        }
    }

    fun requestConnectToNode(nodeId: String) {
        clearMessages()
        if (!prepareConnect()) return

        val node = uiState.value.nodes.find { it.id == nodeId } ?: return
        selectNode(nodeId)

        when (VpnManager.status.value) {
            VpnStatus.Stopped -> {
                viewModelScope.launch { _connectRequests.emit(node) }
            }
            VpnStatus.Started -> {
                val connectedId = VpnAutoReconnect.connectedNode()?.id
                if (connectedId == nodeId) return
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

    fun readLogs(): String = buildString {
        append(str(R.string.logs_file_prefix))
        append(AppLog.logPath())
        append("\n\n")
        append(AppLog.readTail())
    }

    fun saveConnectionSettings(settings: ConnectionSettingsState) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.saveConnectionSettings(settings)
            if (!settings.killSwitchEnabled) {
                KillSwitchVpnService.release(getApplication())
            }
            val node = selectedNode()
            val wasConnected = uiState.value.vpnStatus == VpnStatus.Started
            if (wasConnected && node != null) {
                withContext(Dispatchers.Main) {
                    VpnManager.disconnect(userInitiated = true)
                    VpnManager.connect(node)
                }
            }
        }
    }

    fun selectedNode(): ProxyNode? {
        val state = uiState.value
        return state.nodes.find { it.id == state.selectedNodeId }
    }

    private data class VpnUiState(
        val vpnStatus: VpnStatus,
        val vpnError: String?,
        val connectionElapsedMs: Long,
        val traffic: work.bavshield.vpn.vpn.VpnTrafficStats,
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
        val pingAutoInterval: PingAutoInterval = PingAutoInterval.DEFAULT,
        val pingTestHosts: String = AppPreferences.DEFAULT_PING_TEST_HOSTS,
        val pingMethod: PingMethod = PingMethod.DEFAULT,
    )
}
