package com.xingheyuzhuan.shiguangschedule.ui.settings.quickactions.tweaks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.model.Holiday
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository.TweakMode
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_arrow
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.a11y_save_tweak
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_custom_date
import shiguangschedule.shared.generated.resources.action_select_table
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.arrow_downward_24px
import shiguangschedule.shared.generated.resources.arrow_drop_down_24px
import shiguangschedule.shared.generated.resources.arrow_forward_24px
import shiguangschedule.shared.generated.resources.check_24px
import shiguangschedule.shared.generated.resources.course_time_day_section_details_tweak
import shiguangschedule.shared.generated.resources.course_time_day_time_details_tweak
import shiguangschedule.shared.generated.resources.date_format_month_day
import shiguangschedule.shared.generated.resources.dialog_title_select_export_table
import shiguangschedule.shared.generated.resources.dialog_title_select_workday
import shiguangschedule.shared.generated.resources.double_arrow_24px
import shiguangschedule.shared.generated.resources.label_select_tweak_table
import shiguangschedule.shared.generated.resources.label_tweak_from_date
import shiguangschedule.shared.generated.resources.label_tweak_to_date
import shiguangschedule.shared.generated.resources.sync_alt_24px
import shiguangschedule.shared.generated.resources.text_error
import shiguangschedule.shared.generated.resources.text_no_course
import shiguangschedule.shared.generated.resources.text_tweak_hint
import shiguangschedule.shared.generated.resources.title_tweak_from_course
import shiguangschedule.shared.generated.resources.title_tweak_schedule
import shiguangschedule.shared.generated.resources.title_tweak_to_course
import shiguangschedule.shared.generated.resources.tweak_mode_exchange
import shiguangschedule.shared.generated.resources.tweak_mode_merge
import shiguangschedule.shared.generated.resources.tweak_mode_overwrite
import shiguangschedule.shared.generated.resources.week_days_full_names
import kotlin.time.Instant

@Composable
fun UiTextRes.asString(): String {
    return if (args.isEmpty()) {
        stringResource(resource)
    } else {
        stringResource(resource, *args.toTypedArray())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweakScheduleScreen(
    onBack: () -> Unit,
    viewModel: TweakScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCourseTablePicker by remember { mutableStateOf(false) }
    var showFromDatePicker by remember { mutableStateOf(false) }

    // 控制弹窗类型
    var showWorkdayQuickPicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    val titleTweakSchedule = stringResource(Res.string.title_tweak_schedule)
    val a11yBack = stringResource(Res.string.a11y_back)
    val a11ySaveTweak = stringResource(Res.string.a11y_save_tweak)
    val labelSelectTweakTable = stringResource(Res.string.label_select_tweak_table)
    val actionSelectTable = stringResource(Res.string.action_select_table)
    val labelTweakFromDate = stringResource(Res.string.label_tweak_from_date)
    val labelTweakToDate = stringResource(Res.string.label_tweak_to_date)
    val textTweakHint = stringResource(Res.string.text_tweak_hint)
    val titleTweakFromCourse = stringResource(Res.string.title_tweak_from_course)
    val titleTweakToCourse = stringResource(Res.string.title_tweak_to_course)
    val dialogTitleSelectExportTable = stringResource(Res.string.dialog_title_select_export_table)
    val a11yArrow = stringResource(Res.string.a11y_arrow)

    val errorMessageText = uiState.errorMessage?.asString()
    val successMessageText = uiState.successMessage?.asString()

    LaunchedEffect(errorMessageText) {
        errorMessageText?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetMessages()
        }
    }

    LaunchedEffect(successMessageText) {
        successMessageText?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(titleTweakSchedule) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = a11yBack
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.moveCourses() }) {
                        Icon(imageVector = vectorResource(Res.drawable.check_24px), contentDescription = a11ySaveTweak)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = labelSelectTweakTable, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showCourseTablePicker = true }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = uiState.selectedCourseTable?.name ?: actionSelectTable)
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_forward_24px),
                                contentDescription = actionSelectTable,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    DateButton(label = labelTweakFromDate, date = uiState.fromDate, onClick = { showFromDatePicker = true })
                    DateButton(
                        label = labelTweakToDate,
                        date = uiState.toDate,
                        onClick = {
                            if (uiState.workdays.isNotEmpty()) {
                                showWorkdayQuickPicker = true
                            } else {
                                showToDatePicker = true
                            }
                        }
                    )
                }
            }

            item {
                Text(
                    text = textTweakHint,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isLandscape = maxWidth > maxHeight
                    val (modeIcon, _) = getTweakModeDisplayInfo(uiState.tweakMode)

                    if (isLandscape) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CourseDisplayCard(modifier = Modifier.weight(1f), title = titleTweakFromCourse, courses = uiState.fromCourses)

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TweakModeSelector(currentMode = uiState.tweakMode, onModeSelected = { viewModel.onTweakModeChanged(it) })
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = a11yArrow,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            CourseDisplayCard(modifier = Modifier.weight(1f), title = titleTweakToCourse, courses = uiState.toCourses)
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CourseDisplayCard(title = titleTweakFromCourse, courses = uiState.fromCourses)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                val verticalIcon = when (uiState.tweakMode) {
                                    TweakMode.EXCHANGE -> vectorResource(Res.drawable.sync_alt_24px)
                                    TweakMode.OVERWRITE -> vectorResource(Res.drawable.double_arrow_24px)
                                    TweakMode.MERGE -> vectorResource(Res.drawable.arrow_downward_24px)
                                }

                                val rotationAngle = if (uiState.tweakMode != TweakMode.MERGE) 90f else 0f

                                Icon(
                                    imageVector = verticalIcon,
                                    contentDescription = a11yArrow,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .rotate(rotationAngle),
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                TweakModeSelector(
                                    currentMode = uiState.tweakMode,
                                    onModeSelected = { viewModel.onTweakModeChanged(it) }
                                )
                            }

                            CourseDisplayCard(title = titleTweakToCourse, courses = uiState.toCourses)
                        }
                    }
                }
            }
        }
    }

    if (showCourseTablePicker) {
        CourseTablePickerDialog(
            title = dialogTitleSelectExportTable,
            onDismissRequest = { showCourseTablePicker = false },
            onTableSelected = { viewModel.onCourseTableSelected(it); showCourseTablePicker = false }
        )
    }

    if (showFromDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(uiState.fromDate) {
                uiState.fromDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            viewModel.onFromDateSelected(selectedMillis.toLocalDate())
                        }
                        showFromDatePicker = false
                    }
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showFromDatePicker = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }

    // 快捷补班选择弹窗
    if (showWorkdayQuickPicker) {
        WorkdayQuickPickerDialog(
            toDate = uiState.toDate,
            workdays = uiState.workdays,
            onDismissRequest = { showWorkdayQuickPicker = false },
            onDateSelected = { selectedDate ->
                viewModel.onToDateSelected(selectedDate)
                showWorkdayQuickPicker = false
            },
            onOpenFullDatePicker = {
                showWorkdayQuickPicker = false
                showToDatePicker = true
            }
        )
    }

    // 原生通用 DatePicker 弹窗
    if (showToDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = remember(uiState.toDate) {
                uiState.toDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            }
        )

        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            viewModel.onToDateSelected(selectedMillis.toLocalDate())
                        }
                        showToDatePicker = false
                    }
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showToDatePicker = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkdayQuickPickerDialog(
    toDate: LocalDate,
    workdays: List<Holiday>,
    onDismissRequest: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onOpenFullDatePicker: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(Res.string.dialog_title_select_workday),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(workdays) { holiday ->
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${holiday.name}:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        holiday.dates.sorted().forEach { date ->
                            val isSelected = date == toDate
                            val labelText = "${date.year}/${date.month.number}/${date.day}"

                            FilterChip(
                                selected = isSelected,
                                onClick = { onDateSelected(date) },
                                label = {
                                    Text(
                                        text = labelText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenFullDatePicker) {
                Text(stringResource(Res.string.action_custom_date))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}
@Composable
private fun TweakModeSelector(currentMode: TweakMode, onModeSelected: (TweakMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val (_, label) = getTweakModeDisplayInfo(currentMode)

    Box {
        TextButton(onClick = { expanded = true }) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Icon(vectorResource(Res.drawable.arrow_drop_down_24px), contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TweakMode.entries.forEach { mode ->
                val (mIcon, mLabel) = getTweakModeDisplayInfo(mode)
                DropdownMenuItem(
                    leadingIcon = { Icon(mIcon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text(mLabel) },
                    onClick = { onModeSelected(mode); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun getTweakModeDisplayInfo(mode: TweakMode): Pair<ImageVector, String> {
    return when (mode) {
        TweakMode.MERGE -> vectorResource(Res.drawable.arrow_forward_24px) to stringResource(Res.string.tweak_mode_merge)
        TweakMode.OVERWRITE -> vectorResource(Res.drawable.double_arrow_24px) to stringResource(Res.string.tweak_mode_overwrite)
        TweakMode.EXCHANGE -> vectorResource(Res.drawable.sync_alt_24px) to stringResource(Res.string.tweak_mode_exchange)
    }
}

@Composable
fun CourseDisplayCard(title: String, courses: List<CourseWithWeeks>, modifier: Modifier = Modifier) {
    val textNoCourse = stringResource(Res.string.text_no_course)
    val sectionFormatRes = Res.string.course_time_day_section_details_tweak
    val customTimeFormatRes = Res.string.course_time_day_time_details_tweak

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                if (courses.isEmpty()) {
                    item { Text(text = textNoCourse, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(8.dp)) }
                } else {
                    items(courses) { courseWithWeeks: CourseWithWeeks ->
                        val course = courseWithWeeks.course
                        val dayString = getLocalizedDayString(course.day)
                        val detailsText = if (course.isCustomTime) {
                            stringResource(customTimeFormatRes, dayString, course.customStartTime ?: "??:??", course.customEndTime ?: "??:??")
                        } else {
                            stringResource(sectionFormatRes, dayString, (course.startSection ?: 0).toString(), (course.endSection ?: 0).toString())
                        }
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(text = course.name, style = MaterialTheme.typography.bodyLarge)
                            Text(text = detailsText, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateButton(label: String, date: LocalDate, onClick: () -> Unit) {
    val dateDisplay = stringResource(
        Res.string.date_format_month_day,
        date.month.number,
        date.day
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onClick) { Text(text = dateDisplay, style = MaterialTheme.typography.titleMedium) }
    }
}

private fun Long.toLocalDate(): LocalDate =
    Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

@Composable
private fun getLocalizedDayString(day: Int): String {
    val weekDays = stringArrayResource(Res.array.week_days_full_names)
    return if (day in 1..7) weekDays[day - 1] else stringResource(Res.string.text_error)
}