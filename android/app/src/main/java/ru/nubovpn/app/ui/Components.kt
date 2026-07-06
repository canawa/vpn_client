package ru.nubovpn.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.nubovpn.app.R
import ru.nubovpn.app.data.PingState
import ru.nubovpn.app.vpn.VpnStatus

enum class AppTab { Servers, Home, Settings }

@Composable
fun NuboSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = nuboColors()
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.sky,
            checkedTrackColor = colors.blue,
            checkedBorderColor = colors.borderStrong,
            uncheckedThumbColor = colors.textFaint,
            uncheckedTrackColor = colors.cardHigh,
            uncheckedBorderColor = colors.border,
            disabledCheckedThumbColor = colors.sky.copy(alpha = 0.5f),
            disabledCheckedTrackColor = colors.blue.copy(alpha = 0.35f),
            disabledCheckedBorderColor = colors.border,
            disabledUncheckedThumbColor = colors.textFaint.copy(alpha = 0.5f),
            disabledUncheckedTrackColor = colors.cardHigh.copy(alpha = 0.5f),
            disabledUncheckedBorderColor = colors.border,
        ),
    )
}

@Composable
fun NuboTopBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
) {
    val colors = nuboColors()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.background,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = if (showBackButton) 8.dp else 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = colors.textMain,
                    )
                }
                Text(
                    text = title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.nubo_logo),
                    contentDescription = "NUBO VPN",
                    modifier = Modifier.height(48.dp),
                    contentScale = ContentScale.FillHeight,
                )
                Spacer(modifier = Modifier.weight(1f))
                title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textDim,
                    )
                }
            }
        }
    }
}

@Composable
fun NuboBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()

    // Фиксированные константы — НЕ меняются при переключении вкладок
    val barHeight = 64.dp
    val buttonSize = 68.dp
    val labelHeight = 20.dp       // одинаковая зона подписи у всех пунктов
    val totalHeight = buttonSize + labelHeight  // 88.dp, кнопка торчит на 24.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),   // строго фиксированная высота
    ) {
        // ── Фон + верхняя линия бара ────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter)
                .background(colors.backgroundDeep),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
                    .align(Alignment.TopCenter),
            )
        }

        // ── Боковые пункты меню (выровнены по центру бара) ──────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SideNavItem(
                label = "Серверы",
                icon = Icons.Default.Dns,
                selected = selectedTab == AppTab.Servers,
                barHeight = barHeight,
                labelHeight = labelHeight,
                onClick = { onTabSelected(AppTab.Servers) },
            )
            // Пустое место под кнопку — такого же размера, чтобы боковые
            // пункты симметрично расходились по краям
            Spacer(modifier = Modifier.width(buttonSize + 16.dp))
            SideNavItem(
                label = "Настройки",
                icon = Icons.Default.Settings,
                selected = selectedTab == AppTab.Settings,
                barHeight = barHeight,
                labelHeight = labelHeight,
                onClick = { onTabSelected(AppTab.Settings) },
            )
        }

        // ── Центральная кнопка (поверх всего, по горизонтали центр) ──
        CenterNavItem(
            label = "Подключение",
            selected = selectedTab == AppTab.Home,
            buttonSize = buttonSize,
            labelHeight = labelHeight,
            onClick = { onTabSelected(AppTab.Home) },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun SideNavItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    barHeight: androidx.compose.ui.unit.Dp,
    labelHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val colors = nuboColors()

    // Плавные переходы цвета вместо резкого переключения
    val fg by animateColorAsState(
        targetValue = if (selected) colors.blue else colors.textFaint,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "sideNavFg",
    )
    val iconBg by animateColorAsState(
        targetValue = if (selected) colors.blue.copy(alpha = 0.15f) else Color.Transparent,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "sideNavBg",
    )

    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .height(barHeight)
            .clickable(
                interactionSource = interactionSource,
                // Мягкая круглая подсветка вместо прямоугольной
                indication = ripple(bounded = false, radius = 36.dp, color = colors.blue),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Иконка занимает всё место над зоной подписи
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = fg,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        // Подпись — в зоне фиксированной высоты, одинаковой у всех пунктов
        Box(
            modifier = Modifier.height(labelHeight),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
            )
        }
    }
}

@Composable
private fun CenterNavItem(
    label: String,
    selected: Boolean,
    buttonSize: androidx.compose.ui.unit.Dp,
    labelHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()

    // Пульсирующее свечение — только визуальный эффект, не влияет на layout
    val pulseTransition = rememberInfiniteTransition(label = "navPulse")
    val glowAlpha by pulseTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val labelColor by animateColorAsState(
        targetValue = if (selected) colors.sky else colors.textFaint,
        animationSpec = tween(durationMillis = 250),
        label = "centerLabelColor",
    )

    // Кнопка сверху, подпись — в зоне той же высоты, что у боковых пунктов
    Column(
        modifier = modifier.width(buttonSize + 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // ── Кнопка (фиксированный размер, свечение через drawBehind) ──
        Box(
            modifier = Modifier
                .size(buttonSize)
                .drawBehind {
                    if (selected) {
                        val radius = size.minDimension * 0.85f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1A7FFF).copy(alpha = glowAlpha * 0.6f),
                                    Color(0xFF1A7FFF).copy(alpha = 0f),
                                ),
                                radius = radius * 1.3f,
                            ),
                            radius = radius * 1.3f,
                        )
                    }
                }
                .clip(CircleShape)
                .background(colors.backgroundDeep)
                .border(
                    1.5.dp,
                    if (selected) colors.cyan.copy(alpha = 0.45f) else colors.border,
                    CircleShape,
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Внутренний заполненный круг
            Box(
                modifier = Modifier
                    .size(buttonSize * 0.79f)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            Brush.linearGradient(listOf(Color(0xFF1E5FFF), Color(0xFF0A1A8A)))
                        } else {
                            Brush.linearGradient(listOf(Color(0xFF12213A), Color(0xFF070D1A)))
                        },
                    )
                    .border(
                        2.dp,
                        if (selected) colors.borderStrong else Color(0xFF1A3060).copy(alpha = 0.5f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = label,
                    tint = if (selected) Color.White else colors.textDim,
                    modifier = Modifier.size(26.dp),
                )
            }
        }

        // ── Подпись — та же зона высоты, что у боковых пунктов ──
        Box(
            modifier = Modifier
                .height(labelHeight)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onClick,
                ),
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
            )
        }
    }
}

/**
 * Большая круглая кнопка подключения в стиле NUBO:
 * тёмный круг → две встречные дуги при подключении → неоновое «дыхание», когда подключено.
 */
@Composable
fun NuboConnectButton(
    vpnStatus: VpnStatus,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 200.dp,
) {
    val colors = nuboColors()
    val isConnected = vpnStatus == VpnStatus.Started
    val isConnecting = vpnStatus == VpnStatus.Starting
    val isDisconnecting = vpnStatus == VpnStatus.Stopping
    val isBusy = isConnecting || isDisconnecting

    val connectedProgress by animateFloatAsState(
        targetValue = if (isConnected) 1f else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "connectedProgress",
    )
    val busyProgress by animateFloatAsState(
        targetValue = if (isBusy) 1f else 0f,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "busyProgress",
    )

    // «Дыхание» — медленная пульсация свечения и лёгкое масштабирование
    val breathTransition = rememberInfiniteTransition(label = "connectBreath")
    val breath by breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breath",
    )

    val idleBorderColor = if (!enabled) Color(0xFF1A2A44) else Color(0xFF1A3C78).copy(alpha = 0.5f)
    val busyBorderColor = if (isDisconnecting) colors.red.copy(alpha = 0.5f) else colors.blue.copy(alpha = 0.5f)
    val connectedBorderColor = colors.cyan.copy(alpha = 0.55f + breath * 0.30f)
    val borderColor = lerp(
        start = lerp(idleBorderColor, busyBorderColor, busyProgress),
        stop = connectedBorderColor,
        fraction = connectedProgress,
    )
    val buttonScale = 1f + connectedProgress * breath * 0.02f

    val busyBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF112888), Color(0xFF070D1A)),
        center = Offset(150f, 120f),
        radius = 400f,
    )
    val idleBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF0F1E3A), Color(0xFF070D1A)),
        center = Offset(150f, 120f),
        radius = 400f,
    )
    val connectedBrush = Brush.radialGradient(
        colors = listOf(Color(0xFF1E60FF), Color(0xFF081640)),
        center = Offset(0.38f, 0.32f).let { Offset(it.x * 400f, it.y * 400f) },
        radius = 400f,
    )
    val busyIconTint = if (isDisconnecting) colors.red.copy(alpha = 0.85f) else Color(0xFF4A80E0)
    val idleIconTint = if (!enabled) Color(0xFF1E3050) else Color(0xFF2A4A7A)
    val iconTint = lerp(
        start = lerp(idleIconTint, busyIconTint, busyProgress),
        stop = colors.sky,
        fraction = connectedProgress,
    )
    val innerBorderColor = lerp(
        Color(0xFF28508C).copy(alpha = 0.3f),
        colors.sky.copy(alpha = 0.4f),
        connectedProgress,
    )

    Box(
        modifier = modifier.size(diameter),
        contentAlignment = Alignment.Center,
    ) {
        if (busyProgress > 0.01f || connectedProgress > 0.01f) {
            val connectingGlow = 0.14f * busyProgress * (1f - connectedProgress)
            val connectedGlow = (0.22f + breath * 0.16f) * connectedProgress
            val glowAlpha = connectingGlow + connectedGlow
            Box(
                modifier = Modifier
                    .size(diameter)
                    .background(
                        Brush.radialGradient(
                            listOf(colors.blue.copy(alpha = glowAlpha), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
        }

        if (connectedProgress > 0.01f) {
            ConnectPulseRings(
                color = colors.cyan,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(connectedProgress),
            )
        }

        if (busyProgress > 0.01f) {
            val busyColor = if (isDisconnecting) colors.red else colors.cyan
            Box(modifier = Modifier.alpha(busyProgress)) {
                SpinningArc(
                    color = busyColor,
                    durationMillis = 1_100,
                    sweepAngle = 110f,
                    modifier = Modifier.size(diameter * 0.92f),
                )
                SpinningArc(
                    color = busyColor.copy(alpha = 0.55f),
                    durationMillis = 1_600,
                    sweepAngle = 70f,
                    reverse = true,
                    modifier = Modifier.size(diameter * 0.83f),
                )
            }
        }

        if (connectedProgress > 0.01f) {
            Canvas(
                modifier = Modifier
                    .size(diameter * 0.92f)
                    .alpha(connectedProgress),
            ) {
                drawCircle(
                    color = colors.cyan.copy(alpha = (0.35f + breath * 0.30f) * connectedProgress),
                    radius = size.minDimension / 2f - 1.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx()),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(diameter * 0.78f)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
                .clip(CircleShape)
                .clickable(enabled = enabled && !isBusy, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha((1f - connectedProgress) * (1f - busyProgress))
                    .background(idleBrush),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha((1f - connectedProgress) * busyProgress)
                    .background(busyBrush),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(connectedProgress)
                    .background(connectedBrush),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, borderColor, CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(diameter * 0.57f)
                    .clip(CircleShape)
                    .border(1.dp, innerBorderColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = "Подключить",
                    tint = iconTint,
                    modifier = Modifier.size(diameter * 0.22f),
                )
            }
        }
    }
}

@Composable
private fun SpinningArc(
    color: Color,
    modifier: Modifier = Modifier,
    durationMillis: Int = 1_200,
    sweepAngle: Float = 100f,
    reverse: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "arcSpin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reverse) -360f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "arcAngle",
    )
    Canvas(modifier = modifier.rotate(angle)) {
        val stroke = 2.5.dp.toPx()
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(color.copy(alpha = 0f), color),
            ),
            startAngle = 0f,
            sweepAngle = sweepAngle,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            topLeft = Offset(stroke / 2, stroke / 2),
            size = androidx.compose.ui.geometry.Size(
                size.width - stroke,
                size.height - stroke,
            ),
        )
    }
}

@Composable
private fun ConnectPulseRings(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "connectPulse")
    val pulse1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse1",
    )
    val pulse2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_400, delayMillis = 1_200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse2",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val center = center
        val baseRadius = size.minDimension / 2f * 0.82f
        val strokeWidth = 1.5.dp.toPx()

        listOf(pulse1, pulse2).forEach { progress ->
            val radius = baseRadius * (1f + progress * 0.35f)
            val alpha = (1f - progress) * 0.5f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth),
            )
        }
    }
}

fun formatConnectionDuration(elapsedMs: Long): String {
    val totalSeconds = (elapsedMs / 1_000).coerceAtLeast(0)
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}

fun statusHeadline(vpnStatus: VpnStatus): String = when (vpnStatus) {
    VpnStatus.Stopped -> "ОТКЛЮЧЕНО"
    VpnStatus.Starting -> "ПОДКЛЮЧЕНИЕ…"
    VpnStatus.Started -> "ПОДКЛЮЧЕНО"
    VpnStatus.Stopping -> "ОТКЛЮЧЕНИЕ…"
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = nuboColors().textFaint,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

/** Карточка в стиле NUBO: тёмная поверхность с мягкой синей обводкой. */
@Composable
fun NuboCard(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(16.dp)
    val background = if (selected) {
        Modifier.background(
            Brush.linearGradient(
                listOf(
                    colors.blue.copy(alpha = 0.25f),
                    colors.blueDeep.copy(alpha = 0.30f),
                ),
            ),
            shape,
        )
    } else {
        Modifier.background(colors.card, shape)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .then(background)
            .border(1.dp, if (selected) colors.borderStrong else colors.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        content()
    }
}

/** Цветной бейдж пинга. */
@Composable
fun PingBadge(
    pingText: String,
    pingMs: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val color = when {
        pingMs != null -> colors.pingColor(pingMs)
        pingText == "N/A" -> colors.red
        else -> colors.textDim
    }
    Text(
        text = pingText,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier,
    )
}

/** Столбики уровня сигнала (по пингу). */
@Composable
fun SignalBars(
    pingMs: Int?,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val level = when {
        pingMs == null -> 0
        pingMs <= 100 -> 4
        pingMs <= 200 -> 3
        pingMs <= 400 -> 2
        else -> 1
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((4 + i * 2.5).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i < level) colors.cyan else colors.textFaint.copy(alpha = 0.3f)),
            )
        }
    }
}

@Composable
fun SelectedCheck(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(nuboColors().blue),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Выбран",
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerListCard(
    display: ServerDisplay,
    ping: PingState?,
    selected: Boolean,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(16.dp)
    val selectedProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "serverCardSelected",
    )
    val cardColor by animateColorAsState(
        targetValue = lerp(colors.card, colors.blue.copy(alpha = 0.22f), selectedProgress),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "serverCardBg",
    )
    val borderColor by animateColorAsState(
        targetValue = lerp(colors.border, colors.borderStrong, selectedProgress),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "serverCardBorder",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardColor, shape)
            .border(1.dp, borderColor, shape)
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ServerListFlag(flag = display.flag, height = 30.dp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = display.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (display.subtitle.isNotBlank()) {
                    Text(
                        text = " | ${display.subtitle}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textDim,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = display.protocolLabel,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textFaint,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            ServerPingIndicator(ping = ping)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                AnimatedVisibility(
                    visible = selected,
                    enter = fadeIn(tween(220, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(180, easing = FastOutSlowInEasing)),
                ) {
                    SelectedCheck()
                }
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) "Убрать из избранного" else "В избранное",
                    tint = if (isFavorite) colors.yellow else colors.textFaint,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleFavorite),
                )
            }
        }
    }
}

@Composable
fun ServerPingIndicator(
    ping: PingState?,
    modifier: Modifier = Modifier,
) {
    val pingText = when (ping) {
        null -> "—"
        PingState.Loading -> "…"
        is PingState.Result -> "${ping.latencyMs} ms"
        PingState.Unreachable -> "N/A"
    }
    val pingMs = (ping as? PingState.Result)?.latencyMs

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        PingBadge(pingText = pingText, pingMs = pingMs)
        SignalBars(pingMs = pingMs)
    }
}

@Composable
fun SubscriptionExpiredCard(
    onTelegramBotClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.errorContainer.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.red.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Подписка истекла",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.red,
            )
            Text(
                text = "Продлите подписку, чтобы снова пользоваться VPN",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMid,
            )
            SubscriptionActionButton(
                text = "Продлить в боте",
                icon = Icons.AutoMirrored.Filled.Send,
                onClick = onTelegramBotClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun SubscriptionCard(
    onPasteLinkClick: () -> Unit,
    onTelegramBotClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    NuboCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Добавить подписку",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain,
            )
            Column(
                modifier = Modifier.padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SubscriptionActionButton(
                    text = "Вставить ссылку",
                    icon = Icons.Default.ContentPaste,
                    onClick = onPasteLinkClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                SubscriptionActionButton(
                    text = "Купить в боте",
                    icon = Icons.AutoMirrored.Filled.Send,
                    onClick = onTelegramBotClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun SubInfoTextBlock(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = colors.textMain,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
    }
}

@Composable
fun SubInfoTextBanner(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White, shape)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.blue,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SubInfoLinkBanner(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.blue, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SubscriptionActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(listOf(Color(0xFF1A4FFF), Color(0xFF0A2A9A))),
                shape,
            )
            .border(1.dp, colors.borderStrong, shape)
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ConfigRefreshButton(
    isRefreshing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    IconButton(
        onClick = onClick,
        enabled = enabled && !isRefreshing,
        modifier = modifier.size(44.dp),
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = colors.blue,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Обновить конфиг",
                tint = if (enabled) colors.textMid else colors.textFaint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
fun PingTestButton(
    isPinging: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = nuboColors()
    IconButton(
        onClick = onClick,
        enabled = enabled && !isPinging,
        modifier = modifier.size(44.dp),
    ) {
        if (isPinging) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = colors.blue,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Speed,
                contentDescription = "Проверить пинг",
                tint = if (enabled) colors.textMid else colors.textFaint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
