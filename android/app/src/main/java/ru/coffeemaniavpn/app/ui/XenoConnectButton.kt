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
    val plate = Color(0xFF141B18)
    val plateBorder = when {
        connected -> Color(0xFF00D4A8)
        busy -> Color(0xFF222B28)
        else -> Color(0xFF222B28)
    }
    val plateBorderWidth = if (connected) 3.5.dp else 1.5.dp

    val squareSize = 6.dp
    val squareRadius = 1.dp
    val orbitGap = 22.dp
    val orbitPad = orbitGap + squareSize
    val totalSize = size + orbitPad * 2
    val teal = Color(0xFF00D4A8)
    val trackIdle = Color(0xFF566460).copy(alpha = 0.45f)

    // Connecting/Connected: continuous clockwise spin (~8s / lap)
    val spin by rememberInfiniteTransition(label = "xenoOrbitSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = LinearEasing),
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
        if (connected) {
            Canvas(modifier = Modifier.size(totalSize)) {
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val glowR = size.toPx() * 0.72f
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to teal.copy(alpha = 0.42f),
                            0.42f to teal.copy(alpha = 0.22f),
                            0.70f to teal.copy(alpha = 0.08f),
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

            when {
                // Connecting + Connected: 4 dense comet trails @ 90°, rotating clockwise
                busy || connected -> {
                    val headCount = 4
                    val trailLen = 22
                    val stepDeg = 3.2f // tight spacing → solid-looking head
                    for (h in 0 until headCount) {
                        val headDeg = -90f + spin + h * 90f
                        for (t in 0 until trailLen) {
                            // Trail behind head → counter to clockwise motion
                            val trailDeg = headDeg - t * stepDeg
                            val frac = t / (trailLen - 1).toFloat()
                            // Dense bright block for first ~30%, then fade/thin out
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
                                    color = teal.copy(alpha = 0.38f),
                                    scale = 2.05f,
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
                // Idle: dim static track
                else -> {
                    val dotCount = 36
                    val stepDeg = 360f / dotCount
                    for (i in 0 until dotCount) {
                        drawDot(
                            angleDeg = -90f + i * stepDeg,
                            color = trackIdle,
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
