package ru.coffeemaniavpn.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint
import android.util.TypedValue

/**
 * «ClevVPN» как в macOS / [ru.coffeemaniavpn.app.ui.ClevLogoFull]:
 * SF Pro Bold, Clev светлый, VPN жёлтый, лёгкий отрицательный tracking.
 */
object WidgetBrandLogoTextRenderer {
    private const val CLEV = "Clev"
    private const val VPN = "VPN"
    private val colorClev = Color.parseColor("#F2F2F5")
    private val colorVpn = Color.parseColor("#FAC300")

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
            // SF Display-like tracking на macOS
            letterSpacing = -0.03f
            isFakeBoldText = false
        }

        val clevBounds = Rect()
        val vpnBounds = Rect()
        paint.getTextBounds(CLEV, 0, CLEV.length, clevBounds)
        paint.getTextBounds(VPN, 0, VPN.length, vpnBounds)

        val clevAdvance = paint.measureText(CLEV)
        val vpnAdvance = paint.measureText(VPN)
        val width = (clevAdvance + vpnAdvance).toInt().coerceAtLeast(1) + (2 * density).toInt()
        val fm = paint.fontMetrics
        val height = (fm.descent - fm.ascent).toInt().coerceAtLeast(1) + (2 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val baseline = -fm.ascent + density

        paint.color = colorClev
        canvas.drawText(CLEV, density, baseline, paint)
        paint.color = colorVpn
        canvas.drawText(VPN, density + clevAdvance, baseline, paint)
        return bitmap
    }
}
