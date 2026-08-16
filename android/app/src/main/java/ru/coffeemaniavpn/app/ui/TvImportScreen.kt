package ru.coffeemaniavpn.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.coffeemaniavpn.app.R

@Composable
fun TvImportScreen(
    state: TvImportUiState,
    onDismiss: () -> Unit,
    onDraftUrlChange: (String) -> Unit,
    onSendDraftUrl: () -> Unit,
    onAutoFinish: () -> Unit,
) {
    if (state is TvImportUiState.Hidden) return

    BackHandler(onBack = onDismiss)

    if (state is TvImportUiState.Success) {
        LaunchedEffect(Unit) {
            delay(1_800)
            onAutoFinish()
        }
    }

    val colors = coffemaniaColors()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.tv_import_title),
                color = colors.yellow,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(28.dp))

            when (state) {
                is TvImportUiState.Sending -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = colors.yellow,
                        strokeWidth = 3.dp,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.tv_import_sending),
                        color = colors.onSurface,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                is TvImportUiState.Success -> {
                    Text(
                        text = stringResource(R.string.tv_import_success),
                        color = colors.onSurface,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    DoneButton(onClick = onDismiss)
                }

                is TvImportUiState.NoSubscription -> {
                    Text(
                        text = stringResource(R.string.tv_import_no_subscription),
                        color = colors.onSurface,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    ManualUrlBlock(
                        draftUrl = state.draftUrl,
                        onDraftUrlChange = onDraftUrlChange,
                        onSend = onSendDraftUrl,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.tv_import_close), color = colors.onSurfaceVariant)
                    }
                }

                is TvImportUiState.Error -> {
                    Text(
                        text = state.message,
                        color = colors.onSurface,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                    if (state.allowManualUrl) {
                        Spacer(modifier = Modifier.height(20.dp))
                        ManualUrlBlock(
                            draftUrl = state.draftUrl,
                            onDraftUrlChange = onDraftUrlChange,
                            onSend = onSendDraftUrl,
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    DoneButton(onClick = onDismiss)
                }

                TvImportUiState.Hidden -> Unit
            }
        }
    }
}

@Composable
private fun DoneButton(onClick: () -> Unit) {
    val colors = coffemaniaColors()
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.yellow,
            contentColor = colors.onPrimary,
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.tv_import_done),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun ManualUrlBlock(
    draftUrl: String,
    onDraftUrlChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val colors = coffemaniaColors()
    OutlinedTextField(
        value = draftUrl,
        onValueChange = onDraftUrlChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = {
            Text(
                stringResource(R.string.tv_import_paste_hint),
                color = colors.onSurfaceVariant,
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Send,
        ),
        keyboardActions = KeyboardActions(onSend = { onSend() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            focusedBorderColor = colors.yellow,
            unfocusedBorderColor = colors.outline,
            cursorColor = colors.yellow,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
        ),
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = onSend,
        enabled = draftUrl.isNotBlank(),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.yellow,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceVariant,
            disabledContentColor = colors.onSurfaceVariant,
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.tv_import_send),
            fontWeight = FontWeight.Bold,
        )
    }
}
