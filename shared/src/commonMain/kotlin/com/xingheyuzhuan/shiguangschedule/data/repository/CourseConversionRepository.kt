package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.room3.Transaction
import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBinding
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeekDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlotDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableCombo
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableComboRule
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.ComboScheduleJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.CourseConfigJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.CourseTableExportModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.CourseTableImportModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.ExportCourseJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.ImportCourseJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.PublicScheduleTemplateJsonModel
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport.TimeSlotJsonModel
import com.xingheyuzhuan.shiguangschedule.tool.CalendarAccountManager
import com.xingheyuzhuan.shiguangschedule.tool.IcsExportTool
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import org.koin.core.annotation.Single
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * 课表数据转换仓库：提供课程、作息、组合作息与学期配置的独立/全量导入导出，以及日历同步能力。
 */
@OptIn(ExperimentalUuidApi::class)
@Single
class CourseConversionRepository(
    private val courseDao: CourseDao,
    private val courseWeekDao: CourseWeekDao,
    private val timeSlotDao: TimeSlotDao,
    private val appSettingsRepository: AppSettingsRepository,
    private val styleSettingsRepository: StyleSettingsRepository,
    private val timeScheduleRepository: TimeScheduleRepository,
    private val courseTableRepository: CourseTableRepository
) {
    private val timeRegex = Regex("^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$")

    // 校验与辅助私有函数

    /**
     * 将 "HH:mm" 格式的字符串解析为一天中的总分钟数。
     */
    private fun parseToMinutes(timeStr: String): Int {
        val time = LocalTime.parse(timeStr)
        return time.hour * 60 + time.minute
    }

    /**
     * 校验作息时间段数据的合法性（序号连续性、时间格式及重叠检查）。
     */
    private fun validateTimeSlotsOrThrow(timeSlots: List<TimeSlotJsonModel>) {
        if (timeSlots.isEmpty()) return

        val sortedSlots = timeSlots.sortedBy { it.number }
        var lastEndTimeInMinutes = -1

        sortedSlots.forEachIndexed { index, slot ->
            val expectedNumber = index + 1
            if (slot.number != expectedNumber) {
                throw IllegalArgumentException("时间段编号不连续或未从1开始")
            }

            if (!timeRegex.matches(slot.startTime) || !timeRegex.matches(slot.endTime)) {
                throw IllegalArgumentException("时间格式错误")
            }

            val startMinutes = parseToMinutes(slot.startTime)
            val endMinutes = parseToMinutes(slot.endTime)

            if (startMinutes >= endMinutes) {
                throw IllegalArgumentException("开始时间必须早于结束时间")
            }

            if (lastEndTimeInMinutes != -1 && startMinutes < lastEndTimeInMinutes) {
                throw IllegalArgumentException("时间段配置存在重叠")
            }

            lastEndTimeInMinutes = endMinutes
        }
    }

    /**
     * 校验课程自定义起止时间的合法性。
     */
    private fun validateCustomCourseTimeOrThrow(course: ImportCourseJsonModel) {
        if (!course.isCustomTime) return

        val startTime = course.customStartTime
        val endTime = course.customEndTime

        if (startTime.isNullOrBlank() || endTime.isNullOrBlank()) {
            throw IllegalArgumentException("自定义时间不能为空")
        }

        if (!timeRegex.matches(startTime) || !timeRegex.matches(endTime)) {
            throw IllegalArgumentException("自定义时间格式错误")
        }

        val startMinutes = parseToMinutes(startTime)
        val endMinutes = parseToMinutes(endTime)

        if (startMinutes >= endMinutes) {
            throw IllegalArgumentException("自定义开始时间必须早于结束时间")
        }
    }

    /**
     * 根据课程名称复用颜色或自动分配配色方案索引。
     */
    private fun getOrAssignColorByName(
        jsonCourse: ImportCourseJsonModel,
        colorSize: Int,
        nameToColorMap: MutableMap<String, Int>,
        getNextAutoColor: () -> Int
    ): Int {
        val trimmedName = jsonCourse.name.trim()
        val existingColor = nameToColorMap[trimmedName]

        if (existingColor != null) return existingColor

        val importedColor = jsonCourse.color
        val finalColor = if (importedColor != null && importedColor in 0 until colorSize) {
            importedColor
        } else {
            getNextAutoColor()
        }

        nameToColorMap[trimmedName] = finalColor
        return finalColor
    }

    // 独立模块基础写入接口

    /**
     * 导入指定课表的课程列表数据。
     */
    @Transaction
    suspend fun importCourses(
        tableId: String,
        courses: List<ImportCourseJsonModel>
    ) {
        courses.forEach { validateCustomCourseTimeOrThrow(it) }

        val currentStyle = styleSettingsRepository.styleFlow.first()
        val colorSize = currentStyle.courseColorMaps.size

        courseDao.deleteCoursesByTableId(tableId)

        val courseEntities = ArrayList<Course>(courses.size)
        val courseWeekEntities = mutableListOf<CourseWeek>()

        val nameToColorMap = mutableMapOf<String, Int>()
        var colorOffset = if (colorSize > 0) Random.nextInt(colorSize) else 0

        courses.forEach { jsonCourse ->
            val courseId = Uuid.random().toString()

            val courseIndex = getOrAssignColorByName(
                jsonCourse = jsonCourse,
                colorSize = colorSize,
                nameToColorMap = nameToColorMap,
                getNextAutoColor = {
                    val next = if (colorSize > 0) colorOffset % colorSize else 0
                    colorOffset++
                    next
                }
            )

            courseEntities.add(
                Course(
                    id = courseId,
                    courseTableId = tableId,
                    name = jsonCourse.name,
                    teacher = jsonCourse.teacher,
                    position = jsonCourse.position,
                    day = jsonCourse.day,
                    startSection = jsonCourse.startSection,
                    endSection = jsonCourse.endSection,
                    isCustomTime = jsonCourse.isCustomTime,
                    customStartTime = jsonCourse.customStartTime,
                    customEndTime = jsonCourse.customEndTime,
                    colorInt = courseIndex,
                    remark = jsonCourse.remark?.take(300),
                    credit = jsonCourse.credit
                )
            )

            jsonCourse.weeks.forEach { week ->
                courseWeekEntities.add(
                    CourseWeek(courseId = courseId, weekNumber = week)
                )
            }
        }

        if (courseEntities.isNotEmpty()) courseDao.insertAll(courseEntities)
        if (courseWeekEntities.isNotEmpty()) courseWeekDao.insertAll(courseWeekEntities)
    }

    /**
     * 导入指定课表的专属作息表与预设时间段。
     */
    @Transaction
    suspend fun importTimeSlots(
        tableId: String,
        timeSlots: List<TimeSlotJsonModel>,
        defaultClassDuration: Int? = null,
        defaultBreakDuration: Int? = null
    ) {
        validateTimeSlotsOrThrow(timeSlots)

        val existingTimeTable = timeScheduleRepository.getTimeTableById(tableId).first()
        val newClassDuration = defaultClassDuration ?: existingTimeTable?.defaultClassDuration ?: 45
        val newBreakDuration = defaultBreakDuration ?: existingTimeTable?.defaultBreakDuration ?: 10

        val timeTableToSave = TimeTable(
            id = tableId,
            name = null,
            createdAt = existingTimeTable?.createdAt ?: Clock.System.now().toEpochMilliseconds(),
            defaultClassDuration = newClassDuration,
            defaultBreakDuration = newBreakDuration
        )

        if (timeSlots.isNotEmpty()) {
            val timeSlotEntities = timeSlots.map { slot ->
                TimeSlot(
                    number = slot.number,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    timeTableId = tableId,
                    alias = slot.alias?.take(5)
                )
            }
            timeScheduleRepository.saveExclusiveTimeTable(timeTableToSave, timeSlotEntities)
        } else {
            val currentSlots = timeSlotDao.getTimeSlotsOnceByTimeTableId(tableId)
            timeScheduleRepository.saveExclusiveTimeTable(timeTableToSave, currentSlots)
        }
    }

    /**
     * 导入组合作息方案及按日期分布的公共作息规则。
     */
    @Transaction
    suspend fun importComboSchedule(
        tableId: String,
        comboJson: ComboScheduleJsonModel?
    ) {
        if (comboJson == null || comboJson.publicSchedules.isEmpty()) {
            timeScheduleRepository.bindCourseTableToTimeSchedule(
                courseTableId = tableId,
                targetType = CourseTimeBinding.TargetType.SINGLE,
                targetId = tableId
            )
            return
        }

        comboJson.publicSchedules.forEach { validateTimeSlotsOrThrow(it.timeSlots) }

        val comboId = Uuid.random().toString()
        val tables = courseTableRepository.getAllCourseTables().first()
        val currentTableName = tables.find { it.id == tableId }?.name
        val comboName = comboJson.name?.ifBlank { null } ?: currentTableName ?: "导入课表"

        val combo = TimeTableCombo(
            id = comboId,
            name = comboName,
            baseTimeTableId = null,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )

        val comboRules = comboJson.publicSchedules.map { template ->
            val targetTableId = Uuid.random().toString()
            val publicTimeTable = TimeTable(
                id = targetTableId,
                name = template.name,
                createdAt = Clock.System.now().toEpochMilliseconds(),
                defaultClassDuration = template.defaultClassDuration,
                defaultBreakDuration = template.defaultBreakDuration
            )
            val targetSlots = template.timeSlots.map { slotJson ->
                TimeSlot(
                    number = slotJson.number,
                    startTime = slotJson.startTime,
                    endTime = slotJson.endTime,
                    timeTableId = targetTableId,
                    alias = slotJson.alias?.take(5)
                )
            }
            timeScheduleRepository.savePublicTimeTable(publicTimeTable, targetSlots)

            TimeTableComboRule(
                id = Uuid.random().toString(),
                comboId = comboId,
                startDate = template.startDate,
                endDate = template.endDate,
                targetTimeTableId = targetTableId
            )
        }

        timeScheduleRepository.saveTimeTableCombo(combo, comboRules)
        timeScheduleRepository.bindCourseTableToTimeSchedule(
            courseTableId = tableId,
            targetType = CourseTimeBinding.TargetType.COMBO,
            targetId = comboId
        )
    }

    /**
     * 导入课表学期配置，并同步更新基准作息时长。
     */
    @Transaction
    suspend fun importCourseConfig(
        tableId: String,
        configJsonModel: CourseConfigJsonModel
    ) {
        val currentConfig = appSettingsRepository.getCourseConfigOnce(tableId)
        val updatedConfig = CourseTableConfig(
            courseTableId = tableId,
            showWeekends = currentConfig?.showWeekends ?: false,
            semesterStartDate = configJsonModel.semesterStartDate,
            semesterTotalWeeks = configJsonModel.semesterTotalWeeks,
            firstDayOfWeek = configJsonModel.firstDayOfWeek
        )
        appSettingsRepository.insertOrUpdateCourseConfig(updatedConfig)

        val existingTimeTable = timeScheduleRepository.getTimeTableById(tableId).first()
        if (existingTimeTable != null) {
            val updatedTimeTable = existingTimeTable.copy(
                defaultClassDuration = configJsonModel.defaultClassDuration,
                defaultBreakDuration = configJsonModel.defaultBreakDuration
            )
            val currentSlots = timeSlotDao.getTimeSlotsOnceByTimeTableId(tableId)
            timeScheduleRepository.saveExclusiveTimeTable(updatedTimeTable, currentSlots)
        }
    }

    // 全量 JSON 导入与导出接口

    /**
     * 组合调用基础子模块，从 JSON 模型完成课表全量数据的导入。
     */
    @Transaction
    suspend fun importCourseTableFromJson(
        tableId: String,
        courseTableJsonModel: CourseTableImportModel
    ) {
        importCourses(tableId, courseTableJsonModel.courses)

        val jsonTimeSlots = courseTableJsonModel.timeSlots
        val configJson = courseTableJsonModel.config
        importTimeSlots(
            tableId = tableId,
            timeSlots = jsonTimeSlots ?: emptyList(),
            defaultClassDuration = configJson?.defaultClassDuration,
            defaultBreakDuration = configJson?.defaultBreakDuration
        )

        importComboSchedule(tableId, courseTableJsonModel.comboSchedule)

        if (configJson != null) {
            importCourseConfig(tableId, configJson)
        }
    }

    /**
     * 导出指定课表的全量数据（包含课程、作息、组合作息与配置）。
     */
    suspend fun exportCourseTableToJson(tableId: String): CourseTableExportModel? {
        val coursesWithWeeks = courseDao.getCoursesWithWeeksByTableId(tableId).first()
        if (coursesWithWeeks.isEmpty() && appSettingsRepository.getCourseConfigOnce(tableId) == null) {
            return null
        }

        val exportCourses = coursesWithWeeks.map { courseWithWeeks ->
            val course = courseWithWeeks.course
            val weeks = courseWithWeeks.weeks.map { it.weekNumber }

            ExportCourseJsonModel(
                name = course.name,
                teacher = course.teacher,
                position = course.position,
                day = course.day,
                startSection = course.startSection,
                endSection = course.endSection,
                color = course.colorInt,
                weeks = weeks,
                isCustomTime = course.isCustomTime,
                customStartTime = course.customStartTime,
                customEndTime = course.customEndTime,
                remark = course.remark,
                credit = course.credit
            )
        }

        val binding = timeScheduleRepository.getBinding(tableId)
        var exportTimeSlots: List<TimeSlotJsonModel> = emptyList()
        var comboScheduleExport: ComboScheduleJsonModel? = null
        val effectiveTimeTableId: String

        if (binding == null || (binding.targetType == CourseTimeBinding.TargetType.SINGLE && binding.targetId == tableId)) {
            effectiveTimeTableId = tableId
            val slots = timeSlotDao.getTimeSlotsByTimeTableId(tableId).first()
            exportTimeSlots = slots.map { slot ->
                TimeSlotJsonModel(
                    number = slot.number,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    alias = slot.alias?.take(5)
                )
            }
        } else if (binding.targetType == CourseTimeBinding.TargetType.SINGLE) {
            effectiveTimeTableId = binding.targetId
            val slots = timeSlotDao.getTimeSlotsByTimeTableId(binding.targetId).first()
            exportTimeSlots = slots.map { slot ->
                TimeSlotJsonModel(
                    number = slot.number,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    alias = slot.alias?.take(5)
                )
            }
        } else if (binding.targetType == CourseTimeBinding.TargetType.COMBO) {
            val combo = timeScheduleRepository.getAllCombos().first().find { it.id == binding.targetId }
            val baseTableId = combo?.baseTimeTableId ?: tableId
            effectiveTimeTableId = baseTableId

            val baseSlots = timeSlotDao.getTimeSlotsByTimeTableId(baseTableId).first()
            exportTimeSlots = baseSlots.map { slot ->
                TimeSlotJsonModel(
                    number = slot.number,
                    startTime = slot.startTime,
                    endTime = slot.endTime,
                    alias = slot.alias?.take(5)
                )
            }

            val rules = timeScheduleRepository.getComboRules(binding.targetId).first()
            val publicSchedulesExport = rules.map { rule ->
                val targetSlots = timeSlotDao.getTimeSlotsByTimeTableId(rule.targetTimeTableId).first()
                val targetTable = timeScheduleRepository.getTimeTableById(rule.targetTimeTableId).first()

                PublicScheduleTemplateJsonModel(
                    name = targetTable?.name ?: "公共作息",
                    startDate = rule.startDate,
                    endDate = rule.endDate,
                    defaultClassDuration = targetTable?.defaultClassDuration ?: 45,
                    defaultBreakDuration = targetTable?.defaultBreakDuration ?: 10,
                    timeSlots = targetSlots.map { slot ->
                        TimeSlotJsonModel(
                            number = slot.number,
                            startTime = slot.startTime,
                            endTime = slot.endTime,
                            alias = slot.alias?.take(5)
                        )
                    }
                )
            }

            comboScheduleExport = ComboScheduleJsonModel(
                name = combo?.name,
                publicSchedules = publicSchedulesExport
            )
        } else {
            effectiveTimeTableId = tableId
        }

        val courseConfig = appSettingsRepository.getCourseConfigOnce(tableId)
        val configToExport = courseConfig ?: CourseTableConfig(courseTableId = tableId)
        val effectiveTimeTable = timeScheduleRepository.getTimeTableById(effectiveTimeTableId).first()

        val exportConfig = CourseConfigJsonModel(
            semesterStartDate = configToExport.semesterStartDate,
            semesterTotalWeeks = configToExport.semesterTotalWeeks,
            defaultClassDuration = effectiveTimeTable?.defaultClassDuration ?: 45,
            defaultBreakDuration = effectiveTimeTable?.defaultBreakDuration ?: 10,
            firstDayOfWeek = configToExport.firstDayOfWeek
        )

        return CourseTableExportModel(
            courses = exportCourses,
            timeSlots = exportTimeSlots,
            config = exportConfig,
            comboSchedule = comboScheduleExport
        )
    }

    // 日历与 ICS 导出接口

    /**
     * 将指定课表导出为 ICS 格式的日历文件内容。
     */
    suspend fun exportToIcsString(tableId: String, alarmMinutes: Int?): String? {
        val courses = courseDao.getCoursesWithWeeksByTableId(tableId).first()
        val courseConfig = appSettingsRepository.getCourseConfigOnce(tableId)
        val semesterStartDate = courseConfig?.semesterStartDate?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }

        if (semesterStartDate == null || courseConfig.semesterTotalWeeks <= 0) {
            return null
        }

        val appSettings = appSettingsRepository.getAppSettingsOnce()
        val holidayDatesSet = appSettings.holidays
            .filter { it.isHoliday }
            .flatMap { it.dates }
            .toSet()

        return IcsExportTool.generateIcsFileContent(
            courses = courses,
            getTimeSlotsForDate = { date ->
                timeScheduleRepository.getEffectiveTimeSlotsOnce(tableId, date)
            },
            semesterStartDate = semesterStartDate,
            semesterTotalWeeks = courseConfig.semesterTotalWeeks,
            firstDayOfWeekInt = courseConfig.firstDayOfWeek,
            alarmMinutes = alarmMinutes,
            isHolidayDate = { date -> date in holidayDatesSet }
        )
    }

    /**
     * 将当前生效的课表数据同步到系统日历账户。
     */
    suspend fun syncCurrentTableToSystemCalendar(): Boolean {
        val appSettings = appSettingsRepository.getAppSettingsOnce()
        val currentTableId = appSettings.currentCourseTableId
        if (currentTableId.isEmpty()) return true

        val courses = courseDao.getCoursesWithWeeksByTableId(currentTableId).first()
        val alarmMinutes = appSettings.remindBeforeMinutes
        val courseConfig = appSettingsRepository.getCourseConfigOnce(currentTableId)

        val semesterStartDate = courseConfig?.semesterStartDate?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        } ?: return true

        if (courseConfig.semesterTotalWeeks <= 0 || courses.isEmpty()) {
            return true
        }

        val holidayDatesSet = appSettings.holidays
            .filter { it.isHoliday }
            .flatMap { it.dates }
            .toSet()

        return CalendarAccountManager.syncCurrentTableToSystemCalendar(
            courses = courses,
            getTimeSlotsForDate = { date ->
                timeScheduleRepository.getEffectiveTimeSlotsOnce(currentTableId, date)
            },
            semesterStartDate = semesterStartDate,
            semesterTotalWeeks = courseConfig.semesterTotalWeeks,
            firstDayOfWeekInt = courseConfig.firstDayOfWeek,
            alarmMinutes = alarmMinutes,
            isHolidayDate = { date -> date in holidayDatesSet }
        )
    }
}