package work.bavshield.vpn.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import work.bavshield.vpn.R

/**
 * Inter Bold — открытый аналог San Francisco (системный шрифт macOS / iOS).
 * SF Pro нельзя легально встраивать в Android-приложения.
 */
val BavShieldFontFamily = FontFamily(
    Font(R.font.inter_bold, FontWeight.Normal),
    Font(R.font.inter_bold, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun boldTextStyle(
    fontSize: androidx.compose.ui.unit.TextUnit,
    lineHeight: androidx.compose.ui.unit.TextUnit,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
) = TextStyle(
    fontFamily = BavShieldFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = fontSize,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

val BavShieldTypography = Typography(
    headlineLarge = boldTextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.02).sp,
    ),
    headlineMedium = boldTextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = boldTextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
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
    ),
    labelMedium = boldTextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
