package ru.coffeemaniavpn.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.text.TextPaint
import android.util.TypedValue

/** XENO brand mark + wordmark for home-screen widgets. */
object WidgetBrandLogoTextRenderer {
    private const val WORDMARK = "XENO"
    private val colorText = Color.parseColor("#F2F5F4")
    private val colorOuter = Color.parseColor("#075243")
    private val colorInner = Color.parseColor("#00D4A8")
    private val colorPlate = Color.parseColor("#0A0D0C")

    fun render(context: Context, textSizeSp: Float = 16f): Bitmap {
        val density = context.resources.displayMetrics.density
        val textSizePx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            textSizeSp,
            context.resources.displayMetrics,
        )
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = WidgetFonts.bold(context)
            textSize = textSizePx
            letterSpacing = 0.04f
            color = colorText
        }

        val textBounds = Rect()
        paint.getTextBounds(WORDMARK, 0, WORDMARK.length, textBounds)
        val textAdvance = paint.measureText(WORDMARK)
        val fm = paint.fontMetrics

        val markSize = (textSizePx * 0.95f).toInt().coerceAtLeast((14 * density).toInt())
        val gap = (5 * density).toInt()
        val pad = (2 * density).toInt()
        val width = (pad + markSize + gap + textAdvance + pad).toInt().coerceAtLeast(1)
        val height = maxOf(markSize, (fm.descent - fm.ascent).toInt()) + pad * 2

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val markTop = (height - markSize) / 2f
        val markLeft = pad.toFloat()
        // plate
        canvas.drawRoundRect(
            RectF(markLeft, markTop, markLeft + markSize, markTop + markSize),
            2f * density,
            2f * density,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorPlate },
        )
        val inset = markSize * 0.17f
        canvas.drawRect(
            markLeft + inset,
            markTop + inset,
            markLeft + markSize - inset,
            markTop + markSize - inset,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorOuter },
        )
        val core = markSize * 0.33f
        val cx = markLeft + markSize / 2f
        val cy = markTop + markSize / 2f
        canvas.drawRect(
            cx - core / 2f,
            cy - core / 2f,
            cx + core / 2f,
            cy + core / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorInner },
        )

        val baseline = (height / 2f) - (fm.ascent + fm.descent) / 2f
        canvas.drawText(WORDMARK, markLeft + markSize + gap, baseline, paint)
        return bitmap
    }
}
