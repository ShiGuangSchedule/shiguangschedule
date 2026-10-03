package com.xingheyuzhuan.shiguangschedule.ui.schedule.util

import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * 获取当前系统时区的当前日期
 */
fun getTodayLocalDate(): LocalDate {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}

/**
 * 获取当前系统时区的当前时间
 */
fun getCurrentLocalTime(): LocalTime {
    return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
}

/**
 * 计算当前日期所在周的起始日期（周一/周日等）
 */
fun LocalDate.startOfWeek(firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): LocalDate {
    val dayOfWeek = this.dayOfWeek.isoDayNumber
    val targetIso = firstDayOfWeek.isoDayNumber
    val diff = (dayOfWeek - targetIso + 7) % 7
    return this.minus(diff, DateTimeUnit.DAY)
}

/**
 * 格式化 LocalTime 为 HH:mm 格式
 */
fun LocalTime.formatToHHmm(): String {
    val hourStr = hour.toString().padStart(2, '0')
    val minuteStr = minute.toString().padStart(2, '0')
    return "$hourStr:$minuteStr"
}

/**
 * 根据当前系统时间与 TimeSlots 匹配计算当前属于第几节课
 */
fun calculateCurrentSectionIndex(timeSlots: List<TimeSlot>): Int {
    if (timeSlots.isEmpty()) return -1
    val now = getCurrentLocalTime()
    val currentMinutes = now.hour * 60 + now.minute

    timeSlots.forEachIndexed { index, slot ->
        val startParts = slot.startTime.split(":")
        val endParts = slot.endTime.split(":")

        if (startParts.size == 2 && endParts.size == 2) {
            val startMinutes = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endMinutes = endParts[0].toInt() * 60 + endParts[1].toInt()

            if (currentMinutes in startMinutes until endMinutes) {
                return index + 1
            }
        }
    }
    return -1
}