package online.coffemaniavpn.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.coffemaniavpn.client.data.AppPreferences
import online.coffemaniavpn.client.data.PingState
import online.coffemaniavpn.client.data.ProxyNode
import online.coffemaniavpn.client.data.ServerPinger
import online.coffemaniavpn.client.data.SubscriptionInfo
import online.coffemaniavpn.client.data.SubscriptionRepository
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
        combine(
            isLoading,
            isPinging,
            nodePings,
            message,
            error,
        ) { loading, pinging, pings, info, localError ->
            LocalUiState(loading, pinging, pings, info, localError)
        },
        startupCrash,
    ) { savedData, vpnData, localData, crash ->
        val (savedUrl, nodes, selectedNodeId, subscriptionInfo) = savedData
        val (vpnStatus, vpnError, connectionElapsedMs, inputUrl) = vpnData
        val (loading, pinging, pings, info, localError) = localData

        MainUiState(
            subscriptionUrl = inputUrl.ifBlank { savedUrl },
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
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    init {
        AppLog.i("MainViewModel init")
        viewModelScope.launch(Dispatchers.IO) {
            startupCrash.value = AppLog.readLastCrash()
        }
        viewModelScope.launch {
            preferences.subscriptionUrl.collect { saved ->
                if (subscriptionUrlInput.value.isBlank() && saved.isNotBlank()) {
                    subscriptionUrlInput.value = saved
                }
            }
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

    fun pasteSubscriptionFromClipboard() {
        val text = getApplication<Application>().readClipboardText()
        if (text.isNullOrBlank()) {
            error.value = "Буфер обмена пуст"
            return
        }
        subscriptionUrlInput.value = text
        message.value = "Ссылка вставлена"
        AppLog.i("pasteSubscriptionFromClipboard urlLen=${text.length}")
    }

    fun refreshSubscription() {
        refreshConfig(showUrlRequiredError = true)
    }

    fun refreshConfig(showUrlRequiredError: Boolean = false) {
        val url = uiState.value.subscriptionUrl.trim()
            .ifBlank { subscriptionUrlInput.value.trim() }
        if (url.isBlank()) {
            if (showUrlRequiredError) {
                error.value = "Введите URL подписки"
            }
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            message.value = null
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
            } catch (e: Exception) {
                AppLog.e("refreshConfig failed", e)
                error.value = e.message ?: "Не удалось обновить конфиг"
            } finally {
                isLoading.value = false
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
