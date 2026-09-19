package ru.coffeemaniavpn.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.sin

/**
 * Фон как на [hushvpn.net](https://www.hushvpn.net):
 * — radial wash (cyan / blue / red)
 * — плавно текущие синусоидальные линии (canvas #waves)
 */
private data class WaveLayer(
    val amp: Float,
    val wl: Float,
    val sp: Float,
    val yy: Float,
    val color: Color,
    val opacity: Float,
    val stroke: Float,
)

@Composable
fun HushSiteBackground(
    modifier: Modifier = Modifier,
) {
    val layers = remember {
        listOf(
            // yy чуть выше, чем на сайте — линии не «лежат» внизу экрана
            WaveLayer(0.045f, 1.50f, 0.45f, 0.18f, Color(0xFF38CCFF), 0.40f, 1.6f),
            WaveLayer(0.070f, 1.05f, 0.65f, 0.30f, Color(0xFF2A6EFF), 0.52f, 1.9f),
            WaveLayer(0.115f, 1.25f, 0.50f, 0.42f, Color(0xFF38CCFF), 0.66f, 2.3f),
            WaveLayer(0.140f, 0.82f, 0.90f, 0.52f, Color(0xFF7B6BFF), 0.44f, 1.9f),
            WaveLayer(0.170f, 0.60f, 0.42f, 0.62f, Color(0xFF2A6EFF), 0.52f, 2.1f),
            WaveLayer(0.100f, 1.70f, 1.10f, 0.48f, Color(0xFFFF2E50), 0.44f, 1.7f),
        )
    }

    val transition = rememberInfiniteTransition(label = "hushWaves")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 40).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 55_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )

    // Мягкое «переливание» радиальных пятен
    val washShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "washShift",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Base vertical wash — site linear-gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF05091A),
                    Color(0xFF060D24),
                    Color(0xFF05091A),
                ),
            ),
        )

        val shiftX = (washShift - 0.5f) * w * 0.08f
        val shiftY = (washShift - 0.5f) * h * 0.06f

        // .bgwash radial blobs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2A5BFF).copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * 0.80f + shiftX, h * -0.05f + shiftY),
                radius = w * 0.85f,
            ),
            center = Offset(w * 0.80f + shiftX, h * -0.05f + shiftY),
            radius = w * 0.85f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF2E50).copy(alpha = 0.16f), Color.Transparent),
                center = Offset(w * 0.88f - shiftX * 0.6f, h * 0.06f),
                radius = w * 0.55f,
            ),
            center = Offset(w * 0.88f - shiftX * 0.6f, h * 0.06f),
            radius = w * 0.55f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF38CCFF).copy(alpha = 0.15f), Color.Transparent),
                center = Offset(w * 0.06f + shiftX * 0.4f, h * 0.14f),
                radius = w * 0.70f,
            ),
            center = Offset(w * 0.06f + shiftX * 0.4f, h * 0.14f),
            radius = w * 0.70f,
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFF2E50).copy(alpha = 0.10f), Color.Transparent),
                center = Offset(w * 0.55f, h * 1.08f - shiftY),
                radius = w * 0.55f,
            ),
            center = Offset(w * 0.55f, h * 1.08f - shiftY),
            radius = w * 0.55f,
        )

        val step = (6f * density).coerceAtLeast(4f)
        for (layer in layers) {
            val yc = h * layer.yy
            val amp = h * layer.amp
            val k = (2f * PI.toFloat()) / (w * layer.wl)
            val path = Path()
            var x = 0f
            var first = true
            while (x <= w) {
                val env = sin(x / w * PI.toFloat()).toFloat().coerceAtLeast(0f)
                val y = yc + sin(x * k + phase * layer.sp) * amp * env
                if (first) {
                    path.moveTo(x, y)
                    first = false
                } else {
                    path.lineTo(x, y)
                }
                x += step
            }

            val strokeBrush = Brush.horizontalGradient(
                colorStops = arrayOf(
                    0f to layer.color.copy(alpha = 0f),
                    0.5f to layer.color.copy(alpha = layer.opacity),
                    1f to layer.color.copy(alpha = 0f),
                ),
            )
            // Soft glow pass
            drawPath(
                path = path,
                brush = strokeBrush,
                style = Stroke(
                    width = layer.stroke * density * 3.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
                alpha = 0.22f,
            )
            drawPath(
                path = path,
                brush = strokeBrush,
                style = Stroke(
                    width = layer.stroke * density,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}
