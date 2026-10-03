package com.xingheyuzhuan.shiguangschedule.ui.settings.conversion

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage
import com.xingheyuzhuan.shiguangschedule.tool.ExternalFileManager
import com.xingheyuzhuan.shiguangschedule.tool.FileManagerCallbacks
import com.xingheyuzhuan.shiguangschedule.tool.rememberFileManager
import com.xingheyuzhuan.shiguangschedule.ui.components.ShareDialog
import kotlinx.coroutines.launch
import okio.Buffer
import okio.FileSystem
import okio.SYSTEM
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_details
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.desc_backup_restore
import shiguangschedule.shared.generated.resources.desc_export_ics_with_alarm
import shiguangschedule.shared.generated.resources.desc_export_json_with_config
import shiguangschedule.shared.generated.resources.desc_import_json
import shiguangschedule.shared.generated.resources.desc_school_import_quick
import shiguangschedule.shared.generated.resources.desc_sync_to_system_calendar
import shiguangschedule.shared.generated.resources.item_backup_restore
import shiguangschedule.shared.generated.resources.item_export_course_file
import shiguangschedule.shared.generated.resources.item_export_ics_file
import shiguangschedule.shared.generated.resources.item_import_course_file
import shiguangschedule.shared.generated.resources.item_school_system_import
import shiguangschedule.shared.generated.resources.item_sync_to_system_calendar
import shiguangschedule.shared.generated.resources.section_file_conversion
import shiguangschedule.shared.generated.resources.section_school_import
import shiguangschedule.shared.generated.resources.section_sync
import shiguangschedule.shared.generated.resources.snackbar_file_save_canceled
import shiguangschedule.shared.generated.resources.snackbar_file_selection_canceled
import shiguangschedule.shared.generated.resources.title_conversion
import kotlin.time.Clock

/**
 * 课表导入导出与转换设置主界面。
 * 整合了跨平台文件导入导出、教务系统导入、系统日历同步等功能。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTableConversionScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: CourseTableConversionViewModel = koinViewModel(),
    appStorage: AppStorage = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pendingFile by ExternalFileManager.pendingFile.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val snackbarFileSelectionCanceled = stringResource(Res.string.snackbar_file_selection_canceled)
    val snackbarFileSaveCanceled = stringResource(Res.string.snackbar_file_save_canceled)

    var pendingImportTableId by remember { mutableStateOf<String?>(null) }

    // 用于暂存导出的缓存路径和触发 ShareDialog 的路径状态
    var pendingShareFilePath by remember { mutableStateOf<String?>(null) }
    var shareFilePath by remember { mutableStateOf<String?>(null) }
    var shareFileMimeType by remember { mutableStateOf("application/json") }

    LaunchedEffect(pendingFile) {
        if (pendingFile?.targetDestination == Destination.CourseTableConversion) {
            viewModel.onImportClick()
        }
    }

    val fileManager = rememberFileManager(
        callbacks = FileManagerCallbacks(
            onFileImported = { bytes, _ ->
                val tableId = pendingImportTableId
                if (bytes != null && tableId != null) {
                    val source = Buffer().write(bytes)
                    viewModel.handleFileImport(tableId, source)
                } else if (bytes == null) {
                    coroutineScope.launch { snackbarHostState.showSnackbar(snackbarFileSelectionCanceled) }
                }
                pendingImportTableId = null
            },
            onFileExported = { success ->
                if (success) {
                    shareFilePath = pendingShareFilePath
                } else {
                    pendingShareFilePath = null
                    coroutineScope.launch { snackbarHostState.showSnackbar(snackbarFileSaveCanceled) }
                }
            }
        )
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ConversionEvent.LaunchImportFilePicker -> {
                    pendingImportTableId = event.tableId
                    fileManager.importFile(listOf("json"))
                }
                is ConversionEvent.LaunchExportFileCreator -> {
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    val fileName = "shiguangschedule_$timestamp.json"
                    val bytes = event.jsonContent.encodeToByteArray()

                    val shareTempDir = appStorage.cacheDir / "share_temp"
                    val tempFilePath = shareTempDir / fileName
                    FileSystem.SYSTEM.createDirectories(shareTempDir)
                    FileSystem.SYSTEM.write(tempFilePath) {
                        write(bytes)
                    }
                    pendingShareFilePath = tempFilePath.toString()
                    shareFileMimeType = "application/json"
                    fileManager.exportFile(fileName, bytes)
                }
                is ConversionEvent.LaunchExportIcsFileCreator -> {
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    val fileName = "shiguangschedule_$timestamp.ics"
                    val bytes = event.icsContent.encodeToByteArray()
                    val shareTempDir = appStorage.cacheDir / "share_temp"
                    val tempFilePath = shareTempDir / fileName
                    FileSystem.SYSTEM.createDirectories(shareTempDir)
                    FileSystem.SYSTEM.write(tempFilePath) {
                        write(bytes)
                    }
                    pendingShareFilePath = tempFilePath.toString()
                    shareFileMimeType = "text/calendar"

                    fileManager.exportFile(fileName, bytes)
                }
                is ConversionEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(Res.string.title_conversion)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_back_24px),
                                contentDescription = stringResource(Res.string.a11y_back)
                            )
                        }
                    }
                )
                if (uiState.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text(stringResource(Res.string.section_file_conversion), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConversionRow(
                        title = stringResource(Res.string.item_import_course_file),
                        desc = stringResource(Res.string.desc_import_json),
                        onClick = { viewModel.onImportClick() }
                    )
                    HorizontalDivider()
                    ConversionRow(
                        title = stringResource(Res.string.item_export_course_file),
                        desc = stringResource(Res.string.desc_export_json_with_config),
                        onClick = { viewModel.onExportClick() }
                    )
                    HorizontalDivider()
                    ConversionRow(
                        title = stringResource(Res.string.item_export_ics_file),
                        desc = stringResource(Res.string.desc_export_ics_with_alarm),
                        onClick = { viewModel.onExportIcsClick() }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(stringResource(Res.string.section_school_import), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConversionRow(
                        title = stringResource(Res.string.item_school_system_import),
                        desc = stringResource(Res.string.desc_school_import_quick),
                        onClick = { onNavigate(Destination.SchoolSelectionListScreen) }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(stringResource(Res.string.section_sync), style = MaterialTheme.typography.titleLarge, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConversionRow(
                        title = stringResource(Res.string.item_sync_to_system_calendar),
                        desc = stringResource(Res.string.desc_sync_to_system_calendar),
                        onClick = { viewModel.onSyncToCalendarClick() }
                    )
                    HorizontalDivider()
                    ConversionRow(
                        title = stringResource(Res.string.item_backup_restore),
                        desc = stringResource(Res.string.desc_backup_restore),
                        onClick = { onNavigate(Destination.BackupAndRestore) }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    ConversionDialogOverlay(
        uiState = uiState,
        onDismiss = {
            viewModel.dismissDialog()
            ExternalFileManager.consumeFile()
        },
        onConfirmImport = { tableId ->
            val externalFile = ExternalFileManager.consumeFile()
            if (externalFile?.targetDestination == Destination.CourseTableConversion) {
                val source = Buffer().write(externalFile.bytes)
                viewModel.handleFileImport(tableId, source)
                viewModel.dismissDialog()
            } else {
                viewModel.onImportTableSelected(tableId)
            }
        },
        onConfirmExport = { id, mins -> viewModel.onExportTableSelected(id, mins) }
    )

    shareFilePath?.let { path ->
        ShareDialog(
            filePath = path,
            mimeType = shareFileMimeType,
            onDismiss = {
                shareFilePath = null
                pendingShareFilePath = null
            }
        )
    }
}

/**
 * 转换页面的列表行子组件。
 */
@Composable
private fun ConversionRow(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = vectorResource(Res.drawable.chevron_right_24px),
            contentDescription = stringResource(Res.string.a11y_details),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}