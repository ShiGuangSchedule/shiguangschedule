package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room 实体类，代表“课程”数据表。
 * 它存储每门课程的详细信息，并与 `CourseTable` 关联。
 * 支持标准节次时间和自定义周重复时间两种模式。
 */
@Serializable
@Entity(
    tableName = "courses",
    foreignKeys = [ForeignKey(
        entity = CourseTable::class,
        parentColumns = ["id"],
        childColumns = ["courseTableId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["courseTableId"])]
)
data class Course(
    @PrimaryKey
    val id: String, // 课程的唯一标识符 UUID
    val courseTableId: String, // 课程所属的课表 ID
    val name: String, // 课程名称，例如 "大学物理"
    val teacher: String, // 授课老师姓名
    val position: String, // 上课地点，例如 "主楼A101"
    val day: Int, // 上课的星期几，例如 1=周一, 7=周日

    // 设为可空：当使用自定义时间时，这些字段将被忽略
    val startSection: Int?, // 课程开始的节次
    val endSection: Int?,   // 课程结束的节次
    val isCustomTime: Boolean = false, // 是否使用自定义时间 (true: 使用 HH:MM 字符串)
    val customStartTime: String?, // 自定义起始时间，格式为 "HH:MM"
    val customEndTime: String?,   // 自定义结束时间，格式为 "HH:MM"
    val colorInt: Int, // 课程卡片的颜色索引
    val remark: String? = null, // 课程备注信息，限 300 字以内
)