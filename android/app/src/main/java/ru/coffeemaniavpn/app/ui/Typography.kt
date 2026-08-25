package ru.coffeemaniavpn.app.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R

/**
 * Inter Bold — открытый аналог San Francisco (системный шрифт macOS / iOS).
 * SF Pro нельзя легально встраивать в Android-приложения.
 */
val CoffemaniaFontFamily = FontFamily(
    Font(R.font.inter_bold, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val TextShadowSoft = Shadow(
    color = Color(0x2E3D1C1C),
    offset = Offset(0f, 1f),
    blurRadius = 2.5f,
)

private val TextShadowMedium = Shadow(
    color = Color(0x403D1C1C),
    offset = Offset(0f, 1.5f),
    blurRadius = 3.5f,
)

private val TextShadowStrong = Shadow(
    color = Color(0x523D1C1C),
    offset = Offset(0f, 2f),
    blurRadius = 4.5f,
)

private fun TextStyle.withTextShadow(level: Shadow): TextStyle = copy(shadow = level)

private fun boldTextStyle(
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    shadow: Shadow = TextShadowSoft,
) = TextStyle(
    fontFamily = CoffemaniaFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
).withTextShadow(shadow)

val CoffemaniaTypography = Typography(
    headlineLarge = boldTextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).sp,
        shadow = TextShadowStrong,
    ),
    headlineMedium = boldTextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        shadow = TextShadowStrong,
    ),
    titleMedium = boldTextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        shadow = TextShadowMedium,
    ),
    bodyLarge = boldTextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = boldTextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = boldTextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = boldTextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
        shadow = TextShadowMedium,
    ),
    labelMedium = boldTextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
