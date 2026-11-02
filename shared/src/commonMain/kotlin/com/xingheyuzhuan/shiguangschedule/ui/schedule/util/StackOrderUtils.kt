package com.xingheyuzhuan.shiguangschedule.ui.schedule.util

import com.xingheyuzhuan.shiguangschedule.data.model.StackClusterOrder

/**
 * 冲突课程堆叠顺序工具。
 *
 * 每个冲突格（星期 + 课程 ID 集合）独立记忆顺序：
 * 1. 本周生效的课程优先于非本周课程，置顶也不能让非本周课程覆盖本周课程；
 * 2. 同一生效状态内，用户显式排序按保存顺序决定层级；
 * 3. 未配置的课程保持合并算法给出的原始顺序（开始时间、子列顺序）。
 */
object StackOrderUtils {

    /**
     * 生成冲突格的稳定 key。课程 ID 排序后拼接，避免集合顺序影响匹配。
     */
    fun clusterKey(day: Int, courseIds: List<String>): String {
        return "$day|" + courseIds.sorted().joinToString(",")
    }

    fun findClusterOrder(
        orders: List<StackClusterOrder>,
        clusterKey: String
    ): List<String> {
        return orders.firstOrNull { it.clusterKey == clusterKey }?.courseIds ?: emptyList()
    }

    /**
     * 按堆叠顺序取出冲突簇课程。显式顺序中未出现的课程保持原始顺序。
     * 直接对条目排序，避免同一课程 ID 对应多个时间段时被去重。
     */
    fun <T> sortByStackOrder(
        items: List<T>,
        explicitOrderIds: List<String>,
        idOf: (T) -> String,
        isDemoted: (T) -> Boolean = { false }
    ): List<T> {
        val explicitIndex = explicitOrderIds
            .withIndex()
            .associate { (index, id) -> id to index }
        return items.sortedWith(
            compareBy<T> { if (isDemoted(it)) 1 else 0 }
                .thenBy { explicitIndex[idOf(it)] ?: Int.MAX_VALUE }
        )
    }
}
