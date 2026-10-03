package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 组合作息方案元数据
 */
@Serializable
@Entity(
    tableName = "time_table_combos",
    foreignKeys = [
        ForeignKey(
            entity = TimeTable::class,
            parentColumns = ["id"],
            childColumns = ["baseTimeTableId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["baseTimeTableId"])]
)
data class TimeTableCombo(
    @PrimaryKey
    val id: String,                      // 组合方案 UUID
    val name: String,                    // 如 "夏冬令时自动切换方案"
    val baseTimeTableId: String? = null, // 基准作息 ID：null 代表动态专属作息，非空代表静态公共作息 ID
    val createdAt: Long                  // 创建时间戳
)

/**
 * 组合方案内部的具体调度规则（仅基于日期范围）
 */
@Serializable
@Entity(
    tableName = "time_table_combo_rules",
    foreignKeys = [
        ForeignKey(
            entity = TimeTableCombo::class,
            parentColumns = ["id"],
            childColumns = ["comboId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TimeTable::class,
            parentColumns = ["id"],
            childColumns = ["targetTimeTableId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["comboId"]), Index(value = ["targetTimeTableId"])]
)
data class TimeTableComboRule(
    @PrimaryKey
    val id: String,
    val comboId: String,           // 所属组合方案 ID
    val targetTimeTableId: String, // 满足条件时应用的 TimeTable ID
    val startDate: String,         // 起始日期 "2026-05-01"
    val endDate: String            // 结束日期 "2026-09-30"
)