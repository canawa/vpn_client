package ru.coffeemaniavpn.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** ConnectBurst из ConnectionEffects.swift — волны + вспышка при подключении. */
@Composable
fun ConnectBurst(
    diameter: Dp,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!active) return

    val ringAnims = remember { List(3) { Animatable(0f) } }
    val flashAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        ringAnims.forEachIndexed { index, anim ->
            launch {
                delay(index * 180L)
                anim.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
            }
        }
        flashAnim.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
    }

    val flashT = flashAnim.value

    Box(modifier = modifier.size(diameter * 1.9f), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(diameter * 1.9f)) {
            val base = diameter.toPx()
            val center = this.center
            ringAnims.forEach { anim ->
                val t = anim.value
                val ringScale = 1f + t * 0.9f
                val alpha = 0.55f * (1f - t)
                scale(ringScale, pivot = center) {
                    drawCircle(
                        color = Color(0xFFFAC300).copy(alpha = alpha),
                        radius = base / 2f,
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            val flashScale = 0.5f + flashT * 1f
            val flashAlpha = 0.9f * (1f - flashT)
            scale(flashScale, pivot = center) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.95f * flashAlpha),
                            Color(0xFFFAC300).copy(alpha = 0.4f * flashAlpha),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = base * 0.55f,
                    ),
                    radius = base * 0.55f,
                    center = center,
                )
            }
        }
    }
}
