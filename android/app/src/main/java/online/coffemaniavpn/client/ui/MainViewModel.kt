package online.coffemaniavpn.client.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.coffemaniavpn.client.data.AppPreferences
import online.coffemaniavpn.client.data.PingState
import online.coffemaniavpn.client.data.ProxyNode
import online.coffemaniavpn.client.data.ServerPinger
import online.coffemaniavpn.client.data.SubscriptionInfo
import online.coffemaniavpn.client.data.SubscriptionParser
import online.coffemaniavpn.client.data.SubscriptionRepository
import org.json.JSONObject
import online.coffemaniavpn.client.deeplink.DeepLinkAction
import online.coffemaniavpn.client.deeplink.DeepLinkEffect
import online.coffemaniavpn.client.deeplink.DeepLinkParser
import online.coffemaniavpn.client.ktx.readClipboardText
import online.coffemaniavpn.client.util.AppLog
import online.coffemaniavpn.client.vpn.VpnManager
import online.coffemaniavpn.client.vpn.VpnStatus

data class MainUiState(
    val subscriptionUrl: String = "",
    val nodes: List<ProxyNode> = emptyList(),
    val selectedNodeId: String? = null,
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
            subscriptionUrlInput,
        ) { vpnStatus, vpnError, elapsedMs, inputUrl ->
            VpnUiState(vpnStatus, vpnError, elapsedMs, inputUrl)
        },
        combine(isLoading, isPinging, nodePings, message, error) { loading, pinging, pings, info, localError ->
            LocalUiState(loading, pinging, pings, info, localError)
        },
        startupCrash,
    ) { savedData, vpnData, localData, crash ->
        val (savedUrl, nodes, selectedNodeId, subscriptionInfo) = savedData
        val (vpnStatus, vpnError, connectionElapsedMs, inputUrl) = vpnData
        val (loading, pinging, pings, info, localError) = localData

        MainUiState(
            subscriptionUrl = inputUrl.trim().ifBlank { savedUrl.trim() },
            nodes = nodes,
            selectedNodeId = selectedNodeId ?: nodes.firstOrNull()?.id,
            vpnStatus = vpnStatus,
            connectionElapsedMs = connectionElapsedMs,
            isLoading = loading,
            isPinging = pinging,
            nodePings = pings,
            subscriptionInfo = subscriptionInfo,
            message = info,
            error = localError ?: vpnError,
            startupCrash = crash,
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
    }

    fun onAppResumed() {
        viewModelScope.launch {
            restoreSubscriptionSession()
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
                message.value = "КОФЕМАНИЯ ВПН"
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
            var success = false
            try {
                AppLog.i("refreshConfig start")
                val result = withContext(Dispatchers.IO) {
                    repository.fetchSubscription(url)
                }
                val selected = uiState.value.selectedNodeId?.takeIf { id ->
                    result.nodes.any { it.id == id }
                } ?: result.nodes.first().id
                preferences.saveSubscription(url, result.nodes, selected, result.info)
                message.value = "Конфиг обновлён: ${result.nodes.size} серверов"
                AppLog.i("refreshConfig ok, nodes=${result.nodes.size} info=${result.info}")
                success = true
            } catch (e: Exception) {
                AppLog.e("refreshConfig failed", e)
                error.value = e.message ?: "Не удалось обновить конфиг"
            } finally {
                isLoading.value = false
                onComplete?.invoke(success)
            }
        }
    }

    fun selectNode(nodeId: String) {
        viewModelScope.launch {
            preferences.setSelectedNodeId(nodeId)
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
    )

    private data class LocalUiState(
        val isLoading: Boolean,
        val isPinging: Boolean,
        val nodePings: Map<String, PingState>,
        val message: String?,
        val error: String?,
    )
}
