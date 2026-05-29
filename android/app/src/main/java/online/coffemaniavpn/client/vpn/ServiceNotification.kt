package online.coffemaniavpn.client.vpn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import online.coffemaniavpn.client.App
import online.coffemaniavpn.client.MainActivity
import online.coffemaniavpn.client.R

class ServiceNotification(private val service: Service) {
    private val notificationId = 1
    private val channelId = "coffemania_vpn"

    private val flags =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }

    private val builder by lazy {
        NotificationCompat.Builder(service, channelId)
            .setShowWhen(false)
            .setOngoing(true)
            .setContentTitle(service.getString(R.string.vpn_notification_title))
            .setOnlyAlertOnce(true)
            .setSmallIcon(R.drawable.ic_coffee_bean)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(
                PendingIntent.getActivity(
                    service,
                    0,
                    Intent(service, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                    flags,
                ),
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    service.getString(R.string.vpn_stop),
                    PendingIntent.getBroadcast(
                        service,
                        0,
                        Intent(VpnAction.SERVICE_CLOSE).setPackage(service.packageName),
                        flags,
                    ),
                ).build(),
            )
    }

    fun show(contentText: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            App.notificationManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    service.getString(R.string.vpn_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
        service.startForeground(notificationId, builder.setContentText(contentText).build())
    }

    fun close() {
        ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }
}
