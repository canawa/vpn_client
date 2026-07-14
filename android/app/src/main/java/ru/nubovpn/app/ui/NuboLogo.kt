package ru.nubovpn.app.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Текстовый логотип NUBO VPN без неонового градиента. */
@Composable
fun NuboWordmark(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
) {
    val colors = nuboColors()
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = "NUBO",
            color = colors.textMain,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "VPN",
            color = colors.textMid,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize * 0.72f,
            letterSpacing = 3.sp,
        )
    }
}
