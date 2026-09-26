package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 时间表实体
 * - 专属表：id == courseTableId 且 name == null
 * - 公共表：id 为独立 UUID 且 name != null
 */
@Serializable
@Entity(tableName = "time_tables")
data class TimeTable(
    @PrimaryKey
    val id: String,          // 专属表同 courseTableId；公共表为公共 UUID
    val name: String? = null, // 专属表存 null；公共表存自定义名称
    val createdAt: Long,      // 创建时间戳，有助于排序
    val defaultClassDuration: Int = 45, // 默认上课时长，单位：分钟
    val defaultBreakDuration: Int = 10  // 默认下课休息时长，单位：分钟
)