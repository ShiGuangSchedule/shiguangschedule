package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 课表与作息方案的绑定关系表
 * - courseTableId 为主键，确保每个课表有且仅有一条记录
 * - targetId 支持多对一（多个课表可复用同一个公共/组合作息）
 */
@Serializable
@Entity(
    tableName = "course_time_bindings",
    foreignKeys = [
        ForeignKey(
            entity = CourseTable::class,
            parentColumns = ["id"],
            childColumns = ["courseTableId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CourseTimeBinding(
    @PrimaryKey
    val courseTableId: String,          // 课表 ID
    val targetType: TargetType = TargetType.SINGLE, // 枚举字段，默认 SINGLE
    val targetId: String                // 当等于 courseTableId 时即为专属作息
) {
    /**
     * 作息目标类型
     */
    @Serializable
    enum class TargetType {
        SINGLE, // 单一作息
        COMBO   // 组合作息
    }
}