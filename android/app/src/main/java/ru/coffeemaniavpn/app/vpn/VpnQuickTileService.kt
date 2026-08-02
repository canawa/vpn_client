package ru.coffeemaniavpn.app.vpn

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.util.AppLog

class VpnQuickTileService : TileService() {
    private var listenScope: CoroutineScope? = null
    private var listenJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        listenScope?.cancel()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        listenScope = scope
        listenJob = scope.launch {
            VpnManager.status.collect { status ->
                applyTile(status)
            }
        }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
        listenScope?.cancel()
        listenScope = null
        super.onStopListening()
    }

    override fun onClick() {
        AppLog.i("VpnQuickTileService.onClick status=${VpnManager.status.value}")
        val action = Runnable {
            VpnQuickConnect.toggleFromTile(this)
            applyTile(VpnManager.status.value)
        }
        if (isLocked) {
            unlockAndRun(action)
        } else {
            action.run()
        }
    }

    private fun applyTile(status: VpnStatus) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_vpn)
        tile.label = getString(R.string.qs_vpn_tile_label)

        when (status) {
            VpnStatus.Started -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitleOrContent(getString(R.string.qs_vpn_connected))
            }
            VpnStatus.Starting -> {
                tile.state = Tile.STATE_ACTIVE
                tile.subtitleOrContent(getString(R.string.vpn_starting))
            }
            VpnStatus.Stopping -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitleOrContent(getString(R.string.qs_vpn_disconnecting))
            }
            VpnStatus.Stopped -> {
                tile.state = Tile.STATE_INACTIVE
                tile.subtitleOrContent(getString(R.string.qs_vpn_disconnected))
            }
        }
        tile.updateTile()
    }

    private fun Tile.subtitleOrContent(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            subtitle = text
        } else {
            contentDescription = text
        }
    }
}
