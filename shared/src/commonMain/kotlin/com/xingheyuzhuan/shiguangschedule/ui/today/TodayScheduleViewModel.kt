package com.xingheyuzhuan.shiguangschedule.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.StyleSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeScheduleRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.KoinViewModel
import kotlin.time.Clock

@KoinViewModel
class TodayScheduleViewModel(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val styleSettingsRepository: StyleSettingsRepository,
    private val timeScheduleRepository: TimeScheduleRepository
) : ViewModel() {

    companion object {
        private const val DEFAULT_SEMESTER_TOTAL_WEEKS = 20
        private const val MAX_TIME_SORT_KEY = "99:99"
    }

    val gridStyle: StateFlow<ScheduleGridStyle> = styleSettingsRepository.styleFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleGridStyle())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TodayUiState> = appSettingsRepository.getAppSettings()
        .flatMapLatest { settings ->
            val tableId = settings.currentCourseTableId
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val dayOfWeek = today.dayOfWeek.isoDayNumber

            combine(
                appSettingsRepository.calculateCurrentWeekFromDb(),
                appSettingsRepository.getCourseTableConfigFlow(tableId),
                timeScheduleRepository.observeEffectiveTimeSlots(tableId, today)
            ) { weekIndex: Int?, config: CourseTableConfig?, timeSlots: List<TimeSlot> ->

                val startDate = config?.semesterStartDate?.let {
                    try { LocalDate.parse(it) } catch (e: Exception) { null }
                }

                val totalWeeks = config?.semesterTotalWeeks ?: DEFAULT_SEMESTER_TOTAL_WEEKS
                val firstDayOfWeek = config?.firstDayOfWeek ?: DayOfWeek.MONDAY.isoDayNumber

                // 判定今天是否处于生效的假期中
                val isSkippedDay = settings.holidays.any { holiday ->
                    holiday.isHoliday && holiday.dates.contains(today)
                }

                val status = when {
                    config?.semesterStartDate == null -> TodayStatus.NoSemesterConfig
                    startDate != null && today < startDate -> TodayStatus.Vacation
                    weekIndex == null -> TodayStatus.SemesterEnded
                    else -> TodayStatus.Normal
                }

                // 记录状态与精准计算所需的物理配置
                DataSnapshot(
                    status = status,
                    weekIndex = weekIndex,
                    timeSlots = timeSlots,
                    startDate = startDate,
                    totalWeeks = totalWeeks,
                    firstDayOfWeek = firstDayOfWeek,
                    isSkippedDay = isSkippedDay
                )
            }.flatMapLatest { snapshot ->
                // 只有 Normal 状态且不是跳过日期时才查询数据库
                if (snapshot.status == TodayStatus.Normal && snapshot.weekIndex != null && !snapshot.isSkippedDay) {
                    courseTableRepository.getCoursesForDay(tableId, snapshot.weekIndex, dayOfWeek).map { courses ->
                        createSuccessState(courses, snapshot, today)
                    }
                } else {
                    // 如果是跳过日期或非正常学期状态，直接返回空课程列表
                    flowOf(createSuccessState(emptyList(), snapshot, today))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState.Loading)

    /**
     * 内部辅助快照类
     */
    private data class DataSnapshot(
        val status: TodayStatus,
        val weekIndex: Int?,
        val timeSlots: List<TimeSlot>,
        val startDate: LocalDate?,
        val totalWeeks: Int,
        val firstDayOfWeek: Int,
        val isSkippedDay: Boolean
    )

    private fun createSuccessState(
        courses: List<CourseWithWeeks>,
        snapshot: DataSnapshot,
        today: LocalDate
    ): TodayUiState.Success {
        val slotMap = snapshot.timeSlots.associateBy { it.number }

        val displayModels = courses.map { item ->
            val startSlot = slotMap[item.course.startSection]
            val endSlot = slotMap[item.course.endSection]
            CourseDisplayModel(
                course = item.course,
                startTime = item.course.customStartTime ?: startSlot?.startTime,
                endTime = item.course.customEndTime ?: endSlot?.endTime
            )
        }.sortedWith(
            compareBy<CourseDisplayModel> { it.startTime ?: MAX_TIME_SORT_KEY }
                .thenBy { it.endTime ?: MAX_TIME_SORT_KEY }
        )

        return TodayUiState.Success(
            courses = displayModels,
            weekIndex = snapshot.weekIndex ?: 0,
            today = today,
            status = snapshot.status,
            startDate = snapshot.startDate,
            totalWeeks = snapshot.totalWeeks,
            firstDayOfWeek = snapshot.firstDayOfWeek
        )
    }
}

data class CourseDisplayModel(
    val course: Course,
    val startTime: String?,
    val endTime: String?
)

enum class TodayStatus { Normal, NoSemesterConfig, SemesterEnded, Vacation }

sealed class TodayUiState {
    data object Loading : TodayUiState()
    data class Success(
        val courses: List<CourseDisplayModel>,
        val weekIndex: Int,
        val today: LocalDate,
        val status: TodayStatus,
        val startDate: LocalDate?,
        val totalWeeks: Int,
        val firstDayOfWeek: Int
    ) : TodayUiState()
}