package ru.coffeemaniavpn.app

import android.content.Context
import android.Manifest
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import ru.coffeemaniavpn.app.data.AppLanguage
import ru.coffeemaniavpn.app.BuildConfig
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.ktx.hasPermission
import ru.coffeemaniavpn.app.deeplink.DeepLinkEffect
import ru.coffeemaniavpn.app.ui.AppShell
import ru.coffeemaniavpn.app.ui.CoffemaniaTheme
import ru.coffeemaniavpn.app.ui.MainViewModel
import ru.coffeemaniavpn.app.util.AppLocale
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.util.LogExporter
import ru.coffeemaniavpn.app.vpn.VpnDiagnostics
import ru.coffeemaniavpn.app.vpn.VpnManager
import ru.coffeemaniavpn.app.vpn.VpnQuickConnect

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingConnectNode: ProxyNode? = null
    private var pendingQuickTileConnect by mutableStateOf(false)

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        AppLog.i("vpn permission result=${result.resultCode}")
        if (result.resultCode == RESULT_OK) {
            pendingConnectNode?.let { VpnManager.connect(it) }
        } else {
            AppLog.w("vpn permission denied by user")
            VpnManager.setError(getString(R.string.msg_vpn_permission_denied))
        }
        pendingConnectNode = null
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        AppLog.i("notification permission granted=$granted")
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
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
                val configuration = LocalConfiguration.current
                key(configuration.locales.toLanguageTags()) {
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
                            onToggleFavorite = viewModel::toggleFavorite,
                            onConnectClick = ::requestConnect,
                            onDisconnectClick = VpnManager::disconnect,
                            onRefreshPing = viewModel::pingAllNodes,
                            onRefreshConfig = viewModel::refreshConfig,
                            onPasteLinkClick = viewModel::pasteSubscriptionFromClipboard,
                            onDeleteSubscriptionClick = viewModel::deleteSubscription,
                            onSaveConnectionSettings = viewModel::saveConnectionSettings,
                            onUpdateConnectionSettings = viewModel::updateConnectionSettings,
                            onAddCustomRule = viewModel::addCustomRule,
                            onRemoveCustomRule = viewModel::removeCustomRule,
                            onSubscriptionAutoUpdateIntervalChange = viewModel::setSubscriptionAutoUpdateInterval,
                            onTrafficRoutingModeChange = viewModel::setTrafficRoutingMode,
                            onLanguageChange = ::changeLanguage,
                            onExportLogs = ::shareLogs,
                        )
                    }
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

    private fun changeLanguage(language: AppLanguage) {
        AppLog.i("changeLanguage requested=$language current=${AppLocale.current}")
        AppLocale.apply(language)
        viewModel.persistAppLanguage(language)
        recreate()
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

    private fun shareLogs() {
        VpnDiagnostics.snapshot("user-export")
        AppLog.i("shareLogs requested")
        startActivity(
            Intent.createChooser(
                LogExporter.createShareIntent(this),
                getString(R.string.clev_export_logs_chooser),
            ).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }
}
