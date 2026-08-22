package ru.coffeemaniavpn.app.vpn

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.MainActivity
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.ktx.hasPermission
import ru.coffeemaniavpn.app.util.AppLog

/**
 * Уведомление при пропаже физической сети (Wi‑Fi / LTE):
 * — шторка (system notification);
 * — всплывающий тост в приложении ([lostEvents]).
 * Не срабатывает при переключении Wi‑Fi ↔ LTE.
 */
internal object NetworkLossNotifier {
    private const val DEBOUNCE_MS = 2_000L
    private const val NOTIFICATION_ID = 1002
    private const val CHANNEL_ID = "clevvpn_network"

    private var started = false
    private var hadPhysicalNetwork = false
    private var notified = false
    private var debounceJob: Job? = null
    private val physicalNetworks = mutableSetOf<Network>()

    private val _lostEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    /** Событие для in-app тоста (шторка живёт отдельно). */
    val lostEvents: SharedFlow<Unit> = _lostEvents.asSharedFlow()

    private val _restoredEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val restoredEvents: SharedFlow<Unit> = _restoredEvents.asSharedFlow()

    fun start() {
        if (started) return
        started = true
        runCatching {
            seedPhysicalNetworks()
            hadPhysicalNetwork = physicalNetworks.isNotEmpty()
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()
            App.connectivity.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        App.applicationScope.launch {
                            physicalNetworks.add(network)
                            onConnectivityChanged()
                        }
                    }

                    override fun onLost(network: Network) {
                        App.applicationScope.launch {
                            physicalNetworks.remove(network)
                            onConnectivityChanged()
                        }
                    }
                },
            )
            AppLog.i("NetworkLossNotifier started hadPhysical=$hadPhysicalNetwork")
        }.onFailure {
            started = false
            AppLog.e("NetworkLossNotifier start failed", it)
        }
    }

    private fun onConnectivityChanged() {
        val online = physicalNetworks.isNotEmpty()
        if (online) {
            debounceJob?.cancel()
            debounceJob = null
            hadPhysicalNetwork = true
            if (notified) {
                cancelNotification()
                _restoredEvents.tryEmit(Unit)
                notified = false
            }
            return
        }
        if (!hadPhysicalNetwork || notified) return
        debounceJob?.cancel()
        debounceJob = App.applicationScope.launch {
            delay(DEBOUNCE_MS)
            if (physicalNetworks.isNotEmpty()) return@launch
            if (!hadPhysicalNetwork || notified) return@launch
            hadPhysicalNetwork = false
            notified = true
            _lostEvents.tryEmit(Unit)
            showSystemNotification()
        }
    }

    private fun seedPhysicalNetworks() {
        physicalNetworks.clear()
        App.connectivity.allNetworks.forEach { network ->
            val caps = App.connectivity.getNetworkCapabilities(network) ?: return@forEach
            if (isPhysicalInternet(caps)) physicalNetworks.add(network)
        }
    }

    private fun isPhysicalInternet(caps: NetworkCapabilities): Boolean {
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return false
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    private fun showSystemNotification() {
        val context = App.instance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !context.hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            AppLog.w("NetworkLossNotifier system skip: no POST_NOTIFICATIONS (in-app toast still fired)")
            return
        }
        ensureChannel()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }
        val text = context.getString(R.string.network_lost_notification)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_notif)
            .setContentTitle(context.getString(R.string.vpn_notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    NOTIFICATION_ID,
                    Intent(context, MainActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                    flags,
                ),
            )
            .build()
        App.notificationManager.notify(NOTIFICATION_ID, notification)
        AppLog.i("NetworkLossNotifier system shown")
    }

    private fun cancelNotification() {
        App.notificationManager.cancel(NOTIFICATION_ID)
        AppLog.i("NetworkLossNotifier cancelled")
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        App.notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                App.instance.getString(R.string.network_lost_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}
