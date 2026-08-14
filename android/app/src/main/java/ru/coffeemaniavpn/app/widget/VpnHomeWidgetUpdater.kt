package ru.coffeemaniavpn.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.TypedValue
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
    private const val OUTER_OVER_PLATE = 1.43f
    private const val LARGE_LOGO_DP = 56f
    private const val LARGE_LOGO_MARGIN_DP = 4f

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
    private val colorMuted = Color.parseColor("#8A8A93")
    private val colorYellow = Color.parseColor("#FFC400")

    fun updateAll(context: Context) {
        VpnHomeWidgetAnimator.ensureStarted(context)
        App.applicationScope.launch(Dispatchers.IO) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val largeIds = manager.getAppWidgetIds(
                ComponentName(appContext, VpnHomeWidgetProvider::class.java),
            )
            val smallIds = manager.getAppWidgetIds(
                ComponentName(appContext, VpnHomeWidgetSmallProvider::class.java),
            )
            if (largeIds.isEmpty() && smallIds.isEmpty()) return@launch
            val largePairs = largeIds.map { id -> id to buildLargeViews(appContext, id) }
            val smallPairs = smallIds.map { id -> id to buildSmallViews(appContext, id) }
            withContext(Dispatchers.Main) {
                largePairs.forEach { (id, views) -> manager.updateAppWidget(id, views) }
                smallPairs.forEach { (id, views) -> manager.updateAppWidget(id, views) }
            }
        }
    }

    /** Только кнопка — для покадровой анимации (partial update). */
    suspend fun updateConnectButton(context: Context, anim: WidgetConnectAnimState) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        val largeIds = manager.getAppWidgetIds(
            ComponentName(appContext, VpnHomeWidgetProvider::class.java),
        )
        val smallIds = manager.getAppWidgetIds(
            ComponentName(appContext, VpnHomeWidgetSmallProvider::class.java),
        )
        if (largeIds.isEmpty() && smallIds.isEmpty()) return
        val elapsed = VpnManager.connectionElapsedMs.value
        if (largeIds.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                largeIds.forEach { id ->
                    val bitmap = WidgetConnectButtonRenderer.render(
                        context = appContext,
                        connectionElapsedMs = elapsed,
                        anim = anim,
                        plateDp = largePlateDp(appContext, id),
                    )
                    val views = RemoteViews(appContext.packageName, R.layout.widget_vpn_large).apply {
                        setImageViewBitmap(R.id.widget_connect, bitmap)
                        setOnClickPendingIntent(
                            R.id.widget_connect,
                            broadcastPendingIntent(appContext, ACTION_TOGGLE, requestCode = 100),
                        )
                    }
                    manager.partiallyUpdateAppWidget(id, views)
                }
            }
        }
        if (smallIds.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                smallIds.forEach { id ->
                    val bitmap = WidgetConnectButtonRenderer.render(
                        context = appContext,
                        connectionElapsedMs = elapsed,
                        anim = anim,
                        plateDp = smallPlateDp(appContext, id),
                    )
                    val views = RemoteViews(appContext.packageName, R.layout.widget_vpn_small).apply {
                        setImageViewBitmap(R.id.widget_connect, bitmap)
                        setOnClickPendingIntent(
                            R.id.widget_connect,
                            broadcastPendingIntent(appContext, ACTION_TOGGLE, requestCode = 110),
                        )
                    }
                    manager.partiallyUpdateAppWidget(id, views)
                }
            }
        }
    }

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context.applicationContext)
        val large = manager.getAppWidgetIds(
            ComponentName(context.applicationContext, VpnHomeWidgetProvider::class.java),
        )
        val small = manager.getAppWidgetIds(
            ComponentName(context.applicationContext, VpnHomeWidgetSmallProvider::class.java),
        )
        return large.isNotEmpty() || small.isNotEmpty()
    }

    fun update(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        App.applicationScope.launch(Dispatchers.IO) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val pairs = appWidgetIds.map { id -> id to buildLargeViews(appContext, id) }
            withContext(Dispatchers.Main) {
                pairs.forEach { (id, views) -> manager.updateAppWidget(id, views) }
            }
        }
    }

    fun updateSmall(context: Context, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        App.applicationScope.launch(Dispatchers.IO) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val pairs = appWidgetIds.map { id -> id to buildSmallViews(appContext, id) }
            withContext(Dispatchers.Main) {
                pairs.forEach { (id, views) -> manager.updateAppWidget(id, views) }
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

    private suspend fun buildSmallViews(context: Context, widgetId: Int): RemoteViews {
        val preferences = AppPreferences(context)
        val nodes = preferences.nodes.first()
        val selectedId = preferences.selectedNodeId.first()
        val node = nodes.firstOrNull { it.id == selectedId } ?: nodes.firstOrNull()
        val elapsedMs = VpnManager.connectionElapsedMs.value
        val plateDp = smallPlateDp(context, widgetId)

        val views = RemoteViews(context.packageName, R.layout.widget_vpn_small)
        views.setOnClickPendingIntent(
            R.id.widget_connect,
            broadcastPendingIntent(context, ACTION_TOGGLE, requestCode = 110),
        )
        views.setOnClickPendingIntent(
            R.id.widget_small_root,
            openAppPendingIntent(context),
        )
        views.setOnClickPendingIntent(
            R.id.widget_small_server,
            openAppPendingIntent(context),
        )
        views.setImageViewBitmap(
            R.id.widget_connect,
            WidgetConnectButtonRenderer.render(
                context = context,
                connectionElapsedMs = elapsedMs,
                anim = VpnHomeWidgetAnimator.state,
                plateDp = plateDp,
            ),
        )
        if (node == null) {
            views.setViewVisibility(R.id.widget_small_flag, View.GONE)
            views.setTextViewText(
                R.id.widget_small_name,
                context.getString(R.string.widget_empty_short),
            )
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

    private suspend fun buildLargeViews(context: Context, widgetId: Int): RemoteViews {
        val preferences = AppPreferences(context)
        val nodes = preferences.nodes.first()
        val selectedId = preferences.selectedNodeId.first()
        val page = currentPage(context, nodes.size)
        val elapsedMs = VpnManager.connectionElapsedMs.value

        val views = RemoteViews(context.packageName, R.layout.widget_vpn_large)

        views.setOnClickPendingIntent(
            R.id.widget_connect,
            broadcastPendingIntent(context, ACTION_TOGGLE, requestCode = 100),
        )
        views.setOnClickPendingIntent(
            R.id.widget_logo,
            openAppPendingIntent(context),
        )
        applyLargeLogoSize(views, context, widgetId)
        views.setOnClickPendingIntent(
            R.id.widget_prev,
            broadcastPendingIntent(context, ACTION_PREV, requestCode = 101),
        )
        views.setOnClickPendingIntent(
            R.id.widget_next,
            broadcastPendingIntent(context, ACTION_NEXT, requestCode = 102),
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
                anim = VpnHomeWidgetAnimator.state,
                plateDp = largePlateDp(context, widgetId),
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

    /**
     * Размер пластины по реальной ячейке виджета (dp из AppWidgetOptions),
     * чтобы на разных экранах кнопка не «съезжала» относительно layout.
     * outer ≈ plate × 1.43 (кольца в [WidgetConnectButtonRenderer]).
     */
    private fun largePlateDp(context: Context, widgetId: Int): Float {
        val opts = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
        // Берём max-размер ячейки: иначе битмап меньше ImageView и лаунчер апскейлит (мыло).
        val w = widgetSizeDp(
            opts,
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
            fallback = 250,
        )
        val h = widgetSizeDp(
            opts,
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
            AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
            fallback = 110,
        )
        val leftW = w * 0.38f - 8f
        val btnH = h - 16f - LARGE_LOGO_DP - LARGE_LOGO_MARGIN_DP
        val box = minOf(leftW, btnH).coerceAtLeast(56f)
        return (box / OUTER_OVER_PLATE).coerceIn(48f, 96f)
    }

    /**
     * Лого 2× (56dp), но не шире левой колонки и не выше свободной высоты ячейки.
     * На API 31+ задаём точный квадрат, иначе ImageView match_parent + fitCenter.
     */
    private fun applyLargeLogoSize(views: RemoteViews, context: Context, widgetId: Int) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val dp = largeLogoDp(context, widgetId)
        views.setViewLayoutWidth(R.id.widget_logo, dp, TypedValue.COMPLEX_UNIT_DIP)
        views.setViewLayoutHeight(R.id.widget_logo, dp, TypedValue.COMPLEX_UNIT_DIP)
    }

    private fun largeLogoDp(context: Context, widgetId: Int): Float {
        val opts = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
        val w = widgetFitDp(
            opts,
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
            fallback = 250,
        )
        val h = widgetFitDp(
            opts,
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
            AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
            fallback = 110,
        )
        val leftW = (w - 24f) * 0.38f
        val innerH = h - 16f
        return minOf(LARGE_LOGO_DP, leftW, innerH - LARGE_LOGO_MARGIN_DP).coerceAtLeast(24f)
    }

    /** Для вписывания берём меньшую сторону ячейки — иначе лого вылезет в узкой ориентации. */
    private fun widgetFitDp(
        opts: android.os.Bundle,
        minKey: String,
        maxKey: String,
        fallback: Int,
    ): Int {
        val min = opts.getInt(minKey, 0)
        val max = opts.getInt(maxKey, 0)
        val picked = when {
            min > 0 && max > 0 -> minOf(min, max)
            min > 0 -> min
            max > 0 -> max
            else -> fallback
        }
        return picked
    }

    private fun smallPlateDp(context: Context, widgetId: Int): Float {
        val opts = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId)
        val w = widgetSizeDp(
            opts,
            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
            AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
            fallback = 180,
        )
        val h = widgetSizeDp(
            opts,
            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
            AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
            fallback = 70,
        )
        // Кнопка слева в 3×1, диаметр ограничивает высота ячейки.
        val box = minOf(w * 0.30f, h.toFloat()) - 8f
        return (box.coerceAtLeast(40f) / OUTER_OVER_PLATE).coerceIn(28f, 64f)
    }

    private fun widgetSizeDp(
        opts: android.os.Bundle,
        minKey: String,
        maxKey: String,
        fallback: Int,
    ): Int {
        val min = opts.getInt(minKey, 0)
        val max = opts.getInt(maxKey, 0)
        val picked = maxOf(min, max)
        return if (picked > 0) picked else fallback
    }

    private fun broadcastPendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
        extras: (Intent.() -> Unit)? = null,
    ): PendingIntent {
        val intent = Intent(context, VpnHomeWidgetProvider::class.java).apply {
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
