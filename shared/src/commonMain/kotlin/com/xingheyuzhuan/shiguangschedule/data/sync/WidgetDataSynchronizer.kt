package com.xingheyuzhuan.shiguangschedule.data.sync

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettings
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourse
import com.xingheyuzhuan.shiguangschedule.data.model.AppSettingsModel
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.CourseTableRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.TimeScheduleRepository
import com.xingheyuzhuan.shiguangschedule.data.repository.WidgetRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.koin.core.annotation.Single
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

/**
 * 负责主数据库与 Widget 数据库之间的数据同步（跨平台共享核心逻辑）。
 * 持续监听应用设置、课表及作息调度变化，自动计算并写入优化后的 Widget 专用数据库。
 */
@Single(createdAtStart = true)
class WidgetDataSynchronizer(
    private val appSettingsRepository: AppSettingsRepository,
    private val courseTableRepository: CourseTableRepository,
    private val timeScheduleRepository: TimeScheduleRepository,
    private val widgetRepository: WidgetRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val widgetSyncDays = 7 // 每次同步未来 7 天的数据
    private val isStarted = MutableStateFlow(false)

    // 内部通道：用于向各平台分发“数据同步完成”的通知信号
    private val _syncCompletedChannel = Channel<Unit>(Channel.CONFLATED)

    /** 暴露给各平台（Android / iOS）监听的同步完成事件流 */
    val syncCompletedFlow: Flow<Unit> = _syncCompletedChannel.receiveAsFlow()

    /**
     * 持续监听主数据库变化的 Flow 核心链条。
     * 当当前课表 ID 或其绑定的作息规则改变时，自动触发重新计算。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val syncFlow: Flow<Unit> = appSettingsRepository.getAppSettings()
        .flatMapLatest { appSettings ->
            val tableId = appSettings.currentCourseTableId

            if (tableId.isNotEmpty()) {
                val coursesFlow = courseTableRepository.getCoursesWithWeeksByTableId(tableId)
                // 监听当前课表关联的作息规则或绑定变化
                val timeScheduleFlow = timeScheduleRepository.getTimeScheduleFlowByTableId(tableId)
                val configFlow = appSettingsRepository.getCourseTableConfigFlow(tableId)

                combine(coursesFlow, timeScheduleFlow, configFlow) { courses, _, config ->
                    Triple(appSettings, courses, config)
                }
            } else {
                flowOf(Triple(appSettings, emptyList(), null))
            }
        }
        .map { (appSettings, coursesWithWeeks, config) ->
            if (config != null) {
                performSync(appSettings, config, coursesWithWeeks)
            } else {
                // 配置为空时清空小组件数据
                widgetRepository.deleteAll()
                widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            }
        }

    init {
        // 自动触发启动
        startSync()
    }

    /**
     * 启动自动同步监听（跨平台调用入口）。
     * 会对数据库流的变化进行防抖处理，并在每次同步完成后发出通知。
     */
    @OptIn(FlowPreview::class)
    fun startSync() {
        // 确保防重：若已经启动过则直接返回
        if (isStarted.value) return
        isStarted.value = true

        // 1. 监听课表数据与小组件所需数据的实时变更
        syncFlow
            .debounce(500.milliseconds)
            .onEach {
                _syncCompletedChannel.trySend(Unit)
            }
            .launchIn(scope)

        // 2. 监听通知/自动化配置变更，同样触发同步通知（以便各平台调度 WorkManager/系统闹钟/DND 任务）
        appSettingsRepository.getAppSettings()
            .map { settings ->
                Triple(
                    settings.reminderEnabled to settings.remindBeforeMinutes,
                    settings.autoModeEnabled to settings.autoControlMode,
                    settings.compatWearableSync
                )
            }
            .distinctUntilChanged()
            .onEach {
                _syncCompletedChannel.trySend(Unit)
            }
            .launchIn(scope)
    }

    /**
     * 手动触发一次性数据同步（挂起函数）。
     */
    suspend fun syncNow() {
        val appSettings = appSettingsRepository.getAppSettings().first()
        val tableId = appSettings.currentCourseTableId

        val coursesWithWeeks = if (tableId.isNotEmpty()) courseTableRepository.getCoursesWithWeeksByTableId(tableId).first() else emptyList()
        val courseConfig = if (tableId.isNotEmpty()) appSettingsRepository.getCourseConfigOnce(tableId) else null

        if (courseConfig != null) {
            performSync(appSettings, courseConfig, coursesWithWeeks)
        } else {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
        }
        _syncCompletedChannel.trySend(Unit)
    }

    /**
     * 核心计算与写库逻辑：解析开学日期、按天动态获取生效作息、匹配课程时间并写入 Widget 数据库。
     */
    private suspend fun performSync(
        appSettings: AppSettingsModel,
        courseConfig: CourseTableConfig,
        coursesWithWeeks: List<CourseWithWeeks>
    ) = withContext(Dispatchers.IO) {
        val tableId = courseConfig.courseTableId
        val semesterStartDateString = courseConfig.semesterStartDate ?: run {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            return@withContext
        }
        val semesterTotalWeeks = courseConfig.semesterTotalWeeks
        val firstDayOfWeekInt = courseConfig.firstDayOfWeek

        if (semesterTotalWeeks <= 0) {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            return@withContext
        }

        // 更新小组件的全局基础设置
        val widgetSettings = WidgetAppSettings(
            id = 1,
            semesterStartDate = semesterStartDateString,
            semesterTotalWeeks = semesterTotalWeeks,
            firstDayOfWeek = firstDayOfWeekInt
        )
        widgetRepository.insertOrUpdateAppSettings(widgetSettings)

        val holidayDatesSet = appSettings.holidays
            .filter { it.isHoliday }
            .flatMap { it.dates }
            .toSet()

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

        val semesterStartDate: LocalDate = try {
            LocalDate.parse(semesterStartDateString)
        } catch (_: Exception) {
            widgetRepository.deleteAll()
            widgetRepository.insertOrUpdateAppSettings(WidgetAppSettings(id = 1, semesterStartDate = null))
            return@withContext
        }

        val alignedSemesterStartDate = getStartDayOfWeek(semesterStartDate, firstDayOfWeekInt)

        val widgetCourses = mutableListOf<WidgetCourse>()
        val startSyncDate = if (today < alignedSemesterStartDate) {
            alignedSemesterStartDate
        } else {
            today
        }

        // 循环计算未来指定天数（widgetSyncDays）内的课程安排
        for (i in 0 until widgetSyncDays) {
            val date = startSyncDate.plus(i, DateTimeUnit.DAY)
            val dateString = date.toString()

            val alignedDate = getStartDayOfWeek(date, firstDayOfWeekInt)

            // 计算当前日期是第几周
            val diffDays = alignedSemesterStartDate.daysUntil(alignedDate)
            val diffWeeks = diffDays / 7
            val weekNumber = diffWeeks + 1

            val dayOfWeek = date.dayOfWeek.isoDayNumber

            // 超出学期总周数则跳过
            if (weekNumber !in 1..semesterTotalWeeks) {
                continue
            }

            // 根据当前具体的日期，动态获取当天真实生效的时间段
            val effectiveTimeSlots = timeScheduleRepository.getEffectiveTimeSlotsOnce(tableId, date)
            val timeSlotMap = effectiveTimeSlots.associateBy { it.number }

            for (courseWithWeeks in coursesWithWeeks) {
                if (courseWithWeeks.weeks.any { it.weekNumber == weekNumber } && courseWithWeeks.course.day == dayOfWeek) {
                    val course = courseWithWeeks.course

                    val startTime: String
                    val endTime: String

                    // 处理自定义时间或标准时间段
                    if (course.isCustomTime) {
                        startTime = course.customStartTime ?: ""
                        endTime = course.customEndTime ?: ""
                    } else {
                        startTime = timeSlotMap[course.startSection]?.startTime ?: ""
                        endTime = timeSlotMap[course.endSection]?.endTime ?: ""
                    }

                    // 检查该日期是否被设为调休/停课
                    val isSkipped = date in holidayDatesSet

                    val widgetCourse = WidgetCourse(
                        id = "${course.id}-$dateString",
                        name = course.name,
                        teacher = course.teacher,
                        position = course.position,
                        startTime = startTime,
                        endTime = endTime,
                        isSkipped = isSkipped,
                        date = dateString,
                        colorInt = course.colorInt
                    )
                    widgetCourses.add(widgetCourse)
                }
            }
        }

        // 刷新 Widget 数据库：先清空旧数据，再批量插入新计算的课程
        widgetRepository.deleteAll()
        if (widgetCourses.isNotEmpty()) {
            widgetRepository.insertAll(widgetCourses)
        }
    }

    /**
     * 根据设定的每周起始日（如周一或周日），向前推算并对齐给定日期所在周的起始日。
     */
    private fun getStartDayOfWeek(date: LocalDate, firstDayOfWeekInt: Int): LocalDate {
        val targetFirstDay = DayOfWeek(firstDayOfWeekInt)
        var current = date
        while (current.dayOfWeek != targetFirstDay) {
            current = current.minus(1, DateTimeUnit.DAY)
        }
        return current
    }
}