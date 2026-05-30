package online.coffemaniavpn.client

import android.Manifest
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import online.coffemaniavpn.client.ktx.hasPermission
import online.coffemaniavpn.client.ui.AppShell
import online.coffemaniavpn.client.ui.CoffemaniaTheme
import online.coffemaniavpn.client.ui.LogsDialog
import online.coffemaniavpn.client.ui.MainViewModel
import online.coffemaniavpn.client.util.AppLog
import online.coffemaniavpn.client.vpn.VpnManager

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var pendingConnect = false

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        AppLog.i("vpn permission result=${result.resultCode}")
        if (result.resultCode == RESULT_OK && pendingConnect) {
            connectSelectedNode()
        }
        pendingConnect = false
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            setContent {
                CoffemaniaTheme {
                    var showLogs by remember { mutableStateOf(false) }
                    val state by viewModel.uiState.collectAsState()

                    AppShell(
                        state = state,
                        onRefreshSubscription = viewModel::refreshSubscription,
                        onSelectNode = viewModel::selectNode,
                        onConnectClick = ::requestConnect,
                        onDisconnectClick = VpnManager::disconnect,
                        onShowLogs = { showLogs = true },
                        onRefreshPing = viewModel::pingAllNodes,
                        onRefreshConfig = viewModel::refreshConfig,
                        onPasteLinkClick = viewModel::pasteSubscriptionFromClipboard,
                    )

                    if (showLogs) {
                        LogsDialog(
                            text = viewModel.readLogs(),
                            onDismiss = { showLogs = false },
                        )
                    }
                }
            }
            reportFullyDrawn()
            AppLog.i("MainActivity setContent ok")
        } catch (t: Throwable) {
            AppLog.e("MainActivity.onCreate failed", t)
            throw t
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppResumed()
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

        AppLog.i("requestConnect node=${node.name} protocol=${node.protocol} host=${node.host}:${node.port}")

        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            pendingConnect = true
            vpnPermissionLauncher.launch(prepareIntent)
        } else {
            VpnManager.connect(node)
        }
    }

    private fun connectSelectedNode() {
        val node = viewModel.selectedNode() ?: return
        VpnManager.connect(node)
    }
}
