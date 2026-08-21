package ru.coffeemaniavpn.app.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.MainActivity
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.util.AppLocale

class ServiceNotification(private val service: Service) {
    private val notificationId = 1
    private val channelId = "coffemania_vpn"

    private val flags =
        PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE
            } else {
                0
            }

    private val builder by lazy {
        NotificationCompat.Builder(service, channelId)
            .setShowWhen(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_logo_notif)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(
                PendingIntent.getActivity(
                    service,
                    0,
                    Intent(service, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                    flags,
                ),
            )
    }

    private fun loc(id: Int): String = AppLocale.wrap(App.instance).getString(id)

    fun show(serverLine: String, statusLine: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            App.notificationManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    loc(R.string.vpn_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        val title = serverLine.ifBlank { statusLine }
        val text = if (serverLine.isNotBlank()) statusLine else ""
        val style = NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText(text)
        val stopIntent = PendingIntent.getService(
            service,
            1,
            Intent(service, VPNService::class.java).setAction(VpnAction.SERVICE_CLOSE),
            flags,
        )
        builder.clearActions()
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            loc(R.string.vpn_stop),
            stopIntent,
        )
        service.startForeground(
            notificationId,
            builder
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(style)
                .build(),
        )
    }

    fun show(contentText: String) {
        val lines = contentText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        when (lines.size) {
            0 -> show(serverLine = "", statusLine = contentText)
            1 -> show(serverLine = "", statusLine = lines[0])
            else -> show(serverLine = lines[0], statusLine = lines.drop(1).joinToString(" · "))
        }
    }

    fun close() {
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }
}
