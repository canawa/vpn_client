package ru.coffeemaniavpn.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import ru.coffeemaniavpn.app.R
import ru.coffeemaniavpn.app.data.formatTrafficBytes
import ru.coffeemaniavpn.app.vpn.VpnStatus
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class ConnectUiStatus { Off, Busy, On }

@Composable
fun ClevLogo(
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data("file:///android_asset/logo_mark.svg")
            .decoderFactory(SvgDecoder.Factory())
            .build(),
        contentDescription = "ClevVPN",
        modifier = modifier.height(height),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun YellowCircleIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
    circleSize: Dp = 34.dp,
    content: @Composable () -> Unit,
) {
    val colors = coffemaniaColors()
    Box(
        modifier = modifier
            .size(circleSize)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(listOf(colors.yellow, colors.amber)),
            )
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(circleSize * 0.47f),
                strokeWidth = 2.dp,
                color = Color.Black,
            )
        } else {
            content()
        }
    }
}

@Composable
fun ClevLogoFull(
    modifier: Modifier = Modifier,
    logoHeight: Dp = 22.dp,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ClevLogo(height = logoHeight)
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = colors.espresso, fontWeight = FontWeight.Bold)) {
                    append("Clev")
                }
                withStyle(SpanStyle(color = colors.yellow, fontWeight = FontWeight.Bold)) {
                    append("VPN")
                }
            },
            fontSize = (logoHeight.value * 0.82f).sp,
        )
    }
}

@Composable
fun ClevCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.cappuccino)
            .border(1.dp, colors.latte, shape),
    ) {
        content()
    }
}

@Composable
fun StatusGlow(
    status: ConnectUiStatus,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val target = when (status) {
        ConnectUiStatus.Off -> colors.mocha.copy(alpha = 0.05f)
        ConnectUiStatus.Busy -> CoffemaniaColors.LogoAmber.copy(alpha = 0.16f)
        ConnectUiStatus.On -> colors.logoYellow.copy(alpha = 0.20f)
    }
    val glowColor by animateColorAsState(
        targetValue = target,
        animationSpec = ClevMotion.statusGlowColorSpec,
        label = "statusGlow",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    radius = 900f,
                ),
            ),
    )
}

@Composable
fun ClevConnectButton(
    vpnStatus: VpnStatus,
    connectionElapsedMs: Long,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
) {
    val colors = coffemaniaColors()
    val uiStatus = when {
        !enabled && vpnStatus == VpnStatus.Stopped -> ConnectUiStatus.Off
        vpnStatus == VpnStatus.Started -> ConnectUiStatus.On
        vpnStatus == VpnStatus.Starting || vpnStatus == VpnStatus.Stopping -> ConnectUiStatus.Busy
        else -> ConnectUiStatus.Off
    }
    val showRingSpinner = uiStatus == ConnectUiStatus.Busy

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) ClevMotion.pressScale else 1f,
        animationSpec = ClevMotion.pressSpring,
        label = "press",
    )

    val ringFill = remember { Animatable(if (uiStatus == ConnectUiStatus.On) 1f else 0f) }
    val cometAngle = remember { Animatable(-90f) }
    var showComet by remember { mutableStateOf(false) }
    var showBurst by remember { mutableStateOf(false) }

    var skipInitialConnectAnimation by remember { mutableStateOf(uiStatus == ConnectUiStatus.On) }

    LaunchedEffect(uiStatus) {
        when (uiStatus) {
            ConnectUiStatus.On -> {
                if (skipInitialConnectAnimation) {
                    ringFill.snapTo(1f)
                    skipInitialConnectAnimation = false
                    return@LaunchedEffect
                }
                ringFill.snapTo(0f)
                cometAngle.snapTo(-90f)
                showComet = true
                launch {
                    cometAngle.animateTo(810f, ClevMotion.connectCometSpec)
                }
                launch {
                    delay(ClevMotion.connectRingFillDelayMs)
                    ringFill.animateTo(1f, ClevMotion.connectRingFillSpec)
                    showComet = false
                }
                showBurst = true
                delay(ClevMotion.connectBurstDurationMs)
                showBurst = false
            }
            ConnectUiStatus.Off -> {
                skipInitialConnectAnimation = false
                ringFill.animateTo(0f, ClevMotion.disconnectRingSpec)
                cometAngle.snapTo(-90f)
                showComet = false
                showBurst = false
            }
            ConnectUiStatus.Busy -> {
                ringFill.animateTo(0f, ClevMotion.disconnectRingSpec)
                showComet = false
                showBurst = false
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "spin")
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(ClevMotion.busySpinConnectMs, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "spinAngle",
    )

    val plateOnProgress by animateFloatAsState(
        targetValue = if (uiStatus == ConnectUiStatus.On) 1f else 0f,
        animationSpec = ClevMotion.plateOnSpec,
        label = "plateOn",
    )

    val ringSize = size + 36.dp

    Box(
        modifier = modifier
            .size(ringSize + 28.dp)
            .scale(scale)
            .clickable(
                enabled = enabled || vpnStatus == VpnStatus.Started,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showBurst) {
            ConnectBurst(
                diameter = ringSize,
                active = true,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Canvas(modifier = Modifier.size(ringSize + 20.dp)) {
            val rings = listOf(0.98f, 0.88f, 0.78f)
            rings.forEachIndexed { index, ringScale ->
                val r = this.size.minDimension / 2f * ringScale
                drawCircle(
                    color = Color(0xFF2A2A31).copy(alpha = 0.35f - index * 0.08f),
                    radius = r,
                    style = Stroke(width = 1.dp.toPx()),
                    center = center,
                )
            }
        }

        Canvas(modifier = Modifier.size(ringSize)) {
            val stroke = 3.dp.toPx()
            drawCircle(
                color = Color(0xFF2A2A31).copy(alpha = 0.55f),
                style = Stroke(width = stroke),
            )

            if (uiStatus == ConnectUiStatus.On && ringFill.value > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color(0xFFFAC300), Color(0xFFE39A00), Color(0xFFFAC300)),
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * ringFill.value,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            if (showComet && ringFill.value < 1f) {
                rotate(cometAngle.value) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(Color.Transparent, Color(0xFFFAC300).copy(alpha = 0.7f), Color.White),
                        ),
                        startAngle = -90f,
                        sweepAngle = 360f * 0.18f,
                        useCenter = false,
                        topLeft = Offset.Zero,
                        size = Size(this.size.width, this.size.height),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            if (showRingSpinner) {
                rotate(spin) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color.Transparent,
                                Color(0xFFE39A00).copy(alpha = 0.5f),
                                Color.White,
                            ),
                        ),
                        startAngle = -90f,
                        sweepAngle = 72f,
                        useCenter = false,
                        topLeft = Offset.Zero,
                        size = Size(this.size.width, this.size.height),
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        }

        // Кнопка: утопленный 3D-колодец (свет сверху-слева).
        Box(
            modifier = Modifier
                .size(size)
                .shadow(14.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.55f))
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            val plateYellow = Color(0xFFF5C400)
            val plateAmber = Color(0xFFE8A200)
            val bezelHi = lerp(Color(0xFF4A4A54), Color(0xFFFFE082), plateOnProgress * 0.7f)
            val bezelLo = lerp(Color(0xFF141418), Color(0xFFD48900), plateOnProgress * 0.85f)
            val wellTop = lerp(Color(0xFF07070A), Color(0xFFA87400), plateOnProgress * 0.7f)
            val wellBot = lerp(Color(0xFF22222C), Color(0xFFE8B020), plateOnProgress * 0.85f)
            val floorHi = lerp(Color(0xFF2C2C36), plateYellow, plateOnProgress)
            val floorLo = lerp(Color(0xFF16161C), plateAmber, plateOnProgress)
            val insetShadowAlpha = 0.55f * (1f - plateOnProgress * 0.35f)
            val rimGlowAlpha = 0.06f + plateOnProgress * 0.06f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val d = this.size.minDimension
                val r = d / 2f

                // Скос обода: свет сверху-слева → тень снизу-справа
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(bezelHi, bezelLo),
                        start = Offset(0f, 0f),
                        end = Offset(d, d),
                    ),
                )

                // Стенка колодца: тёмный верх = «углубление вниз»
                val wellInset = d * 0.055f
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(wellTop, wellBot),
                        start = Offset(d * 0.15f, 0f),
                        end = Offset(d * 0.85f, d),
                    ),
                    radius = r - wellInset,
                )

                // Inner-shadow по верхней кромке (слабее на жёлтом, чтобы не «грязнить»)
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.35f to Color.Black.copy(alpha = insetShadowAlpha),
                        0.55f to Color.Black.copy(alpha = insetShadowAlpha),
                        0.75f to Color.Transparent,
                        1.0f to Color.Transparent,
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(wellInset, wellInset),
                    size = Size(d - wellInset * 2f, d - wellInset * 2f),
                    style = Stroke(width = d * 0.07f),
                )

                // Пол — блик сверху-слева
                val floorInset = d * 0.13f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(floorHi, floorLo),
                        center = Offset(d * 0.38f, d * 0.34f),
                        radius = r - floorInset * 0.55f,
                    ),
                    radius = r - floorInset,
                )

                // Мягкий блик на нижнем правом ободе
                drawArc(
                    color = Color.White.copy(alpha = rimGlowAlpha),
                    startAngle = 20f,
                    sweepAngle = 70f,
                    useCenter = false,
                    style = Stroke(width = d * 0.035f, cap = StrokeCap.Round),
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = size * 0.08f),
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = when {
                        uiStatus == ConnectUiStatus.On -> Color.Black.copy(alpha = 0.75f)
                        uiStatus == ConnectUiStatus.Busy -> colors.logoYellow
                        else -> colors.mocha
                    },
                    modifier = Modifier.size(size * 0.22f),
                )
                Spacer(modifier = Modifier.height(if (uiStatus == ConnectUiStatus.On) 7.dp else 6.dp))
                if (uiStatus == ConnectUiStatus.On) {
                    Text(
                        text = stringResource(R.string.clev_connected),
                        color = Color.Black.copy(alpha = 0.45f),
                        fontSize = (size.value * 0.058f).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = formatSession(connectionElapsedMs),
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = (size.value * 0.11f).sp,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    val label = when {
                        uiStatus == ConnectUiStatus.Busy -> stringResource(R.string.clev_connecting)
                        else -> stringResource(R.string.clev_start)
                    }
                    val isLongLabel = uiStatus == ConnectUiStatus.Busy
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isLongLabel) size * 0.02f else 0.dp),
                        color = colors.mocha,
                        fontSize = (size.value * if (isLongLabel) 0.055f else 0.075f).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (isLongLabel) 0.sp else 1.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
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

fun VpnStatus.toConnectUi(): ConnectUiStatus = when (this) {
    VpnStatus.Started -> ConnectUiStatus.On
    VpnStatus.Starting, VpnStatus.Stopping -> ConnectUiStatus.Busy
    VpnStatus.Stopped -> ConnectUiStatus.Off
}

/** Круг-индикатор выбора: жёлтое кольцо + жёлтая точка / тонкое серое кольцо. */
@Composable
fun ClevSelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp,
) {
    val colors = coffemaniaColors()
    Canvas(modifier = modifier.size(size)) {
        val diameter = this.size.minDimension
        val radius = diameter / 2f
        if (selected) {
            drawCircle(
                color = colors.yellow,
                radius = radius - diameter * 0.04f,
                style = Stroke(width = diameter * 0.09f),
            )
            drawCircle(
                color = colors.yellow,
                radius = diameter * 0.22f,
            )
        } else {
            drawCircle(
                color = colors.latte.copy(alpha = 0.5f),
                radius = radius - diameter * 0.045f,
                style = Stroke(width = diameter * 0.07f),
            )
        }
    }
}

@Composable
fun ClevFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .then(
                if (selected) {
                    Modifier.background(Brush.linearGradient(listOf(colors.yellow, colors.amber)))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (selected) Color.Black else colors.mocha,
        )
    }
}

/** Бейдж протокола — как QuickServerRow в Components.swift. */
@Composable
fun ProtocolLabelBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(50)
    Text(
        text = label,
        color = colors.yellow,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceVariant, shape)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/** Название + бейдж протокола; жёлтая звезда только если сервер в избранном. */
@Composable
fun ServerTitleWithProtocolBadge(
    title: String,
    protocolLabel: String,
    favorite: Boolean,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()

    SubcomposeLayout(modifier = modifier) { constraints ->
        val gap = 4.dp.roundToPx()

        val starPlaceable = if (favorite) {
            val starSize = 22.dp.roundToPx()
            subcompose("star") {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onFavoriteClick)
                        .semantics { role = Role.Button },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = stringResource(R.string.clev_remove_favorite),
                        tint = colors.yellow,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }.first().measure(Constraints.fixed(starSize, starSize))
        } else {
            null
        }

        val badgePlaceable = subcompose("badge") {
            ProtocolLabelBadge(label = protocolLabel)
        }.first().measure(Constraints())

        val starWidth = starPlaceable?.width ?: 0
        val gaps = if (starPlaceable != null) gap * 2 else gap
        val textMaxWidth = (constraints.maxWidth - starWidth - badgePlaceable.width - gaps)
            .coerceAtLeast(0)
        val textPlaceable = subcompose("text") {
            Text(
                text = title,
                color = colors.espresso,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }.first().measure(Constraints(maxWidth = textMaxWidth))

        val height = maxOf(
            starPlaceable?.height ?: 0,
            textPlaceable.height,
            badgePlaceable.height,
        )
        layout(constraints.maxWidth, height) {
            var x = 0
            starPlaceable?.let {
                it.place(x, (height - it.height) / 2)
                x += it.width + gap
            }
            textPlaceable.place(x, (height - textPlaceable.height) / 2)
            x += textPlaceable.width + gap
            badgePlaceable.place(x, (height - badgePlaceable.height) / 2)
        }
    }
}

/** Пинг с цветовой точкой — как PingLabel в Components.swift. */
@Composable
fun PingLabel(
    ms: Int,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(CoffemaniaColors.pingColor(ms)),
        )
        Text(
            text = stringResource(R.string.clev_ping_ms, ms),
            color = colors.mocha,
            fontSize = 12.sp,
        )
    }
}

/** Иконка info-bar без жёлтого круга — как на iOS/macOS. */
@Composable
fun InfoBarIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = coffemaniaColors()
    Box(
        modifier = modifier
            .size(28.dp)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = colors.yellow,
            )
        } else {
            content()
        }
    }
}

private val ClevSwitchThumbColor = Color(0xFFF4F4F5)

/** Плавный pill-переключатель: жёлтый трек и светлый бегунок. */
@Composable
fun ClevAnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = coffemaniaColors()
    val trackWidth = 42.dp
    val trackHeight = 24.dp
    val thumbSize = 20.dp
    val trackPadding = 2.dp

    val thumbProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "clevSwitchThumb",
    )
    val trackColor by animateColorAsState(
        targetValue = when {
            !enabled && checked -> colors.yellow.copy(alpha = 0.45f)
            !enabled -> colors.latte.copy(alpha = 0.6f)
            checked -> colors.yellow
            else -> colors.latte
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "clevSwitchTrack",
    )
    val thumbColor = if (enabled) ClevSwitchThumbColor else ClevSwitchThumbColor.copy(alpha = 0.65f)

    BoxWithConstraints(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .clip(RoundedCornerShape(percent = 50))
            .background(trackColor)
            .semantics { role = Role.Switch }
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        val travel = maxWidth - thumbSize - trackPadding * 2
        Box(
            modifier = Modifier
                .padding(trackPadding)
                .size(thumbSize)
                .offset(x = travel * thumbProgress)
                .clip(CircleShape)
                .background(thumbColor),
        )
    }
}

/** Переключатель + корзина в общей тёмной «капсуле». */
@Composable
fun ClevRuleActionGroup(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceVariant)
            .padding(start = 5.dp, end = 2.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ClevAnimatedSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete)
                .padding(5.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clev_trash),
                contentDescription = stringResource(R.string.clev_rule_delete),
                tint = colors.error.copy(alpha = 0.88f),
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

fun formatTrafficInfoLine(used: Long, total: Long): String {
    val usedText = formatTrafficBytesLocalized(used.coerceAtLeast(0))
    return if (total > 0) {
        "$usedText / ${formatTrafficBytesLocalized(total)}"
    } else {
        usedText
    }
}

/** Пара стрелок ↑↓ для блока трафика. */
@Composable
private fun InfoBarTrafficArrows(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-1).dp),
    ) {
        Icon(
            imageVector = Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp),
        )
        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp),
        )
    }
}

private val InfoBarActionCircleSize = 20.dp
private val InfoBarActionIconSize = 11.dp

/** Жёлтая круглая кнопка info-bar — плоский жёлтый фон. */
@Composable
private fun InfoBarYellowCircleButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    loading: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = coffemaniaColors()
    Box(
        modifier = modifier
            .size(InfoBarActionCircleSize)
            .clip(CircleShape)
            .background(colors.yellow)
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(InfoBarActionIconSize),
                strokeWidth = 1.25.dp,
                color = Color.Black,
            )
        } else {
            content()
        }
    }
}

/** Левая часть info-bar: жёлтый круг + стрелки + трафик (как на macOS). */
@Composable
fun InfoBarTrafficCluster(
    onPingClick: () -> Unit,
    pinging: Boolean,
    used: Long? = null,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        InfoBarYellowCircleButton(
            onClick = onPingClick,
            enabled = !pinging,
            loading = pinging,
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier
                    .size(InfoBarActionIconSize + 5.dp)
                    .rotate(30f),
            )
        }
        if (used != null) {
            InfoBarTrafficArrows(tint = colors.mocha)
            Text(
                text = formatTrafficBytesLocalized(used.coerceAtLeast(0)),
                color = colors.mocha,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Правая часть info-bar: календарь + дата + жёлтый refresh (как на macOS). */
@Composable
fun InfoBarExpiryCluster(
    expireLabel: String?,
    onRefreshClick: () -> Unit,
    refreshing: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (!expireLabel.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = colors.mocha,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = expireLabel,
                color = colors.mocha,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        InfoBarYellowCircleButton(
            onClick = onRefreshClick,
            enabled = !refreshing,
            loading = refreshing,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(InfoBarActionIconSize),
            )
        }
    }
}

/** Текст announce из подписки — как на macOS: крупные строки по центру, последняя мельче. */
@Composable
fun SubscriptionAnnounceContent(
    text: String,
    modifier: Modifier = Modifier,
    lineSpacing: Dp = 3.dp,
    hintSpacing: Dp = 2.dp,
    compact: Boolean = false,
) {
    val colors = coffemaniaColors()
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    if (lines.isEmpty()) return

    val mainLines = if (lines.size > 1) lines.dropLast(1) else lines
    val hintLine = lines.takeIf { it.size > 1 }?.last()
    val mainSize = if (compact) 12.sp else 15.sp
    val mainLineHeight = if (compact) 15.sp else 20.sp
    val hintSize = if (compact) 10.sp else 12.sp
    val hintLineHeight = if (compact) 13.sp else 16.sp

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(lineSpacing),
    ) {
        mainLines.forEach { line ->
            Text(
                text = line,
                color = colors.espresso,
                fontSize = mainSize,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = mainLineHeight,
            )
        }
        hintLine?.let { line ->
            Text(
                text = line,
                color = colors.espresso.copy(alpha = 0.92f),
                fontSize = hintSize,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = hintLineHeight,
                modifier = Modifier.padding(top = hintSpacing),
            )
        }
    }
}

private val RuLocale = Locale("ru", "RU")

fun formatSubscriptionTraffic(used: Long, total: Long): String {
    val usedText = formatTrafficBytesLocalized(used.coerceAtLeast(0))
    return if (total > 0) {
        "$usedText / ${formatTrafficBytesLocalized(total)}"
    } else {
        usedText
    }
}

private fun formatTrafficBytesLocalized(bytes: Long): String {
    if (bytes <= 0) return if (Locale.getDefault().language == "ru") "0 Б" else "0 B"
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    val ru = Locale.getDefault().language == "ru"
    return when {
        bytes >= gb -> if (ru) {
            String.format(RuLocale, "%.2f ГБ", bytes / gb)
        } else {
            String.format(Locale.US, "%.2f GB", bytes / gb)
        }
        bytes >= mb -> if (ru) {
            String.format(RuLocale, "%.1f МБ", bytes / mb)
        } else {
            String.format(Locale.US, "%.1f MB", bytes / mb)
        }
        bytes >= kb -> if (ru) {
            String.format(RuLocale, "%.1f КБ", bytes / kb)
        } else {
            String.format(Locale.US, "%.1f KB", bytes / kb)
        }
        else -> if (ru) "$bytes Б" else "$bytes B"
    }
}

@Deprecated("Use formatSubscriptionTraffic", ReplaceWith("formatSubscriptionTraffic(used, total)"))
fun formatSubscriptionTrafficRu(used: Long, total: Long): String = formatSubscriptionTraffic(used, total)
