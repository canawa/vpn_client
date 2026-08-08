package ru.coffeemaniavpn.app.vpn

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.MainActivity
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.util.AppLog

class VpnTileService : TileService() {
    private var statusJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        refreshTile()
        statusJob?.cancel()
        statusJob = App.applicationScope.launch {
            VpnManager.status.collectLatest {
                refreshTile()
            }
        }
    }

    override fun onStopListening() {
        statusJob?.cancel()
        statusJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        unlockAndRun {
            App.applicationScope.launch {
                when (val result = VpnQuickConnect.resolveToggle(this@VpnTileService)) {
                    is VpnQuickConnectResult.Connect -> {
                        AppLog.i("VpnTileService connect node=${result.node.name}")
                        VpnManager.connect(result.node)
                        refreshTile()
                    }
                    VpnQuickConnectResult.Disconnect -> {
                        AppLog.i("VpnTileService disconnect")
                        VpnManager.disconnect(userInitiated = true)
                        refreshTile()
                    }
                    VpnQuickConnectResult.NeedsVpnPermission,
                    VpnQuickConnectResult.OpenApp,
                    -> openAppForConnect()
                }
            }
        }
    }

    private fun refreshTile() {
        val tile = qsTile ?: return
        val status = VpnManager.status.value
        tile.label = getString(R.string.qs_tile_label)
        tile.state = when (status) {
            VpnStatus.Started, VpnStatus.Starting -> Tile.STATE_ACTIVE
            VpnStatus.Stopping, VpnStatus.Stopped -> Tile.STATE_INACTIVE
        }
        tile.contentDescription = when (status) {
            VpnStatus.Started -> getString(R.string.qs_tile_connected)
            VpnStatus.Starting -> getString(R.string.vpn_starting)
            VpnStatus.Stopping -> getString(R.string.vpn_stop)
            VpnStatus.Stopped -> getString(R.string.qs_tile_disconnected)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = when (status) {
                VpnStatus.Started -> getString(R.string.qs_tile_connected)
                VpnStatus.Starting -> getString(R.string.vpn_starting)
                else -> getString(R.string.qs_tile_disconnected)
            }
        }
        tile.updateTile()
    }

    private fun openAppForConnect() {
        val intent = Intent(this, MainActivity::class.java).apply {
            action = MainActivity.ACTION_CONNECT_FROM_TILE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_CONNECT_FROM_TILE, true)
        }
        val pending = PendingIntent.getActivity(
            this,
            REQUEST_CONNECT,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        private const val REQUEST_CONNECT = 42

        fun requestUpdate(context: Context) {
            runCatching {
                requestListeningState(
                    context.applicationContext,
                    ComponentName(context.applicationContext, VpnTileService::class.java),
                )
            }.onFailure {
                AppLog.w("VpnTileService.requestUpdate failed: ${it.message}")
            }
        }
    }
}
