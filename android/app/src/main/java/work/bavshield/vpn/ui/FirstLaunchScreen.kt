package work.bavshield.vpn.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import work.bavshield.vpn.R

@Composable
fun FirstLaunchScreen(
    isLoading: Boolean,
    error: String?,
    onPasteClick: () -> Unit,
    onBuyOnWebsiteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = bavShieldColors()
    val buttonShape = RoundedCornerShape(14.dp)
    Box(modifier = modifier.fillMaxSize()) {
        CyberBackground(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(R.drawable.bav_logo),
                contentDescription = stringResource(R.string.app_name),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(56.dp),
            )

            Spacer(modifier = Modifier.height(18.dp))

            Image(
                painter = painterResource(R.drawable.bav_shield),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(168.dp),
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(R.string.first_launch_tagline),
                color = colors.mocha,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            Spacer(modifier = Modifier.height(36.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(buttonShape)
                    .background(colors.cappuccino)
                    .border(1.dp, colors.espresso, buttonShape)
                    .clickable(enabled = !isLoading, onClick = onPasteClick),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = colors.espresso,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.first_launch_paste),
                        color = colors.espresso,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(buttonShape)
                    .background(colors.cappuccino.copy(alpha = 0.35f))
                    .border(1.dp, colors.espresso.copy(alpha = 0.65f), buttonShape)
                    .clickable(enabled = !isLoading, onClick = onBuyOnWebsiteClick),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.subscription_buy_website),
                    color = colors.espresso,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = error,
                    color = colors.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = stringResource(R.string.first_launch_footer),
                color = colors.mocha,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
    }
}
