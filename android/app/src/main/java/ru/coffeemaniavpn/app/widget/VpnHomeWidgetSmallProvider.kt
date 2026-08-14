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
        VpnHomeWidgetAnimator.ensureStarted(context)
        VpnHomeWidgetUpdater.updateSmall(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        AppLog.i("VpnHomeWidgetSmall onEnabled")
        VpnHomeWidgetAnimator.ensureStarted(context)
        VpnHomeWidgetUpdater.updateAll(context)
    }

    override fun onDisabled(context: Context) {
        if (!VpnHomeWidgetUpdater.hasWidgets(context)) {
            VpnHomeWidgetAnimator.stop()
        }
        super.onDisabled(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle?,
    ) {
        AppLog.i("VpnHomeWidgetSmall optionsChanged id=$appWidgetId")
        VpnHomeWidgetUpdater.updateSmall(context, intArrayOf(appWidgetId))
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
