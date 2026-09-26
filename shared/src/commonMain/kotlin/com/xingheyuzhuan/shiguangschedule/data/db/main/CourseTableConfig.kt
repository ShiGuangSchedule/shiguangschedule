package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.Serializable

/**
 * Room 实体类，代表“课表配置”数据表。
 * 与 CourseTable 形成一对一关系，存储课表专属的设置。
 */
@Serializable
@Entity(
    tableName = "course_table_config",
    foreignKeys = [
        ForeignKey(
            entity = CourseTable::class,
            parentColumns = ["id"],
            childColumns = ["courseTableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["courseTableId"], unique = true)
    ]
)
data class CourseTableConfig(
    @PrimaryKey
    val courseTableId: String, // 【主键/外键】关联的课表 ID，确保一个课表只有一条配置记录

    val showWeekends: Boolean = false, // 是否在课表视图中显示周末（周六、周日）
    val semesterStartDate: String? = null, // 学期开始日期（格式为 "yyyy-MM-dd"），用于计算当前周数
    val semesterTotalWeeks: Int = 20, // 本学期的总周数，用于定义学期范围
    val firstDayOfWeek: Int = DayOfWeek.MONDAY.isoDayNumber // 该课表计算周数时，一周的起始日（1=周一，7=周日）
)