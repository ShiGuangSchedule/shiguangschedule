package com.xingheyuzhuan.shiguangschedule

import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.tool.IcsExportTool
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.CourseMergeUtils
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.formatToHHmm
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.startOfWeek
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleCoreLogicTest {

    private val sampleTimeSlots = listOf(
        TimeSlot(timeTableId = "t1", number = 1, startTime = "08:00", endTime = "08:45"),
        TimeSlot(timeTableId = "t1", number = 2, startTime = "08:55", endTime = "09:40"),
        TimeSlot(timeTableId = "t1", number = 3, startTime = "10:00", endTime = "10:45"),
        TimeSlot(timeTableId = "t1", number = 4, startTime = "10:55", endTime = "11:40")
    )

    @Test
    fun testStartOfWeekAndFormatToHHmm() {
        // 2026-09-03 是周四
        val thursday = LocalDate(2026, 9, 3)
        assertEquals(LocalDate(2026, 8, 31), thursday.startOfWeek(DayOfWeek.MONDAY))
        assertEquals(LocalDate(2026, 8, 30), thursday.startOfWeek(DayOfWeek.SUNDAY))

        // 周日当天在周一为起始日与周日为起始日下的边界
        val sunday = LocalDate(2026, 9, 6)
        assertEquals(LocalDate(2026, 8, 31), sunday.startOfWeek(DayOfWeek.MONDAY))
        assertEquals(LocalDate(2026, 9, 6), sunday.startOfWeek(DayOfWeek.SUNDAY))

        assertEquals("08:05", LocalTime(8, 5).formatToHHmm())
        assertEquals("23:59", LocalTime(23, 59).formatToHHmm())
    }

    @Test
    fun testTimeToGridScaleAndGridScaleToTime() {
        // 24小时模式转换
        val scale24h = CourseMergeUtils.timeToGridScale(
            time = LocalTime(9, 30),
            timeSlots = sampleTimeSlots,
            mode = ScheduleModeProto.TIME_24H_MODE
        )
        assertTrue(abs(scale24h - 10.5f) < 0.001f)

        val restored24h = CourseMergeUtils.gridScaleToTime(
            gridSection = 9.5f,
            timeSlots = sampleTimeSlots,
            mode = ScheduleModeProto.TIME_24H_MODE
        )
        assertEquals(LocalTime(9, 30), restored24h)

        // 节次模式边界与正常转换
        assertEquals(
            1.0f,
            CourseMergeUtils.timeToGridScale(
                time = LocalTime(7, 30),
                timeSlots = sampleTimeSlots,
                mode = ScheduleModeProto.SECTION_MODE
            )
        )
        assertEquals(
            5.0f,
            CourseMergeUtils.timeToGridScale(
                time = LocalTime(12, 0),
                timeSlots = sampleTimeSlots,
                mode = ScheduleModeProto.SECTION_MODE
            )
        )

        val restoredSectionStart = CourseMergeUtils.gridScaleToTime(
            gridSection = 0.0f,
            timeSlots = sampleTimeSlots,
            mode = ScheduleModeProto.SECTION_MODE
        )
        assertEquals(LocalTime(8, 0), restoredSectionStart)
    }

    @Test
    fun testMergeCoursesHandlesOverlapsAndDemotedWeeks() {
        val activeCourse = CourseWithWeeks(
            course = Course(
                id = "c1",
                courseTableId = "t1",
                name = "高等数学",
                teacher = "张老师",
                position = "A101",
                day = 1,
                startSection = 1,
                endSection = 2,
                isCustomTime = false,
                customStartTime = null,
                customEndTime = null,
                colorInt = 0
            ),
            weeks = listOf(CourseWeek("c1", 1), CourseWeek("c1", 2))
        )
        val overlappingInactiveCourse = CourseWithWeeks(
            course = Course(
                id = "c2",
                courseTableId = "t1",
                name = "大学物理",
                teacher = "李老师",
                position = "B202",
                day = 1,
                startSection = 2,
                endSection = 3,
                isCustomTime = false,
                customStartTime = null,
                customEndTime = null,
                colorInt = 1
            ),
            weeks = listOf(CourseWeek("c2", 2))
        )

        val merged = CourseMergeUtils.mergeCourses(
            courses = listOf(activeCourse, overlappingInactiveCourse),
            timeSlots = sampleTimeSlots,
            currentWeek = 1,
            mode = ScheduleModeProto.SECTION_MODE
        )

        assertEquals(2, merged.size)
        val mathBlock = merged.first { it.courses.first().course.id == "c1" }
        val physicsBlock = merged.first { it.courses.first().course.id == "c2" }

        assertFalse(mathBlock.isVisualDemoted)
        assertTrue(physicsBlock.isVisualDemoted)

        // 两门课在第 2 节重叠，应被分配到 2 个子列 (totalSubColumns = 2.0f)
        assertEquals(2.0f, mathBlock.nonActiveRanges.first().second)
        assertEquals(2.0f, physicsBlock.nonActiveRanges.first().second)
        assertTrue(mathBlock.nonActiveRanges.first().first != physicsBlock.nonActiveRanges.first().first)

        // 空节次表在 SECTION_MODE 下应安全返回空列表
        assertTrue(
            CourseMergeUtils.mergeCourses(
                courses = listOf(activeCourse),
                timeSlots = emptyList(),
                currentWeek = 1,
                mode = ScheduleModeProto.SECTION_MODE
            ).isEmpty()
        )
    }

    @Test
    fun testIcsExportToolProcessCourseInstancesSkipsHolidaysAndOutOfBoundsWeeks() {
        val courseWithWeeks = CourseWithWeeks(
            course = Course(
                id = "c_ics",
                courseTableId = "t1",
                name = "软件工程",
                teacher = "王教授",
                position = "C303",
                day = 2, // 周二
                startSection = 1,
                endSection = 2,
                isCustomTime = false,
                customStartTime = null,
                customEndTime = null,
                colorInt = 2
            ),
            weeks = listOf(
                CourseWeek("c_ics", 1),
                CourseWeek("c_ics", 2), // 将模拟第 2 周的周二为节假日
                CourseWeek("c_ics", 25) // 超出总周数 20 周
            )
        )

        val recorded = mutableListOf<Pair<LocalDateTime, Int>>()
        val semesterStart = LocalDate(2026, 8, 31) // 周一
        val holidayDate = LocalDate(2026, 9, 8)    // 第 2 周周二

        // processCourseInstances 为 suspend inline，使用协程体直接调用测试
        var completed = false
        suspend fun runTest() {
            IcsExportTool.processCourseInstances(
                courses = listOf(courseWithWeeks),
                getTimeSlotsForDate = { sampleTimeSlots },
                semesterStartDate = semesterStart,
                semesterTotalWeeks = 20,
                firstDayOfWeekInt = 1,
                isHolidayDate = { it == holidayDate }
            ) { _, startDateTime, endDateTime, weekNumber ->
                recorded.add(startDateTime to weekNumber)
                assertEquals(LocalDateTime(2026, 9, 1, 8, 0), startDateTime)
                assertEquals(LocalDateTime(2026, 9, 1, 9, 40), endDateTime)
            }
            completed = true
        }

        var err: Throwable? = null
        val block: suspend () -> Unit = { runTest() }
        block.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) {
                err = result.exceptionOrNull()
            }
        })
        err?.let { throw it }

        assertTrue(completed)
        assertEquals(1, recorded.size)
        assertEquals(1, recorded.first().second)
    }
}
