package ru.coffeemaniavpn.app.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Типографика в духе iOS: SF Pro, плотные веса, чуть отрицательный трекинг у заголовков. */
private fun iosStyle(
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
    headlineLarge = iosStyle(FontWeight.Bold, 28f, 34f, -0.4f),
    headlineMedium = iosStyle(FontWeight.Bold, 22f, 28f, -0.3f),
    titleMedium = iosStyle(FontWeight.SemiBold, 17f, 22f, -0.2f),
    bodyLarge = iosStyle(FontWeight.Normal, 17f, 22f, -0.2f),
    bodyMedium = iosStyle(FontWeight.Normal, 15f, 20f, -0.1f),
    bodySmall = iosStyle(FontWeight.Normal, 13f, 18f, -0.1f),
    labelSmall = iosStyle(FontWeight.SemiBold, 13f, 18f, -0.1f),
    labelMedium = iosStyle(FontWeight.SemiBold, 15f, 20f, -0.1f),
)

/** Стиль по умолчанию для Text без MaterialTheme.typography — как iOS body. */
val ClevTextStyle = TextStyle(
    fontFamily = ClevFontFamily,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = (-0.2f).sp,
)
