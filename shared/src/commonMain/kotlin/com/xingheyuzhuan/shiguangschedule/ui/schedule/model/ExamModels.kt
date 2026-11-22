package com.xingheyuzhuan.shiguangschedule.ui.schedule.model

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.getTodayLocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * 考试课程块的名字前缀。
 *
 * 与适配脚本（GDOU 等）约定一致：导入考试时课程名写成 `【考】课程名`，
 * 于是 App 无需数据库改动就能把考试和普通课程区分开。
 * 适配脚本同时会带上 `isExam: true` 字段，将来若改成独立字段，这里可平滑切换。
 */
const val EXAM_NAME_PREFIX = "【考】"

/** 判断一条课程是不是考试块 */
fun isExamCourseName(name: String): Boolean = name.startsWith(EXAM_NAME_PREFIX)

/** 判断 CourseWithWeeks 是不是考试块：优先看 isExam 字段，旧数据回退到名字前缀 */
fun CourseWithWeeks.isExamCourse(): Boolean = course.isExam || isExamCourseName(course.name)

/**
 * 考试日程条目：一条考试课程块 + 由「周次/星期」换算出的实际日期。
 */
data class ExamEntry(
    val courseId: String,
    /** 去掉 【考】 前缀后的课程名 */
    val title: String,
    val date: LocalDate,
    val week: Int,
    val day: Int,
    val startTime: String,
    val endTime: String,
    val position: String,
    val remark: String?,
    /** 距离今天的天数，负数表示已过去（kotlinx-datetime 的 daysUntil 返回 Int） */
    val daysUntil: Int
)

/**
 * 把考试课程块换算成带日期的日程列表。
 *
 * 日期算法与课表渲染保持一致（见 IcsExportTool.processCourseInstances）：
 * 先把学期开始日回退到所在周的第一天，再按 `(周次-1)*7 + 星期偏移` 推日期。
 *
 * @param courses 当前课表的全部课程（非考试条目会被过滤掉）
 * @param semesterStartDate 学期开始日，为空时无法换算，返回空列表
 * @param firstDayOfWeek 课表设置的每周第一天（1=周一）
 */
fun buildExamEntries(
    courses: List<CourseWithWeeks>,
    semesterStartDate: LocalDate?,
    firstDayOfWeek: Int = DayOfWeek.MONDAY.isoDayNumber,
    today: LocalDate = getTodayLocalDate()
): List<ExamEntry> {
    if (semesterStartDate == null) return emptyList()

    val firstDay = DayOfWeek(firstDayOfWeek)
    val alignBack = (semesterStartDate.dayOfWeek.isoDayNumber - firstDay.isoDayNumber + 7) % 7
    val alignedStart = semesterStartDate.minus(alignBack.toLong(), DateTimeUnit.DAY)

    return courses
        .filter { it.isExamCourse() }
        .flatMap { wrapper ->
            val course = wrapper.course
            val dayOfWeek = runCatching { DayOfWeek(course.day) }.getOrNull() ?: return@flatMap emptyList()
            val dayOffset = (dayOfWeek.isoDayNumber - firstDay.isoDayNumber + 7) % 7

            wrapper.weeks
                .sortedBy { it.weekNumber }
                .map { week ->
                    val date = alignedStart
                        .plus((week.weekNumber - 1).toLong(), DateTimeUnit.WEEK)
                        .plus(dayOffset.toLong(), DateTimeUnit.DAY)

                    ExamEntry(
                        courseId = course.id,
                        title = course.name.removePrefix(EXAM_NAME_PREFIX).ifBlank { course.name },
                        date = date,
                        week = week.weekNumber,
                        day = course.day,
                        startTime = course.customStartTime.orEmpty(),
                        endTime = course.customEndTime.orEmpty(),
                        position = course.position,
                        remark = course.remark,
                        daysUntil = today.daysUntil(date)
                    )
                }
        }
        .sortedWith(compareBy({ it.date }, { it.startTime }))
}
