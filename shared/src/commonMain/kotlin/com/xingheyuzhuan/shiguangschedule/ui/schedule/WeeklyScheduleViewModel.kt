package com.xingheyuzhuan.shiguangschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeScheduleRepository
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.ScheduleGridStyleComposed.Companion.toComposedStyle
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.ScheduleConfigPackage
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.WeeklyScheduleUiState
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.CourseMergeUtils.gridScaleToTime
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.CourseMergeUtils.mergeCourses
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.calculateCurrentSectionIndex
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.formatToHHmm
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.getTodayLocalDate
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.startOfWeek
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.koin.core.annotation.KoinViewModel
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)
@KoinViewModel
class WeeklyScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val timeScheduleRepository: TimeScheduleRepository,
    private val styleSettingsRepository: StyleSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyScheduleUiState())
    val uiState: StateFlow<WeeklyScheduleUiState> = _uiState.asStateFlow()

    private val _pagerMondayDate = MutableStateFlow(
        getTodayLocalDate().startOfWeek(DayOfWeek.MONDAY)
    )

    private val appSettingsFlow = appSettingsRepository.getAppSettings()
    private val styleFlow = styleSettingsRepository.styleFlow

    private val courseTableConfigFlow = appSettingsFlow.flatMapLatest { settings ->
        val tableId = settings.currentCourseTableId
        if (tableId.isNotEmpty()) {
            appSettingsRepository.getCourseTableConfigFlow(tableId)
        } else {
            flowOf(null)
        }
    }

    // 侧边栏时间/节次流
    private val timeSlotsFlow = combine(
        appSettingsFlow,
        _pagerMondayDate,
        courseTableConfigFlow
    ) { settings, pagerDate, config ->
        val tableId = settings.currentCourseTableId
        val today = getTodayLocalDate()

        val firstDayOfWeekInt = config?.firstDayOfWeek ?: DayOfWeek.MONDAY.isoDayNumber
        val firstDayOfWeek = DayOfWeek(firstDayOfWeekInt)

        val currentWeekStart = today.startOfWeek(firstDayOfWeek)

        val isCurrentWeek = pagerDate == currentWeekStart

        // 本周使用真实的今天日期 today（以显示今天对应的特殊/轮番作息），非本周使用该周起始日 pagerDate
        val targetDateForSidebar = if (isCurrentWeek) today else pagerDate
        tableId to targetDateForSidebar
    }.flatMapLatest { (tableId, targetDate) ->
        if (tableId.isNotEmpty()) {
            timeScheduleRepository.observeEffectiveTimeSlots(tableId, targetDate)
        } else {
            flowOf(emptyList())
        }
    }

    private val currentCoursesFlow = combine(
        _pagerMondayDate,
        appSettingsFlow,
        courseTableConfigFlow,
        styleFlow
    ) { date, settings, config, style ->
        val tableId = settings.currentCourseTableId
        val mode = style.toComposedStyle().scheduleMode

        if (config != null && tableId.isNotEmpty()) {
            val window = listOf(
                date.minus(1, DateTimeUnit.WEEK),
                date,
                date.plus(1, DateTimeUnit.WEEK)
            )

            combine(window.map { day ->
                val pageWeekNum = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = day,
                    startDateStr = config.semesterStartDate,
                    firstDayOfWeekInt = config.firstDayOfWeek
                )

                val isWithinSemester = pageWeekNum != null && pageWeekNum in 1..config.semesterTotalWeeks

                val coursesFlow = if (settings.showNonCurrentWeekCourses && isWithinSemester) {
                    courseTableRepository.getCoursesWithWeeksByTableId(tableId).map { allCourses ->
                        allCourses.filter { cw ->
                            cw.weeks.any { it.weekNumber >= pageWeekNum }
                        }
                    }
                } else {
                    courseTableRepository.getCoursesWithWeeksByDate(tableId, day, config)
                }

                val daySlotsFlow = timeScheduleRepository.observeEffectiveTimeSlots(tableId, day)

                combine(coursesFlow, daySlotsFlow) { courses, daySlots ->
                    day.toString() to mergeCourses(courses, daySlots, pageWeekNum ?: -1, mode)
                }
            }) { results -> results.toMap() }
        } else {
            flowOf(emptyMap())
        }
    }.flatMapLatest { it }

    init {
        viewModelScope.launch {
            val configAndTimeFlow = combine(
                appSettingsFlow,
                courseTableConfigFlow,
                styleFlow,
                _pagerMondayDate
            ) { settings, config, style, mondayDate ->
                ScheduleConfigPackage(settings, config, style, mondayDate)
            }

            combine(configAndTimeFlow, currentCoursesFlow, timeSlotsFlow) { configPkg, cache, timeSlots ->
                val config = configPkg.config
                val startDate = config?.semesterStartDate?.let { LocalDate.parse(it) }
                val firstDayOfWeekInt = config?.firstDayOfWeek ?: DayOfWeek.MONDAY.isoDayNumber
                val totalWeeks = config?.semesterTotalWeeks ?: 20
                val today = getTodayLocalDate()

                val currentWeekNum = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = today,
                    startDateStr = config?.semesterStartDate,
                    firstDayOfWeekInt = firstDayOfWeekInt
                )

                val weekIndex = appSettingsRepository.getWeekIndexAtDate(
                    targetDate = configPkg.mondayDate,
                    startDateStr = config?.semesterStartDate,
                    firstDayOfWeekInt = firstDayOfWeekInt
                )

                val currentSectionIndex = calculateCurrentSectionIndex(timeSlots)

                val daysUntil = if (startDate != null && today < startDate) {
                    today.daysUntil(startDate).toLong()
                } else 0L

                val currentWeekCourses = cache[configPkg.mondayDate.toString()] ?: emptyList()
                fixInvalidCourseColors(currentWeekCourses.flatMap { it.courses }, configPkg.style)

                val previousState = _uiState.value

                WeeklyScheduleUiState(
                    style = configPkg.style,
                    showWeekends = config?.showWeekends ?: false,
                    totalWeeks = totalWeeks,
                    courseCache = cache,
                    currentMergedCourses = cache[configPkg.mondayDate.toString()] ?: emptyList(),
                    timeSlots = timeSlots,
                    isSemesterSet = startDate != null,
                    semesterStartDate = startDate,
                    firstDayOfWeek = firstDayOfWeekInt,
                    weekIndexInPager = weekIndex,
                    currentWeekNumber = currentWeekNum,
                    pagerMondayDate = configPkg.mondayDate,
                    currentSectionIndex = currentSectionIndex,
                    daysUntilStart = daysUntil,
                    floatingCourse = previousState.floatingCourse,
                    floatingSourceWeek = previousState.floatingSourceWeek
                )
            }.collect { _uiState.value = it }
        }
    }

    fun updatePagerDate(newDate: LocalDate) = _pagerMondayDate.update { newDate }

    fun switchCourseTable(tableId: String) {
        viewModelScope.launch {
            val currentSettings = appSettingsRepository.getAppSettingsOnce()
            val newSettings = currentSettings.copy(currentCourseTableId = tableId)
            appSettingsRepository.insertOrUpdateAppSettings(newSettings)
        }
    }

    private fun fixInvalidCourseColors(courses: List<CourseWithWeeks>, style: ScheduleGridStyle) {
        viewModelScope.launch {
            val validRange = style.courseColorMaps.indices
            courses.forEach { cw ->
                if (cw.course.colorInt !in validRange) {
                    courseTableRepository.updateCourseColor(cw.course.id, style.generateRandomColorIndex())
                }
            }
        }
    }

    /**
     * 进入跨周移动暂存状态
     */
    fun enterFloatingMode(course: CourseWithWeeks, sourceWeek: Int) {
        _uiState.update {
            it.copy(
                floatingCourse = course,
                floatingSourceWeek = sourceWeek
            )
        }
    }

    /**
     * 全清空或取消挂起队列
     */
    fun exitFloatingMode() {
        _uiState.update {
            it.copy(
                floatingCourse = null,
                floatingSourceWeek = null
            )
        }
    }

    /**
     * 配合跨周结算的最终持久化落地更新
     */
    fun updateCourseTimeByFloatingGesture(
        targetWeek: Int,
        targetDay: Int,
        startSection: Float,
        endSection: Float,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val courseWrapper = state.floatingCourse ?: return@launch
                val sourceWeek = state.floatingSourceWeek ?: return@launch
                val mode = state.style.toComposedStyle().scheduleMode
                val slots = state.timeSlots

                val currentSettings = appSettingsRepository.getAppSettingsOnce()
                val tableId = currentSettings.currentCourseTableId
                if (tableId.isBlank()) return@launch

                val originalCourse = courseWrapper.course

                val updatedCourseForTime = if (mode == ScheduleModeProto.TIME_24H_MODE) {
                    val baseStartTime = gridScaleToTime(startSection, slots, mode)
                    val origStart = LocalTime.parse(originalCourse.customStartTime ?: "08:00")
                    val origEnd = LocalTime.parse(originalCourse.customEndTime ?: "09:00")
                    val originalDurationMinutes = ((origEnd.toSecondOfDay() - origStart.toSecondOfDay()) / 60).coerceAtLeast(1)
                    val newStartTime = baseStartTime
                    val startMinutesFromMidnight = newStartTime.hour * 60 + newStartTime.minute
                    val rawEndMinutes = startMinutesFromMidnight + originalDurationMinutes

                    val (finalEndTime, isTruncatedToMidnight) = if (rawEndMinutes >= 1440) {
                        LocalTime(23, 59) to true
                    } else {
                        val endSec = (newStartTime.toSecondOfDay() + originalDurationMinutes * 60) % (24 * 3600)
                        LocalTime(endSec / 3600, (endSec % 3600) / 60) to false
                    }
                    val calcStartSection = newStartTime.hour + 1

                    val finalEndSection = if (isTruncatedToMidnight) {
                        24
                    } else {
                        val calcEndSection = if (finalEndTime.minute > 0) finalEndTime.hour + 1 else finalEndTime.hour
                        if (calcEndSection == 0) 24 else calcEndSection
                    }

                    originalCourse.copy(
                        day = targetDay,
                        isCustomTime = true,
                        customStartTime = newStartTime.formatToHHmm(),
                        customEndTime = finalEndTime.formatToHHmm(),
                        startSection = calcStartSection.coerceIn(1, 24),
                        endSection = finalEndSection.coerceIn(1, 24)
                    )
                } else {
                    val newStartSection = startSection.toInt().coerceIn(1, slots.size)
                    val newEndSection = endSection.toInt().coerceIn(1, slots.size)
                    if (newStartSection > newEndSection) return@launch

                    originalCourse.copy(
                        day = targetDay,
                        isCustomTime = false,
                        customStartTime = null,
                        customEndTime = null,
                        startSection = newStartSection,
                        endSection = newEndSection
                    )
                }

                val isNoPositionChange = originalCourse.day == updatedCourseForTime.day &&
                        originalCourse.startSection == updatedCourseForTime.startSection &&
                        originalCourse.endSection == updatedCourseForTime.endSection &&
                        originalCourse.customStartTime == updatedCourseForTime.customStartTime &&
                        originalCourse.customEndTime == updatedCourseForTime.customEndTime

                if (sourceWeek == targetWeek && isNoPositionChange) {
                    return@launch
                }

                val isSingleWeek = courseWrapper.weeks.size <= 1

                if (isSingleWeek) {
                    val weekNumbers = listOf(targetWeek)
                    courseTableRepository.upsertCourse(updatedCourseForTime, weekNumbers)
                } else {
                    val remainingWeeks = courseWrapper.weeks
                        .map { it.weekNumber }
                        .filter { it != sourceWeek }
                    courseTableRepository.upsertCourse(originalCourse, remainingWeeks)

                    val clonedNewId = Uuid.random().toString()
                    val finalClonedCourse = updatedCourseForTime.copy(id = clonedNewId)
                    courseTableRepository.upsertCourse(finalClonedCourse, listOf(targetWeek))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update {
                    it.copy(
                        floatingCourse = null,
                        floatingSourceWeek = null
                    )
                }
                onComplete()
            }
        }
    }

    /**
     * 统一持久化调度手势调课方法（拆分并更新单周/多周周次逻辑）
     */
    fun updateCourseTimeByGesture(
        courseId: String,
        targetDay: Int,
        startSection: Float,
        endSection: Float,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val mode = state.style.toComposedStyle().scheduleMode
                val slots = state.timeSlots
                val currentWeek = state.weekIndexInPager ?: state.currentWeekNumber ?: return@launch

                val currentSettings = appSettingsRepository.getAppSettingsOnce()
                val tableId = currentSettings.currentCourseTableId
                if (tableId.isBlank()) return@launch

                val allCoursesWithWeeks = courseTableRepository.getCoursesWithWeeksByTableId(tableId).firstOrNull() ?: return@launch
                val targetWrapper = allCoursesWithWeeks.find { it.course.id == courseId } ?: return@launch
                val originalCourse = targetWrapper.course

                val updatedCourseForTime = if (mode == ScheduleModeProto.TIME_24H_MODE) {
                    val newStartTime = gridScaleToTime(startSection, slots, mode)
                    val newEndTime = gridScaleToTime(endSection, slots, mode)

                    originalCourse.copy(
                        day = targetDay,
                        isCustomTime = true,
                        customStartTime = newStartTime.formatToHHmm(),
                        customEndTime = newEndTime.formatToHHmm(),
                        startSection = (startSection.toInt() + 1).coerceIn(1, 24),
                        endSection = (endSection.toInt() + 1).coerceIn(1, 24)
                    )
                } else {
                    val newStartSection = (startSection.toInt() + 1).coerceIn(1, slots.size)
                    val newEndSection = endSection.toInt().coerceIn(1, slots.size)
                    if (newStartSection > newEndSection) return@launch

                    originalCourse.copy(
                        day = targetDay,
                        isCustomTime = false,
                        customStartTime = null,
                        customEndTime = null,
                        startSection = newStartSection,
                        endSection = newEndSection
                    )
                }
                val isNoPositionChange = originalCourse.day == updatedCourseForTime.day &&
                        originalCourse.startSection == updatedCourseForTime.startSection &&
                        originalCourse.endSection == updatedCourseForTime.endSection &&
                        originalCourse.customStartTime == updatedCourseForTime.customStartTime &&
                        originalCourse.customEndTime == updatedCourseForTime.customEndTime

                if (isNoPositionChange) {
                    return@launch
                }

                val isSingleWeek = targetWrapper.weeks.size <= 1

                if (isSingleWeek) {
                    val weekNumbers = targetWrapper.weeks.map { it.weekNumber }
                    courseTableRepository.upsertCourse(updatedCourseForTime, weekNumbers)
                } else {
                    val remainingWeeks = targetWrapper.weeks
                        .map { it.weekNumber }
                        .filter { it != currentWeek }
                    courseTableRepository.upsertCourse(originalCourse, remainingWeeks)

                    val clonedNewId = Uuid.random().toString()
                    val finalClonedCourse = updatedCourseForTime.copy(id = clonedNewId)
                    courseTableRepository.upsertCourse(finalClonedCourse, listOf(currentWeek))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete()
            }
        }
    }
}