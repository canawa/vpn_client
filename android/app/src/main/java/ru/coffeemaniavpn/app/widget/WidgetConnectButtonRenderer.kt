package ru.coffeemaniavpn.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import ru.coffeemaniavpn.app.vpn.VpnManager
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Bitmap-зеркало [ru.coffeemaniavpn.app.ui.XenoConnectButton] для RemoteViews.
 */
object WidgetConnectButtonRenderer {
    /** Диаметр пластины (large / small). */
    const val PLATE_DP = 72f
    const val PLATE_DP_SMALL = 56f
    /** 1×1: полный диаметр кнопки без подложки. */
    const val BUTTON_TOTAL_DP = 56f

    /** Как в Compose: plate 188, orbitGap 22, square 6. */
    private const val REF_PLATE = 188f
    private const val REF_ORBIT_GAP = 22f
    private const val REF_SQUARE = 6f
    private const val REF_SQUARE_RADIUS = 1f
    private const val REF_ICON = 56f
    private const val REF_STROKE = 4.5f
    private const val REF_STEM_H = 18f
    private const val REF_GAP_DEG = 72f
    /** Head glow на орбите — больше базового квадрата. */
    private const val MAX_DOT_SCALE = 2.05f
    /** Supersample: рисуем в 2× пикселях → downscale в ImageView даёт чёткость. */
    private const val SUPER_SAMPLE = 2f

    private val teal = 0xFF00D4A8.toInt()
    private val plateColor = 0xFF141B18.toInt()
    private val plateBorderIdle = 0xFF222B28.toInt()
    private val trackIdle = ColorUtils.setAlphaComponent(0xFF566460.toInt(), (0.45f * 255).toInt())
    private val accentIdle = 0xFF6B7672.toInt()

    /** Полный размер кнопки (dp) при данном диаметре пластины. */
    fun totalDpForPlate(plateDp: Float): Float {
        val gap = plateDp * (REF_ORBIT_GAP / REF_PLATE)
        val square = plateDp * (REF_SQUARE / REF_PLATE)
        // 2 * (plate/2 + gap + square*(1+maxScale)/2)
        return plateDp + 2f * gap + square * (1f + MAX_DOT_SCALE)
    }

    fun plateDpForTotal(totalDp: Float): Float {
        val k = totalDpForPlate(1f)
        return (totalDp / k).coerceAtLeast(1f)
    }

    fun render(
        context: Context,
        @Suppress("UNUSED_PARAMETER") connectionElapsedMs: Long,
        anim: WidgetConnectAnimState = WidgetConnectAnimState.snapped(VpnManager.status.value),
        plateDp: Float = PLATE_DP,
        /** Если задан — пластина подгоняется так, чтобы всё влезло в этот диаметр. */
        maxTotalDp: Float? = null,
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val effectivePlateDp = if (maxTotalDp != null) {
            plateDpForTotal(maxTotalDp)
        } else {
            plateDp
        }
        // Рисуем в SUPER_SAMPLE × density пикселей на dp.
        val px = density * SUPER_SAMPLE
        val plate = effectivePlateDp * px
        val square = REF_SQUARE / REF_PLATE * plate
        val orbitGap = REF_ORBIT_GAP / REF_PLATE * plate
        val maxExtent = plate / 2f + orbitGap + square * (1f + MAX_DOT_SCALE) / 2f
        // Небольшой запас под AA, без выхода за край ImageView.
        val pad = 1.5f * SUPER_SAMPLE
        val size = ((maxExtent + pad) * 2f).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f

        val busy = anim.labelMode == WidgetConnectAnimState.LabelMode.Busy
        val on = anim.labelMode == WidgetConnectAnimState.LabelMode.On
        val active = busy || on
        val accent = if (active) teal else accentIdle
        val border = if (on) teal else plateBorderIdle
        val unit = plate / REF_PLATE
        val borderWidth = if (on) 3.5f * unit else 1.5f * unit

        if (on) {
            // Glow остаётся внутри орбиты, не раздуваем за края bitmap.
            drawConnectedGlow(canvas, cx, cy, plate * 0.58f)
        }
        drawOrbit(
            canvas = canvas,
            cx = cx,
            cy = cy,
            plate = plate,
            square = square,
            orbitGap = orbitGap,
            spin = anim.spinAngle,
            active = active,
        )
        drawPlate(canvas, cx, cy, plate, border, borderWidth)
        drawPowerGlyph(canvas, cx, cy, plate, accent, unit)
        return bitmap
    }

    private fun drawConnectedGlow(canvas: Canvas, cx: Float, cy: Float, glowR: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                cx,
                cy,
                glowR,
                intArrayOf(
                    ColorUtils.setAlphaComponent(teal, (0.36f * 255).toInt()),
                    ColorUtils.setAlphaComponent(teal, (0.16f * 255).toInt()),
                    ColorUtils.setAlphaComponent(teal, (0.05f * 255).toInt()),
                    0x00000000,
                ),
                floatArrayOf(0f, 0.45f, 0.75f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(cx, cy, glowR, paint)
    }

    private fun drawOrbit(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        plate: Float,
        square: Float,
        orbitGap: Float,
        spin: Float,
        active: Boolean,
    ) {
        val corner = REF_SQUARE_RADIUS / REF_SQUARE * square
        val orbit = plate / 2f + orbitGap + square / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
        }

        fun drawDot(angleDeg: Float, color: Int, scale: Float = 1f) {
            val rad = Math.toRadians(angleDeg.toDouble())
            val x = cx + (orbit * cos(rad)).toFloat()
            val y = cy + (orbit * sin(rad)).toFloat()
            val s = square * scale
            paint.color = color
            canvas.drawRoundRect(
                RectF(x - s / 2f, y - s / 2f, x + s / 2f, y + s / 2f),
                corner * scale,
                corner * scale,
                paint,
            )
        }

        if (active) {
            val headCount = 4
            val trailLen = 22
            val stepDeg = 3.2f
            for (h in 0 until headCount) {
                val headDeg = -90f + spin + h * 90f
                for (t in 0 until trailLen) {
                    val trailDeg = headDeg - t * stepDeg
                    val frac = t / (trailLen - 1).toFloat()
                    val alpha = when {
                        t <= 4 -> 1f
                        else -> (1f - ((t - 4) / (trailLen - 5f))).toDouble()
                            .pow(1.35)
                            .toFloat()
                            .coerceIn(0.06f, 1f)
                    }
                    val scale = when {
                        t <= 3 -> 1.05f
                        else -> (1f - frac * 0.78f).coerceIn(0.22f, 1f)
                    }
                    if (t == 0) {
                        drawDot(
                            angleDeg = trailDeg,
                            color = ColorUtils.setAlphaComponent(teal, (0.38f * 255).toInt()),
                            scale = MAX_DOT_SCALE,
                        )
                    }
                    drawDot(
                        angleDeg = trailDeg,
                        color = ColorUtils.setAlphaComponent(teal, (alpha * 255).toInt()),
                        scale = scale,
                    )
                }
            }
        } else {
            val dotCount = 36
            val stepDeg = 360f / dotCount
            for (i in 0 until dotCount) {
                drawDot(angleDeg = -90f + i * stepDeg, color = trackIdle)
            }
        }
    }

    private fun drawPlate(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        plate: Float,
        border: Int,
        borderWidth: Float,
    ) {
        val r = plate / 2f
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = plateColor }
        canvas.drawCircle(cx, cy, r, fill)
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth
            color = border
        }
        canvas.drawCircle(cx, cy, r - borderWidth / 2f, stroke)
    }

    private fun drawPowerGlyph(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        plate: Float,
        accent: Int,
        unit: Float,
    ) {
        val icon = plate * (REF_ICON / REF_PLATE)
        val stroke = unit * REF_STROKE
        val pad = stroke / 2f
        val left = cx - icon / 2f
        val top = cy - icon / 2f
        val oval = RectF(left + pad, top + pad, left + icon - pad, top + icon - pad)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = accent
        }
        canvas.drawArc(oval, 270f + REF_GAP_DEG / 2f, 360f - REF_GAP_DEG, false, paint)

        val stemW = stroke
        val stemH = unit * REF_STEM_H
        val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
        val stemLeft = cx - stemW / 2f
        val stemTop = top + unit * 1f
        canvas.drawRoundRect(
            RectF(stemLeft, stemTop, stemLeft + stemW, stemTop + stemH),
            unit * 3f,
            unit * 3f,
            stemPaint,
        )
    }
}
