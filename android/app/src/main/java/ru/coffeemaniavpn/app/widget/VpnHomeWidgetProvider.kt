package ru.coffeemaniavpn.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import ru.coffeemaniavpn.app.util.AppLog

class VpnHomeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        AppLog.i("VpnHomeWidget onUpdate ids=${appWidgetIds.size}")
        VpnHomeWidgetAnimator.ensureStarted(context)
        VpnHomeWidgetUpdater.update(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        AppLog.i("VpnHomeWidget onEnabled")
        VpnHomeWidgetAnimator.ensureStarted(context)
        VpnHomeWidgetUpdater.updateAll(context)
    }

    override fun onDisabled(context: Context) {
        if (!VpnHomeWidgetUpdater.hasWidgets(context)) {
            VpnHomeWidgetAnimator.stop()
        }
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (
            action == VpnHomeWidgetUpdater.ACTION_TOGGLE ||
            action == VpnHomeWidgetUpdater.ACTION_SELECT ||
            action == VpnHomeWidgetUpdater.ACTION_PREV ||
            action == VpnHomeWidgetUpdater.ACTION_NEXT
        ) {
            VpnHomeWidgetUpdater.handleAction(context, intent)
            return
        }
        super.onReceive(context, intent)
    }
}
