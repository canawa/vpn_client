package online.coffemaniavpn.client.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import online.coffemaniavpn.client.data.AppPreferences
import online.coffemaniavpn.client.data.ProxyNode
import online.coffemaniavpn.client.data.SubscriptionRepository
import online.coffemaniavpn.client.util.AppLog
import online.coffemaniavpn.client.vpn.VpnManager
import online.coffemaniavpn.client.vpn.VpnStatus

data class MainUiState(
    val subscriptionUrl: String = "",
    val nodes: List<ProxyNode> = emptyList(),
    val selectedNodeId: String? = null,
    val vpnStatus: VpnStatus = VpnStatus.Stopped,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val startupCrash: String? = null,
    val logsPreview: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = AppPreferences(application)
    private val repository = SubscriptionRepository()

    private val subscriptionUrlInput = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)
    private val startupCrash = MutableStateFlow(AppLog.readLastCrash())

    val uiState: StateFlow<MainUiState> = combine(
        combine(
            preferences.subscriptionUrl,
            preferences.nodes.catch { e ->
                AppLog.e("nodes flow failed, clearing saved nodes", e)
                viewModelScope.launch { preferences.clearNodes() }
                emit(emptyList())
            },
            preferences.selectedNodeId,
        ) { savedUrl, nodes, selectedNodeId ->
            AppLog.i("prefs loaded urlLen=${savedUrl.length} nodes=${nodes.size}")
            Triple(savedUrl, nodes, selectedNodeId)
        },
        combine(
            VpnManager.status,
            VpnManager.lastError,
            subscriptionUrlInput,
        ) { vpnStatus, vpnError, inputUrl ->
            Triple(vpnStatus, vpnError, inputUrl)
        },
        combine(isLoading, message, error, startupCrash) { loading, info, localError, crash ->
            Quad(loading, info, localError, crash)
        },
    ) { savedData, vpnData, localData ->
        val (savedUrl, nodes, selectedNodeId) = savedData
        val (vpnStatus, vpnError, inputUrl) = vpnData
        val (loading, info, localError, crash) = localData

        MainUiState(
            subscriptionUrl = inputUrl.ifBlank { savedUrl },
            nodes = nodes,
            selectedNodeId = selectedNodeId ?: nodes.firstOrNull()?.id,
            vpnStatus = vpnStatus,
            isLoading = loading,
            message = info,
            error = localError ?: vpnError,
            startupCrash = crash,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(startupCrash = AppLog.readLastCrash()),
    )

    init {
        AppLog.i("MainViewModel init")
        viewModelScope.launch {
            preferences.subscriptionUrl.collect { saved ->
                if (subscriptionUrlInput.value.isBlank() && saved.isNotBlank()) {
                    subscriptionUrlInput.value = saved
                }
            }
        }
    }

    fun onSubscriptionUrlChange(value: String) {
        subscriptionUrlInput.value = value
    }

    fun refreshSubscription() {
        val url = subscriptionUrlInput.value.trim()
        if (url.isBlank()) {
            error.value = "Введите URL подписки"
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            message.value = null
            try {
                AppLog.i("refreshSubscription start")
                val nodes = withContext(Dispatchers.IO) {
                    repository.fetchNodes(url)
                }
                val selected = uiState.value.selectedNodeId?.takeIf { id ->
                    nodes.any { it.id == id }
                } ?: nodes.first().id
                preferences.saveSubscription(url, nodes, selected)
                message.value = "Загружено серверов: ${nodes.size}"
                AppLog.i("refreshSubscription ok, nodes=${nodes.size}")
            } catch (e: Exception) {
                AppLog.e("refreshSubscription failed", e)
                error.value = e.message ?: "Не удалось загрузить подписку"
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

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )
}
