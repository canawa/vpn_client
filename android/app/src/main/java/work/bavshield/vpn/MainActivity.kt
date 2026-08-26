package work.bavshield.vpn

import android.Manifest
import android.content.Context
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import work.bavshield.vpn.BuildConfig
import work.bavshield.vpn.data.AppLanguage
import work.bavshield.vpn.data.LocaleHelper
import work.bavshield.vpn.data.ProxyNode
import work.bavshield.vpn.ktx.hasPermission
import work.bavshield.vpn.deeplink.DeepLinkEffect
import work.bavshield.vpn.ui.AppShell
import work.bavshield.vpn.ui.BavShieldTheme
import work.bavshield.vpn.ui.LogsDialog
import work.bavshield.vpn.ui.MainViewModel
import work.bavshield.vpn.util.AppLog
import work.bavshield.vpn.util.LogExporter
import work.bavshield.vpn.vpn.VpnManager
import work.bavshield.vpn.vpn.VpnQuickConnect

class MainActivity : ComponentActivity() {
    companion object {
        const val ACTION_CONNECT_FROM_TILE = "work.bavshield.vpn.action.CONNECT_FROM_TILE"
        const val EXTRA_CONNECT_FROM_TILE = "connect_from_tile"
    }

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
            .onSuccess { toast(getString(R.string.logs_saved)) }
            .onFailure { toast(getString(R.string.logs_save_failed, it.message.orEmpty())) }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(work.bavshield.vpn.data.LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Launchers may deliver a fresh MAIN/LAUNCHER intent while the app is already running,
        // which would spawn a duplicate activity on top of the live one. Hand back to the
        // existing instance instead of starting over.
        if (!isTaskRoot &&
            intent.action == Intent.ACTION_MAIN &&
            intent.hasCategory(Intent.CATEGORY_LAUNCHER)
        ) {
            AppLog.i("MainActivity.onCreate: duplicate launch intent, finishing")
            finish()
            return
        }

        AppLog.i("MainActivity.onCreate")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            setContent {
                val state by viewModel.uiState.collectAsState()
                BavShieldTheme {
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
                        onPasteLinkClick = viewModel::pasteSubscriptionFromClipboard,
                        onBuyOnWebsiteClick = ::openSite,
                        onTelegramBotClick = ::openTelegramBot,
                        onSupportClick = ::openSupport,
                        onPaySubscriptionClick = ::openPaySubscription,
                        onPayDevicesClick = ::openPayDevices,
                        onDeleteSubscriptionClick = viewModel::deleteSubscription,
                        onTelegramChannelClick = ::openTelegramChannel,
                        onCloseApp = { finish() },
                        onSaveConnectionSettings = viewModel::saveConnectionSettings,
                        onSubscriptionAutoUpdateIntervalChange = viewModel::setSubscriptionAutoUpdateInterval,
                        onAppLanguageChange = ::applyLanguage,
                        onPingAutoIntervalChange = viewModel::setPingAutoInterval,
                        onPingTestHostsChange = viewModel::setPingTestHosts,
                        onPingNow = viewModel::pingAllNodes,
                        onEmailClick = ::openSupportEmail,
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
            handleTileConnectIntent(intent)
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
        handleTileConnectIntent(intent)
    }

    private fun handleTileConnectIntent(intent: Intent?) {
        if (intent == null) return
        val fromTile = intent.action == ACTION_CONNECT_FROM_TILE ||
            intent.getBooleanExtra(EXTRA_CONNECT_FROM_TILE, false)
        if (!fromTile) return
        intent.action = null
        intent.removeExtra(EXTRA_CONNECT_FROM_TILE)
        AppLog.i("MainActivity connect from QS tile")
        lifecycleScope.launch {
            val node = VpnQuickConnect.loadSelectedNode(this@MainActivity)
            if (node == null) {
                toast(getString(R.string.error_add_subscription_to_connect))
                return@launch
            }
            launchConnect(node)
        }
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
                    toast(getString(R.string.logs_saved_downloads, fileName))
                    AppLog.i("downloadLogs ok file=$fileName")
                }
                .onFailure {
                    AppLog.e("downloadLogs failed", it)
                    toast(getString(R.string.logs_save_failed, it.message.orEmpty()))
                }
        } else {
            saveLogsLauncher.launch(LogExporter.suggestedFileName())
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        if (LocaleHelper.current(this) == language) return
        LocaleHelper.persist(this, language)
        AppLog.i("MainActivity.applyLanguage ${language.tag}")
        recreate()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            AppLog.e("openUrl failed url=$url", it)
            toast(getString(R.string.error_open_url))
        }
    }

    private fun openTelegramChannel() = openUrl(BuildConfig.TELEGRAM_CHANNEL_URL)

    private fun openTelegramBot() = openUrl(BuildConfig.TELEGRAM_BOT_URL)

    private fun openSite() = openUrl(BuildConfig.SITE_URL)

    private fun openSupport() = openUrl(BuildConfig.SUPPORT_URL)

    private fun openPaySubscription() = openUrl(BuildConfig.PAY_SUBSCRIPTION_URL)

    private fun openPayDevices() = openUrl(BuildConfig.PAY_DEVICES_URL)

    private fun openSupportEmail() {
        runCatching {
            startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}")),
            )
        }.onFailure {
            AppLog.e("openSupportEmail failed", it)
            toast(getString(R.string.error_open_url))
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
