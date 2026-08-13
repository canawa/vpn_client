package ru.coffeemaniavpn.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.coffeemaniavpn.app.vpn.VpnStatus
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

@Composable
fun XenoConnectButton(
    vpnStatus: VpnStatus,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Figma plate diameter (disconnected: 188×188). */
    size: Dp = 188.dp,
) {
    val connected = vpnStatus == VpnStatus.Started
    val busy = vpnStatus == VpnStatus.Starting || vpnStatus == VpnStatus.Stopping
    val active = connected || busy
    val accent = when {
        !enabled -> Color(0xFF6B7672)
        active -> Color(0xFF00D4A8)
        else -> Color(0xFF6B7672)
    }
    // Figma disconnected plate: bg #141B18, border 1.5 #222B28
    val plate = Color(0xFF141B18)
    val plateBorder = if (active) Color(0xFF00D4A8) else Color(0xFF222B28)
    val plateBorderWidth = if (connected) 3.5.dp else 1.5.dp

    // Ring geometry (Figma / video)
    val squareSize = 6.dp
    val squareRadius = 1.dp
    val orbitGap = 22.dp
    val orbitPad = orbitGap + squareSize
    val totalSize = size + orbitPad * 2
    val dotCount = 30
    val headCount = 4
    val trailLen = 11 // squares behind each head
    val teal = Color(0xFF00D4A8)
    val trackIdle = Color(0xFF566460).copy(alpha = 0.5f)
    val trackActive = teal.copy(alpha = 0.22f)

    // Video: ~8s per full clockwise lap
    val spin by rememberInfiniteTransition(label = "xenoSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )

    Box(
        modifier = modifier
            .size(totalSize)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (active) {
            val glowAlpha = if (connected) 1f else 0.55f
            Canvas(modifier = Modifier.size(totalSize)) {
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val glowR = size.toPx() * 0.72f
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to teal.copy(alpha = 0.42f * glowAlpha),
                            0.42f to teal.copy(alpha = 0.22f * glowAlpha),
                            0.70f to teal.copy(alpha = 0.08f * glowAlpha),
                            1.00f to Color.Transparent,
                        ),
                        center = Offset(cx, cy),
                        radius = glowR,
                    ),
                    radius = glowR,
                    center = Offset(cx, cy),
                )
            }
        }

        Canvas(modifier = Modifier.size(totalSize)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val plateR = size.toPx() / 2f
            val square = squareSize.toPx()
            val corner = squareRadius.toPx()
            val orbit = plateR + orbitGap.toPx() + square / 2f
            val stepDeg = 360f / dotCount
            val trackColor = if (active) trackActive else trackIdle

            fun drawDot(angleDeg: Float, color: Color, scale: Float = 1f) {
                val rad = Math.toRadians(angleDeg.toDouble())
                val x = cx + (orbit * cos(rad)).toFloat()
                val y = cy + (orbit * sin(rad)).toFloat()
                val s = square * scale
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x - s / 2f, y - s / 2f),
                    size = Size(s, s),
                    cornerRadius = CornerRadius(corner * scale, corner * scale),
                )
            }

            // 1) Static track — stays put; only color shifts when active
            for (i in 0 until dotCount) {
                // 12 o'clock start: Compose 0° = 3 o'clock → offset -90°
                drawDot(angleDeg = -90f + i * stepDeg, color = trackColor)
            }

            // 2) 4 heads @ 90° + comet tracers (busy + connected)
            // Clockwise motion: spin increases; trail is behind → smaller angle
            if (active) {
                for (h in 0 until headCount) {
                    val headDeg = -90f + spin + h * 90f
                    for (t in 0 until trailLen) {
                        val trailDeg = headDeg - t * stepDeg
                        val frac = t / trailLen.toFloat()
                        val alpha = (1f - frac).toDouble().pow(1.6).toFloat().coerceIn(0.04f, 1f)
                        val scale = (1f - frac * 0.7f).coerceIn(0.3f, 1f)
                        if (t == 0) {
                            // Soft bloom under head
                            drawDot(
                                angleDeg = trailDeg,
                                color = teal.copy(alpha = 0.28f),
                                scale = 1.85f,
                            )
                        }
                        drawDot(
                            angleDeg = trailDeg,
                            color = teal.copy(alpha = alpha),
                            scale = scale,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(plate)
                .border(plateBorderWidth, plateBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(56.dp)) {
                val stroke = 4.5.dp.toPx()
                val pad = stroke / 2f
                val gapDeg = 72f
                drawArc(
                    color = accent,
                    startAngle = 270f + gapDeg / 2f,
                    sweepAngle = 360f - gapDeg,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = Size(this.size.width - stroke, this.size.height - stroke),
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                val stemW = 4.5.dp.toPx()
                val stemH = 18.dp.toPx()
                drawRoundRect(
                    color = accent,
                    topLeft = Offset((this.size.width - stemW) / 2f, 1.dp.toPx()),
                    size = Size(stemW, stemH),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                )
            }
        }
    }
}

@Composable
fun XenoSquareDashes(
    active: Boolean,
    modifier: Modifier = Modifier,
    count: Int = 9,
) {
    val tint = if (active) Color(0xFF00D4A8) else Color(0xFF3A3A3A)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(tint),
            )
        }
    }
}
