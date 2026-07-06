package ru.coffeemaniavpn.app.ui

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
import ru.coffeemaniavpn.app.data.AppPreferences
import ru.coffeemaniavpn.app.data.ConnectionSettingsState
import ru.coffeemaniavpn.app.data.PingState
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.data.ServerPinger
import ru.coffeemaniavpn.app.data.AppThemeMode
import ru.coffeemaniavpn.app.data.SubscriptionAutoUpdateInterval
import ru.coffeemaniavpn.app.data.SubscriptionInfo
import ru.coffeemaniavpn.app.data.SubscriptionParser
import ru.coffeemaniavpn.app.data.SubscriptionRepository
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
    val favoriteNodeIds: Set<String> = emptySet(),
    val vpnStatus: VpnStatus = VpnStatus.Stopped,
    val connectionElapsedMs: Long = 0L,
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
    val appThemeMode: AppThemeMode = AppThemeMode.DEFAULT,
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
            preferences.favoriteNodeIds,
        ) { savedUrl, nodes, selectedNodeId, subscriptionInfo, favoriteNodeIds ->
            AppLog.i("prefs loaded urlLen=${savedUrl.length} nodes=${nodes.size}")
            SavedData(savedUrl, nodes, selectedNodeId, subscriptionInfo, favoriteNodeIds)
        },
        combine(
            VpnManager.status,
            VpnManager.lastError,
            VpnManager.connectionElapsedMs,
            subscriptionUrlInput,
        ) { vpnStatus, vpnError, elapsedMs, inputUrl ->
            VpnUiState(vpnStatus, vpnError, elapsedMs, inputUrl)
        },
        combine(isLoading, isPinging, nodePings, message, error) { loading, pinging, pings, info, localError ->
            LocalUiState(loading, pinging, pings, info, localError)
        },
        combine(
            preferences.connectionSettings,
            preferences.subscriptionAutoUpdateInterval,
            preferences.appThemeMode,
        ) { connectionSettings, autoUpdateInterval, appThemeMode ->
            SettingsUiState(connectionSettings, autoUpdateInterval, appThemeMode)
        },
        startupCrash,
    ) { savedData, vpnData, localData, settingsData, crash ->
        val (connectionSettings, autoUpdateInterval, appThemeMode) = settingsData
        val (savedUrl, nodes, selectedNodeId, subscriptionInfo, favoriteNodeIds) = savedData
        val (vpnStatus, vpnError, connectionElapsedMs, inputUrl) = vpnData
        val (loading, pinging, pings, info, localError) = localData

        MainUiState(
            subscriptionUrl = inputUrl.trim().ifBlank { savedUrl.trim() },
            nodes = nodes,
            selectedNodeId = selectedNodeId ?: nodes.firstOrNull()?.id,
            favoriteNodeIds = favoriteNodeIds,
            vpnStatus = vpnStatus,
            connectionElapsedMs = connectionElapsedMs,
            isLoading = loading,
            isPinging = pinging,
            nodePings = pings,
            subscriptionInfo = subscriptionInfo,
            message = info,
            error = localError ?: vpnError,
            startupCrash = crash,
            connectionSettings = connectionSettings,
            subscriptionAutoUpdateInterval = autoUpdateInterval,
            appThemeMode = appThemeMode,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainUiState(),
    )

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
            preferences.subscriptionAutoUpdateInterval.collect { interval ->
                restartSubscriptionAutoUpdate(interval)
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

    fun setAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setAppThemeMode(mode)
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
                if (showErrors) error.value = "Дождитесь загрузки серверов"
                false
            }
            state.subscriptionUrl.isBlank() -> {
                if (showErrors) error.value = "Вставьте ссылку подписки"
                false
            }
            state.subscriptionInfo?.isExpired() == true -> {
                if (showErrors) error.value = "Подписка истекла"
                false
            }
            else -> true
        }
    }

    fun pingAllNodes() {
        pingAllNodes(uiState.value.nodes)
    }

    private var pingJob: Job? = null

    private fun pingAllNodes(nodes: List<ProxyNode>) {
        if (nodes.isEmpty()) return

        pingJob?.cancel()
        pingJob = viewModelScope.launch(Dispatchers.IO) {
            isPinging.value = true
            nodePings.value = nodes.associate { it.id to PingState.Loading }
            try {
                ServerPinger.pingAll(nodes) { nodeId, state ->
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
            error.value = "Неподдерживаемая ссылка"
            return
        }
        AppLog.i("processDeepLink action=$action")
        when (action) {
            DeepLinkAction.Open -> {
                message.value = "NUBO VPN"
            }
            DeepLinkAction.Connect -> {
                if (prepareConnect(showErrors = true)) {
                    onEffect(DeepLinkEffect.RequestConnect)
                }
            }
            DeepLinkAction.Disconnect -> {
                VpnManager.disconnect()
                message.value = "Отключено"
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
            "Подписка добавлена, подключаемся…"
        } else {
            "Подписка добавлена"
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
                if (nodes.isEmpty()) error("Подписка пуста")
                preferences.saveSubscription(
                    LOCAL_IMPORT_URL,
                    nodes,
                    nodes.first().id,
                    null,
                )
                subscriptionUrlInput.value = LOCAL_IMPORT_URL
                message.value = "Импортировано серверов: ${nodes.size}"
                AppLog.i("importSubscriptionPayload ok nodes=${nodes.size}")
                if (connectAfter && prepareConnect(showErrors = true)) {
                    onEffect(DeepLinkEffect.RequestConnect)
                }
            } catch (e: Exception) {
                AppLog.e("importSubscriptionPayload failed", e)
                error.value = e.message ?: "Не удалось импортировать конфиг"
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun saveRoutingFromDeepLink(profileJson: String, enable: Boolean) {
        viewModelScope.launch {
            try {
                val name = JSONObject(profileJson).optString("Name").ifBlank { "Профиль" }
                preferences.saveRoutingProfile(profileJson, enable)
                message.value = if (enable) {
                    "Маршрутизация включена: $name"
                } else {
                    "Маршрутизация сохранена: $name"
                }
            } catch (e: Exception) {
                AppLog.e("saveRoutingFromDeepLink failed", e)
                error.value = "Неверный профиль маршрутизации"
            }
        }
    }

    fun pasteSubscriptionFromClipboard() {
        val text = getApplication<Application>().readClipboardText()
        if (text.isNullOrBlank()) {
            error.value = "Буфер обмена пуст"
            return
        }
        subscriptionUrlInput.value = text
        message.value = "Ссылка вставлена"
        AppLog.i("pasteSubscriptionFromClipboard urlLen=${text.length}")
        refreshConfig(showUrlRequiredError = false)
    }

    fun deleteSubscription() {
        viewModelScope.launch {
            VpnManager.disconnect()
            pingJob?.cancel()
            isPinging.value = false
            preferences.clearSubscription()
            subscriptionUrlInput.value = ""
            nodePings.value = emptyMap()
            clearMessages()
            message.value = "Подписка удалена"
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
                error.value = "Вставьте ссылку подписки"
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
                message.value = "Конфиг обновлён: $nodeCount серверов"
                AppLog.i("refreshConfig ok, nodes=$nodeCount")
            }.onFailure { e ->
                AppLog.e("refreshConfig failed", e)
                error.value = (e as? Exception)?.message ?: "Не удалось обновить конфиг"
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
            AppLog.i("autoRefreshSubscription on resume elapsed=${elapsed}ms interval=${interval.label}")
            autoRefreshSubscriptionSilent()
        }
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

    fun toggleFavoriteNode(nodeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.toggleFavoriteNodeId(nodeId)
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
        append("Файл: ")
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
        val inputUrl: String,
    )

    private data class SavedData(
        val subscriptionUrl: String,
        val nodes: List<ProxyNode>,
        val selectedNodeId: String?,
        val subscriptionInfo: SubscriptionInfo?,
        val favoriteNodeIds: Set<String>,
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
        val appThemeMode: AppThemeMode,
    )
}
