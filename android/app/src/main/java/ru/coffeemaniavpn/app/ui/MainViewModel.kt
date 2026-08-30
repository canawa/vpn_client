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
import ru.coffeemaniavpn.app.data.TrafficRoutingMode
import org.json.JSONObject
import ru.coffeemaniavpn.app.data.TvImportClient
import ru.coffeemaniavpn.app.data.TvImportSubmitResult
import ru.coffeemaniavpn.app.deeplink.DeepLinkAction
import ru.coffeemaniavpn.app.deeplink.DeepLinkEffect
import ru.coffeemaniavpn.app.deeplink.DeepLinkParser
import ru.coffeemaniavpn.app.deeplink.TvImportHostValidator
import kotlinx.coroutines.flow.asStateFlow
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
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val LOCAL_IMPORT_URL = "deeplink://imported"
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
    private val tvImportUi = MutableStateFlow<TvImportUiState>(TvImportUiState.Hidden)
    val tvImportState: StateFlow<TvImportUiState> = tvImportUi.asStateFlow()
    private var tvImportJob: Job? = null
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
        startupCrash,
    ) { savedData, vpnData, localData, settingsData, crash ->
        val (savedUrl, nodes, selectedNodeId, subscriptionInfo) = savedData
        val (vpnStatus, vpnError, connectionElapsedMs, downlinkBps, uplinkBps, _) = vpnData
        val (loading, pinging, pings, info, localError) = localData

        MainUiState(
            subscriptionUrl = savedUrl.trim(),
            nodes = nodes,
            selectedNodeId = selectedNodeId ?: nodes.firstOrNull()?.id,
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
                val node = uiState.value.nodes.find { it.id == nodeId } ?: return@collect
                _connectRequests.emit(node)
            }
        }
    }

    fun onAppResumed() {
        viewModelScope.launch {
            restoreSubscriptionSession()
            VpnAutoReconnect.tryReconnectOnResume()
            maybeAutoRefreshSubscriptionOnResume()
        }
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
            val batchTimeoutMs = ServerPinger.estimatedBatchTimeoutMs(nodes.size)
            val watchdogs = nodes.map { node ->
                launch {
                    delay(batchTimeoutMs)
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
            if (uri.host.equals("tv-import", ignoreCase = true)) {
                AppLog.w("processDeepLink tv-import invalid params uri=$uri")
                tvImportUi.value = TvImportUiState.Error(
                    target = null,
                    message = appStr(R.string.tv_import_bad_params),
                )
                return
            }
            AppLog.w("processDeepLink unsupported uri=$uri")
            error.value = invalidSubscriptionLink()
            return
        }
        AppLog.i("processDeepLink action=$action")
        when (action) {
            DeepLinkAction.Open -> {
                message.value = "POROZOFF VPN"
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
            is DeepLinkAction.TvImport -> startTvImport(action)
        }
    }

    fun dismissTvImport() {
        tvImportJob?.cancel()
        tvImportJob = null
        tvImportUi.value = TvImportUiState.Hidden
    }

    fun onTvImportDraftUrlChange(value: String) {
        when (val current = tvImportUi.value) {
            is TvImportUiState.NoSubscription ->
                tvImportUi.value = current.copy(draftUrl = value)
            is TvImportUiState.Error ->
                tvImportUi.value = current.copy(draftUrl = value)
            else -> Unit
        }
    }

    fun sendTvImportDraftUrl() {
        val current = tvImportUi.value
        val target = when (current) {
            is TvImportUiState.NoSubscription -> current.target
            is TvImportUiState.Error -> current.target
            else -> null
        } ?: return
        val draft = when (current) {
            is TvImportUiState.NoSubscription -> current.draftUrl
            is TvImportUiState.Error -> current.draftUrl
            else -> ""
        }.trim()
        if (!isHttpSubscriptionUrl(draft)) {
            tvImportUi.value = TvImportUiState.Error(
                target = target,
                message = appStr(R.string.tv_import_bad_url),
                allowManualUrl = true,
                draftUrl = draft,
            )
            return
        }
        submitTvImport(target, draft)
    }

    private fun startTvImport(action: DeepLinkAction.TvImport) {
        val target = TvImportTarget(
            host = action.host,
            port = action.port,
            token = action.token,
        )
        AppLog.i("startTvImport host=${target.host} port=${target.port} tokenLen=${target.token.length}")
        if (!TvImportHostValidator.isAllowed(target.host)) {
            tvImportUi.value = TvImportUiState.Error(
                target = null,
                message = appStr(R.string.tv_import_bad_host),
            )
            return
        }
        tvImportJob?.cancel()
        tvImportJob = viewModelScope.launch {
            val saved = preferences.subscriptionUrl.first().trim()
                .ifBlank { subscriptionUrlInput.value.trim() }
            if (!isHttpSubscriptionUrl(saved)) {
                AppLog.w("startTvImport: no saved subscription url")
                tvImportUi.value = TvImportUiState.NoSubscription(target = target)
                return@launch
            }
            submitTvImport(target, saved)
        }
    }

    private fun submitTvImport(target: TvImportTarget, subscriptionUrl: String) {
        tvImportJob?.cancel()
        tvImportJob = viewModelScope.launch {
            tvImportUi.value = TvImportUiState.Sending(target)
            val result = withContext(Dispatchers.IO) {
                TvImportClient.submit(
                    host = target.host,
                    port = target.port,
                    token = target.token,
                    subscriptionUrl = subscriptionUrl,
                )
            }
            tvImportUi.value = when (result) {
                TvImportSubmitResult.Success -> TvImportUiState.Success(target)
                is TvImportSubmitResult.RejectedUrl -> TvImportUiState.Error(
                    target = target,
                    message = appStr(R.string.tv_import_rejected_url),
                    allowManualUrl = true,
                    draftUrl = subscriptionUrl,
                )
                is TvImportSubmitResult.Forbidden -> TvImportUiState.Error(
                    target = target,
                    message = appStr(R.string.tv_import_forbidden),
                )
                TvImportSubmitResult.NetworkError -> TvImportUiState.Error(
                    target = target,
                    message = appStr(R.string.tv_import_network),
                )
                is TvImportSubmitResult.HttpError -> TvImportUiState.Error(
                    target = target,
                    message = appStr(R.string.tv_import_http_error, result.code),
                )
            }
        }
    }

    private fun addSubscriptionFromDeepLink(
        url: String,
        connectAfter: Boolean,
        onEffect: (DeepLinkEffect) -> Unit = {},
    ) {
        subscriptionUrlInput.value = url.trim()
        message.value = if (connectAfter) {
            appStr(R.string.msg_subscription_added_connecting)
        } else {
            appStr(R.string.msg_subscription_added)
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
        onEffect: (DeepLinkEffect) -> Unit = {},
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
                preferences.saveSubscription(
                    LOCAL_IMPORT_URL,
                    nodes,
                    nodes.first().id,
                    null,
                )
                subscriptionUrlInput.value = LOCAL_IMPORT_URL
                message.value = appStr(R.string.msg_servers_imported, nodes.size)
                AppLog.i("importSubscriptionPayload ok nodes=${nodes.size}")
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
        applySubscriptionLink(text, "pasteSubscriptionFromClipboard")
    }

    fun applySubscriptionFromQr(raw: String) {
        applySubscriptionLink(raw, "applySubscriptionFromQr")
    }

    private fun applySubscriptionLink(text: String, logTag: String) {
        val payload = normalizePastedSubscription(text)
        if (payload.isBlank()) {
            error.value = invalidSubscriptionLink()
            return
        }
        AppLog.i("$logTag len=${payload.length} prefix=${payload.take(24)}")
        when {
            isHttpSubscriptionUrl(payload) -> {
                subscriptionUrlInput.value = payload
                refreshConfig(showUrlRequiredError = false)
            }
            looksLikeNodeShareLink(payload) ->
                importSubscriptionPayload(payload, connectAfter = false)
            else -> {
                val action = runCatching { DeepLinkParser.parse(Uri.parse(payload)) }.getOrNull()
                when (action) {
                    is DeepLinkAction.Add -> addSubscriptionFromDeepLink(
                        url = action.url,
                        connectAfter = action.connectAfter,
                    )
                    is DeepLinkAction.Import -> importSubscriptionPayload(
                        payload = action.payload,
                        connectAfter = false,
                    )
                    else -> error.value = invalidSubscriptionLink()
                }
            }
        }
    }

    private fun normalizePastedSubscription(raw: String): String {
        val trimmed = raw.trim().removePrefix("\uFEFF").trim()
        if (trimmed.isBlank()) return ""
        if (isHttpSubscriptionUrl(trimmed) || looksLikeNodeShareLink(trimmed)) return trimmed

        trimmed.lineSequence()
            .map { it.trim() }
            .firstOrNull { isHttpSubscriptionUrl(it) || looksLikeNodeShareLink(it) }
            ?.let { return it }

        Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
            .find(trimmed)
            ?.value
            ?.trimEnd(',', '.', ';', ')')
            ?.let { return it }

        return trimmed
    }

    private fun looksLikeNodeShareLink(text: String): Boolean {
        val value = text.trim()
        return value.startsWith("vless://", ignoreCase = true) ||
            value.startsWith("vmess://", ignoreCase = true) ||
            value.startsWith("ss://", ignoreCase = true) ||
            value.startsWith("ssr://", ignoreCase = true) ||
            value.startsWith("trojan://", ignoreCase = true) ||
            value.startsWith("hy2://", ignoreCase = true) ||
            value.startsWith("hysteria2://", ignoreCase = true) ||
            value.startsWith("tuic://", ignoreCase = true)
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
            result.nodes.any { it.id == id }
        } ?: result.nodes.first().id
        preferences.saveSubscription(url, result.nodes, selected, result.info)
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
        if (maybeRefreshStaleNodes()) return
        val interval = preferences.subscriptionAutoUpdateInterval.first()
        if (interval == SubscriptionAutoUpdateInterval.OFF) return
        val url = preferences.subscriptionUrl.first().trim()
        if (url.isBlank() || url == LOCAL_IMPORT_URL) return
        val last = preferences.subscriptionLastAutoRefreshAt.first()
        val elapsed = System.currentTimeMillis() - last
        if (last == 0L || elapsed >= interval.durationMs) {
            AppLog.i("autoRefreshSubscription on resume elapsed=${elapsed}ms interval=${interval.label}")
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
        viewModelScope.launch {
            preferences.setSelectedNodeId(nodeId)
        }
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
        viewModelScope.launch(Dispatchers.IO) {
            val normalized = RoutingRuleInput.normalizeWebsite(rawValue)
            if (normalized.isBlank()) return@launch

            val current = preferences.connectionSettings.first()
            val duplicate = current.customRules.any { rule ->
                rule.matcher == RoutingRuleMatcher.DomainSuffix &&
                    RoutingRuleInput.normalizeWebsite(rule.value) == normalized
            }
            if (duplicate) return@launch

            persistConnectionSettings(
                current.copy(
                    customRules = current.customRules + RoutingRule(
                        value = normalized,
                        matcher = RoutingRuleMatcher.DomainSuffix,
                        target = target,
                    ),
                    sitesEnabled = true,
                ),
            )

            if (preferences.trafficRoutingMode.first() != TrafficRoutingMode.CUSTOM) {
                preferences.setTrafficRoutingMode(TrafficRoutingMode.CUSTOM)
            }
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
        val node = selectedNode()
        val wasConnected = uiState.value.vpnStatus == VpnStatus.Started
        if (wasConnected && node != null) {
            withContext(Dispatchers.Main) {
                VpnManager.disconnect(userInitiated = true)
                VpnManager.connect(node)
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
