package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import kotlinx.serialization.Serializable

/**
 * Room 实体类，代表课程和周数的关联数据表。
 * 它解决了课程和周数之间的多对多关系。
 */
@Serializable
@Entity(
    tableName = "course_weeks",
    // 联合主键，确保每对关联是唯一的
    primaryKeys = ["courseId", "weekNumber"],
    // 外键，当课程被删除时，其对应的周数关联也一并删除
    foreignKeys = [
        ForeignKey(
            entity = Course::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // 索引，以提高查询性能
    indices = [Index(value = ["courseId"]), Index(value = ["weekNumber"])]
)
data class CourseWeek(
    val courseId: String, // 课程的唯一标识符
    val weekNumber: Int   // 课程所在的周数
)