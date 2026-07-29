package ru.coffeemaniavpn.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.coffeemaniavpn.app.BuildConfig
import ru.coffeemaniavpn.app.data.ProxyNode
import ru.coffeemaniavpn.app.ktx.hasPermission
import ru.coffeemaniavpn.app.deeplink.DeepLinkEffect
import ru.coffeemaniavpn.app.ui.AppShell
import ru.coffeemaniavpn.app.ui.CoffemaniaTheme
import ru.coffeemaniavpn.app.ui.LogsDialog
import ru.coffeemaniavpn.app.ui.MainViewModel
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.util.LogExporter
import ru.coffeemaniavpn.app.vpn.VpnManager

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingConnectNode: ProxyNode? = null

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

    private val saveLogsLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        LogExporter.writeToUri(this, uri)
            .onSuccess { toast("Логи сохранены") }
            .onFailure { toast("Не удалось сохранить: ${it.message}") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppLog.i("MainActivity.onCreate")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            setContent {
                val state by viewModel.uiState.collectAsState()
                CoffemaniaTheme {
                    var showLogs by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        viewModel.connectRequests.collect { node ->
                            launchConnect(node)
                        }
                    }

                    AppShell(
                        state = state,
                        onRefreshSubscription = viewModel::refreshSubscription,
                        onSelectNode = viewModel::selectNode,
                        onConnectToNode = viewModel::requestConnectToNode,
                        onConnectClick = ::requestConnect,
                        onDisconnectClick = VpnManager::disconnect,
                        onShowLogs = { showLogs = true },
                        onDownloadLogs = ::downloadLogs,
                        onRefreshPing = viewModel::pingAllNodes,
                        onRefreshConfig = viewModel::refreshConfig,
                        onPasteLinkClick = viewModel::pasteSubscriptionFromClipboard,
                        onBuyOnWebsiteClick = ::openSubscriptionRegister,
                        onDeleteSubscriptionClick = viewModel::deleteSubscription,
                        onTelegramChannelClick = ::openTelegramChannel,
                        onRenewTelegramClick = ::openTelegramBot,
                        onCloseApp = { finish() },
                        onSaveConnectionSettings = viewModel::saveConnectionSettings,
                        onSubscriptionAutoUpdateIntervalChange = viewModel::setSubscriptionAutoUpdateInterval,
                    )

                    BackHandler(enabled = showLogs) {
                        showLogs = false
                    }

                    if (showLogs) {
                        LogsDialog(
                            text = viewModel.readLogs(),
                            onDismiss = { showLogs = false },
                            onDownloadLogs = ::downloadLogs,
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
        handleDeepLinkIntent(intent)
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

    private fun downloadLogs() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            LogExporter.saveToDownloads(this)
                .onSuccess { fileName ->
                    toast("Сохранено в «Загрузки»: $fileName")
                    AppLog.i("downloadLogs ok file=$fileName")
                }
                .onFailure {
                    AppLog.e("downloadLogs failed", it)
                    toast("Не удалось сохранить: ${it.message}")
                }
        } else {
            saveLogsLauncher.launch(LogExporter.suggestedFileName())
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun openTelegramChannel() {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.TELEGRAM_CHANNEL_URL)))
        }.onFailure {
            AppLog.e("openTelegramChannel failed", it)
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
