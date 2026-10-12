package com.xingheyuzhuan.shiguangschedule.ui.settings.time

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.ui.components.ToastManager
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.components.DefaultDurationSettings
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.components.TimeSlotEditContent
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.components.TimeSlotItem
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.components.calculateInitialTimes
import com.xingheyuzhuan.shiguangschedule.ui.settings.time.components.parseLocalTimeSafely
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_add_time_slot
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_save_all_settings
import shiguangschedule.shared.generated.resources.add_24px
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.common_action_continue_editing
import shiguangschedule.shared.generated.resources.common_action_exit_without_save
import shiguangschedule.shared.generated.resources.common_dialog_msg_unsaved_changes
import shiguangschedule.shared.generated.resources.common_dialog_title_abandon_changes
import shiguangschedule.shared.generated.resources.error_name_cannot_be_empty
import shiguangschedule.shared.generated.resources.label_schedule_scheme_name
import shiguangschedule.shared.generated.resources.save_24px
import shiguangschedule.shared.generated.resources.text_no_time_slots_hint
import shiguangschedule.shared.generated.resources.title_edit_exclusive_schedule
import shiguangschedule.shared.generated.resources.title_edit_public_schedule
import shiguangschedule.shared.generated.resources.toast_settings_saved
import shiguangschedule.shared.generated.resources.toast_slot_added_unsaved
import shiguangschedule.shared.generated.resources.toast_slot_modified_unsaved
import shiguangschedule.shared.generated.resources.toast_slot_removed_unsaved

/**
 * 单个作息方案编辑界面（包括公共作息与专属作息）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleScheduleEditScreen(
    tableId: String?,
    isPublic: Boolean,
    copyFromId: String? = null,
    onBack: () -> Unit
) {
    val viewModel: SingleScheduleEditViewModel = koinViewModel(
        parameters = {
            parametersOf(
                tableId ?: "",
                isPublic,
                copyFromId ?: ""
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val toastSettingsSaved = stringResource(Res.string.toast_settings_saved)

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            ToastManager.show(toastSettingsSaved)
            onBack()
        }
    }

    LaunchedEffect(uiState.errorMsg) {
        val msg = uiState.errorMsg
        if (!msg.isNullOrBlank()) {
            ToastManager.show(msg)
            viewModel.clearError()
        }
    }

    if (!uiState.isDataLoaded) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // 本地编辑状态缓存
    val localTimeSlots = remember {
        mutableStateListOf<TimeSlot>().apply { addAll(uiState.slots.sortedBy { it.number }) }
    }
    var localName by remember { mutableStateOf(uiState.name) }
    var localDefaultClassDuration by remember { mutableIntStateOf(uiState.defaultClassDuration) }
    var localDefaultBreakDuration by remember { mutableIntStateOf(uiState.defaultBreakDuration) }

    var showExitConfirmDialog by remember { mutableStateOf(false) }
    var showEditBottomSheet by remember { mutableStateOf(false) }
    var editingTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }

    // 公共作息模式下的名称有效性校验
    val isNameValid = !uiState.isPublic || localName.isNotBlank()

    // 监听数据加载，同步服务器/本地初始数据
    LaunchedEffect(uiState.isDataLoaded) {
        if (uiState.isDataLoaded) {
            localTimeSlots.clear()
            localTimeSlots.addAll(uiState.slots.sortedBy { it.number })
            localName = uiState.name
            localDefaultClassDuration = uiState.defaultClassDuration
            localDefaultBreakDuration = uiState.defaultBreakDuration
        }
    }

    // 未保存变更检查
    val hasUnsavedChanges = remember(
        localTimeSlots.toList(),
        localName,
        localDefaultClassDuration,
        localDefaultBreakDuration,
        uiState
    ) {
        val nameChanged = uiState.isPublic && localName != uiState.name
        val slotsChanged = localTimeSlots.toList() != uiState.slots.sortedBy { it.number }
        val durationChanged = localDefaultClassDuration != uiState.defaultClassDuration ||
                localDefaultBreakDuration != uiState.defaultBreakDuration
        nameChanged || slotsChanged || durationChanged
    }

    val handleBackPress = {
        if (hasUnsavedChanges) {
            showExitConfirmDialog = true
        } else {
            onBack()
        }
    }

    val navEventState = rememberNavigationEventState(currentInfo = NavigationEventInfo.None)
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = true,
        onBackCompleted = { handleBackPress() }
    )

    val a11yBack = stringResource(Res.string.a11y_back)
    val a11yAddTimeSlot = stringResource(Res.string.a11y_add_time_slot)
    val a11ySaveAllSettings = stringResource(Res.string.a11y_save_all_settings)
    val toastSlotRemovedUnsaved = stringResource(Res.string.toast_slot_removed_unsaved)
    val textNoTimeSlotsHint = stringResource(Res.string.text_no_time_slots_hint)
    val toastSlotModifiedUnsaved = stringResource(Res.string.toast_slot_modified_unsaved)
    val toastSlotAddedUnsaved = stringResource(Res.string.toast_slot_added_unsaved)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isPublic) stringResource(Res.string.title_edit_public_schedule)
                        else stringResource(Res.string.title_edit_exclusive_schedule)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBackPress) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = a11yBack
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editingTimeSlot = null
                        showEditBottomSheet = true
                    }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.add_24px),
                            contentDescription = a11yAddTimeSlot
                        )
                    }
                    IconButton(
                        onClick = {
                            val sortedAndNumberedSlots = localTimeSlots
                                .sortedBy { parseLocalTimeSafely(it.startTime) }
                                .mapIndexed { index, slot -> slot.copy(number = index + 1) }

                            if (uiState.isPublic) {
                                viewModel.onNameChange(localName)
                            }
                            viewModel.onDefaultDurationChange(localDefaultClassDuration, localDefaultBreakDuration)

                            viewModel.saveWithData(
                                newName = localName,
                                classDuration = localDefaultClassDuration,
                                breakDuration = localDefaultBreakDuration,
                                slots = sortedAndNumberedSlots
                            )
                        },
                        enabled = isNameValid
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.save_24px),
                            contentDescription = a11ySaveAllSettings
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                HorizontalDivider()

                if (uiState.isPublic) {
                    OutlinedTextField(
                        value = localName,
                        onValueChange = { localName = it },
                        label = { Text(stringResource(Res.string.label_schedule_scheme_name)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        singleLine = true,
                        isError = !isNameValid,
                        supportingText = if (!isNameValid) {
                            {
                                Text(
                                    text = stringResource(Res.string.error_name_cannot_be_empty),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else null
                    )
                }

                DefaultDurationSettings(
                    defaultClassDuration = localDefaultClassDuration,
                    onClassDurationChange = { newValue -> localDefaultClassDuration = newValue },
                    defaultBreakDuration = localDefaultBreakDuration,
                    onBreakDurationChange = { newValue -> localDefaultBreakDuration = newValue }
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                if (localTimeSlots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(textNoTimeSlotsHint)
                    }
                }
            }

            // 时间段列表项
            itemsIndexed(
                items = localTimeSlots,
                key = { _, slot -> "${slot.number}-${slot.startTime}-${slot.endTime}" }
            ) { _, timeSlot ->
                TimeSlotItem(
                    timeSlot = timeSlot,
                    onEditClick = {
                        editingTimeSlot = timeSlot
                        showEditBottomSheet = true
                    },
                    onDeleteClick = {
                        localTimeSlots.removeAll { it.number == timeSlot.number }
                        val renumberedList = localTimeSlots
                            .sortedBy { parseLocalTimeSafely(it.startTime) }
                            .mapIndexed { i, slot -> slot.copy(number = i + 1) }
                        localTimeSlots.clear()
                        localTimeSlots.addAll(renumberedList)
                        ToastManager.show(toastSlotRemovedUnsaved)
                    }
                )
            }
        }

        // 时间段编辑/新增底部弹窗
        if (showEditBottomSheet) {
            val sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
            )
            val isEditing = editingTimeSlot != null
            val (initialStart, initialEnd) = calculateInitialTimes(
                isEditing = isEditing,
                editingTimeSlot = editingTimeSlot,
                localTimeSlots = localTimeSlots,
                breakDur = localDefaultBreakDuration,
                classDur = localDefaultClassDuration
            )

            ModalBottomSheet(
                onDismissRequest = {
                    showEditBottomSheet = false
                    editingTimeSlot = null
                },
                sheetState = sheetState
            ) {
                TimeSlotEditContent(
                    existingTimeSlots = localTimeSlots.toList(),
                    initialNumber = editingTimeSlot?.number ?: (localTimeSlots.maxOfOrNull { it.number }?.plus(1) ?: 1),
                    initialStartTime = initialStart,
                    initialEndTime = initialEnd,
                    initialAlias = editingTimeSlot?.alias,
                    isEditing = isEditing,
                    onDismiss = {
                        showEditBottomSheet = false
                        editingTimeSlot = null
                    },
                    onConfirm = { number, startTime, endTime, alias ->
                        val newOrUpdatedSlot = TimeSlot(
                            number = number,
                            startTime = startTime,
                            endTime = endTime,
                            timeTableId = tableId ?: "",
                            alias = alias
                        )

                        val updatedList = localTimeSlots.toMutableList()
                        if (isEditing) {
                            val targetIdx = updatedList.indexOfFirst { it.number == editingTimeSlot?.number }
                            if (targetIdx != -1) {
                                updatedList[targetIdx] = newOrUpdatedSlot
                                ToastManager.show(toastSlotModifiedUnsaved)
                            }
                        } else {
                            updatedList.add(newOrUpdatedSlot)
                            ToastManager.show(toastSlotAddedUnsaved)
                        }

                        val finalSorted = updatedList
                            .sortedBy { parseLocalTimeSafely(it.startTime) }
                            .mapIndexed { i, slot -> slot.copy(number = i + 1) }

                        localTimeSlots.clear()
                        localTimeSlots.addAll(finalSorted)

                        showEditBottomSheet = false
                        editingTimeSlot = null
                    }
                )
            }
        }

        // 放弃修改确认对话框
        if (showExitConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showExitConfirmDialog = false },
                title = { Text(text = stringResource(Res.string.common_dialog_title_abandon_changes)) },
                text = { Text(text = stringResource(Res.string.common_dialog_msg_unsaved_changes)) },
                confirmButton = {
                    TextButton(onClick = {
                        showExitConfirmDialog = false
                        onBack()
                    }) {
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
}