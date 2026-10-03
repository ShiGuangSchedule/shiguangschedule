package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import kotlinx.serialization.Serializable

/**
 * Room 实体类，代表“节次时间段”数据表。
 * 纯粹存储时间节点，与具体的 TimeTable 绑定。
 */
@Serializable
@Entity(
    tableName = "time_slots",
    foreignKeys = [
        ForeignKey(
            entity = TimeTable::class,
            parentColumns = ["id"],
            childColumns = ["timeTableId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["timeTableId"])],
    primaryKeys = ["timeTableId", "number"]
)
data class TimeSlot(
    val timeTableId: String, // 对应的 TimeTable ID（专属作息即为 courseTableId）
    val number: Int,         // 第几节课 (1, 2, 3...)
    val startTime: String,   // 开始时间 "08:00"
    val endTime: String,     // 结束时间 "08:45"
    val alias: String? = null // 时间段别名（可选，如 "早自习"）
)