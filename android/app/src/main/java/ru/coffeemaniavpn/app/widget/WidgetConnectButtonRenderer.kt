package ru.coffeemaniavpn.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.text.TextPaint
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.ColorUtils
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.vpn.VpnManager
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Статичный рендер [XenoPowerConnectButton] для RemoteViews — те же кольца и 3D-колодец.
 */
object WidgetConnectButtonRenderer {
    /** Диаметр пластины (large / small). */
    const val PLATE_DP = 72f
    /** Та же отрисовка, что large; меньше пластина, чтобы влезать в 3×1. */
    const val PLATE_DP_SMALL = 56f
    private const val REF_PLATE_DP = 150f
    private const val REF_RING_PAD_DP = 36f
    private const val REF_OUTER_PAD_DP = 28f
    private const val REF_HALO_PAD_DP = 20f

    private val mocha = 0xFF9A9AA3.toInt()
    private val logoYellow = 0xFFFAC300.toInt()
    private val stroke = 0xFF2A2A31.toInt()
    private val plateYellow = 0xFFF5C400.toInt()
    private val plateAmber = 0xFFE8A200.toInt()

    fun render(
        context: Context,
        connectionElapsedMs: Long,
        anim: WidgetConnectAnimState = WidgetConnectAnimState.snapped(VpnManager.status.value),
        plateDp: Float = PLATE_DP,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val scale = plateDp / REF_PLATE_DP
        val plate = plateDp * density
        val ring = plate + REF_RING_PAD_DP * scale * density
        val outer = ring + REF_OUTER_PAD_DP * scale * density
        val halo = ring + REF_HALO_PAD_DP * scale * density
        val size = outer.toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val densityScaled = density * scale

        if (anim.burstAlpha > 0.01f) {
            drawBurst(canvas, cx, cy, ring, anim.burstAlpha, densityScaled)
        }
        drawHaloRings(canvas, cx, cy, halo, densityScaled, anim.haloPulse)
        drawStatusRing(canvas, cx, cy, ring, densityScaled, anim)
        drawPlate(canvas, cx, cy, plate, anim.plateOn)
        val busy = anim.labelMode == WidgetConnectAnimState.LabelMode.Busy
        val on = anim.labelMode == WidgetConnectAnimState.LabelMode.On
        drawContent(context, canvas, cx, cy, plate, density, on, busy, connectionElapsedMs)
        return bitmap
    }

    private fun drawBurst(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        diameter: Float,
        alpha: Float,
        density: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        val r = diameter / 2f
        for (i in 0..2) {
            val expand = 1f + i * 0.12f + alpha * 0.2f
            paint.color = ColorUtils.setAlphaComponent(
                0xFFFAC300.toInt(),
                ((alpha * (0.35f - i * 0.1f)) * 255).toInt().coerceIn(0, 255),
            )
            canvas.drawCircle(cx, cy, r * expand, paint)
        }
    }

    private fun drawHaloRings(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        diameter: Float,
        density: Float,
        pulse: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
        }
        val rings = floatArrayOf(0.98f, 0.88f, 0.78f)
        rings.forEachIndexed { index, scale ->
            val a = ((0.35f - index * 0.08f) * pulse).coerceIn(0f, 1f)
            paint.color = ColorUtils.setAlphaComponent(stroke, (a * 255).toInt())
            canvas.drawCircle(cx, cy, diameter / 2f * scale, paint)
        }
    }

    private fun drawStatusRing(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        diameter: Float,
        density: Float,
        anim: WidgetConnectAnimState,
    ) {
        val r = diameter / 2f
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        val base = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f * density
            color = ColorUtils.setAlphaComponent(stroke, (0.55f * 255).toInt())
        }
        canvas.drawCircle(cx, cy, r, base)

        if (anim.ringFill > 0.001f) {
            val colors = intArrayOf(0xFFFAC300.toInt(), 0xFFE39A00.toInt(), 0xFFFAC300.toInt())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
                strokeCap = Paint.Cap.ROUND
                shader = SweepGradient(cx, cy, colors, null).also { shader ->
                    val m = Matrix()
                    m.postRotate(-90f, cx, cy)
                    shader.setLocalMatrix(m)
                }
            }
            canvas.drawArc(oval, -90f, 360f * anim.ringFill, false, paint)
        }

        if (anim.showComet && anim.ringFill < 0.999f) {
            val colors = intArrayOf(
                0x00FFFFFF,
                ColorUtils.setAlphaComponent(0xFFFAC300.toInt(), (0.7f * 255).toInt()),
                0xFFFFFFFF.toInt(),
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * density
                strokeCap = Paint.Cap.ROUND
                shader = SweepGradient(cx, cy, colors, floatArrayOf(0f, 0.55f, 1f)).also { shader ->
                    val m = Matrix()
                    m.postRotate(anim.cometAngle, cx, cy)
                    shader.setLocalMatrix(m)
                }
            }
            canvas.drawArc(oval, -90f, 360f * 0.18f, false, paint)
        }

        if (anim.showSpinner) {
            // Полный круг с вращающимся «хвостом» — как indeterminate progress.
            canvas.save()
            canvas.rotate(anim.spinAngle, cx, cy)
            val colors = intArrayOf(
                0x00FFFFFF,
                ColorUtils.setAlphaComponent(0xFFE39A00.toInt(), (0.25f * 255).toInt()),
                0xFFE39A00.toInt(),
                0xFFFAC300.toInt(),
                0xFFFFFFFF.toInt(),
            )
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f * density
                strokeCap = Paint.Cap.ROUND
                shader = SweepGradient(
                    cx,
                    cy,
                    colors,
                    floatArrayOf(0f, 0.45f, 0.7f, 0.88f, 1f),
                ).also { shader ->
                    val m = Matrix()
                    m.postRotate(-90f, cx, cy)
                    shader.setLocalMatrix(m)
                }
            }
            canvas.drawCircle(cx, cy, r, paint)
            canvas.restore()
        } else if (anim.ringFill >= 0.999f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2f * density
                strokeCap = Paint.Cap.ROUND
                color = ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), (0.35f * 255).toInt())
            }
            canvas.save()
            canvas.rotate(anim.spinAngle, cx, cy)
            canvas.drawArc(oval, -90f, 28f, false, paint)
            canvas.restore()
        }
    }

    private fun drawPlate(canvas: Canvas, cx: Float, cy: Float, diameter: Float, plateOn: Float) {
        val r = diameter / 2f
        val left = cx - r
        val top = cy - r

        val bezelHi = lerpColor(0xFF4A4A54.toInt(), 0xFFFFE082.toInt(), plateOn * 0.7f)
        val bezelLo = lerpColor(0xFF141418.toInt(), 0xFFD48900.toInt(), plateOn * 0.85f)
        val wellTop = lerpColor(0xFF07070A.toInt(), 0xFFA87400.toInt(), plateOn * 0.7f)
        val wellBot = lerpColor(0xFF22222C.toInt(), 0xFFE8B020.toInt(), plateOn * 0.85f)
        val floorHi = lerpColor(0xFF2C2C36.toInt(), plateYellow, plateOn)
        val floorLo = lerpColor(0xFF16161C.toInt(), plateAmber, plateOn)
        val insetShadowAlpha = 0.55f * (1f - plateOn * 0.35f)
        val rimGlowAlpha = 0.06f + plateOn * 0.06f

        val bezelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                left, top, left + diameter, top + diameter,
                bezelHi, bezelLo, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, r, bezelPaint)

        val wellInset = diameter * 0.055f
        val wellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                left + diameter * 0.15f, top,
                left + diameter * 0.85f, top + diameter,
                wellTop, wellBot, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, r - wellInset, wellPaint)

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = diameter * 0.07f
            shader = SweepGradient(
                cx, cy,
                intArrayOf(
                    0x00000000,
                    ColorUtils.setAlphaComponent(0xFF000000.toInt(), (insetShadowAlpha * 255).toInt()),
                    ColorUtils.setAlphaComponent(0xFF000000.toInt(), (insetShadowAlpha * 255).toInt()),
                    0x00000000,
                    0x00000000,
                ),
                floatArrayOf(0f, 0.35f, 0.55f, 0.75f, 1f),
            )
        }
        canvas.drawCircle(cx, cy, r - wellInset, shadowPaint)

        val floorInset = diameter * 0.13f
        val floorR = r - floorInset
        val floorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                left + diameter * 0.38f,
                top + diameter * 0.34f,
                floorR * 0.9f,
                floorHi,
                floorLo,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, floorR, floorPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = diameter * 0.035f
            strokeCap = Paint.Cap.ROUND
            color = ColorUtils.setAlphaComponent(0xFFFFFFFF.toInt(), (rimGlowAlpha * 255).toInt())
        }
        val oval = RectF(cx - r, cy - r, cx + r, cy + r)
        canvas.drawArc(oval, 20f, 70f, false, glowPaint)
    }

    private fun drawContent(
        context: Context,
        canvas: Canvas,
        cx: Float,
        cy: Float,
        plate: Float,
        density: Float,
        on: Boolean,
        busy: Boolean,
        connectionElapsedMs: Long,
    ) {
        val iconSize = plate * 0.22f
        val iconColor = when {
            on -> ColorUtils.setAlphaComponent(0xFF000000.toInt(), (0.75f * 255).toInt())
            busy -> logoYellow
            else -> mocha
        }
        val gap = if (on) 7f * density else 6f * density

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = WidgetFonts.bold(context)
            textAlign = Paint.Align.LEFT
            letterSpacing = 0f
            isFakeBoldText = false
        }

        if (on) {
            textPaint.color = ColorUtils.setAlphaComponent(0xFF000000.toInt(), (0.45f * 255).toInt())
            textPaint.textSize = plate * 0.058f
            val label = context.getString(R.string.xeno_connected)
            fitTextSize(textPaint, label, plate * 0.84f)
            val labelFm = textPaint.fontMetrics
            val labelH = labelFm.descent - labelFm.ascent

            textPaint.textSize = plate * 0.11f
            val timer = formatSession(connectionElapsedMs)
            fitTextSize(textPaint, timer, plate * 0.84f)
            val timerFm = textPaint.fontMetrics
            val timerH = timerFm.descent - timerFm.ascent
            val timerGap = 3f * density

            // Блок по центру пластины: icon + gap + label + gap + timer
            textPaint.textSize = plate * 0.058f
            fitTextSize(textPaint, label, plate * 0.84f)
            val blockH = iconSize + gap + labelH + timerGap + timerH
            var y = cy - blockH / 2f
            drawPowerIcon(context, canvas, cx, y + iconSize / 2f, iconSize, iconColor)
            y += iconSize + gap
            textPaint.color = ColorUtils.setAlphaComponent(0xFF000000.toInt(), (0.45f * 255).toInt())
            textPaint.textSize = plate * 0.058f
            fitTextSize(textPaint, label, plate * 0.84f)
            drawOpticalCenteredText(canvas, label, cx, y - textPaint.fontMetrics.ascent, textPaint)
            y += labelH + timerGap
            textPaint.color = ColorUtils.setAlphaComponent(0xFF000000.toInt(), (0.7f * 255).toInt())
            textPaint.textSize = plate * 0.11f
            fitTextSize(textPaint, timer, plate * 0.84f)
            drawOpticalCenteredText(canvas, timer, cx, y - textPaint.fontMetrics.ascent, textPaint)
        } else {
            textPaint.color = mocha
            val label = if (busy) {
                context.getString(R.string.xeno_connecting)
            } else {
                context.getString(R.string.xeno_start)
            }
            textPaint.textSize = plate * if (busy) 0.055f else 0.078f
            fitTextSize(textPaint, label, plate * 0.84f)
            val fm = textPaint.fontMetrics
            val labelH = fm.descent - fm.ascent
            val blockH = iconSize + gap + labelH
            val top = cy - blockH / 2f
            drawPowerIcon(context, canvas, cx, top + iconSize / 2f, iconSize, iconColor)
            val baseline = top + iconSize + gap - fm.ascent
            drawOpticalCenteredText(canvas, label, cx, baseline, textPaint)
        }
    }

    private fun fitTextSize(paint: TextPaint, text: String, maxWidth: Float) {
        while (paint.measureText(text) > maxWidth && paint.textSize > 8f) {
            paint.textSize *= 0.96f
        }
    }

    /** Центр по визуальным границам глифов, не по advance width. */
    private fun drawOpticalCenteredText(
        canvas: Canvas,
        text: String,
        cx: Float,
        baselineY: Float,
        paint: TextPaint,
    ) {
        val bounds = android.graphics.Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val x = cx - (bounds.left + bounds.right) / 2f
        canvas.drawText(text, x, baselineY, paint)
    }

    private fun drawPowerIcon(
        context: Context,
        canvas: Canvas,
        cx: Float,
        cy: Float,
        size: Float,
        color: Int,
    ) {
        val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_widget_power)?.mutate() ?: return
        drawable.setTint(color)
        val half = size / 2f
        drawable.setBounds(
            (cx - half).toInt(),
            (cy - half).toInt(),
            (cx + half).toInt(),
            (cy + half).toInt(),
        )
        drawable.draw(canvas)
    }

    private fun formatSession(elapsedMs: Long): String {
        val totalSec = TimeUnit.MILLISECONDS.toSeconds(elapsedMs.coerceAtLeast(0))
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    private fun lerpColor(from: Int, to: Int, t: Float): Int =
        ColorUtils.blendARGB(from, to, t.coerceIn(0f, 1f))
}
