package com.xingheyuzhuan.shiguangschedule.ui.settings.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.xingheyuzhuan.shiguangschedule.data.model.AutoControlMode
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_go_to_settings
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.desc_compat_wearable_sync
import shiguangschedule.shared.generated.resources.dialog_text_dnd_permission
import shiguangschedule.shared.generated.resources.dialog_text_exact_alarm_permission
import shiguangschedule.shared.generated.resources.dialog_text_notification_permission_denied
import shiguangschedule.shared.generated.resources.dialog_title_auto_mode_selection
import shiguangschedule.shared.generated.resources.dialog_title_dnd_permission
import shiguangschedule.shared.generated.resources.dialog_title_exact_alarm_permission
import shiguangschedule.shared.generated.resources.dialog_title_notification_permission_denied
import shiguangschedule.shared.generated.resources.dialog_title_set_remind_time
import shiguangschedule.shared.generated.resources.error_input_empty
import shiguangschedule.shared.generated.resources.error_input_range
import shiguangschedule.shared.generated.resources.item_background_and_autostart
import shiguangschedule.shared.generated.resources.item_compat_wearable_sync
import shiguangschedule.shared.generated.resources.item_course_reminder
import shiguangschedule.shared.generated.resources.item_dnd_permission
import shiguangschedule.shared.generated.resources.item_exact_alarm_permission
import shiguangschedule.shared.generated.resources.item_ignore_battery_optimization
import shiguangschedule.shared.generated.resources.item_promoted_notifications
import shiguangschedule.shared.generated.resources.item_remind_time_before
import shiguangschedule.shared.generated.resources.label_minutes_input
import shiguangschedule.shared.generated.resources.remind_time_minutes_format
import shiguangschedule.shared.generated.resources.section_title_general
import shiguangschedule.shared.generated.resources.section_title_permission
import shiguangschedule.shared.generated.resources.status_authorized
import shiguangschedule.shared.generated.resources.status_unauthorized
import shiguangschedule.shared.generated.resources.title_course_notification_settings

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit,
    viewModel: NotificationSettingsViewModel = koinViewModel()
) {
    val settingPadding = 16.dp
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by viewModel.uiState.collectAsState()
    val permissions by viewModel.permissionsState.collectAsState()

    var showRemindTimeDialog by remember { mutableStateOf(false) }
    var showAutoModeDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var showDndPermissionDialog by remember { mutableStateOf(false) }

    val isAndroid16Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA
    val isAndroid13Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** 同步查询精确闹钟权限 */
    fun isExactAlarmGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    }

    /** 同步查询勿扰权限 */
    fun isDndGranted(): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (!isExactAlarmGranted()) {
                showExactAlarmDialog = true
            } else {
                viewModel.onReminderEnabledChanged(true)
            }
        } else {
            showNotificationPermissionDialog = true
        }
    }

    /** 开启课程提醒前的统一检查入口 */
    fun attemptToEnableReminder() {
        val notificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!notificationEnabled) {
            if (isAndroid13Plus) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showNotificationPermissionDialog = true
            }
        } else if (!isExactAlarmGranted()) {
            showExactAlarmDialog = true
        } else {
            viewModel.onReminderEnabledChanged(true)
        }
    }

    /** 保存上课自动模式前的统一检查入口 */
    fun handleAutoModeSelected(mode: AutoControlMode) {
        if (mode == AutoControlMode.OFF) {
            viewModel.onAutoControlModeChanged(mode)
            return
        }
        if (!isExactAlarmGranted()) {
            showExactAlarmDialog = true
            return
        }
        if (!isDndGranted()) {
            showDndPermissionDialog = true
            return
        }
        viewModel.onAutoControlModeChanged(mode)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.refreshPermissions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /** 课程提醒开关的统一切换入口 */
    val toggleReminder = {
        if (uiState.appSettings.reminderEnabled) {
            viewModel.onReminderEnabledChanged(false)
        } else {
            attemptToEnableReminder()
        }
    }

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
            val androidSettings = uiState.androidSettings

            // 用普通 Column + verticalScroll 替代 LazyColumn
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = settingPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 常规标题
                Text(
                    text = stringResource(Res.string.section_title_general),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )

                // 常规卡片
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        SettingItem(
                            title = stringResource(Res.string.item_course_reminder),
                            onClick = toggleReminder
                        ) {
                            Switch(
                                checked = appSettings.reminderEnabled,
                                onCheckedChange = { toggleReminder() }
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        SettingItem(
                            title = stringResource(Res.string.item_remind_time_before),
                            onClick = { showRemindTimeDialog = true }
                        ) {
                            Text(
                                text = stringResource(Res.string.remind_time_minutes_format, appSettings.remindBeforeMinutes.toString()),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        SettingItem(
                            title = stringResource(Res.string.dialog_title_auto_mode_selection),
                            onClick = { showAutoModeDialog = true }
                        ) {
                            Text(
                                text = stringResource(androidSettings.autoControlMode.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isAndroid16Plus) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            SettingItem(
                                title = stringResource(Res.string.item_compat_wearable_sync),
                                subtitle = stringResource(Res.string.desc_compat_wearable_sync),
                                onClick = { viewModel.onCompatWearableSyncChanged(!androidSettings.compatWearableSync) }
                            ) {
                                Switch(
                                    checked = androidSettings.compatWearableSync,
                                    onCheckedChange = { viewModel.onCompatWearableSyncChanged(it) }
                                )
                            }
                        }
                    }
                }

                // 权限标题
                Text(
                    text = stringResource(Res.string.section_title_permission),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 16.dp, start = 4.dp)
                )

                // 权限卡片
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        PermissionItem(
                            title = stringResource(Res.string.item_exact_alarm_permission),
                            isGranted = permissions.hasExactAlarmPermission,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = "package:${context.packageName}".toUri()
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        PermissionItem(
                            title = stringResource(Res.string.item_dnd_permission),
                            isGranted = permissions.hasDndPermission,
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            }
                        )

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        PermissionItem(
                            title = stringResource(Res.string.item_ignore_battery_optimization),
                            isGranted = permissions.isIgnoringBatteryOptimizations,
                            onClick = {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(intent)
                            }
                        )

                        if (isAndroid16Plus) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            PermissionItem(
                                title = stringResource(Res.string.item_promoted_notifications),
                                isGranted = permissions.canPostPromotedNotifications,
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }

                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                        SettingItem(
                            title = stringResource(Res.string.item_background_and_autostart),
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.chevron_right_24px),
                                contentDescription = null
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
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

            if (showAutoModeDialog) {
                AutoModeSelectionDialog(
                    currentMode = androidSettings.autoControlMode,
                    onDismiss = { showAutoModeDialog = false },
                    onConfirm = { mode ->
                        showAutoModeDialog = false
                        handleAutoModeSelected(mode)
                    }
                )
            }

            if (showNotificationPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showNotificationPermissionDialog = false },
                    title = { Text(stringResource(Res.string.dialog_title_notification_permission_denied)) },
                    text = { Text(stringResource(Res.string.dialog_text_notification_permission_denied)) },
                    confirmButton = {
                        Button(onClick = {
                            showNotificationPermissionDialog = false
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        }) {
                            Text(stringResource(Res.string.action_go_to_settings))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotificationPermissionDialog = false }) {
                            Text(stringResource(Res.string.action_cancel))
                        }
                    }
                )
            }

            if (showExactAlarmDialog) {
                AlertDialog(
                    onDismissRequest = { showExactAlarmDialog = false },
                    title = { Text(stringResource(Res.string.dialog_title_exact_alarm_permission)) },
                    text = { Text(stringResource(Res.string.dialog_text_exact_alarm_permission)) },
                    confirmButton = {
                        Button(onClick = {
                            showExactAlarmDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = "package:${context.packageName}".toUri()
                                }
                                context.startActivity(intent)
                            }
                        }) {
                            Text(stringResource(Res.string.action_go_to_settings))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExactAlarmDialog = false }) {
                            Text(stringResource(Res.string.action_cancel))
                        }
                    }
                )
            }

            if (showDndPermissionDialog) {
                AlertDialog(
                    onDismissRequest = { showDndPermissionDialog = false },
                    title = { Text(stringResource(Res.string.dialog_title_dnd_permission)) },
                    text = { Text(stringResource(Res.string.dialog_text_dnd_permission)) },
                    confirmButton = {
                        Button(onClick = {
                            showDndPermissionDialog = false
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }) {
                            Text(stringResource(Res.string.action_go_to_settings))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDndPermissionDialog = false }) {
                            Text(stringResource(Res.string.action_cancel))
                        }
                    }
                )
            }
        }
    }
}

/** 统一的设置项 */
@Composable
private fun SettingItem(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        trailingContent()
    }
}

/** 权限项：右侧展示“已授权/未授权”状态 */
@Composable
private fun PermissionItem(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    SettingItem(title = title, onClick = onClick) {
        Text(
            text = stringResource(if (isGranted) Res.string.status_authorized else Res.string.status_unauthorized),
            style = MaterialTheme.typography.bodyMedium,
            color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** 提前提醒时间输入弹窗，限制范围 0~60 分钟 */
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

/** 上课自动模式选择弹窗 */
@Composable
private fun AutoModeSelectionDialog(
    currentMode: AutoControlMode,
    onDismiss: () -> Unit,
    onConfirm: (AutoControlMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_title_auto_mode_selection)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().selectableGroup()) {
                AutoControlMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedMode == mode, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(mode.labelRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedMode) }) {
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