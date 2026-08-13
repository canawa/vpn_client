package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.coffeemaniavpn.app.R

private enum class ActivationStep { Start, Choice, StepsTelegram, StepsSite, Import }

@Composable
fun XenoActivationFlow(
    isLoading: Boolean,
    error: String?,
    clipboardUrl: String?,
    showForeignPrompt: Boolean,
    onPasteLinkClick: () -> Unit,
    onAcceptClipboard: () -> Unit,
    onDismissClipboard: () -> Unit,
    onDismissForeignPrompt: () -> Unit,
    onBuyTelegram: () -> Unit,
    onBuyWebsite: () -> Unit,
    onImportUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by remember { mutableStateOf(ActivationStep.Start) }
    var draftUrl by remember { mutableStateOf("") }

    when (step) {
        ActivationStep.Start -> XenoStartScreen(
            modifier = modifier,
            isLoading = isLoading,
            error = error,
            onHasKey = {
                if (clipboardUrl != null) onAcceptClipboard() else step = ActivationStep.Import
            },
            onNeedSubscription = { step = ActivationStep.Choice },
        )
        ActivationStep.Choice -> XenoSubscribeChoiceScreen(
            modifier = modifier,
            onBack = { step = ActivationStep.Start },
            onTelegram = { step = ActivationStep.StepsTelegram },
            onWebsite = { step = ActivationStep.StepsSite },
            onHelpOpenBot = {
                onBuyTelegram()
                step = ActivationStep.StepsTelegram
            },
            onHelpOpenSite = {
                onBuyWebsite()
                step = ActivationStep.StepsSite
            },
        )
        ActivationStep.StepsTelegram -> XenoStepsTelegramScreen(
            modifier = modifier,
            onBack = { step = ActivationStep.Choice },
            onOpenBot = onBuyTelegram,
        )
        ActivationStep.StepsSite -> XenoStepsSiteScreen(
            modifier = modifier,
            onBack = { step = ActivationStep.Choice },
            onOpenCabinet = onBuyWebsite,
        )
        ActivationStep.Import -> XenoImportScreen(
            modifier = modifier,
            url = draftUrl,
            onUrlChange = { draftUrl = it },
            isLoading = isLoading,
            error = error,
            onBack = { step = ActivationStep.Start },
            onPasteFromClipboard = onPasteLinkClick,
            onImport = { onImportUrl(draftUrl.trim()) },
            onSubscribeTelegram = { step = ActivationStep.StepsTelegram },
        )
    }

    if (showForeignPrompt) {
        AlertDialog(
            onDismissRequest = onDismissForeignPrompt,
            title = { Text(stringResource(R.string.subscription_not_ours_title)) },
            text = { Text(stringResource(R.string.subscription_not_ours_body)) },
            confirmButton = {
                TextButton(onClick = { onDismissForeignPrompt(); onBuyTelegram() }) {
                    Text(stringResource(R.string.xeno_tg_bot))
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismissForeignPrompt(); onBuyWebsite() }) {
                    Text(stringResource(R.string.xeno_web_cabinet))
                }
            },
        )
    }
}

@Composable
private fun XenoStartScreen(
    isLoading: Boolean,
    error: String?,
    onHasKey: () -> Unit,
    onNeedSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam)
            .padding(start = 22.dp, end = 22.dp, top = 64.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        XenoLogoMark()

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "// один шаг до сети",
                color = colors.primary,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 13.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Start,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.xeno_start_title),
                color = colors.espresso,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                lineHeight = 34.5.sp, // 115% of 30
                letterSpacing = 0.3.sp,
                textAlign = TextAlign.Start,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.xeno_start_body),
                color = colors.mocha,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 21.sp, // 150% of 14
                letterSpacing = 0.sp,
                textAlign = TextAlign.Start,
            )
            error?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = it, color = colors.error, fontSize = 13.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val pasteShape = RoundedCornerShape(13.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(pasteShape)
                    .background(colors.cappuccino)
                    .border(0.5.dp, colors.latte, pasteShape)
                    .clickable(enabled = !isLoading, onClick = onHasKey)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.xeno_start_has_key),
                            color = colors.espresso,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = "→",
                            color = colors.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(pasteShape)
                    .background(if (!isLoading) colors.primary else colors.primary.copy(alpha = 0.4f))
                    .clickable(enabled = !isLoading, onClick = onNeedSubscription)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.xeno_start_need_sub),
                        color = Color.Black,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = stringResource(R.string.xeno_trial_badge),
                        color = Color.Black.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun XenoSubscribeChoiceScreen(
    onBack: () -> Unit,
    onTelegram: () -> Unit,
    onWebsite: () -> Unit,
    onHelpOpenBot: () -> Unit,
    onHelpOpenSite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    var showHelpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam)
            .padding(start = 22.dp, end = 22.dp, top = 64.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = colors.espresso,
                    )
                }
                XenoLogoMark()
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.xeno_choice_title),
                color = colors.espresso,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 23.sp,
                lineHeight = 27.6.sp,
                letterSpacing = 0.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.xeno_choice_body),
                color = colors.mocha,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                letterSpacing = 0.sp,
            )

            Spacer(modifier = Modifier.height(20.dp))

            XenoChoiceCard(
                title = stringResource(R.string.xeno_choice_telegram),
                subtitle = stringResource(R.string.xeno_choice_telegram_hint),
                badge = stringResource(R.string.xeno_choice_fastest),
                highlighted = true,
                iconBackground = Color(0xFF04342C),
                icon = {
                    Text(text = "✈️", fontSize = 22.sp)
                },
                onClick = onTelegram,
            )
            Spacer(modifier = Modifier.height(13.dp))
            XenoChoiceCard(
                title = stringResource(R.string.xeno_choice_site),
                subtitle = stringResource(R.string.xeno_choice_site_hint),
                badge = null,
                highlighted = false,
                iconBackground = Color(0xFF161816),
                icon = { XenoSiteGlyph() },
                onClick = onWebsite,
            )
        }

        XenoDashedButton(
            text = stringResource(R.string.xeno_choice_help),
            onClick = { showHelpDialog = true },
            leadingAccent = "?",
        )
    }

    if (showHelpDialog) {
        XenoHelpTelegramAccessDialog(
            onDismiss = { showHelpDialog = false },
            onYes = {
                showHelpDialog = false
                onHelpOpenBot()
            },
            onNo = {
                showHelpDialog = false
                onHelpOpenSite()
            },
        )
    }
}

@Composable
private fun XenoHelpTelegramAccessDialog(
    onDismiss: () -> Unit,
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    val plate = Color(0xFF121A17)
    val stroke = Color(0xFF222B28)
    val teal = Color(0xFF00D4A8)
    val text = Color(0xFFF2F5F4)
    val shape = RoundedCornerShape(18.dp)

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color(0xFF0A0D0C))
                .border(1.dp, stroke, shape)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = stringResource(R.string.xeno_help_dialog_tag),
                color = teal,
                fontFamily = JetBrainsMonoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
            )
            Text(
                text = stringResource(R.string.xeno_help_dialog_title),
                color = text,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                XenoHelpDialogButton(
                    label = stringResource(R.string.xeno_help_dialog_yes),
                    modifier = Modifier.weight(1f),
                    plate = plate,
                    stroke = stroke,
                    textColor = text,
                    onClick = onYes,
                )
                XenoHelpDialogButton(
                    label = stringResource(R.string.xeno_help_dialog_no),
                    modifier = Modifier.weight(1f),
                    plate = plate,
                    stroke = stroke,
                    textColor = text,
                    onClick = onNo,
                )
            }
        }
    }
}

@Composable
private fun XenoHelpDialogButton(
    label: String,
    plate: Color,
    stroke: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(plate)
            .border(1.dp, stroke, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = textColor,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun XenoChoiceCard(
    title: String,
    subtitle: String,
    badge: String?,
    highlighted: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    iconBackground: Color = Color(0xFF1C1C1C),
) {
    val colors = coffemaniaColors()
    val shape = RoundedCornerShape(14.dp)
    val border = if (highlighted) Color(0xFF00E091) else Color(0xFF2A2A2A)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (badge != null) 6.dp else 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(shape)
                .background(Color(0xFF141414))
                .border(1.5.dp, border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) { icon() }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = colors.mocha,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF6E6E6E),
                modifier = Modifier.size(20.dp),
            )
        }

        badge?.let {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = 28.dp, y = (-8).dp)
                    .width(90.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF00E091))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = it,
                    color = Color.Black,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    letterSpacing = 0.sp,
                )
            }
        }
    }
}

@Composable
private fun XenoSiteGlyph() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val stroke = Stroke(width = 1.6.dp.toPx())
        val c = Color(0xFFE8E8E8)
        drawCircle(color = c, radius = size.minDimension * 0.46f, style = stroke)
        drawCircle(color = c, radius = size.minDimension * 0.28f, style = stroke)
        drawCircle(color = c, radius = size.minDimension * 0.08f)
    }
}

@Composable
private fun XenoStepsTelegramScreen(
    onBack: () -> Unit,
    onOpenBot: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam)
            .padding(start = 22.dp, end = 22.dp, top = 48.dp, bottom = 40.dp),
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.espresso)
                }
                Text(
                    text = stringResource(R.string.xeno_steps_tg_title),
                    color = colors.espresso,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                XenoTgStepCompact(
                    number = 1,
                    active = true,
                    showLine = true,
                    title = stringResource(R.string.xeno_steps_tg_1_title),
                    body = stringResource(R.string.xeno_steps_tg_1_body),
                    imageRes = null,
                    lineHeight = 18.dp,
                )
                Spacer(modifier = Modifier.height(10.dp))
                XenoTgStepCompact(
                    number = 2,
                    active = false,
                    showLine = true,
                    title = stringResource(R.string.xeno_steps_tg_2_title),
                    body = stringResource(R.string.xeno_steps_tg_2_body),
                    imageRes = R.drawable.xeno_tg_step_trial,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.height(10.dp))
                XenoTgStepCompact(
                    number = 3,
                    active = false,
                    showLine = false,
                    title = stringResource(R.string.xeno_steps_tg_3_title),
                    body = stringResource(R.string.xeno_steps_tg_3_body),
                    imageRes = R.drawable.xeno_tg_step_connect,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF00E091))
                        .clickable(onClick = onOpenBot)
                        .padding(vertical = 15.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "✈️", fontSize = 16.sp)
                        Text(
                            text = stringResource(R.string.xeno_steps_open_bot),
                            color = Color.Black,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.xeno_steps_tg_footer),
                        color = Color(0xFF7A7F78),
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.5.sp,
                        lineHeight = 17.5.sp, // 140% of 12.5
                        letterSpacing = 0.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
    }
}

@Composable
private fun XenoTgStepCompact(
    number: Int,
    active: Boolean,
    showLine: Boolean,
    title: String,
    body: String,
    imageRes: Int?,
    modifier: Modifier = Modifier,
    lineHeight: androidx.compose.ui.unit.Dp? = null,
) {
    val colors = coffemaniaColors()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = if (imageRes != null) Modifier.fillMaxHeight() else Modifier,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (active) Color(0xFF00E091) else Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$number",
                    color = if (active) Color.Black else Color(0xFF9A9A9A),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            if (showLine) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .then(
                            if (lineHeight != null) Modifier.height(lineHeight)
                            else Modifier.weight(1f).padding(vertical = 4.dp),
                        )
                        .background(Color(0xFF2A2A2A)),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (imageRes != null) Modifier.fillMaxHeight() else Modifier),
        ) {
            Text(
                text = title,
                color = Color.White,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Text(
                text = body,
                color = colors.mocha,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 2,
            )
            if (imageRes != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}

@Composable
private fun XenoStepsSiteScreen(
    onBack: () -> Unit,
    onOpenCabinet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam),
        contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 48.dp, bottom = 40.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.espresso)
                }
                Text(
                    text = stringResource(R.string.xeno_steps_site_title),
                    color = colors.espresso,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        item {
            Text(
                text = stringResource(R.string.xeno_steps_site_subtitle),
                color = Color(0xFF7A7F78),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            XenoSiteStep(
                number = 1,
                active = true,
                showLine = true,
                title = stringResource(R.string.xeno_steps_site_1_title),
                body = stringResource(R.string.xeno_steps_site_1_body),
                imageRes = null,
            )
        }
        item { Spacer(modifier = Modifier.height(14.dp)) }
        item {
            XenoSiteStep(
                number = 2,
                active = false,
                showLine = true,
                title = stringResource(R.string.xeno_steps_site_2_title),
                body = stringResource(R.string.xeno_steps_site_2_body),
                imageRes = R.drawable.xeno_site_step_email,
            )
        }
        item { Spacer(modifier = Modifier.height(14.dp)) }
        item {
            XenoSiteStep(
                number = 3,
                active = false,
                showLine = true,
                title = stringResource(R.string.xeno_steps_site_3_title),
                body = stringResource(R.string.xeno_steps_site_3_body),
                imageRes = R.drawable.xeno_site_step_order,
            )
        }
        item { Spacer(modifier = Modifier.height(14.dp)) }
        item {
            XenoSiteStep(
                number = 4,
                active = false,
                showLine = false,
                title = stringResource(R.string.xeno_steps_site_4_title),
                body = stringResource(R.string.xeno_steps_site_4_body),
                imageRes = R.drawable.xeno_site_step_setup,
            )
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF00E091))
                    .clickable(onClick = onOpenCabinet)
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val stroke = 1.5.dp.toPx()
                        drawCircle(
                            color = Color.Black,
                            radius = size.minDimension / 2f - stroke / 2f,
                            style = Stroke(width = stroke),
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 2.5.dp.toPx(),
                        )
                    }
                    Text(
                        text = stringResource(R.string.xeno_steps_open_cabinet),
                        color = Color.Black,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.xeno_steps_site_footer),
                    color = Color(0xFF7A7F78),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp, // 140% of 12.5
                    letterSpacing = 0.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(346.dp),
                )
            }
        }
    }
}

@Composable
private fun XenoSiteStep(
    number: Int,
    active: Boolean,
    showLine: Boolean,
    title: String,
    body: String,
    imageRes: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (active) Color(0xFF00E091) else Color(0xFF2A2A2A)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$number",
                    color = if (active) Color.Black else Color(0xFF9A9A9A),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
            if (showLine) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(20.dp)
                        .background(Color(0xFF2A2A2A)),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                lineHeight = 14.5.sp,
                letterSpacing = 0.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(306.dp)
                    .height(18.dp),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                color = Color(0xFF7A7F78),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.5.sp,
                lineHeight = 18.125.sp, // 145% of 12.5
                letterSpacing = 0.sp,
                modifier = Modifier
                    .width(306.dp)
                    .heightIn(min = 36.dp),
            )
            if (imageRes != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}

@Composable
private fun XenoImportScreen(
    url: String,
    onUrlChange: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onImport: () -> Unit,
    onSubscribeTelegram: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    val fieldShape = RoundedCornerShape(13.dp)
    val buttonShape = RoundedCornerShape(999.dp)
    val canImport = !isLoading && url.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam)
            .padding(start = 22.dp, end = 22.dp, top = 48.dp, bottom = 40.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.espresso)
            }
            Text(
                text = stringResource(R.string.xeno_import_title),
                color = colors.espresso,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        BasicTextField(
            value = url,
            onValueChange = onUrlChange,
            enabled = !isLoading,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 14.5.sp,
                fontFamily = InterFontFamily,
            ),
            cursorBrush = SolidColor(Color(0xFF00E091)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(fieldShape)
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF2A2A2A), fieldShape)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (url.isBlank()) {
                        Text(
                            text = stringResource(R.string.xeno_import_hint),
                            color = Color(0xFF7A7F78),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.5.sp,
                        )
                    }
                    inner()
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(buttonShape)
                .background(Color(0xFF141414))
                .border(1.dp, Color(0xFF2A2A2A), buttonShape)
                .clickable(enabled = !isLoading, onClick = onPasteFromClipboard),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.xeno_import_clipboard),
                color = Color(0xFF00E091),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(buttonShape)
                .background(
                    if (canImport) Color(0xFF00E091) else Color(0xFF00E091).copy(alpha = 0.4f),
                )
                .clickable(enabled = canImport, onClick = onImport),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.Black,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = stringResource(R.string.xeno_import_action),
                    color = Color.Black,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }

        error?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = colors.error,
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                lineHeight = 18.85.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(buttonShape)
                    .background(Color(0xFF141414))
                    .border(1.dp, Color(0xFF2A2A2A), buttonShape)
                    .clickable(onClick = onSubscribeTelegram),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.xeno_import_telegram),
                    color = Color(0xFF00E091),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}
