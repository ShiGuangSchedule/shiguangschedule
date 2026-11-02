package com.xingheyuzhuan.shiguangschedule.ui.schedule.util

import com.xingheyuzhuan.shiguangschedule.data.model.StackClusterOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class StackOrderUtilsTest {

    private fun ordered(
        candidateIds: List<String>,
        explicitOrderIds: List<String> = emptyList(),
        demotedIds: Set<String> = emptySet()
    ): List<String> {
        return StackOrderUtils.sortByStackOrder(
            items = candidateIds,
            explicitOrderIds = explicitOrderIds,
            idOf = { it },
            isDemoted = { it in demotedIds }
        )
    }

    @Test
    fun clusterKeyIgnoresCourseIdOrder() {
        assertEquals(
            StackOrderUtils.clusterKey(3, listOf("b", "a")),
            StackOrderUtils.clusterKey(3, listOf("a", "b"))
        )
    }

    @Test
    fun clusterKeySeparatesDays() {
        val monday = StackOrderUtils.clusterKey(1, listOf("a", "b"))
        val tuesday = StackOrderUtils.clusterKey(2, listOf("a", "b"))
        assertNotEquals(monday, tuesday)
    }

    @Test
    fun findClusterOrderOnlyReturnsMatchingGroup() {
        val key = StackOrderUtils.clusterKey(1, listOf("a", "b"))
        val orders = listOf(
            StackClusterOrder(clusterKey = key, courseIds = listOf("b", "a")),
            StackClusterOrder(clusterKey = "other", courseIds = listOf("x"))
        )
        assertEquals(listOf("b", "a"), StackOrderUtils.findClusterOrder(orders, key))
        assertEquals(emptyList(), StackOrderUtils.findClusterOrder(orders, "missing"))
    }

    @Test
    fun activeCourseOutranksPinnedDemotedCourse() {
        val result = ordered(
            candidateIds = listOf("oddWeek", "evenWeek"),
            explicitOrderIds = listOf("oddWeek"),
            demotedIds = setOf("oddWeek")
        )
        assertEquals(listOf("evenWeek", "oddWeek"), result)
    }

    @Test
    fun explicitOrderStillAppliesWithinActiveCourses() {
        val result = ordered(
            candidateIds = listOf("first", "pinned", "second"),
            explicitOrderIds = listOf("second", "first")
        )
        assertEquals(listOf("second", "first", "pinned"), result)
    }

    @Test
    fun explicitOrderAppliesWhenAllCoursesAreDemoted() {
        val result = ordered(
            candidateIds = listOf("a", "b"),
            explicitOrderIds = listOf("b"),
            demotedIds = setOf("a", "b")
        )
        assertEquals(listOf("b", "a"), result)
    }

    @Test
    fun activeCourseFirstWithoutExplicitOrder() {
        val result = ordered(
            candidateIds = listOf("demoted", "active", "alsoDemoted"),
            demotedIds = setOf("demoted", "alsoDemoted")
        )
        assertEquals(listOf("active", "demoted", "alsoDemoted"), result)
    }

    @Test
    fun duplicateCourseIdsAreNotDeduplicated() {
        val result = ordered(
            candidateIds = listOf("same", "same"),
            explicitOrderIds = listOf("same")
        )
        assertEquals(listOf("same", "same"), result)
    }
}