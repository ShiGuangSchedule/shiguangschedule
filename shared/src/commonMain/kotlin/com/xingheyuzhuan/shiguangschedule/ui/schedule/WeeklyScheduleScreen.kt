package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.navigation.AddEditCourseChannel
import com.xingheyuzhuan.shiguangschedule.navigation.PresetCourseData
import com.xingheyuzhuan.shiguangschedule.ui.components.CourseTablePickerDialog
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.CourseDetailBottomSheet
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.FloatingCourseBar
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGrid
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridActions
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridViewState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.WeekSelectorBottomSheet
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.rememberScheduleGridState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_select_table
import shiguangschedule.shared.generated.resources.arrow_drop_down_24px
import shiguangschedule.shared.generated.resources.format_week_display
import shiguangschedule.shared.generated.resources.snackbar_add_course_within_semester
import shiguangschedule.shared.generated.resources.swap_horiz_24px
import shiguangschedule.shared.generated.resources.title_current_week
import shiguangschedule.shared.generated.resources.title_semester_not_set
import shiguangschedule.shared.generated.resources.title_vacation
import shiguangschedule.shared.generated.resources.title_vacation_until_start
import kotlin.time.Clock

private const val INFINITE_PAGER_CENTER = Int.MAX_VALUE / 2

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WeeklyScheduleScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    onNavHideFractionChanged: (Float) -> Unit = {},
    viewModel: WeeklyScheduleViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarMsg = stringResource(Res.string.snackbar_add_course_within_semester)

    val pagerState = rememberPagerState(
        initialPage = INFINITE_PAGER_CENTER,
        pageCount = { Int.MAX_VALUE }
    )

    fun getPreviousOrSameDay(date: LocalDate, targetDayOfWeek: DayOfWeek): LocalDate {
        var current = date
        while (current.dayOfWeek != targetDayOfWeek) {
            current = current.plus(-1, DateTimeUnit.DAY)
        }
        return current
    }

    LaunchedEffect(pagerState.currentPage, uiState.firstDayOfWeek) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                val offsetWeeks = (pageIndex - INFINITE_PAGER_CENTER).toLong()
                val firstDay = DayOfWeek(uiState.firstDayOfWeek)
                val thisMonday = getPreviousOrSameDay(today, firstDay)
                val targetMonday = thisMonday.plus(offsetWeeks * 7, DateTimeUnit.DAY)
                viewModel.updatePagerDate(targetMonday)
            }
    }

    var showWeekSelector by remember { mutableStateOf(false) }
    var showTableSwitcher by remember { mutableStateOf(false) }
    var isGridHolding by remember { mutableStateOf(false) }
    var selectedBlockForDetail by remember { mutableStateOf<MergedCourseBlock?>(null) }

    val composedStyle by remember(uiState.style) {
        derivedStateOf { with(ScheduleGridStyleComposed) { uiState.style.toComposedStyle() } }
    }

    val floatingCourse = uiState.floatingCourse

    val floatingDuration by remember(floatingCourse, composedStyle.scheduleMode) {
        derivedStateOf {
            if (floatingCourse != null) {
                val start = floatingCourse.course.startSection?.toFloat() ?: 1f
                val end = floatingCourse.course.endSection?.toFloat() ?: 1f

                if (composedStyle.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
                    (end - start).coerceAtLeast(1.0f)
                } else {
                    (end - start + 1f).coerceAtLeast(1.0f)
                }
            } else {
                1.0f
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridScrollState = rememberScrollState()

    val customTextColor = composedStyle.pageTextColor ?: MaterialTheme.colorScheme.onSurface
    val customSubTextColor = customTextColor.copy(alpha = 0.7f)

    val (displayTitle, titleTextColor) = when {
        !uiState.isSemesterSet || uiState.semesterStartDate == null -> {
            stringResource(Res.string.title_semester_not_set) to MaterialTheme.colorScheme.error
        }
        uiState.daysUntilStart > 0 -> {
            stringResource(Res.string.title_vacation_until_start, uiState.daysUntilStart.toString()) to customTextColor
        }
        uiState.weekIndexInPager != null && uiState.weekIndexInPager!! in 1..uiState.totalWeeks -> {
            stringResource(Res.string.title_current_week, uiState.weekIndexInPager.toString()) to customTextColor
        }
        else -> {
            stringResource(Res.string.title_vacation) to customTextColor
        }
    }

    val collapseFraction = scrollBehavior.state.collapsedFraction
    val navHideFraction = if (floatingCourse != null) 1f else collapseFraction

    LaunchedEffect(navHideFraction) {
        onNavHideFractionChanged(navHideFraction)
    }

    val systemNavigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val dynamicBottomOffset = systemNavigationBarInset + (88.dp * (1f - navHideFraction))

    Box(modifier = Modifier.fillMaxSize()) {
        if (composedStyle.backgroundImagePath.isNotEmpty()) {
            AsyncImage(
                model = composedStyle.backgroundImagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    if (!uiState.isSemesterSet || uiState.semesterStartDate == null) {
                                        onNavigate(Destination.Settings)
                                    } else {
                                        showWeekSelector = true
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = titleTextColor
                            )
                            Icon(
                                imageVector = vectorResource(Res.drawable.arrow_drop_down_24px),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp)
                                    .offset(y = (-4).dp),
                                tint = customSubTextColor
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTableSwitcher = true }) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.swap_horiz_24px),
                                contentDescription = stringResource(Res.string.action_select_table),
                                tint = customTextColor
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                    ),
                    scrollBehavior = scrollBehavior
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = dynamicBottomOffset)
                )
            }
        ) { scaffoldInnerPadding ->

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(
                        start = scaffoldInnerPadding.calculateStartPadding(LayoutDirection.Ltr),
                        top = scaffoldInnerPadding.calculateTopPadding(),
                        end = scaffoldInnerPadding.calculateEndPadding(LayoutDirection.Ltr)
                    )
                    .fillMaxSize(),
                beyondViewportPageCount = 1,
                userScrollEnabled = !isGridHolding
            ) { pageIndex ->

                val pageMondayDate = remember(pageIndex, uiState.firstDayOfWeek) {
                    val offsetWeeks = (pageIndex - INFINITE_PAGER_CENTER).toLong()
                    val firstDay = DayOfWeek(uiState.firstDayOfWeek)
                    getPreviousOrSameDay(today, firstDay).plus(offsetWeeks * 7, DateTimeUnit.DAY)
                }

                val pageYearString = remember(pageMondayDate) {
                    pageMondayDate.year.toString()
                }

                val pageDateStrings = remember(pageMondayDate) {
                    (0..6).map { i ->
                        val d = pageMondayDate.plus(i.toLong(), DateTimeUnit.DAY)
                        val month = d.month.number.toString().padStart(2, '0')
                        val day = d.day.toString().padStart(2, '0')
                        "$month-$day"
                    }
                }

                val pageTodayIndex = remember(pageMondayDate) {
                    val weekDates = (0..6).map { pageMondayDate.plus(it.toLong(), DateTimeUnit.DAY) }
                    weekDates.indexOf(today)
                }

                val pageCourses = uiState.courseCache[pageMondayDate.toString()] ?: emptyList()
                val gridState = rememberScheduleGridState(gridScrollState = gridScrollState)

                val weekIndex = uiState.weekIndexInPager
                val totalWeeks = uiState.totalWeeks
                val weekStr = if (weekIndex != null && weekIndex in 1..totalWeeks) {
                    stringResource(Res.string.format_week_display, weekIndex)
                } else {
                    null
                }

                val gridViewState = remember(pageDateStrings, pageYearString, uiState, pageCourses, pageTodayIndex, weekStr) {
                    ScheduleGridViewState(
                        dates = pageDateStrings,
                        currentYear = pageYearString,
                        currentWeek = weekStr,
                        timeSlots = uiState.timeSlots,
                        mergedCourses = pageCourses,
                        showWeekends = uiState.showWeekends,
                        todayIndex = pageTodayIndex,
                        firstDayOfWeek = uiState.firstDayOfWeek,
                        currentSectionIndex = if (pageTodayIndex >= 0) uiState.currentSectionIndex else -1
                    )
                }

                val gridActions = remember(uiState, floatingDuration, snackbarMsg) {
                    object : ScheduleGridActions {
                        override fun onCourseBlockClicked(block: MergedCourseBlock) {
                            selectedBlockForDetail = block
                        }

                        override fun onGridCellClicked(day: Int, section: Int) {
                            if (floatingCourse != null) {
                                val targetWeek = uiState.weekIndexInPager ?: uiState.currentWeekNumber ?: return
                                val startSec = section.toFloat()
                                val endSec = if (composedStyle.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
                                    startSec + floatingDuration
                                } else {
                                    startSec + floatingDuration - 1f
                                }

                                coroutineScope.launch {
                                    viewModel.updateCourseTimeByFloatingGesture(
                                        targetWeek = targetWeek,
                                        targetDay = day,
                                        startSection = startSec,
                                        endSection = endSec
                                    )
                                }
                            } else {
                                val currentWeek = uiState.weekIndexInPager ?: 0
                                val isCurrentPageValid = currentWeek in 1..uiState.totalWeeks

                                if (isCurrentPageValid) {
                                    coroutineScope.launch {
                                        val currentWeekSet = setOf(currentWeek)
                                        val presetData = if (composedStyle.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
                                            val startHour = section.coerceIn(0, 23)
                                            val endHour = (startHour + 1) % 24

                                            val startTimeStr = "${startHour.toString().padStart(2, '0')}:00"
                                            val endTimeStr = "${endHour.toString().padStart(2, '0')}:00"

                                            PresetCourseData(
                                                day = day,
                                                isCustomTime = true,
                                                customStartTime = startTimeStr,
                                                customEndTime = endTimeStr,
                                                presetWeeks = currentWeekSet
                                            )
                                        } else {
                                            PresetCourseData(
                                                day = day,
                                                startSection = section,
                                                endSection = section,
                                                isCustomTime = false,
                                                presetWeeks = currentWeekSet
                                            )
                                        }

                                        AddEditCourseChannel.sendEvent(presetData)
                                        onNavigate(Destination.AddEditCourse())
                                    }
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(snackbarMsg)
                                    }
                                }
                            }
                        }

                        override fun onTimeSlotClicked() {
                            onNavigate(Destination.TimeScheduleManagement)
                        }

                        override fun onHoldStateChanged(isHolding: Boolean) {
                            isGridHolding = isHolding
                        }

                        override fun onCourseMovedWithinGrid(
                            block: MergedCourseBlock,
                            newDay: Int,
                            newStartSection: Float,
                            newEndSection: Float
                        ) {
                            val currentWeek = uiState.weekIndexInPager ?: 0
                            if (currentWeek in 1..uiState.totalWeeks) {
                                block.courses.firstOrNull()?.course?.id?.let { courseId ->
                                    coroutineScope.launch {
                                        viewModel.updateCourseTimeByGesture(
                                            courseId = courseId,
                                            targetDay = newDay,
                                            startSection = newStartSection,
                                            endSection = newEndSection
                                        )
                                    }
                                }
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarMsg) }
                            }
                        }

                        override fun onCourseTimeAdjusted(
                            block: MergedCourseBlock,
                            newStart: Float,
                            newEnd: Float
                        ) {
                            val currentWeek = uiState.weekIndexInPager ?: 0
                            if (currentWeek in 1..uiState.totalWeeks) {
                                block.courses.firstOrNull()?.course?.id?.let { courseId ->
                                    coroutineScope.launch {
                                        viewModel.updateCourseTimeByGesture(
                                            courseId = courseId,
                                            targetDay = block.day,
                                            startSection = newStart,
                                            endSection = newEnd
                                        )
                                    }
                                }
                            } else {
                                coroutineScope.launch { snackbarHostState.showSnackbar(snackbarMsg) }
                            }
                        }

                        override fun onInitiateFloatingMode(block: MergedCourseBlock) {
                            val targetCourseWrapper = block.courses.firstOrNull()
                            val currentWeek = uiState.weekIndexInPager ?: uiState.currentWeekNumber
                            if (targetCourseWrapper != null && currentWeek != null) {
                                viewModel.enterFloatingMode(
                                    course = targetCourseWrapper,
                                    sourceWeek = currentWeek
                                )
                            }
                        }
                    }
                }

                ScheduleGrid(
                    state = gridState,
                    viewState = gridViewState,
                    actions = gridActions,
                    style = composedStyle,
                    modifier = Modifier.padding(bottom = systemNavigationBarInset)
                )
            }
        }

        FloatingCourseBar(
            floatingCourse = floatingCourse,
            onCancelClick = { viewModel.exitFloatingMode() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp + systemNavigationBarInset)
        )
    }

    // 周次选择弹窗
    if (showWeekSelector) {
        WeekSelectorBottomSheet(
            totalWeeks = uiState.totalWeeks,
            currentWeek = uiState.currentWeekNumber ?: 1,
            selectedWeek = uiState.weekIndexInPager ?: (uiState.currentWeekNumber ?: 1),
            onWeekSelected = { week ->
                val currentWeekAtPage = uiState.weekIndexInPager ?: 1
                val offset = week - currentWeekAtPage
                coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + offset)
                }
                showWeekSelector = false
            },
            onDismissRequest = { showWeekSelector = false }
        )
    }

    // 课表切换弹窗
    if (showTableSwitcher) {
        CourseTablePickerDialog(
            title = stringResource(Res.string.action_select_table),
            onDismissRequest = { showTableSwitcher = false },
            onTableSelected = { table: CourseTable ->
                viewModel.switchCourseTable(table.id)
                showTableSwitcher = false
            }
        )
    }

    // 课程详情弹窗
    if (selectedBlockForDetail != null) {
        CourseDetailBottomSheet(
            block = selectedBlockForDetail!!,
            onDismissRequest = { selectedBlockForDetail = null },
            onEditClick = { courseId ->
                selectedBlockForDetail = null
                onNavigate(Destination.AddEditCourse(courseId = courseId))
            }
        )
    }
}