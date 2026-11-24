package com.xingheyuzhuan.shiguangschedule.ui.settings

import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.di.TargetPlatform
import com.xingheyuzhuan.shiguangschedule.data.di.isPlatformSupported
import com.xingheyuzhuan.shiguangschedule.ui.components.LocalScaffoldPadding
import com.xingheyuzhuan.shiguangschedule.ui.components.NativeNumberPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.SettingsGroupCard
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsIcon
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsRow
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsSwitchRow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.chevron_right_24px
import shiguangschedule.shared.generated.resources.date_format_year_month_day
import shiguangschedule.shared.generated.resources.day_of_week_monday
import shiguangschedule.shared.generated.resources.day_of_week_sunday
import shiguangschedule.shared.generated.resources.desc_course_conversion
import shiguangschedule.shared.generated.resources.desc_course_management
import shiguangschedule.shared.generated.resources.desc_current_week_manual
import shiguangschedule.shared.generated.resources.desc_first_day_of_week
import shiguangschedule.shared.generated.resources.desc_manage_course_tables
import shiguangschedule.shared.generated.resources.desc_more_options
import shiguangschedule.shared.generated.resources.desc_notification_settings
import shiguangschedule.shared.generated.resources.desc_personalization
import shiguangschedule.shared.generated.resources.desc_quick_actions
import shiguangschedule.shared.generated.resources.desc_schedule_management
import shiguangschedule.shared.generated.resources.desc_set_start_date
import shiguangschedule.shared.generated.resources.desc_show_non_current_week
import shiguangschedule.shared.generated.resources.desc_show_weekends
import shiguangschedule.shared.generated.resources.desc_total_weeks
import shiguangschedule.shared.generated.resources.dialog_title_manual_set_week
import shiguangschedule.shared.generated.resources.dialog_title_select_total_weeks
import shiguangschedule.shared.generated.resources.dialog_title_set_first_day_of_week
import shiguangschedule.shared.generated.resources.holiday_management_desc
import shiguangschedule.shared.generated.resources.holiday_management_title
import shiguangschedule.shared.generated.resources.item_course_conversion
import shiguangschedule.shared.generated.resources.item_course_management
import shiguangschedule.shared.generated.resources.item_current_week
import shiguangschedule.shared.generated.resources.item_first_day_of_week
import shiguangschedule.shared.generated.resources.item_more_options
import shiguangschedule.shared.generated.resources.item_personalization
import shiguangschedule.shared.generated.resources.item_quick_actions
import shiguangschedule.shared.generated.resources.item_set_start_date
import shiguangschedule.shared.generated.resources.item_show_non_current_week
import shiguangschedule.shared.generated.resources.item_show_weekends
import shiguangschedule.shared.generated.resources.item_total_weeks
import shiguangschedule.shared.generated.resources.more_horiz_24px
import shiguangschedule.shared.generated.resources.section_title_advanced_features
import shiguangschedule.shared.generated.resources.section_title_general_settings
import shiguangschedule.shared.generated.resources.status_current_week_format
import shiguangschedule.shared.generated.resources.status_not_set
import shiguangschedule.shared.generated.resources.status_set_start_date_first
import shiguangschedule.shared.generated.resources.status_total_weeks_format
import shiguangschedule.shared.generated.resources.title_course_notification_settings
import shiguangschedule.shared.generated.resources.title_manage_course_tables
import shiguangschedule.shared.generated.resources.title_schedule_management
import shiguangschedule.shared.generated.resources.title_schedule_settings
import shiguangschedule.shared.generated.resources.title_vacation

private val SETTING_PADDING = 16.dp
private val SECTION_SPACING = 16.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val appBarState = rememberTopAppBarState()
    var availableHeightPx by remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = appBarState,
        canScroll = { availableHeightPx > 0 },
        snapAnimationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
    )
    LaunchedEffect(availableHeightPx) {
        appBarState.heightOffset = 0f
        appBarState.contentOffset = 0f
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .onSizeChanged { availableHeightPx = it.height },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.title_schedule_settings)) },
                scrollBehavior = scrollBehavior,
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
            val courseTableConfig = uiState.courseConfig
            val displayCurrentWeek = uiState.currentWeek

            val showWeekends = courseTableConfig?.showWeekends ?: false
            val semesterStartDateString = courseTableConfig?.semesterStartDate
            val semesterTotalWeeks = courseTableConfig?.semesterTotalWeeks ?: 20
            val firstDayOfWeekInt = courseTableConfig?.firstDayOfWeek ?: DayOfWeek.MONDAY.isoDayNumber

            val semesterStartDate: LocalDate? = remember(semesterStartDateString) {
                semesterStartDateString?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            var showTotalWeeksDialog by remember { mutableStateOf(false) }
            var showManualWeekDialog by remember { mutableStateOf(false) }
            var showDatePickerModal by remember { mutableStateOf(false) }
            var showFirstDayOfWeekDialog by remember { mutableStateOf(false) }

            val scaffoldPadding = LocalScaffoldPadding.current
            val layoutDirection = LocalLayoutDirection.current

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = SETTING_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SECTION_SPACING),
                contentPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = scaffoldPadding.calculateBottomPadding() +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
            ) {
                item {
                    GeneralSettingsSection(
                        modifier = Modifier.padding(top = 8.dp),
                        showNonCurrentWeek = appSettings.showNonCurrentWeekCourses,
                        onShowNonCurrentWeekChanged = { isChecked -> viewModel.onShowNonCurrentWeekChanged(isChecked) },
                        showWeekends = showWeekends,
                        onShowWeekendsChanged = { isChecked -> viewModel.onShowWeekendsChanged(isChecked) },
                        semesterStartDate = semesterStartDate,
                        semesterTotalWeeks = semesterTotalWeeks,
                        firstDayOfWeekInt = firstDayOfWeekInt,
                        displayCurrentWeek = displayCurrentWeek,
                        onSemesterStartDateClick = { showDatePickerModal = true },
                        onSemesterTotalWeeksClick = { showTotalWeeksDialog = true },
                        onManualWeekClick = { showManualWeekDialog = true },
                        onFirstDayOfWeekClick = { showFirstDayOfWeekDialog = true },
                        onQuickActionsClick = { onNavigate(Destination.QuickActions) }
                    )
                }
                item {
                    AdvancedSettingsSection(onNavigate = onNavigate)
                }
            }

            if (showDatePickerModal) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = remember(semesterStartDate) {
                        semesterStartDate?.let {
                            LocalDate(it.year, it.month.number, it.day)
                                .atStartOfDayIn(TimeZone.UTC)
                                .toEpochMilliseconds()
                        }
                    }
                )

                DatePickerDialog(
                    onDismissRequest = { showDatePickerModal = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { viewModel.onSemesterStartDateSelected(it) }
                            showDatePickerModal = false
                        }) {
                            Text(stringResource(Res.string.action_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePickerModal = false }) {
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

            if (showTotalWeeksDialog) {
                NumberPickerDialog(
                    title = stringResource(Res.string.dialog_title_select_total_weeks),
                    range = 1..30,
                    initialValue = semesterTotalWeeks,
                    onDismiss = { showTotalWeeksDialog = false },
                    onConfirm = { selectedWeeks ->
                        viewModel.onSemesterTotalWeeksSelected(selectedWeeks)
                        showTotalWeeksDialog = false
                    }
                )
            }

            if (showManualWeekDialog) {
                ManualWeekPickerDialog(
                    totalWeeks = semesterTotalWeeks,
                    currentWeek = displayCurrentWeek,
                    onDismiss = { showManualWeekDialog = false },
                    onConfirm = { weekNumber ->
                        viewModel.onCurrentWeekManuallySet(weekNumber)
                        showManualWeekDialog = false
                    }
                )
            }

            if (showFirstDayOfWeekDialog) {
                DayOfWeekPickerDialog(
                    initialDayOfWeekInt = firstDayOfWeekInt,
                    onDismiss = { showFirstDayOfWeekDialog = false },
                    onConfirm = { selectedDayInt ->
                        viewModel.onFirstDayOfWeekSelected(selectedDayInt)
                        showFirstDayOfWeekDialog = false
                    }
                )
            }
        }
    }
}

/**
 * 通用设置卡片
 */
@Composable
private fun GeneralSettingsSection(
    modifier: Modifier = Modifier,
    showNonCurrentWeek: Boolean,
    onShowNonCurrentWeekChanged: (Boolean) -> Unit,
    showWeekends: Boolean,
    onShowWeekendsChanged: (Boolean) -> Unit,
    semesterStartDate: LocalDate?,
    semesterTotalWeeks: Int,
    firstDayOfWeekInt: Int,
    displayCurrentWeek: Int?,
    onSemesterStartDateClick: () -> Unit,
    onSemesterTotalWeeksClick: () -> Unit,
    onManualWeekClick: () -> Unit,
    onFirstDayOfWeekClick: () -> Unit,
    onQuickActionsClick: () -> Unit
) {
    SettingsGroupCard(
        modifier = modifier,
        title = stringResource(Res.string.section_title_general_settings)
    ) {
        item {
            SettingsSwitchRow(
                title = stringResource(Res.string.item_show_non_current_week),
                subtitle = stringResource(Res.string.desc_show_non_current_week),
                checked = showNonCurrentWeek,
                onCheckedChange = onShowNonCurrentWeekChanged
            )
        }

        item {
            SettingsSwitchRow(
                title = stringResource(Res.string.item_show_weekends),
                subtitle = stringResource(Res.string.desc_show_weekends),
                checked = showWeekends,
                onCheckedChange = onShowWeekendsChanged
            )
        }

        item {
            val formattedDate = semesterStartDate?.let {
                stringResource(
                    Res.string.date_format_year_month_day,
                    it.year.toString(),
                    it.month.number.toString(),
                    it.day.toString()
                )
            } ?: stringResource(Res.string.status_not_set)

            SettingsRow(
                title = stringResource(Res.string.item_set_start_date),
                subtitle = stringResource(Res.string.desc_set_start_date),
                onClick = onSemesterStartDateClick,
                trailing = {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        item {
            SettingsRow(
                title = stringResource(Res.string.item_total_weeks),
                subtitle = stringResource(Res.string.desc_total_weeks),
                onClick = onSemesterTotalWeeksClick,
                trailing = {
                    Text(
                        text = stringResource(Res.string.status_total_weeks_format, semesterTotalWeeks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        item {
            val weekStatusText = when {
                semesterStartDate == null -> stringResource(Res.string.status_set_start_date_first)
                displayCurrentWeek == null -> stringResource(Res.string.title_vacation)
                else -> stringResource(Res.string.status_current_week_format, displayCurrentWeek)
            }

            SettingsRow(
                title = stringResource(Res.string.item_current_week),
                subtitle = stringResource(Res.string.desc_current_week_manual),
                onClick = onManualWeekClick,
                trailing = {
                    Text(
                        text = weekStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        item {
            val dayText = when (firstDayOfWeekInt) {
                DayOfWeek.MONDAY.isoDayNumber -> stringResource(Res.string.day_of_week_monday)
                DayOfWeek.SUNDAY.isoDayNumber -> stringResource(Res.string.day_of_week_sunday)
                else -> stringResource(Res.string.day_of_week_monday)
            }

            SettingsRow(
                title = stringResource(Res.string.item_first_day_of_week),
                subtitle = stringResource(Res.string.desc_first_day_of_week),
                onClick = onFirstDayOfWeekClick,
                trailing = {
                    Text(
                        text = dayText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        item {
            SettingsRow(
                title = stringResource(Res.string.item_quick_actions),
                subtitle = stringResource(Res.string.desc_quick_actions),
                onClick = onQuickActionsClick,
                trailing = {
                    SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px))
                }
            )
        }
    }
}

/**
 * 高级功能卡片
 */
@Composable
private fun AdvancedSettingsSection(
    modifier: Modifier = Modifier,
    onNavigate: (Destination) -> Unit
) {
    SettingsGroupCard(
        modifier = modifier,
        title = stringResource(Res.string.section_title_advanced_features)
    ) {
        item {
            SettingsRow(
                title = stringResource(Res.string.item_course_conversion),
                subtitle = stringResource(Res.string.desc_course_conversion),
                onClick = { onNavigate(Destination.CourseTableConversion) },
                trailing = {
                    SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px))
                }
            )
        }
        item {
            SettingsRow(
                title = stringResource(Res.string.title_course_notification_settings),
                subtitle = stringResource(Res.string.desc_notification_settings),
                onClick = {
                    val destination = when {
                        isPlatformSupported(TargetPlatform.ANDROID) -> Destination.AndroidNotificationSettings
                        isPlatformSupported(TargetPlatform.IOS) -> Destination.IosNotificationSettings
                        isPlatformSupported(TargetPlatform.desktop) -> Destination.DesktopNotificationSettings
                        else -> return@SettingsRow
                    }
                    onNavigate(destination)
                },
                trailing = {
                    SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px))
                }
            )
        }
        item {
            SettingsRow(
                title = stringResource(Res.string.title_manage_course_tables),
                subtitle = stringResource(Res.string.desc_manage_course_tables),
                onClick = { onNavigate(Destination.ManageCourseTables) },
                trailing = {
                    SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px))
                }
            )
        }
        item {
            SettingsRow(
                title = stringResource(Res.string.item_course_management),
                subtitle = stringResource(Res.string.desc_course_management),
                onClick = { onNavigate(Destination.CourseManagementList) },
                trailing = {
                    SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px))
                }
            )
        }
        item {
            SettingsRow(
                title = stringResource(Res.string.title_schedule_management),
                subtitle = stringResource(Res.string.desc_schedule_management),
                onClick = { onNavigate(Destination.TimeScheduleManagement) },
                trailing = {
                    SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px))
                }
            )
        }
        item {
            SettingsRow(
                title = stringResource(Res.string.item_personalization),
                subtitle = stringResource(Res.string.desc_personalization),
                onClick = { onNavigate(Destination.StyleSettings) },
                trailing = {
                    SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px))
                }
            )
        }
        item {
            SettingsRow(
                title = stringResource(Res.string.holiday_management_title),
                subtitle = stringResource(Res.string.holiday_management_desc),
                onClick = { onNavigate(Destination.HolidayManagement) },
                trailing = {
                    SettingsIcon(imageVector = vectorResource(Res.drawable.chevron_right_24px))
                }
            )
        }
        item {
            SettingsRow(
                title = stringResource(Res.string.item_more_options),
                subtitle = stringResource(Res.string.desc_more_options),
                onClick = { onNavigate(Destination.MoreOptions) },
                trailing = {
                    SettingsIcon(imageVector = vectorResource(Res.drawable.more_horiz_24px))
                }
            )
        }
    }
}

/**
 * 手动周数选择器对话框
 */
@Composable
fun ManualWeekPickerDialog(
    totalWeeks: Int,
    currentWeek: Int?,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit
) {
    val optionOnVacationText = stringResource(Res.string.title_vacation)

    val weekOptions = listOf(optionOnVacationText) + (1..totalWeeks).map {
        stringResource(Res.string.status_current_week_format, it)
    }

    val initialSelectedValue = when (currentWeek) {
        null -> optionOnVacationText
        else -> stringResource(Res.string.status_current_week_format, currentWeek)
    }

    var dialogSelectedValue by remember { mutableStateOf(initialSelectedValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_title_manual_set_week)) },
        text = {
            NativeNumberPicker(
                values = weekOptions,
                selectedValue = dialogSelectedValue,
                onValueChange = { newValue ->
                    dialogSelectedValue = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val weekNumber = if (dialogSelectedValue == optionOnVacationText) {
                    null
                } else {
                    dialogSelectedValue.filter { it.isDigit() }.toIntOrNull()
                }
                onConfirm(weekNumber)
            }) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

/**
 * 每周起始日选择器对话框
 */
@Composable
fun DayOfWeekPickerDialog(
    initialDayOfWeekInt: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val dayOfWeekMondayText = stringResource(Res.string.day_of_week_monday)
    val dayOfWeekSundayText = stringResource(Res.string.day_of_week_sunday)

    val dayOptionsMap = mapOf(
        dayOfWeekMondayText to DayOfWeek.MONDAY.isoDayNumber,
        dayOfWeekSundayText to DayOfWeek.SUNDAY.isoDayNumber
    )
    val dayOptions = dayOptionsMap.keys.toList()

    val initialSelectedDayText = dayOptionsMap.entries.firstOrNull { it.value == initialDayOfWeekInt }?.key
        ?: dayOfWeekMondayText

    var dialogSelectedText by remember { mutableStateOf(initialSelectedDayText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.dialog_title_set_first_day_of_week)) },
        text = {
            NativeNumberPicker(
                values = dayOptions,
                selectedValue = dialogSelectedText,
                onValueChange = { newValue ->
                    dialogSelectedText = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val selectedDayInt = dayOptionsMap[dialogSelectedText] ?: DayOfWeek.MONDAY.isoDayNumber
                onConfirm(selectedDayInt)
            }) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

/**
 * 数字选择器对话框
 */
@Composable
private fun NumberPickerDialog(
    title: String,
    range: IntRange,
    initialValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var dialogSelectedValue by remember { mutableIntStateOf(initialValue.coerceIn(range)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            NativeNumberPicker(
                values = range.toList(),
                selectedValue = initialValue.coerceIn(range),
                onValueChange = { newValue ->
                    dialogSelectedValue = newValue
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(dialogSelectedValue) }) {
                Text(stringResource(Res.string.action_confirm))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}