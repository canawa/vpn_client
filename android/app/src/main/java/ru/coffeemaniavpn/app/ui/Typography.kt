package ru.coffeemaniavpn.app.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun clevStyle(
    weight: FontWeight,
    size: Float,
    lineHeight: Float,
    letterSpacing: Float = 0f,
) = TextStyle(
    fontFamily = ClevFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

val CoffemaniaTypography = Typography(
    headlineLarge = clevStyle(FontWeight.Bold, 28f, 36f, -0.02f),
    headlineMedium = clevStyle(FontWeight.SemiBold, 22f, 28f),
    titleMedium = clevStyle(FontWeight.SemiBold, 16f, 24f),
    bodyLarge = clevStyle(FontWeight.Normal, 16f, 24f),
    bodyMedium = clevStyle(FontWeight.Normal, 14f, 20f),
    bodySmall = clevStyle(FontWeight.Normal, 12f, 16f),
    labelSmall = clevStyle(FontWeight.SemiBold, 12f, 16f, 0.2f),
    labelMedium = clevStyle(FontWeight.Medium, 14f, 20f),
)

/** Стиль по умолчанию для Text без MaterialTheme.typography. */
val ClevTextStyle = TextStyle(fontFamily = ClevFontFamily)
