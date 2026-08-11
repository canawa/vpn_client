package ru.coffeemaniavpn.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import ru.coffeemaniavpn.app.ui.FlagUtils

/** PNG-флаги из assets — как в списке серверов приложения. */
object WidgetFlagBitmaps {
    private const val HEIGHT_DP = 18f
    private const val CORNER_DP = 5f

    private val cache = object : LruCache<String, Bitmap>(48) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun get(context: Context, flagOrCode: String): Bitmap {
        val code = FlagUtils.resolveCountryCodeOrDefault(flagOrCode)
        val density = context.resources.displayMetrics.density
        val heightPx = (HEIGHT_DP * density).toInt().coerceAtLeast(1)
        val widthPx = (heightPx * 3 / 2).coerceAtLeast(1)
        val cacheKey = "$code@${widthPx}x$heightPx"
        cache.get(cacheKey)?.let { cached ->
            if (!cached.isRecycled) return cached
        }

        val decoded = decodeAsset(context, code) ?: decodeAsset(context, FlagUtils.DEFAULT_FLAG_CODE)
        val bitmap = if (decoded != null) {
            roundCrop(decoded, widthPx, heightPx, CORNER_DP * density)
        } else {
            Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        }
        cache.put(cacheKey, bitmap)
        // RemoteViews может удержать bitmap — отдаём копию, кэш не трогаем.
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun decodeAsset(context: Context, code: String): Bitmap? =
        runCatching {
            context.assets.open("flags/$code.png").use { stream ->
                BitmapFactory.Options().run {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    BitmapFactory.decodeStream(stream, null, this)
                }
            }
        }.getOrNull()

    private fun roundCrop(source: Bitmap, width: Int, height: Int, cornerPx: Float): Bitmap {
        val scaled = if (source.width == width && source.height == height) {
            source
        } else {
            Bitmap.createScaledBitmap(source, width, height, true)
        }
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawRoundRect(
            RectF(0f, 0f, width.toFloat(), height.toFloat()),
            cornerPx,
            cornerPx,
            paint,
        )
        if (scaled !== source) {
            scaled.recycle()
        }
        if (!source.isRecycled) {
            source.recycle()
        }
        return output
    }
}
