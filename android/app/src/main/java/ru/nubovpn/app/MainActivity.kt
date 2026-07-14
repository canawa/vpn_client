package ru.nubovpn.app

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
import ru.nubovpn.app.BuildConfig
import ru.nubovpn.app.data.ProxyNode
import ru.nubovpn.app.ktx.hasPermission
import ru.nubovpn.app.deeplink.DeepLinkEffect
import ru.nubovpn.app.ui.AppShell
import ru.nubovpn.app.ui.LogsDialog
import ru.nubovpn.app.ui.NuboTheme
import ru.nubovpn.app.ui.MainViewModel
import ru.nubovpn.app.ui.qr.QrScanActivity
import ru.nubovpn.app.util.AppLog
import ru.nubovpn.app.util.LogExporter
import ru.nubovpn.app.vpn.VpnManager

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

    private val qrScanLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val content = result.data?.getStringExtra(QrScanActivity.EXTRA_RESULT)
        if (content.isNullOrBlank()) {
            AppLog.i("qr scan cancelled")
            return@registerForActivityResult
        }
        viewModel.importFromQr(content)
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
                NuboTheme {
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
                        onConnectClick = ::requestConnect,
                        onDisconnectClick = VpnManager::disconnect,
                        onShowLogs = { showLogs = true },
                        onDownloadLogs = ::downloadLogs,
                        onRefreshPing = viewModel::pingAllNodes,
                        onRefreshConfig = viewModel::refreshConfig,
                        onPasteLinkClick = viewModel::pasteSubscriptionFromClipboard,
                        onScanQrClick = ::scanQrCode,
                        onToggleSortByPing = viewModel::toggleSortByPing,
                        onDeleteSubscriptionClick = viewModel::deleteSubscription,
                        onTelegramChannelClick = ::openTelegramChannel,
                        onTelegramBotClick = ::openTelegramBot,
                        onSubInfoButtonClick = ::openUrl,
                        onOpenSiteClick = { openUrl(BuildConfig.SUBSCRIPTION_STORE_URL) },
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

    private fun scanQrCode() {
        runCatching {
            qrScanLauncher.launch(Intent(this, QrScanActivity::class.java))
        }.onFailure {
            AppLog.e("scanQrCode failed", it)
            toast("Не удалось открыть сканер QR-кода")
        }
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

    private fun openUrl(url: String) {
        runCatching {
            val normalized = url.trim().let { raw ->
                if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
                    raw
                } else {
                    "https://$raw"
                }
            }
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(normalized)))
        }.onFailure {
            AppLog.e("openUrl failed url=$url", it)
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
