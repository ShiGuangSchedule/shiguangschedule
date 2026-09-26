package com.xingheyuzhuan.shiguangschedule.ui.settings.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_delete
import shiguangschedule.shared.generated.resources.a11y_save
import shiguangschedule.shared.generated.resources.action_add
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.check_24px
import shiguangschedule.shared.generated.resources.common_action_continue_editing
import shiguangschedule.shared.generated.resources.common_action_exit_without_save
import shiguangschedule.shared.generated.resources.common_dialog_msg_unsaved_changes
import shiguangschedule.shared.generated.resources.common_dialog_title_abandon_changes
import shiguangschedule.shared.generated.resources.delete_24px
import shiguangschedule.shared.generated.resources.label_course_name
import shiguangschedule.shared.generated.resources.title_add_course
import shiguangschedule.shared.generated.resources.title_edit_course
import shiguangschedule.shared.generated.resources.toast_delete_success
import shiguangschedule.shared.generated.resources.toast_name_empty
import shiguangschedule.shared.generated.resources.toast_save_success
import shiguangschedule.shared.generated.resources.toast_time_invalid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditCourseScreen(
    onBack: () -> Unit,
    courseId: String? = null,
    viewModel: AddEditCourseViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(courseId) {
        viewModel.initWithId(courseId)
    }

    // 状态追踪：记录当前正在操作哪一个方案
    var activeSchemeId by remember { mutableStateOf<String?>(null) }

    // 弹窗控制状态
    var showWeekSelectorDialog by remember { mutableStateOf(false) }
    var showColorSelectorDialog by remember { mutableStateOf(false) }
    var showTimePickerSelector by remember { mutableStateOf(false) }
    var showDayPickerDialog by remember { mutableStateOf(false) }

    // 拦截退出弹窗状态
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    // 提示文本资源
    val saveSuccessText = stringResource(Res.string.toast_save_success)
    val deleteSuccessText = stringResource(Res.string.toast_delete_success)
    val nameEmptyText = stringResource(Res.string.toast_name_empty)
    val toastTimeInvalid = stringResource(Res.string.toast_time_invalid)

    // 统一的退出拦截逻辑
    val handleBackPress = {
        if (viewModel.hasUnsavedChanges()) {
            showExitConfirmDialog = true
        } else {
            onBack()
        }
    }

    val navEventState = rememberNavigationEventState(
        currentInfo = NavigationEventInfo.None
    )

    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = true,
        onBackCompleted = {
            handleBackPress()
        }
    )

    // 处理 ViewModel 事件
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                UiEvent.SaveSuccess -> {
                    ToastManager.show(saveSuccessText)
                    onBack()
                }
                UiEvent.DeleteSuccess -> {
                    ToastManager.show(deleteSuccessText)
                    onBack()
                }
                UiEvent.Cancel -> onBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.isEditing) stringResource(Res.string.title_edit_course)
                        else stringResource(Res.string.title_add_course)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(Res.string.a11y_back)
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = viewModel::onDelete) {
                            Icon(vectorResource(Res.drawable.delete_24px), contentDescription = stringResource(Res.string.a11y_delete))
                        }
                    }
                    IconButton(
                        onClick = {
                            if (uiState.name.isBlank()) {
                                ToastManager.show(nameEmptyText)
                            } else {
                                val allValid = uiState.schemes.all { s ->
                                    if (s.isCustomTime) {
                                        s.customStartTime.isNotBlank() && s.customEndTime.isNotBlank() && s.customStartTime < s.customEndTime
                                    } else {
                                        s.startSection <= s.endSection
                                    }
                                }
                                if (allValid) viewModel.onSave()
                                else ToastManager.show(toastTimeInvalid)
                            }
                        }
                    ) {
                        Icon(vectorResource(Res.drawable.check_24px), contentDescription = stringResource(Res.string.a11y_save))
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 课程名称输入
            item {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text(stringResource(Res.string.label_course_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 方案卡片列表
            items(uiState.schemes, key = { it.id }) { scheme ->
                CourseSchemeCard(
                    scheme = scheme,
                    courseColorMaps = uiState.courseColorMaps,
                    timeSlots = uiState.timeSlots,
                    onTeacherChange = { newTeacher ->
                        viewModel.updateScheme(scheme.id) { it.copy(teacher = newTeacher) }
                    },
                    onPositionChange = { newPos ->
                        viewModel.updateScheme(scheme.id) { it.copy(position = newPos) }
                    },
                    onCreditChange = { newCredit ->
                        viewModel.updateScheme(scheme.id) { it.copy(credit = newCredit) }
                    },
                    onRemarkChange = { newRemark ->
                        viewModel.onSchemeRemarkChange(scheme.id, newRemark)
                    },
                    onColorClick = {
                        activeSchemeId = scheme.id
                        showColorSelectorDialog = true
                    },
                    onTimeClick = {
                        activeSchemeId = scheme.id
                        showTimePickerSelector = true
                    },
                    onWeeksClick = {
                        activeSchemeId = scheme.id
                        showWeekSelectorDialog = true
                    },
                    onDayClick = {
                        activeSchemeId = scheme.id
                        showDayPickerDialog = true
                    },
                    onRemoveClick = { viewModel.removeScheme(scheme.id) },
                    onToggleCustomTime = { isCustom ->
                        viewModel.toggleCustomTime(scheme.id, isCustom)
                    },
                    showRemoveButton = uiState.schemes.size > 1
                )
            }

            // 添加方案按钮
            item {
                Button(
                    onClick = viewModel::addScheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(vectorResource(Res.drawable.add_24px), contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.action_add),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    // --- 弹窗逻辑区块 ---
    val activeScheme = uiState.schemes.find { it.id == activeSchemeId }

    if (activeScheme != null) {
        // 周次选择器
        if (showWeekSelectorDialog) {
            WeekSelectorBottomSheet(
                totalWeeks = uiState.semesterTotalWeeks,
                selectedWeeks = activeScheme.weeks,
                onDismissRequest = { showWeekSelectorDialog = false },
                onConfirm = { weeks: Set<Int> ->
                    viewModel.updateScheme(activeScheme.id) { it.copy(weeks = weeks) }
                    showWeekSelectorDialog = false
                }
            )
        }

        // 颜色选择器
        if (showColorSelectorDialog) {
            ColorPickerBottomSheet(
                colorMaps = uiState.courseColorMaps,
                selectedIndex = activeScheme.colorIndex,
                showApplyToAllOption = uiState.schemes.size > 1,
                onDismissRequest = { showColorSelectorDialog = false },
                onConfirm = { index: Int, applyToAll: Boolean ->
                    if (applyToAll) {
                        viewModel.updateAllSchemesColor(index)
                    } else {
                        viewModel.updateScheme(activeScheme.id) { it.copy(colorIndex = index) }
                    }
                    showColorSelectorDialog = false
                }
            )
        }

        // 时间/节次选择器
        if (showTimePickerSelector) {
            if (activeScheme.isCustomTime) {
                CustomTimeRangePickerBottomSheet(
                    initialStartTime = activeScheme.customStartTime.ifBlank { "08:00" },
                    initialEndTime = activeScheme.customEndTime.ifBlank { "09:45" },
                    onDismissRequest = { showTimePickerSelector = false },
                    onTimeRangeSelected = { start, end ->
                        viewModel.updateScheme(activeScheme.id) { it.copy(customStartTime = start, customEndTime = end) }
                        showTimePickerSelector = false
                    }
                )
            } else {
                CourseTimePickerBottomSheet(
                    selectedDay = activeScheme.day,
                    onDaySelected = { d -> viewModel.updateScheme(activeScheme.id) { it.copy(day = d) } },
                    startSection = activeScheme.startSection,
                    onStartSectionChange = { s -> viewModel.updateScheme(activeScheme.id) { it.copy(startSection = s) } },
                    endSection = activeScheme.endSection,
                    onEndSectionChange = { e -> viewModel.updateScheme(activeScheme.id) { it.copy(endSection = e) } },
                    timeSlots = uiState.timeSlots,
                    onDismissRequest = { showTimePickerSelector = false }
                )
            }
        }

        // 星期选择器 (用于自定义模式下的简单星期切换)
        if (showDayPickerDialog) {
            DayPickerDialog(
                selectedDay = activeScheme.day,
                onDismissRequest = { showDayPickerDialog = false },
                onDaySelected = { newDay ->
                    viewModel.updateScheme(activeScheme.id) { it.copy(day = newDay) }
                    showDayPickerDialog = false
                }
            )
        }
    }

    // 退出确认弹窗
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = {
                Text(text = stringResource(Res.string.common_dialog_title_abandon_changes))
            },
            text = {
                Text(text = stringResource(Res.string.common_dialog_msg_unsaved_changes))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitConfirmDialog = false
                        onBack()
                    }
                ) {
                    Text(text = stringResource(Res.string.common_action_exit_without_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(text = stringResource(Res.string.common_action_continue_editing))
                }
            }
        )
    }
}