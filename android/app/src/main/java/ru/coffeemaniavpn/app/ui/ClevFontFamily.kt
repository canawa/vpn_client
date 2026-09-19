package ru.coffeemaniavpn.app.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import ru.coffeemaniavpn.app.R

/**
 * San Francisco Pro — системный шрифт iOS/macOS (как в ClevVPN).
 * Статические OTF читаются надёжнее variable-шрифтов на Android.
 */
val ClevFontFamily = FontFamily(
    Font(R.font.sf_pro_regular, FontWeight.Normal),
    Font(R.font.sf_pro_medium, FontWeight.Medium),
    Font(R.font.sf_pro_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_bold, FontWeight.Bold),
)

/** Алиасы: весь UI на SF Pro. */
val HushDisplayFontFamily = ClevFontFamily
val HushBodyFontFamily = ClevFontFamily
