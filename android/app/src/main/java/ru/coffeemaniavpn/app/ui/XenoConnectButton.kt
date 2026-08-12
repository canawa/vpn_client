package ru.coffeemaniavpn.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.coffeemaniavpn.app.vpn.VpnStatus
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun XenoConnectButton(
    vpnStatus: VpnStatus,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 176.dp,
) {
    val connected = vpnStatus == VpnStatus.Started
    val busy = vpnStatus == VpnStatus.Starting || vpnStatus == VpnStatus.Stopping
    val accent = when {
        !enabled -> Color(0xFF7A7F78)
        connected || busy -> Color(0xFF00E091)
        else -> Color(0xFF7A7F78)
    }
    val plate = if (connected) Color(0xFF141414) else Color(0xFF1A1A1A)
    val ringColor = if (connected) Color(0xFF00E091) else Color(0xFF2A2A2A)
    val squareColor = if (connected) Color(0xFF00E091) else Color(0xFF3A3A3A)

    val spin by rememberInfiniteTransition(label = "xenoSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "spin",
    )

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val outerR = this.size.minDimension / 2f
            val square = outerR * 0.055f
            val orbit = outerR - square * 1.6f
            val count = 28
            val highlightCount = if (busy) 6 else 0
            val baseAngle = if (busy) Math.toRadians(spin.toDouble()) else -Math.PI / 2

            for (i in 0 until count) {
                val angle = baseAngle + (2.0 * Math.PI * i / count)
                val x = cx + (orbit * cos(angle)).toFloat()
                val y = cy + (orbit * sin(angle)).toFloat()
                val lit = when {
                    // Connected: full teal square ring (Figma)
                    connected -> true
                    busy -> {
                        val phase = ((i + (spin / 360f * count).toInt()) % count)
                        phase < highlightCount
                    }
                    else -> false
                }
                drawRoundRect(
                    color = when {
                        lit -> squareColor
                        connected -> squareColor.copy(alpha = 0.25f)
                        else -> squareColor.copy(alpha = 0.7f)
                    },
                    topLeft = Offset(x - square / 2f, y - square / 2f),
                    size = Size(square, square),
                    cornerRadius = CornerRadius(square * 0.2f, square * 0.2f),
                )
            }

            val ringR = outerR * 0.62f
            drawCircle(
                color = ringColor,
                radius = ringR,
                center = Offset(cx, cy),
                style = Stroke(width = if (connected) 3.5.dp.toPx() else 2.dp.toPx()),
            )
        }

        Box(
            modifier = Modifier
                .size(size * 0.52f)
                .clip(CircleShape)
                .background(plate),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(size * 0.22f),
            )
        }
    }
}

@Composable
fun XenoSquareDashes(
    active: Boolean,
    modifier: Modifier = Modifier,
    count: Int = 9,
) {
    val tint = if (active) Color(0xFF00E091) else Color(0xFF3A3A3A)
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
