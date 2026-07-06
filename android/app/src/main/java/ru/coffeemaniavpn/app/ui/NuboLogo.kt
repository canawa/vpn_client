package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R

/** Круглый значок NUBO — щит на сине-градиентном фоне. */
@Composable
fun NuboLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
) {
    val colors = nuboColors()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1A4FFF), Color(0xFF0A1A6A)),
                ),
            )
            .border(1.dp, colors.borderStrong, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_vpn),
            contentDescription = "NUBO VPN",
            tint = Color(0xFF7DD3FC),
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** Текстовый логотип NUBO VPN с синим градиентом. */
@Composable
fun NuboWordmark(
    modifier: Modifier = Modifier,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF7DD3FC), Color(0xFF1A7FFF)),
    )
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = "NUBO",
            style = TextStyle(
                brush = gradient,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize,
                letterSpacing = 2.sp,
            ),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "VPN",
            style = TextStyle(
                color = nuboColors().textMid,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize * 0.72f,
                letterSpacing = 3.sp,
            ),
        )
    }
}
