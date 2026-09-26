package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room 实体类，代表“课表元数据”数据表。
 * 存储每个课表的唯一信息，例如名称和 ID。
 */
@Serializable
@Entity(tableName = "course_tables")
data class CourseTable(
    @PrimaryKey
    val id: String, // 使用 String 作为主键，与现有逻辑兼容
    val name: String, // 课表名称
    val createdAt: Long // 创建时间戳，有助于排序
)