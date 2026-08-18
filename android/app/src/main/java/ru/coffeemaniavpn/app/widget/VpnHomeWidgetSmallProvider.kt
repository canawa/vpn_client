package ru.coffeemaniavpn.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import ru.coffeemaniavpn.app.util.AppLog

/** Компактный виджет: кнопка + имя выбранного сервера. */
class VpnHomeWidgetSmallProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        AppLog.i("VpnHomeWidgetSmall onUpdate ids=${appWidgetIds.size}")
        VpnHomeWidgetUpdater.updateSmall(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        AppLog.i("VpnHomeWidgetSmall onEnabled")
        VpnHomeWidgetUpdater.updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (
            action == VpnHomeWidgetUpdater.ACTION_TOGGLE ||
            action == VpnHomeWidgetUpdater.ACTION_OPEN_APP
        ) {
            VpnHomeWidgetUpdater.handleAction(context, intent)
            return
        }
        super.onReceive(context, intent)
    }
}
