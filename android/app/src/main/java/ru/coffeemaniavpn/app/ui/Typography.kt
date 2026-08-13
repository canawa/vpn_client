package ru.coffeemaniavpn.app.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun xenoStyle(
    weight: FontWeight,
    size: Float,
    lineHeight: Float,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = XenoFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val CoffemaniaTypography = Typography(
    headlineLarge = xenoStyle(FontWeight.Bold, 28f, 36f, -0.02f),
    headlineMedium = xenoStyle(FontWeight.Bold, 22f, 28f),
    titleMedium = xenoStyle(FontWeight.Bold, 16f, 24f),
    bodyLarge = xenoStyle(FontWeight.Bold, 16f, 24f),
    bodyMedium = xenoStyle(FontWeight.Bold, 14f, 20f),
    bodySmall = xenoStyle(FontWeight.Bold, 12f, 16f),
    labelSmall = xenoStyle(FontWeight.Bold, 12f, 16f, 0.2f),
    labelMedium = xenoStyle(FontWeight.Bold, 14f, 20f),
)

/** Стиль по умолчанию для Text без MaterialTheme.typography. */
val XenoTextStyle = TextStyle(
    fontFamily = XenoFontFamily,
    fontWeight = FontWeight.Bold,
)
