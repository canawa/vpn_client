package ru.coffeemaniavpn.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
            clipboardUrl = clipboardUrl,
            onHasKey = {
                if (clipboardUrl != null) onAcceptClipboard() else step = ActivationStep.Import
            },
            onNeedSubscription = { step = ActivationStep.Choice },
            onAcceptClipboard = onAcceptClipboard,
            onDismissClipboard = onDismissClipboard,
        )
        ActivationStep.Choice -> XenoSubscribeChoiceScreen(
            modifier = modifier,
            onBack = { step = ActivationStep.Start },
            onTelegram = { step = ActivationStep.StepsTelegram },
            onWebsite = { step = ActivationStep.StepsSite },
            onHelp = { step = ActivationStep.StepsTelegram },
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
    clipboardUrl: String?,
    onHasKey: () -> Unit,
    onNeedSubscription: () -> Unit,
    onAcceptClipboard: () -> Unit,
    onDismissClipboard: () -> Unit,
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
            clipboardUrl?.let {
                Spacer(modifier = Modifier.height(20.dp))
                XenoCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.clipboard_subscription_detected),
                            color = colors.espresso,
                            fontSize = 14.sp,
                        )
                        XenoPrimaryButton(
                            text = stringResource(R.string.clipboard_add_subscription),
                            onClick = onAcceptClipboard,
                        )
                        TextButton(onClick = onDismissClipboard) {
                            Text(stringResource(R.string.clipboard_dismiss), color = colors.mocha)
                        }
                    }
                }
            }
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
    onHelp: () -> Unit,
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
                lineHeight = 27.6.sp, // 120% of 23
                letterSpacing = 0.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.xeno_choice_body),
                color = colors.mocha,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 19.5.sp, // 150% of 13
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
                icon = {
                    Text(
                        text = "◎",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                },
                onClick = onWebsite,
            )
        }

        XenoDashedButton(
            text = stringResource(R.string.xeno_choice_help),
            onClick = onHelp,
            leadingAccent = "?",
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
    XenoStepsScaffold(
        modifier = modifier,
        title = stringResource(R.string.xeno_steps_tg_title),
        subtitle = null,
        onBack = onBack,
        cta = stringResource(R.string.xeno_steps_open_bot),
        footer = stringResource(R.string.xeno_steps_tg_footer),
        onCta = onOpenBot,
        steps = listOf(
            stringResource(R.string.xeno_steps_tg_1_title) to stringResource(R.string.xeno_steps_tg_1_body),
            stringResource(R.string.xeno_steps_tg_2_title) to stringResource(R.string.xeno_steps_tg_2_body),
            stringResource(R.string.xeno_steps_tg_3_title) to stringResource(R.string.xeno_steps_tg_3_body),
        ),
    )
}

@Composable
private fun XenoStepsSiteScreen(
    onBack: () -> Unit,
    onOpenCabinet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    XenoStepsScaffold(
        modifier = modifier,
        title = stringResource(R.string.xeno_steps_site_title),
        subtitle = stringResource(R.string.xeno_steps_site_subtitle),
        onBack = onBack,
        cta = stringResource(R.string.xeno_steps_open_cabinet),
        footer = stringResource(R.string.xeno_steps_site_footer),
        onCta = onOpenCabinet,
        steps = listOf(
            stringResource(R.string.xeno_steps_site_1_title) to stringResource(R.string.xeno_steps_site_1_body),
            stringResource(R.string.xeno_steps_site_2_title) to stringResource(R.string.xeno_steps_site_2_body),
            stringResource(R.string.xeno_steps_site_3_title) to stringResource(R.string.xeno_steps_site_3_body),
            stringResource(R.string.xeno_steps_site_4_title) to stringResource(R.string.xeno_steps_site_4_body),
        ),
    )
}

@Composable
private fun XenoStepsScaffold(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    cta: String,
    footer: String,
    onCta: () -> Unit,
    steps: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    val colors = coffemaniaColors()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = colors.espresso)
            }
            Text(title, color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            subtitle?.let {
                Text(it, color = colors.mocha, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(16.dp))
            }
            steps.forEachIndexed { index, (stepTitle, body) ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (index == 0) colors.primary else colors.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = if (index == 0) Color.Black else colors.mocha,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                        if (index < steps.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(48.dp)
                                    .background(colors.latte),
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(bottom = 18.dp)) {
                        Text(stepTitle, color = colors.espresso, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(body, color = colors.mocha, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            XenoPrimaryButton(text = cta, onClick = onCta)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = footer,
                color = colors.mocha,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.milkFoam)
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.espresso)
            }
            Text(
                text = stringResource(R.string.xeno_import_title),
                color = colors.espresso,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        BasicTextField(
            value = url,
            onValueChange = onUrlChange,
            textStyle = androidx.compose.ui.text.TextStyle(color = colors.espresso, fontSize = 14.sp),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.cappuccino, RoundedCornerShape(12.dp))
                .border(1.dp, colors.latte, RoundedCornerShape(12.dp))
                .padding(16.dp),
            decorationBox = { inner ->
                Box {
                    if (url.isBlank()) {
                        Text(stringResource(R.string.xeno_import_hint), color = colors.mocha, fontSize = 14.sp)
                    }
                    inner()
                }
            },
        )
        Spacer(modifier = Modifier.height(12.dp))
        XenoOutlineButton(text = stringResource(R.string.xeno_import_clipboard), onClick = onPasteFromClipboard)
        Spacer(modifier = Modifier.height(12.dp))
        XenoPrimaryButton(
            text = stringResource(R.string.xeno_import_action),
            onClick = onImport,
            enabled = !isLoading && url.isNotBlank(),
        )
        error?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = it, color = colors.error, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(12.dp))
            XenoOutlineButton(text = stringResource(R.string.xeno_import_telegram), onClick = onSubscribeTelegram)
        }
    }
}
