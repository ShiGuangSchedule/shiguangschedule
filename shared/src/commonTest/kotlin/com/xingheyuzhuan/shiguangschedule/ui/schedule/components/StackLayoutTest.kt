package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.model.StackClusterOrder
import com.xingheyuzhuan.shiguangschedule.ui.schedule.model.MergedCourseBlock
import com.xingheyuzhuan.shiguangschedule.ui.schedule.util.StackOrderUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StackLayoutTest {

    private fun courseWithWeeks(id: String, name: String, weeks: List<Int>): CourseWithWeeks {
        return CourseWithWeeks(
            course = Course(
                id = id,
                courseTableId = "table",
                name = name,
                teacher = "老师",
                position = "教室",
                day = 1,
                startSection = 1,
                endSection = 2,
                customStartTime = null,
                customEndTime = null,
                colorInt = 0
            ),
            weeks = weeks.map { CourseWeek(courseId = id, weekNumber = it) }
        )
    }

    private fun block(
        course: CourseWithWeeks,
        isDemoted: Boolean,
        cluster: List<CourseWithWeeks>
    ): MergedCourseBlock {
        return MergedCourseBlock(
            day = 1,
            startSection = 0f,
            endSection = 1f,
            courses = listOf(course),
            isVisualDemoted = isDemoted,
            nonActiveRanges = listOf(0f to 1f),
            clusterCourses = cluster
        )
    }

    @Test
    fun oddEvenWeekVariantsAreNotCountedAsConflict() {
        val odd = courseWithWeeks("odd", "单周课程", listOf(1, 3, 5, 7, 9, 11))
        val even = courseWithWeeks("even", "双周课程", listOf(2, 4, 6, 8, 10, 12))
        val cluster = listOf(odd, even)
        val clusterKey = StackOrderUtils.clusterKey(day = 1, courseIds = cluster.map { it.course.id })
        val pinnedOdd = StackClusterOrder(clusterKey = clusterKey, courseIds = listOf("odd"))

        // 双周：odd 不在本周，即使被置顶也不能盖住 even
        val items = calculateSingleSchedulables(
            mergedCourses = listOf(
                block(odd, isDemoted = true, cluster = cluster),
                block(even, isDemoted = false, cluster = cluster)
            ),
            firstDayOfWeek = 1,
            showWeekends = true,
            stackConflictCourses = true,
            stackClusterOrders = listOf(pinnedOdd)
        )

        val oddItem = items.single { it.courseWrapper.course.id == "odd" }
        val evenItem = items.single { it.courseWrapper.course.id == "even" }

        assertTrue(evenItem.isStackTop)
        assertFalse(oddItem.isStackTop)
        assertEquals(1, evenItem.conflictCount)
        assertEquals(2, evenItem.stackCount)
        assertEquals(0, evenItem.stackRank)
        assertEquals(1, oddItem.stackRank)
    }

    @Test
    fun activeConflictsKeepExplicitOrderAndConflictCount() {
        val first = courseWithWeeks("first", "第一门", listOf(4))
        val second = courseWithWeeks("second", "第二门", listOf(4))
        val cluster = listOf(first, second)
        val clusterKey = StackOrderUtils.clusterKey(day = 1, courseIds = cluster.map { it.course.id })
        val pinnedSecond = StackClusterOrder(clusterKey = clusterKey, courseIds = listOf("second"))

        val items = calculateSingleSchedulables(
            mergedCourses = listOf(
                block(first, isDemoted = false, cluster = cluster),
                block(second, isDemoted = false, cluster = cluster)
            ),
            firstDayOfWeek = 1,
            showWeekends = true,
            stackConflictCourses = true,
            stackClusterOrders = listOf(pinnedSecond)
        )

        val firstItem = items.single { it.courseWrapper.course.id == "first" }
        val secondItem = items.single { it.courseWrapper.course.id == "second" }

        assertTrue(secondItem.isStackTop)
        assertFalse(firstItem.isStackTop)
        assertEquals(0, secondItem.stackRank)
        assertEquals(1, firstItem.stackRank)
        assertEquals(2, secondItem.conflictCount)
        assertEquals(2, secondItem.stackCount)
    }
}