package com.xingheyuzhuan.shiguangschedule.tool

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import kotlinx.datetime.LocalDate

expect object CalendarAccountManager {
    /**
     * 将当前课表同步到系统日历
     */
    suspend fun syncCurrentTableToSystemCalendar(
        courses: List<CourseWithWeeks>,
        getTimeSlotsForDate: suspend (LocalDate) -> List<TimeSlot>,
        semesterStartDate: LocalDate,
        semesterTotalWeeks: Int,
        firstDayOfWeekInt: Int,
        alarmMinutes: Int?,
        isHolidayDate: suspend (LocalDate) -> Boolean = { false }
    ): Boolean
}