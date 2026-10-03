package com.xingheyuzhuan.shiguangschedule.ui.schedule.util

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.NormalizedCourse
import kotlinx.datetime.LocalTime

object CourseMergeUtils {

    /**
     * 核心统一时间换算器：将任意 [LocalTime] 转化为网格上的 Float 纵坐标
     * @return 距离网格最顶部的浮点偏置量（1.0f 代表第 1 个格子的顶部起点）
     */
    fun timeToGridScale(
        time: LocalTime,
        timeSlots: List<TimeSlot>,
        mode: ScheduleModeProto
    ): Float {
        return when (mode) {
            ScheduleModeProto.TIME_24H_MODE -> {
                val currentMinutes = time.hour * 60 + time.minute
                val hourOffset = currentMinutes.toFloat() / 60f
                1.0f + hourOffset
            }
            ScheduleModeProto.SECTION_MODE -> {
                if (timeSlots.isEmpty()) return 1.0f
                val sortedSlots = timeSlots.sortedBy { it.number }

                val firstSlotStart = LocalTime.parse(sortedSlots.first().startTime)
                val lastSlotEnd = LocalTime.parse(sortedSlots.last().endTime)

                if (time <= firstSlotStart) return 1.0f
                if (time >= lastSlotEnd) return (sortedSlots.size + 1).toFloat()

                val currentSlot = sortedSlots.find {
                    val s = LocalTime.parse(it.startTime)
                    val e = LocalTime.parse(it.endTime)
                    time in s..e
                }

                if (currentSlot != null) {
                    val sTime = LocalTime.parse(currentSlot.startTime)
                    val eTime = LocalTime.parse(currentSlot.endTime)
                    val duration = (eTime.toSecondOfDay() - sTime.toSecondOfDay()) / 60
                    val safeDuration = if (duration <= 0) 1 else duration
                    val elapsedMinutes = (time.toSecondOfDay() - sTime.toSecondOfDay()) / 60
                    return currentSlot.number.toFloat() + (elapsedMinutes.toFloat() / safeDuration)
                }

                val nextSlot = sortedSlots.find { LocalTime.parse(it.startTime) > time }
                nextSlot?.number?.toFloat() ?: (sortedSlots.size + 1).toFloat()
            }
        }
    }

    /**
     * 反向坐标时间换算器
     * 将 Layout 的浮点偏移量 (0f..maxSection) 完美逆向转换为真实的物理 LocalTime
     */
    fun gridScaleToTime(
        gridSection: Float,
        timeSlots: List<TimeSlot>,
        mode: ScheduleModeProto
    ): LocalTime {
        return when (mode) {
            ScheduleModeProto.TIME_24H_MODE -> {
                val totalMinutes = (gridSection * 60f).toInt().coerceIn(0, 24 * 60 - 1)
                val hour = totalMinutes / 60
                val minute = totalMinutes % 60
                LocalTime(hour, minute)
            }
            ScheduleModeProto.SECTION_MODE -> {
                if (timeSlots.isEmpty()) return LocalTime(8, 0)
                val sortedSlots = timeSlots.sortedBy { it.number }

                val targetScale = gridSection + 1.0f
                val integerPart = targetScale.toInt()
                val fraction = targetScale - integerPart

                val matchedSlot = sortedSlots.find { it.number == integerPart }
                if (matchedSlot != null) {
                    val sTime = LocalTime.parse(matchedSlot.startTime)
                    val eTime = LocalTime.parse(matchedSlot.endTime)
                    val totalDuration = (eTime.toSecondOfDay() - sTime.toSecondOfDay()) / 60
                    val addedMinutes = (fraction * totalDuration).toInt()
                    val totalSeconds = sTime.toSecondOfDay() + addedMinutes * 60
                    val finalHour = (totalSeconds / 3600) % 24
                    val finalMinute = (totalSeconds % 3600) / 60
                    LocalTime(finalHour, finalMinute)
                } else {
                    if (integerPart < sortedSlots.first().number) {
                        LocalTime.parse(sortedSlots.first().startTime)
                    } else {
                        LocalTime.parse(sortedSlots.last().endTime)
                    }
                }
            }
        }
    }

    /**
     * 无损展平排版调度引擎
     */
    fun mergeCourses(
        courses: List<CourseWithWeeks>,
        timeSlots: List<TimeSlot>,
        currentWeek: Int,
        mode: ScheduleModeProto = ScheduleModeProto.SECTION_MODE
    ): List<MergedCourseBlock> {
        if (timeSlots.isEmpty() && mode == ScheduleModeProto.SECTION_MODE) return emptyList()

        val maxSection = if (mode == ScheduleModeProto.TIME_24H_MODE) 24f else timeSlots.size.toFloat()
        val limit = maxSection + 1.0f
        val minSafeHeight = if (mode == ScheduleModeProto.TIME_24H_MODE) 0.0f else 0.3f

        val normalizedList = courses.mapNotNull { cw ->
            try {
                val c = cw.course

                val (sTime, eTime) = if (c.isCustomTime) {
                    LocalTime.parse(c.customStartTime ?: return@mapNotNull null) to
                            LocalTime.parse(c.customEndTime ?: return@mapNotNull null)
                } else {
                    val startSlot = timeSlots.find { it.number == c.startSection } ?: return@mapNotNull null
                    val endSlot = timeSlots.find { it.number == c.endSection } ?: return@mapNotNull null
                    LocalTime.parse(startSlot.startTime) to LocalTime.parse(endSlot.endTime)
                }

                val s = timeToGridScale(sTime, timeSlots, mode)
                val e = timeToGridScale(eTime, timeSlots, mode)

                var finalStart = s
                var finalEnd = e
                if (finalStart >= limit) {
                    finalEnd = limit
                    finalStart = limit - minSafeHeight
                } else if (finalEnd <= 1.0f) {
                    finalStart = 1.0f
                    finalEnd = 1.0f + minSafeHeight
                }

                if (finalEnd - finalStart < minSafeHeight) {
                    if (finalEnd + minSafeHeight <= limit) {
                        finalEnd = finalStart + minSafeHeight
                    } else {
                        finalStart = finalEnd - minSafeHeight
                    }
                }

                NormalizedCourse(cw, finalStart.coerceIn(1.0f, limit - 0.1f), finalEnd.coerceIn(1.0f + 0.1f, limit))
            } catch (e: Exception) { null }
        }

        val result = mutableListOf<MergedCourseBlock>()

        normalizedList.groupBy { it.raw.course.day }.forEach { (day, dailyCourses) ->
            if (dailyCourses.isEmpty()) return@forEach

            val sorted = dailyCourses.sortedWith(
                compareBy<NormalizedCourse> { it.start }.thenByDescending { it.end - it.start }
            )

            val currentClusters = mutableListOf<MutableList<NormalizedCourse>>()

            for (item in sorted) {
                val targetCluster = currentClusters.find { cluster ->
                    cluster.any { existing ->
                        item.start < existing.end - 0.01f && item.end > existing.start + 0.01f
                    }
                }
                if (targetCluster != null) {
                    targetCluster.add(item)
                } else {
                    currentClusters.add(mutableListOf(item))
                }
            }

            for (cluster in currentClusters) {
                val columnEnds = mutableListOf<Float>()
                val itemToColumnIndex = mutableMapOf<NormalizedCourse, Int>()

                for (item in cluster) {
                    var assignedIndex = -1
                    for (i in columnEnds.indices) {
                        if (columnEnds[i] <= item.start + 0.01f) {
                            assignedIndex = i
                            columnEnds[i] = item.end
                            break
                        }
                    }
                    if (assignedIndex == -1) {
                        columnEnds.add(item.end)
                        assignedIndex = columnEnds.size - 1
                    }
                    itemToColumnIndex[item] = assignedIndex
                }

                val sortedClusterCourses = cluster.sortedWith(
                    compareBy<NormalizedCourse> { it.start }
                        .thenBy { itemToColumnIndex[it] ?: 0 }
                ).map { it.raw }

                val totalSubColumns = columnEnds.size

                for (item in cluster) {
                    val cw = item.raw
                    val isCurrentWeekActive = cw.weeks.any { it.weekNumber == currentWeek }
                    val myColumnIndex = itemToColumnIndex[item] ?: 0

                    result.add(
                        MergedCourseBlock(
                            day = day,
                            startSection = (item.start - 1f).coerceIn(0f, maxSection),
                            endSection = (item.end - 1f).coerceIn(0f, maxSection),
                            courses = listOf(cw),
                            needsProportionalRendering = (mode == ScheduleModeProto.TIME_24H_MODE) || cw.course.isCustomTime,
                            isVisualDemoted = !isCurrentWeekActive,
                            nonActiveRanges = listOf(myColumnIndex.toFloat() to totalSubColumns.toFloat()),
                            clusterCourses = sortedClusterCourses
                        )
                    )
                }
            }
        }
        return result
    }
}