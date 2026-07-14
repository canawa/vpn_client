package ru.nubovpn.app.ui

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
import ru.nubovpn.app.data.AppPreferences
import ru.nubovpn.app.data.ConnectionSettingsState
import ru.nubovpn.app.data.PingState
import ru.nubovpn.app.data.ProxyNode
import ru.nubovpn.app.data.ServerPinger
import ru.nubovpn.app.data.AppThemeMode
import ru.nubovpn.app.data.SubscriptionAutoUpdateInterval
import ru.nubovpn.app.data.SubscriptionInfo
import ru.nubovpn.app.data.SubscriptionParser
import ru.nubovpn.app.data.SubscriptionRepository
import org.json.JSONObject
import ru.nubovpn.app.App
import ru.nubovpn.app.deeplink.DeepLinkAction
import ru.nubovpn.app.deeplink.DeepLinkEffect
import ru.nubovpn.app.deeplink.DeepLinkParser
import java.util.concurrent.ConcurrentHashMap
import ru.nubovpn.app.ktx.readClipboardText
import ru.nubovpn.app.util.AppLog
import ru.nubovpn.app.vpn.KillSwitchVpnService
import ru.nubovpn.app.vpn.VpnAutoReconnect
import ru.nubovpn.app.vpn.VpnManager
import ru.nubovpn.app.vpn.VpnStatus

data class MainUiState(
    val subscriptionUrl: String = "",
    val nodes: List<ProxyNode> = emptyList(),
    val selectedNodeId: String? = null,
    val favoriteNodeIds: Set<String> = emptySet(),
    val vpnStatus: VpnStatus = VpnStatus.Stopped,
    val connectionElapsedMs: Long = 0L,
    val isLoading: Boolean = false,
    val subscriptionLoad: SubscriptionLoadState = SubscriptionLoadState(),
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
    val sortByPing: Boolean = false,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val LOCAL_IMPORT_URL = "deeplink://imported"
    }

    private val preferences = AppPreferences(application)
    private val repository = SubscriptionRepository(application)

    private val subscriptionUrlInput = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private val subscriptionLoadState = MutableStateFlow(SubscriptionLoadState())
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
        combine(
            combine(isLoading, subscriptionLoadState) { loading, load -> loading to load },
            isPinging,
            nodePings,
            message,
            error,
        ) { loadingPair, pinging, pings, info, localError ->
            val (loading, load) = loadingPair
            LocalUiState(loading, load, pinging, pings, info, localError)
        },
        combine(
            preferences.connectionSettings,
            preferences.subscriptionAutoUpdateInterval,
            preferences.appThemeMode,
            preferences.sortByPing,
        ) { connectionSettings, autoUpdateInterval, appThemeMode, sortByPing ->
            SettingsUiState(connectionSettings, autoUpdateInterval, appThemeMode, sortByPing)
        },
        startupCrash,
    ) { savedData, vpnData, localData, settingsData, crash ->
        val (connectionSettings, autoUpdateInterval, appThemeMode, sortByPing) = settingsData
        val (savedUrl, nodes, selectedNodeId, subscriptionInfo, favoriteNodeIds) = savedData
        val (vpnStatus, vpnError, connectionElapsedMs, inputUrl) = vpnData
        val (loading, load, pinging, pings, info, localError) = localData

        MainUiState(
            subscriptionUrl = inputUrl.trim().ifBlank { savedUrl.trim() },
            nodes = nodes,
            selectedNodeId = selectedNodeId ?: nodes.firstOrNull()?.id,
            favoriteNodeIds = favoriteNodeIds,
            vpnStatus = vpnStatus,
            connectionElapsedMs = connectionElapsedMs,
            isLoading = loading,
            subscriptionLoad = load,
            isPinging = pinging,
            nodePings = pings,
            subscriptionInfo = subscriptionInfo,
            message = info,
            error = localError ?: vpnError,
            startupCrash = crash,
            connectionSettings = connectionSettings,
            subscriptionAutoUpdateInterval = autoUpdateInterval,
            appThemeMode = appThemeMode,
            sortByPing = sortByPing,
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
        viewModelScope.launch(Dispatchers.IO) {
            preferences.nodes.collect { nodes ->
                if (nodes.isEmpty()) return@collect
                val flags = nodes.map { node -> ServerDisplayMapper.map(node).flag }
                FlagImagePrefetcher.prefetch(App.instance, flags)
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
            val state = uiState.value
            if (state.sortByPing && state.nodes.isNotEmpty() && state.nodePings.isEmpty() && !state.isPinging) {
                pingAllNodes(
                    nodes = state.nodes,
                    selectBestOnComplete = true,
                )
            }
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
        pingAllNodes(
            nodes = uiState.value.nodes,
            selectBestOnComplete = uiState.value.sortByPing,
        )
    }

    private var pingJob: Job? = null

    private fun pingAllNodes(
        nodes: List<ProxyNode>,
        selectBestOnComplete: Boolean = false,
    ) {
        if (nodes.isEmpty()) return

        pingJob?.cancel()
        pingJob = viewModelScope.launch(Dispatchers.IO) {
            isPinging.value = true
            nodePings.value = nodes.associate { it.id to PingState.Loading }
            val pending = ConcurrentHashMap<String, PingState>()
            val flushJob = launch {
                while (isActive) {
                    delay(150)
                    if (pending.isEmpty()) continue
                    val batch = pending.toMap()
                    pending.keys.forEach { pending.remove(it) }
                    nodePings.value = nodePings.value + batch
                }
            }
            try {
                ServerPinger.pingAll(nodes) { nodeId, state ->
                    pending[nodeId] = state
                }
                if (pending.isNotEmpty()) {
                    nodePings.value = nodePings.value + pending.toMap()
                    pending.clear()
                }
            } finally {
                flushJob.cancel()
                isPinging.value = false
                if (selectBestOnComplete) {
                    selectBestNodeByPing(nodes)
                }
            }
        }
    }

    private fun selectBestNodeByPing(nodes: List<ProxyNode>) {
        val pings = nodePings.value
        val bestNodeId = nodes
            .mapNotNull { node ->
                when (val ping = pings[node.id]) {
                    is PingState.Result -> node.id to ping.latencyMs
                    else -> null
                }
            }
            .minByOrNull { it.second }
            ?.first
            ?: return

        val bestNode = nodes.find { it.id == bestNodeId } ?: return
        val currentId = uiState.value.selectedNodeId
        if (currentId == bestNodeId) return

        viewModelScope.launch {
            preferences.setSelectedNodeId(bestNodeId)
            message.value = "Выбран лучший сервер: ${bestNode.name}"
            AppLog.i("selectBestNodeByPing node=${bestNode.name} ping=${pings[bestNodeId]}")
            if (VpnManager.status.value != VpnStatus.Stopped) {
                switchVpnToNode(bestNodeId)
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
            runSubscriptionLoad {
                report(0.15f, "Чтение конфигурации…")
                val nodes = withContext(Dispatchers.IO) {
                    report(0.45f, "Разбор серверов…")
                    SubscriptionParser.parse(trimmed)
                }
                if (nodes.isEmpty()) error("Подписка пуста")
                report(0.85f, "Сохранение…")
                preferences.saveSubscription(
                    LOCAL_IMPORT_URL,
                    nodes,
                    nodes.first().id,
                    null,
                )
                subscriptionUrlInput.value = LOCAL_IMPORT_URL
                nodes.size
            }.onSuccess { count ->
                message.value = "Импортировано серверов: $count"
                AppLog.i("importSubscriptionPayload ok nodes=$count")
                if (connectAfter && prepareConnect(showErrors = true)) {
                    onEffect(DeepLinkEffect.RequestConnect)
                }
            }.onFailure { e ->
                AppLog.e("importSubscriptionPayload failed", e)
                error.value = e.message ?: "Не удалось импортировать конфиг"
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
            error.value = null
            message.value = null
            val success = runSubscriptionLoad {
                fetchAndSaveSubscription(url) { progress, msg -> report(progress, msg) }
            }.onSuccess { nodeCount ->
                message.value = "Конфиг обновлён: $nodeCount серверов"
                AppLog.i("refreshConfig ok, nodes=$nodeCount")
            }.onFailure { e ->
                AppLog.e("refreshConfig failed", e)
                error.value = (e as? Exception)?.message ?: "Не удалось обновить конфиг"
            }.isSuccess
            onComplete?.invoke(success)
        }
    }

    private suspend fun runSubscriptionLoad(
        block: suspend SubscriptionLoadReporter.() -> Int,
    ): Result<Int> {
        isLoading.value = true
        val reporter = SubscriptionLoadReporter { progress, msg ->
            subscriptionLoadState.value = SubscriptionLoadState(
                active = true,
                progress = progress.coerceIn(0f, 1f),
                message = msg,
            )
        }
        return try {
            reporter.report(0.05f, "Подготовка…")
            delay(120)
            val count = block(reporter)
            reporter.report(1f, "Готово!")
            delay(450)
            Result.success(count)
        } catch (e: Throwable) {
            Result.failure(e)
        } finally {
            subscriptionLoadState.value = SubscriptionLoadState()
            isLoading.value = false
        }
    }

    private class SubscriptionLoadReporter(
        private val emit: (Float, String) -> Unit,
    ) {
        fun report(progress: Float, message: String) = emit(progress, message)
    }

    private suspend fun fetchAndSaveSubscription(
        url: String,
        report: (Float, String) -> Unit = { _, _ -> },
    ): Int {
        report(0.2f, "Подключение к серверу…")
        val result = withContext(Dispatchers.IO) {
            repository.fetchSubscription(url) { attempt, maxAttempts ->
                if (attempt == 1) {
                    report(0.45f, "Загрузка подписки…")
                } else {
                    report(0.45f, "Повторная попытка $attempt из $maxAttempts…")
                }
            }
        }
        report(0.72f, "Разбор серверов…")
        delay(150)
        report(0.88f, "Сохранение…")
        val selected = uiState.value.selectedNodeId?.takeIf { id ->
            result.nodes.any { it.id == id }
        } ?: result.nodes.first().id
        preferences.saveSubscription(url, result.nodes, selected, result.info)
        preferences.setSubscriptionLastAutoRefreshAt(System.currentTimeMillis())
        if (preferences.sortByPing.first()) {
            withContext(Dispatchers.Main) {
                pingAllNodes(
                    nodes = result.nodes,
                    selectBestOnComplete = true,
                )
            }
        }
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
        // Как в v2rayNG: если VPN уже запущен, смена сервера сразу переподключает туннель
        if (VpnManager.status.value != VpnStatus.Stopped) {
            switchVpnToNode(nodeId)
        }
    }

    fun toggleFavoriteNode(nodeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            preferences.toggleFavoriteNodeId(nodeId)
        }
    }

    fun toggleSortByPing() {
        val enabling = !uiState.value.sortByPing
        viewModelScope.launch(Dispatchers.IO) {
            preferences.setSortByPing(enabling)
        }
        if (enabling && uiState.value.nodes.isNotEmpty()) {
            pingAllNodes(
                nodes = uiState.value.nodes,
                selectBestOnComplete = true,
            )
        }
    }

    /** Импорт подписки из отсканированного QR-кода (ссылка или конфиг). */
    fun importFromQr(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) {
            error.value = "QR-код пуст"
            return
        }
        AppLog.i("importFromQr len=${trimmed.length} prefix=${trimmed.take(24)}")
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            subscriptionUrlInput.value = trimmed
            refreshConfig(showUrlRequiredError = false)
        } else {
            // vless://, hy2://, base64 или JSON-конфиг
            importSubscriptionPayload(trimmed, connectAfter = false, onEffect = {})
        }
    }

    fun requestConnectToNode(nodeId: String) {
        clearMessages()
        if (!prepareConnect()) return
        if (uiState.value.nodes.none { it.id == nodeId }) return

        viewModelScope.launch { preferences.setSelectedNodeId(nodeId) }
        switchVpnToNode(nodeId)
    }

    /** Подключает (или переподключает уже запущенный VPN) к указанному серверу. */
    private fun switchVpnToNode(nodeId: String) {
        val node = uiState.value.nodes.find { it.id == nodeId } ?: return

        when (VpnManager.status.value) {
            VpnStatus.Stopped -> {
                viewModelScope.launch { _connectRequests.emit(node) }
            }
            VpnStatus.Started -> {
                val connectedId = VpnAutoReconnect.connectedNode()?.id
                if (connectedId == nodeId) return
                AppLog.i("switchVpnToNode reconnect to ${node.name}")
                message.value = "Переподключение к ${node.name}…"
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
        val subscriptionLoad: SubscriptionLoadState,
        val isPinging: Boolean,
        val nodePings: Map<String, PingState>,
        val message: String?,
        val error: String?,
    )

    private data class SettingsUiState(
        val connectionSettings: ConnectionSettingsState,
        val subscriptionAutoUpdateInterval: SubscriptionAutoUpdateInterval,
        val appThemeMode: AppThemeMode,
        val sortByPing: Boolean,
    )
}
