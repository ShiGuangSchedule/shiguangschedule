package com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.*

/**
 * 宿主：监听 Bridge / 原生 WebView 事件，负责显示 JS 触发及平台抛出的 Compose 弹窗。
 */
@Composable
fun WebDialogHost(
    uiEvents: Flow<WebUiEvent>
) {
    var currentEvent by remember { mutableStateOf<WebUiEvent?>(null) }

    LaunchedEffect(uiEvents) {
        uiEvents.collect { event ->
            currentEvent = event
        }
    }

    when (val event = currentEvent) {
        is WebUiEvent.ShowAlert -> {
            AlertHost(event.data, onConfirm = {
                event.callback(true)
                currentEvent = null
            }, onDismiss = {
                event.callback(false)
                currentEvent = null
            })
        }
        is WebUiEvent.ShowPrompt -> {
            PromptHost(
                event.data,
                onRequestValidation = { input ->
                    event.onRequestValidation(input) {
                        currentEvent = null
                    }
                },
                errorFlow = event.errorFeedbackFlow,
                onCancel = {
                    event.onCancel()
                    currentEvent = null
                }
            )
        }
        is WebUiEvent.ShowSingleSelection -> {
            SingleSelectionHost(event.data, onResult = { index ->
                event.callback(index)
                currentEvent = null
            })
        }
        is WebUiEvent.ShowSslWarning -> {
            SslWarningHost(
                data = event.data,
                onProceed = {
                    event.data.onProceed()
                    currentEvent = null
                },
                onCancel = {
                    event.data.onCancel()
                    currentEvent = null
                }
            )
        }
        null -> Unit
    }
}

/** 显示 SSL 证书错误警告弹窗。 */
@Composable
private fun SslWarningHost(
    data: SslErrorData,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(stringResource(Res.string.dialog_ssl_warning_title))
        },
        text = {
            Text(stringResource(Res.string.dialog_ssl_warning_message, data.primaryErrorUrl))
        },
        confirmButton = {
            Button(onClick = onProceed) {
                Text(stringResource(Res.string.action_continue_visit))
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

/** 显示 Alert/Confirm 弹窗。 */
@Composable
private fun AlertHost(data: AlertDialogData, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(data.title) },
        text = { Text(data.content) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(data.confirmText) }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        }
    )
}

/** 显示 Prompt 弹窗。 */
@Composable
private fun PromptHost(
    data: PromptDialogData,
    onRequestValidation: (String) -> Unit,
    errorFlow: Flow<String?>,
    onCancel: () -> Unit
) {
    var inputText by rememberSaveable { mutableStateOf(data.defaultText) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(errorFlow) {
        errorFlow.collect { message ->
            errorText = message
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(data.title) },
        text = {
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        errorText = null
                    },
                    label = { Text(data.tip) },
                    isError = errorText != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onRequestValidation(inputText) }
            ) { Text(stringResource(Res.string.action_confirm)) }
        },
        dismissButton = {
            Button(onClick = onCancel) { Text(stringResource(Res.string.action_cancel)) }
        }
    )
}

/** 显示单选列表弹窗。 */
@Composable
private fun SingleSelectionHost(data: SingleSelectionDialogData, onResult: (Int?) -> Unit) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(data.defaultSelectedIndex) }

    AlertDialog(
        onDismissRequest = { onResult(null) },
        title = { Text(data.title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                data.items.forEachIndexed { index, item ->
                    ListItem(
                        headlineContent = { Text(item) },
                        leadingContent = {
                            RadioButton(
                                selected = (index == selectedIndex),
                                onClick = null
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedIndex = index }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onResult(selectedIndex) },
                enabled = selectedIndex != -1
            ) { Text(stringResource(Res.string.action_confirm)) }
        },
        dismissButton = {
            Button(onClick = { onResult(null) }) { Text(stringResource(Res.string.action_cancel)) }
        }
    )
}