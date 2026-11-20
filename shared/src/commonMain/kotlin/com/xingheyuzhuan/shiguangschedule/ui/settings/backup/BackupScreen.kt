package com.xingheyuzhuan.shiguangschedule.ui.settings.backup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.tool.FileManagerCallbacks
import com.xingheyuzhuan.shiguangschedule.tool.rememberFileManager
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroupCard
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsIcon
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsRow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import okio.Buffer
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_reset
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.backup_target_local_zip
import shiguangschedule.shared.generated.resources.backup_target_webdav
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.cloud_24px
import shiguangschedule.shared.generated.resources.desc_backup_data
import shiguangschedule.shared.generated.resources.desc_restore_data
import shiguangschedule.shared.generated.resources.desc_webdav_connected
import shiguangschedule.shared.generated.resources.desc_webdav_path_hint
import shiguangschedule.shared.generated.resources.desc_webdav_unconfigured
import shiguangschedule.shared.generated.resources.dialog_title_backup_target
import shiguangschedule.shared.generated.resources.dialog_title_config_webdav
import shiguangschedule.shared.generated.resources.dialog_title_restore_source
import shiguangschedule.shared.generated.resources.download_24px
import shiguangschedule.shared.generated.resources.error_stream_open_failed
import shiguangschedule.shared.generated.resources.error_webdav_unconfigured
import shiguangschedule.shared.generated.resources.item_backup_data
import shiguangschedule.shared.generated.resources.item_backup_restore
import shiguangschedule.shared.generated.resources.item_restore_data
import shiguangschedule.shared.generated.resources.item_webdav_config
import shiguangschedule.shared.generated.resources.label_webdav_account
import shiguangschedule.shared.generated.resources.label_webdav_path
import shiguangschedule.shared.generated.resources.label_webdav_pwd_empty
import shiguangschedule.shared.generated.resources.label_webdav_pwd_saved
import shiguangschedule.shared.generated.resources.label_webdav_url
import shiguangschedule.shared.generated.resources.section_data_maintenance
import shiguangschedule.shared.generated.resources.section_service_config
import shiguangschedule.shared.generated.resources.title_loading
import shiguangschedule.shared.generated.resources.toast_operation_failed
import shiguangschedule.shared.generated.resources.toast_operation_success
import shiguangschedule.shared.generated.resources.upload_24px
import kotlin.time.Clock

/**
 * 备份/恢复的目标媒介枚举
 */
enum class BackupTarget(val stringRes: StringResource) {
    WEBDAV(Res.string.backup_target_webdav),
    LOCAL_ZIP(Res.string.backup_target_local_zip)
}

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = koinViewModel()
) {
    val settingPadding = 16.dp

    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showConfigDialog by remember { mutableStateOf(false) }
    var showBackupTargetDialog by remember { mutableStateOf(false) }
    var showRestoreTargetDialog by remember { mutableStateOf(false) }

    val streamOpenFailedMsg = stringResource(Res.string.error_stream_open_failed)
    val webdavUnconfiguredMsg = stringResource(Res.string.error_webdav_unconfigured)
    val opSuccessMsg = stringResource(Res.string.toast_operation_success)
    val opFailedPrefix = stringResource(Res.string.toast_operation_failed, "")

    // 初始化 KMP 平台的 FileManager 回调
    val fileManager = rememberFileManager(
        FileManagerCallbacks(
            onFileImported = { bytes, _ ->
                if (bytes != null) {
                    scope.launch {
                        try {
                            // 将 ByteArray 转化为 Okio 的 Buffer/BufferedSource
                            val buffer = Buffer().write(bytes)
                            viewModel.importFromLocalZip(buffer)
                        } catch (_: Exception) {
                            snackbarHostState.showSnackbar(streamOpenFailedMsg)
                        }
                    }
                }
            },
            onFileExported = { success ->
                if (!success) {
                    scope.launch { snackbarHostState.showSnackbar(streamOpenFailedMsg) }
                }
            }
        )
    )

    LaunchedEffect(state.testResult) {
        when (val result = state.testResult) {
            is TestResult.Error -> {
                snackbarHostState.showSnackbar(
                    message = opFailedPrefix + result.message,
                    duration = SnackbarDuration.Short
                )
            }
            is TestResult.Success -> {
                snackbarHostState.showSnackbar(
                    message = opSuccessMsg,
                    duration = SnackbarDuration.Short
                )
            }
            TestResult.Idle -> {}
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(Res.string.item_backup_restore)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_back_24px),
                                contentDescription = stringResource(Res.string.a11y_back)
                            )
                        }
                    }
                )
                if (state.isBusy || state.isTesting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
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
                title = stringResource(Res.string.section_data_maintenance)
            ) {
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_backup_data),
                        subtitle = stringResource(Res.string.desc_backup_data),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.upload_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        enabled = !state.isBusy,
                        onClick = { showBackupTargetDialog = true }
                    )
                }
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_restore_data),
                        subtitle = stringResource(Res.string.desc_restore_data),
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.download_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        enabled = !state.isBusy,
                        onClick = { showRestoreTargetDialog = true }
                    )
                }
            }

            SettingsGroupCard(title = stringResource(Res.string.section_service_config)) {
                item {
                    SettingsRow(
                        title = stringResource(Res.string.item_webdav_config),
                        subtitle = if (state.baseUrl.isBlank()) {
                            stringResource(Res.string.desc_webdav_unconfigured)
                        } else {
                            stringResource(Res.string.desc_webdav_connected, state.baseUrl)
                        },
                        leading = { SettingsIcon(imageVector = vectorResource(Res.drawable.cloud_24px)) },
                        trailing = { SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px)) },
                        onClick = { showConfigDialog = true }
                    )
                }
            }
            Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }

    if (showConfigDialog) {
        WebDavConfigDialog(
            state = state,
            onDismiss = { showConfigDialog = false },
            onSave = { u, n, p, r ->
                viewModel.testWebDavConnection(u, n, p, r)
                showConfigDialog = false
            },
            onDisconnect = {
                viewModel.disconnectWebDav()
                showConfigDialog = false
            }
        )
    }

    if (showBackupTargetDialog) {
        TargetSelectionDialog(
            title = stringResource(Res.string.dialog_title_backup_target),
            onDismiss = { showBackupTargetDialog = false },
            onTargetSelected = { target ->
                showBackupTargetDialog = false
                when (target) {
                    BackupTarget.WEBDAV -> {
                        if (state.baseUrl.isNotBlank()) {
                            viewModel.backupToWebDav()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(webdavUnconfiguredMsg) }
                        }
                    }
                    BackupTarget.LOCAL_ZIP -> {
                        scope.launch {
                            val buffer = Buffer()
                            val isSuccess = viewModel.exportToLocalZip(buffer)

                            if (isSuccess) {
                                val bytes = buffer.readByteArray()

                                if (bytes.isNotEmpty()) {
                                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                    val year = now.year
                                    val month = now.month.number.toString().padStart(2, '0')
                                    val day = now.day.toString().padStart(2, '0')
                                    val defaultFileName = "Shiguang_Backup_${year}${month}${day}.zip"
                                    fileManager.exportFile(defaultFileName, bytes)
                                } else {
                                    snackbarHostState.showSnackbar(streamOpenFailedMsg)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showRestoreTargetDialog) {
        TargetSelectionDialog(
            title = stringResource(Res.string.dialog_title_restore_source),
            onDismiss = { showRestoreTargetDialog = false },
            onTargetSelected = { target ->
                showRestoreTargetDialog = false
                when (target) {
                    BackupTarget.WEBDAV -> {
                        if (state.baseUrl.isNotBlank()) {
                            viewModel.restoreFromWebDav()
                        } else {
                            scope.launch { snackbarHostState.showSnackbar(webdavUnconfiguredMsg) }
                        }
                    }
                    BackupTarget.LOCAL_ZIP -> {
                        fileManager.importFile(listOf("zip"))
                    }
                }
            }
        )
    }
}

@Composable
fun TargetSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onTargetSelected: (BackupTarget) -> Unit
) {
    var selectedTarget by remember { mutableStateOf(BackupTarget.entries.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                BackupTarget.entries.forEach { target ->
                    val isSelected = target == selectedTarget

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTarget = target }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { selectedTarget = target }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(target.stringRes),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        confirmButton = {
            Button(
                onClick = { onTargetSelected(selectedTarget) }
            ) {
                Text(stringResource(Res.string.action_confirm))
            }
        }
    )
}

@Composable
fun WebDavConfigDialog(
    state: BackupUiState,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    onDisconnect: () -> Unit
) {
    var inputUrl by remember { mutableStateOf(state.baseUrl) }
    var inputUsername by remember { mutableStateOf(state.username) }
    var inputPassword by remember { mutableStateOf("") }
    var inputRootPath by remember { mutableStateOf(state.rootPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_title_config_webdav)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    label = { Text(stringResource(Res.string.label_webdav_url)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputUsername,
                    onValueChange = { inputUsername = it },
                    label = { Text(stringResource(Res.string.label_webdav_account)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = { inputPassword = it },
                    label = {
                        Text(
                            if (state.hasSavedPassword) stringResource(Res.string.label_webdav_pwd_saved)
                            else stringResource(Res.string.label_webdav_pwd_empty)
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = inputRootPath,
                    onValueChange = { inputRootPath = it },
                    label = { Text(stringResource(Res.string.label_webdav_path)) },
                    supportingText = { Text(stringResource(Res.string.desc_webdav_path_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(inputUrl, inputUsername, inputPassword, inputRootPath) },
                enabled = !state.isTesting
            ) {
                Text(
                    if (state.isTesting) stringResource(Res.string.title_loading)
                    else stringResource(Res.string.action_confirm)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDisconnect()
                onDismiss()
            }) {
                Text(
                    text = stringResource(Res.string.action_reset),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}
