package work.bavshield.vpn.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private val HexIdle = Color(0xFF151E19)
private val HexLit = Color(0xFF39FF88)

@Composable
fun CyberBackground(
    modifier: Modifier = Modifier,
    connected: Boolean = false,
) {
    val intensity by animateFloatAsState(
        targetValue = if (connected) 1f else 0f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "hexLit",
    )
    val strokePx = with(LocalDensity.current) { 0.85.dp.toPx() }
    val hexRadius = with(LocalDensity.current) { 20.dp.toPx() }

    Canvas(modifier.fillMaxSize()) {
        drawRect(BavShieldColors.Canvas)

        val glowCenter = Offset(size.width / 2f, size.height * 0.34f)
        val glowRadius = size.minDimension * 0.55f

        // Soft wash from shield glow — makes nearby hexes readable
        if (intensity > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        HexLit.copy(alpha = 0.10f * intensity),
                        HexLit.copy(alpha = 0.035f * intensity),
                        Color.Transparent,
                    ),
                    center = glowCenter,
                    radius = glowRadius,
                ),
                radius = glowRadius,
                center = glowCenter,
            )
        }

        val hexW = hexRadius * 1.7320508f
        val hexH = hexRadius * 1.5f
        var row = 0
        var y = -hexRadius
        while (y < size.height + hexRadius) {
            var x = if (row % 2 == 0) 0f else hexW / 2f
            while (x < size.width + hexW) {
                val path = Path()
                for (i in 0..5) {
                    val angle = Math.toRadians(60.0 * i - 30.0)
                    val px = x + hexRadius * cos(angle).toFloat()
                    val py = y + hexRadius * sin(angle).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()

                val dist = hypot((x - glowCenter.x).toDouble(), (y - glowCenter.y).toDouble()).toFloat()
                val falloff = (1f - (dist / glowRadius).coerceIn(0f, 1f))
                // Smooth falloff curve so lit zone matches shield glow
                val lit = (falloff * falloff) * intensity

                val color = lerp(HexIdle, HexLit, lit).copy(
                    alpha = 0.055f + 0.55f * lit,
                )
                drawPath(
                    path,
                    color,
                    style = Stroke(width = strokePx * (1f + 0.85f * lit)),
                )
                x += hexW
            }
            y += hexH
            row++
        }
    }
}
