@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroupCard
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsRow
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsSwitchRow
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.dialog_title_set_remind_time
import shiguangschedule.shared.generated.resources.error_input_empty
import shiguangschedule.shared.generated.resources.error_input_range
import shiguangschedule.shared.generated.resources.item_course_reminder
import shiguangschedule.shared.generated.resources.item_remind_time_before
import shiguangschedule.shared.generated.resources.label_minutes_input
import shiguangschedule.shared.generated.resources.remind_time_minutes_format
import shiguangschedule.shared.generated.resources.section_title_general
import shiguangschedule.shared.generated.resources.title_course_notification_settings

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    val settingPadding = 16.dp

    val uiState by viewModel.uiState.collectAsState()
    var showRemindTimeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.title_course_notification_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (!uiState.isReady) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
        } else {
            val appSettings = uiState.appSettings

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = settingPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SettingsGroupCard(
                    modifier = Modifier.padding(top = 8.dp),
                    title = stringResource(Res.string.section_title_general)
                ) {
                    item {
                        SettingsSwitchRow(
                            title = stringResource(Res.string.item_course_reminder),
                            checked = appSettings.reminderEnabled,
                            onCheckedChange = viewModel::onReminderEnabledChanged
                        )
                    }

                    item {
                        SettingsRow(
                            title = stringResource(Res.string.item_remind_time_before),
                            onClick = { showRemindTimeDialog = true },
                            trailing = {
                                Text(
                                    text = stringResource(
                                        Res.string.remind_time_minutes_format,
                                        appSettings.remindBeforeMinutes.toString()
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                // 底部留白，避免滚到底时贴边
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (showRemindTimeDialog) {
                RemindTimeInputDialog(
                    initialMinutes = appSettings.remindBeforeMinutes,
                    onDismiss = { showRemindTimeDialog = false },
                    onConfirm = {
                        viewModel.onRemindBeforeMinutesChanged(it)
                        showRemindTimeDialog = false
                    }
                )
            }
        }
    }
}

/**
 * 提前提醒时间输入框弹窗
 */
@Composable
private fun RemindTimeInputDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(initialMinutes.toString()) }

    val intValue = text.toIntOrNull()
    val isValid = intValue != null && intValue in 0..60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_title_set_remind_time)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    if (input.length <= 3 && input.all { it.isDigit() }) {
                        text = input
                    }
                },
                label = { Text(stringResource(Res.string.label_minutes_input)) },
                singleLine = true,
                isError = !isValid,
                supportingText = {
                    when {
                        text.isEmpty() -> Text(stringResource(Res.string.error_input_empty))
                        !isValid -> Text(stringResource(Res.string.error_input_range))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { intValue?.let { onConfirm(it) } },
                enabled = isValid
            ) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}