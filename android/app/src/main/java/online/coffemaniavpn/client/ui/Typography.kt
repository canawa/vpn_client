package online.coffemaniavpn.client.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

val CoffemaniaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).sp,
    ).withTextShadow(TextShadowStrong),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ).withTextShadow(TextShadowStrong),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ).withTextShadow(TextShadowMedium),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ).withTextShadow(TextShadowSoft),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ).withTextShadow(TextShadowSoft),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ).withTextShadow(TextShadowSoft),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.6.sp,
    ).withTextShadow(TextShadowMedium),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ).withTextShadow(TextShadowSoft),
)
