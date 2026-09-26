package com.xingheyuzhuan.shiguangschedule.ui.schedule.model

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.getTodayLocalDate
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.startOfWeek
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/**
 * 课表展示块：封装单次或冲突课程
 * startSection/endSection：逻辑节次偏移量（0.0 代表网格最顶端：第一节课顶部 / 或者是24小时模式下的 00:00）
 */
data class MergedCourseBlock(
    val day: Int,
    val startSection: Float,
    val endSection: Float,
    val courses: List<CourseWithWeeks>,
    val needsProportionalRendering: Boolean = false,
    val isVisualDemoted: Boolean = false,
    val nonActiveRanges: List<Pair<Float, Float>> = emptyList(),
    val clusterCourses: List<CourseWithWeeks> = emptyList()
)

data class WeeklyScheduleUiState(
    val style: ScheduleGridStyle = ScheduleGridStyle(),
    val showWeekends: Boolean = false,
    val totalWeeks: Int = 20,
    val timeSlots: List<TimeSlot> = emptyList(),
    val courseCache: Map<String, List<MergedCourseBlock>> = emptyMap(),
    val currentMergedCourses: List<MergedCourseBlock> = emptyList(),
    val isSemesterSet: Boolean = false,
    val semesterStartDate: LocalDate? = null,
    val firstDayOfWeek: Int = DayOfWeek.MONDAY.isoDayNumber,
    val weekIndexInPager: Int? = null,
    val currentWeekNumber: Int? = null,
    val pagerMondayDate: LocalDate = getTodayLocalDate().startOfWeek(DayOfWeek.MONDAY),
    val currentSectionIndex: Int = -1,
    val daysUntilStart: Long = 0,
    val floatingCourse: CourseWithWeeks? = null,
    val floatingSourceWeek: Int? = null
)

/**
 * 规范化课程坐标的中间对象
 */
internal data class NormalizedCourse(
    val raw: CourseWithWeeks,
    val start: Float,
    val end: Float
)

internal data class ScheduleConfigPackage(
    val settings: AppSettingsModel,
    val config: CourseTableConfig?,
    val style: ScheduleGridStyle,
    val mondayDate: LocalDate
)