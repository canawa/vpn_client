package ru.coffeemaniavpn.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import ru.coffeemaniavpn.app.util.AppLog

/** Виджет 1×1: только кнопка подключения к последнему выбранному серверу. */
class VpnHomeWidgetButtonProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        AppLog.i("VpnHomeWidgetButton onUpdate ids=${appWidgetIds.size}")
        VpnHomeWidgetUpdater.updateButton(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        AppLog.i("VpnHomeWidgetButton onEnabled")
        VpnHomeWidgetUpdater.updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == VpnHomeWidgetUpdater.ACTION_TOGGLE) {
            VpnHomeWidgetUpdater.handleAction(context, intent)
            return
        }
        super.onReceive(context, intent)
    }
}
