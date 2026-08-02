package ru.coffeemaniavpn.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.coffeemaniavpn.app.BuildConfig
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.ktx.hasPermission
import ru.coffeemaniavpn.app.deeplink.DeepLinkEffect
import ru.coffeemaniavpn.app.ui.AppShell
import ru.coffeemaniavpn.app.ui.CoffemaniaTheme
import ru.coffeemaniavpn.app.ui.MainViewModel
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.VpnManager
import ru.coffeemaniavpn.app.vpn.VpnQuickConnect

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingConnectNode: ProxyNode? = null
    private var pendingQuickTileConnect by mutableStateOf(false)

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        AppLog.i("vpn permission result=${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            pendingConnectNode?.let { VpnManager.connect(it) }
        }
        pendingConnectNode = null
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        AppLog.i("notification permission granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppLog.i("MainActivity.onCreate")
        consumeQuickTileIntent(intent)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            setContent {
                val state by viewModel.uiState.collectAsState()
                CoffemaniaTheme {
                    LaunchedEffect(Unit) {
                        viewModel.connectRequests.collect { node ->
                            launchConnect(node)
                        }
                    }

                    LaunchedEffect(pendingQuickTileConnect, state.nodes, state.subscriptionInfo) {
                        if (!pendingQuickTileConnect) return@LaunchedEffect
                        if (state.nodes.isEmpty()) return@LaunchedEffect
                        pendingQuickTileConnect = false
                        requestConnect()
                    }

                    AppShell(
                        state = state,
                        onRefreshSubscription = viewModel::refreshSubscription,
                        onSelectNode = viewModel::selectNode,
                        onConnectToNode = viewModel::requestConnectToNode,
                        onConnectClick = ::requestConnect,
                        onDisconnectClick = VpnManager::disconnect,
                        onRefreshPing = viewModel::pingAllNodes,
                        onRefreshConfig = viewModel::refreshConfig,
                        onPasteLinkClick = viewModel::pasteSubscriptionFromClipboard,
                        onBuyOnWebsiteClick = ::openSubscriptionRegister,
                        onDeleteSubscriptionClick = viewModel::deleteSubscription,
                        onRenewTelegramClick = ::openTelegramBot,
                        onCloseApp = { finish() },
                        onSaveConnectionSettings = viewModel::saveConnectionSettings,
                        onSubscriptionAutoUpdateIntervalChange = viewModel::setSubscriptionAutoUpdateInterval,
                    )
                }
            }
            reportFullyDrawn()
            AppLog.i("MainActivity setContent ok")
            handleDeepLinkIntent(intent)
        } catch (t: Throwable) {
            AppLog.e("MainActivity.onCreate failed", t)
            throw t
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeQuickTileIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun consumeQuickTileIntent(intent: Intent?) {
        if (intent?.action != VpnQuickConnect.ACTION_CONNECT) return
        intent.action = Intent.ACTION_MAIN
        pendingQuickTileConnect = true
        AppLog.i("MainActivity quick tile connect requested")
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        intent.data = null
        viewModel.processDeepLink(uri) { effect ->
            when (effect) {
                DeepLinkEffect.RequestConnect -> requestConnect()
                DeepLinkEffect.FinishActivity -> finish()
                DeepLinkEffect.None -> Unit
            }
        }
    }

    private fun openTelegramBot() {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.TELEGRAM_BOT_URL)))
        }.onFailure {
            AppLog.e("openTelegramBot failed", it)
        }
    }

    private fun openSubscriptionRegister() {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.SUBSCRIPTION_REGISTER_URL)))
        }.onFailure {
            AppLog.e("openSubscriptionRegister failed", it)
        }
    }

    private fun requestConnect() {
        viewModel.clearMessages()
        if (!viewModel.prepareConnect()) {
            AppLog.w("requestConnect: subscription link or nodes missing")
            return
        }
        val node = viewModel.selectedNode()
        if (node == null) {
            AppLog.w("requestConnect: no selected node")
            return
        }
        launchConnect(node)
    }

    private fun launchConnect(node: ProxyNode) {
        AppLog.i("launchConnect node=${node.name} protocol=${node.protocol} host=${node.host}:${node.port}")

        pendingConnectNode = node
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            VpnManager.connect(node)
            pendingConnectNode = null
        }
    }
}
