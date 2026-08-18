package ru.coffeemaniavpn.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.coffeemaniavpn.app.App
import ru.coffeemaniavpn.app.MainActivity
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.AppPreferences
import ru.coffeemaniavpn.app.ui.ServerDisplayMapper
import ru.coffeemaniavpn.app.util.AppLog
import ru.coffeemaniavpn.app.vpn.VpnManager
import ru.coffeemaniavpn.app.vpn.VpnQuickConnect

object VpnHomeWidgetUpdater {
    const val ACTION_TOGGLE = "ru.coffeemaniavpn.app.widget.ACTION_TOGGLE"
    const val ACTION_SELECT = "ru.coffeemaniavpn.app.widget.ACTION_SELECT"
    const val ACTION_PREV = "ru.coffeemaniavpn.app.widget.ACTION_PREV"
    const val ACTION_NEXT = "ru.coffeemaniavpn.app.widget.ACTION_NEXT"
    const val ACTION_OPEN_APP = "ru.coffeemaniavpn.app.widget.ACTION_OPEN_APP"
    const val EXTRA_SLOT = "slot"

    private const val PREFS = "vpn_home_widget"
    private const val KEY_PAGE = "page"
    private const val PAGE_SIZE = 4

    private val rowIds = intArrayOf(
        R.id.widget_row_0,
        R.id.widget_row_1,
        R.id.widget_row_2,
        R.id.widget_row_3,
    )
    private val rowTextIds = intArrayOf(
        R.id.widget_row_0_text,
        R.id.widget_row_1_text,
        R.id.widget_row_2_text,
        R.id.widget_row_3_text,
    )
    private val rowFlagIds = intArrayOf(
        R.id.widget_row_0_flag,
        R.id.widget_row_1_flag,
        R.id.widget_row_2_flag,
        R.id.widget_row_3_flag,
    )

    private val colorText = Color.parseColor("#F2F2F5")
    private val colorMuted = Color.parseColor("#6B7672")
    private val colorYellow = Color.parseColor("#FFC400")

    fun updateAll(context: Context) {
        App.applicationScope.launch(Dispatchers.IO) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val largeIds = manager.getAppWidgetIds(
                ComponentName(appContext, VpnHomeWidgetProvider::class.java),
            )
            val smallIds = manager.getAppWidgetIds(
                ComponentName(appContext, VpnHomeWidgetSmallProvider::class.java),
            )
            val buttonIds = manager.getAppWidgetIds(
                ComponentName(appContext, VpnHomeWidgetButtonProvider::class.java),
            )
            if (largeIds.isEmpty() && smallIds.isEmpty() && buttonIds.isEmpty()) return@launch
            val largeViews = if (largeIds.isNotEmpty()) buildLargeViews(appContext) else null
            val smallViews = if (smallIds.isNotEmpty()) buildSmallViews(appContext) else null
            val buttonViews = if (buttonIds.isNotEmpty()) buildButtonViews(appContext) else null
            withContext(Dispatchers.Main) {
                largeViews?.let { views ->
                    largeIds.forEach { id -> manager.updateAppWidget(id, views) }
                }
                smallViews?.let { views ->
                    smallIds.forEach { id -> manager.updateAppWidget(id, views) }
                }
                buttonViews?.let { views ->
                    buttonIds.forEach { id -> manager.updateAppWidget(id, views) }
                }
            }
        }
    }

    fun update(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        App.applicationScope.launch(Dispatchers.IO) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val views = buildLargeViews(appContext)
            withContext(Dispatchers.Main) {
                appWidgetIds.forEach { id -> manager.updateAppWidget(id, views) }
            }
        }
    }

    fun updateSmall(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        App.applicationScope.launch(Dispatchers.IO) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val views = buildSmallViews(appContext)
            withContext(Dispatchers.Main) {
                appWidgetIds.forEach { id -> manager.updateAppWidget(id, views) }
            }
        }
    }

    fun updateButton(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        App.applicationScope.launch(Dispatchers.IO) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val views = buildButtonViews(appContext)
            withContext(Dispatchers.Main) {
                appWidgetIds.forEach { id -> manager.updateAppWidget(id, views) }
            }
        }
    }

    fun handleAction(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE -> {
                AppLog.i("VpnHomeWidget ACTION_TOGGLE")
                VpnQuickConnect.toggleFromTile(context)
                updateAll(context)
            }
            ACTION_OPEN_APP -> {
                context.startActivity(
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                )
            }
            ACTION_SELECT -> {
                val slot = intent.getIntExtra(EXTRA_SLOT, -1)
                if (slot !in 0 until PAGE_SIZE) return
                App.applicationScope.launch(Dispatchers.IO) {
                    selectSlot(context, slot)
                    updateAll(context)
                }
            }
            ACTION_PREV -> {
                App.applicationScope.launch(Dispatchers.IO) {
                    shiftPage(context, -1)
                    updateAll(context)
                }
            }
            ACTION_NEXT -> {
                App.applicationScope.launch(Dispatchers.IO) {
                    shiftPage(context, 1)
                    updateAll(context)
                }
            }
        }
    }

    private suspend fun selectSlot(context: Context, slot: Int) {
        val preferences = AppPreferences(context.applicationContext)
        val nodes = preferences.nodes.first()
        if (nodes.isEmpty()) return
        val page = currentPage(context, nodes.size)
        val index = page * PAGE_SIZE + slot
        val node = nodes.getOrNull(index) ?: return
        AppLog.i("VpnHomeWidget select node=${node.name}")
        preferences.setSelectedNodeId(node.id)
    }

    private suspend fun shiftPage(context: Context, delta: Int) {
        val preferences = AppPreferences(context.applicationContext)
        val nodes = preferences.nodes.first()
        val pageCount = pageCount(nodes.size)
        if (pageCount <= 1) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getInt(KEY_PAGE, 0).coerceIn(0, pageCount - 1)
        val next = (current + delta + pageCount) % pageCount
        prefs.edit().putInt(KEY_PAGE, next).apply()
        AppLog.i("VpnHomeWidget page $current -> $next")
    }

    private suspend fun buildButtonViews(context: Context): RemoteViews {
        val elapsedMs = VpnManager.connectionElapsedMs.value
        val views = RemoteViews(context.packageName, R.layout.widget_vpn_button)
        val toggle = broadcastPendingIntent(
            context,
            ACTION_TOGGLE,
            requestCode = 120,
            receiver = VpnHomeWidgetButtonProvider::class.java,
        )
        views.setOnClickPendingIntent(R.id.widget_connect, toggle)
        views.setOnClickPendingIntent(R.id.widget_button_root, toggle)
        views.setImageViewBitmap(
            R.id.widget_connect,
            WidgetConnectButtonRenderer.render(
                context = context,
                connectionElapsedMs = elapsedMs,
                maxTotalDp = WidgetConnectButtonRenderer.BUTTON_TOTAL_DP,
            ),
        )
        return views
    }

    private suspend fun buildSmallViews(context: Context): RemoteViews {
        val preferences = AppPreferences(context)
        val nodes = preferences.nodes.first()
        val selectedId = preferences.selectedNodeId.first()
        val elapsedMs = VpnManager.connectionElapsedMs.value
        val node = nodes.find { it.id == selectedId } ?: nodes.firstOrNull()

        val views = RemoteViews(context.packageName, R.layout.widget_vpn_small)
        views.setOnClickPendingIntent(
            R.id.widget_connect,
            broadcastPendingIntent(
                context,
                ACTION_TOGGLE,
                requestCode = 110,
                receiver = VpnHomeWidgetSmallProvider::class.java,
            ),
        )
        views.setOnClickPendingIntent(
            R.id.widget_small_server,
            openAppPendingIntent(context),
        )
        views.setOnClickPendingIntent(
            R.id.widget_small_logo,
            openAppPendingIntent(context),
        )
        views.setOnClickPendingIntent(
            R.id.widget_small_root,
            openAppPendingIntent(context),
        )
        views.setImageViewBitmap(
            R.id.widget_connect,
            WidgetConnectButtonRenderer.render(
                context = context,
                connectionElapsedMs = elapsedMs,
                plateDp = WidgetConnectButtonRenderer.PLATE_DP_SMALL,
            ),
        )
        views.setImageViewBitmap(
            R.id.widget_small_logo_text,
            WidgetBrandLogoTextRenderer.render(context, textSizeSp = 12f),
        )

        if (node == null) {
            views.setViewVisibility(R.id.widget_small_flag, View.GONE)
            views.setTextViewText(R.id.widget_small_name, context.getString(R.string.widget_empty))
            views.setTextColor(R.id.widget_small_name, colorMuted)
        } else {
            val display = ServerDisplayMapper.map(node)
            views.setViewVisibility(R.id.widget_small_flag, View.VISIBLE)
            views.setImageViewBitmap(
                R.id.widget_small_flag,
                WidgetFlagBitmaps.get(context, display.flag),
            )
            views.setTextViewText(
                R.id.widget_small_name,
                display.title.ifBlank { "Сервер" },
            )
            views.setTextColor(R.id.widget_small_name, colorText)
        }
        return views
    }

    private suspend fun buildLargeViews(context: Context): RemoteViews {
        val preferences = AppPreferences(context)
        val nodes = preferences.nodes.first()
        val selectedId = preferences.selectedNodeId.first()
        val page = currentPage(context, nodes.size)
        val elapsedMs = VpnManager.connectionElapsedMs.value

        val views = RemoteViews(context.packageName, R.layout.widget_vpn_large)
        views.setImageViewBitmap(
            R.id.widget_logo_text,
            WidgetBrandLogoTextRenderer.render(context, textSizeSp = 16f),
        )

        views.setOnClickPendingIntent(
            R.id.widget_connect,
            broadcastPendingIntent(
                context,
                ACTION_TOGGLE,
                requestCode = 100,
                receiver = VpnHomeWidgetProvider::class.java,
            ),
        )
        views.setOnClickPendingIntent(
            R.id.widget_logo,
            openAppPendingIntent(context),
        )
        views.setOnClickPendingIntent(
            R.id.widget_prev,
            broadcastPendingIntent(
                context,
                ACTION_PREV,
                requestCode = 101,
                receiver = VpnHomeWidgetProvider::class.java,
            ),
        )
        views.setOnClickPendingIntent(
            R.id.widget_next,
            broadcastPendingIntent(
                context,
                ACTION_NEXT,
                requestCode = 102,
                receiver = VpnHomeWidgetProvider::class.java,
            ),
        )
        views.setOnClickPendingIntent(
            R.id.widget_root,
            openAppPendingIntent(context),
        )

        views.setImageViewBitmap(
            R.id.widget_connect,
            WidgetConnectButtonRenderer.render(
                context = context,
                connectionElapsedMs = elapsedMs,
            ),
        )

        val pageCount = pageCount(nodes.size)
        val canPage = pageCount > 1
        views.setViewVisibility(R.id.widget_prev, if (canPage) View.VISIBLE else View.INVISIBLE)
        views.setViewVisibility(R.id.widget_next, if (canPage) View.VISIBLE else View.INVISIBLE)

        for (slot in 0 until PAGE_SIZE) {
            val index = page * PAGE_SIZE + slot
            val node = nodes.getOrNull(index)
            val rowId = rowIds[slot]
            val textId = rowTextIds[slot]
            if (node == null) {
                views.setViewVisibility(rowId, View.INVISIBLE)
                continue
            }
            views.setViewVisibility(rowId, View.VISIBLE)
            val display = ServerDisplayMapper.map(node)
            val label = formatRowLabel(display.title, display.protocolLabel)
            views.setTextViewText(textId, label)
            views.setImageViewBitmap(
                rowFlagIds[slot],
                WidgetFlagBitmaps.get(context, display.flag),
            )
            views.setViewVisibility(rowFlagIds[slot], View.VISIBLE)
            val selected = node.id == selectedId ||
                (selectedId.isNullOrBlank() && index == 0 && nodes.isNotEmpty())
            views.setInt(
                rowId,
                "setBackgroundResource",
                if (selected) R.drawable.widget_row_selected else R.drawable.widget_row,
            )
            views.setTextColor(textId, if (selected) colorYellow else colorText)
            views.setOnClickPendingIntent(
                rowId,
                broadcastPendingIntent(
                    context,
                    ACTION_SELECT,
                    requestCode = 200 + slot,
                    receiver = VpnHomeWidgetProvider::class.java,
                    extras = { putExtra(EXTRA_SLOT, slot) },
                ),
            )
        }

        if (nodes.isEmpty()) {
            views.setViewVisibility(R.id.widget_row_0, View.VISIBLE)
            views.setViewVisibility(R.id.widget_row_0_flag, View.GONE)
            views.setTextViewText(
                R.id.widget_row_0_text,
                context.getString(R.string.widget_empty),
            )
            views.setTextColor(R.id.widget_row_0_text, colorMuted)
            views.setInt(R.id.widget_row_0, "setBackgroundResource", R.drawable.widget_row)
            views.setOnClickPendingIntent(R.id.widget_row_0, openAppPendingIntent(context))
            for (slot in 1 until PAGE_SIZE) {
                views.setViewVisibility(rowIds[slot], View.INVISIBLE)
            }
        }

        return views
    }

    private fun formatRowLabel(title: String, protocol: String): String {
        val name = title.ifBlank { "Сервер" }
        return "$name · $protocol"
    }

    private fun currentPage(context: Context, nodeCount: Int): Int {
        val pageCount = pageCount(nodeCount)
        if (pageCount <= 0) return 0
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val page = prefs.getInt(KEY_PAGE, 0)
        return page.coerceIn(0, pageCount - 1)
    }

    private fun pageCount(nodeCount: Int): Int =
        if (nodeCount <= 0) 0 else (nodeCount + PAGE_SIZE - 1) / PAGE_SIZE

    private fun broadcastPendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
        receiver: Class<*>,
        extras: (Intent.() -> Unit)? = null,
    ): PendingIntent {
        val intent = Intent(context, receiver).apply {
            this.action = action
            extras?.invoke(this)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

}
