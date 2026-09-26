package com.xingheyuzhuan.shiguangschedule.data.model

import com.xingheyuzhuan.shiguangschedule.data.db.main.Course
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfig
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBinding
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeek
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTable
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableCombo
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableComboRule
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json

object CourseImportExport {

    /**
     * 核心数据规范版本号
     * v1: 单表 JSON 导出的多课表集合协议 (List<SingleTablePack>)
     * v2: 全盘 CBOR 结构化全量数据备份协议 (CourseDatabasePayload)
     */
    const val COURSE_SCHEMA_VERSION = 2

    /**
     * 自定义 Json 解析器（用于单表 JSON 导出导入）
     * ignoreUnknownKeys = true: 确保旧版 App 遇到新加的字段时能跳过而不崩溃
     * encodeDefaults = true: 导出时即使字段是默认值也会包含在 JSON 中
     * coerceInputValues = true: 容错解析，如 null 转为默认值
     */
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    /**
     * CBOR 解析器（用于全盘数据结构化备份流场景）
     */
    @OptIn(ExperimentalSerializationApi::class)
    val cbor = Cbor {
        ignoreUnknownKeys = true
    }

    // 全盘二进制备份/恢复协议结构（用于 BackupRepository 全量备份）

    /**
     * 全盘备份文件的最外层“集装箱”
     */
    @Serializable
    data class TotalAppBackupEnvelope(
        val backupTimestamp: Long,          // 备份生成的时间戳
        val appVersionCode: Int,            // 实际承载 COURSE_SCHEMA_VERSION，代表数据协议版本
        val currentCourseTableId: String,   // 备份前用户当前激活/选中的课表 ID
        val databasePayload: CourseDatabasePayload // 核心数据库所有实体的结构化载体
    )

    /**
     * 全量数据库实体载体（替换原本的二进制 .db 字节数组载体）
     */
    @Serializable
    data class CourseDatabasePayload(
        // 1. 课表核心相关表数据
        val courseTables: List<CourseTable> = emptyList(),
        val courseTableConfigs: List<CourseTableConfig> = emptyList(),
        val courseTimeBindings: List<CourseTimeBinding> = emptyList(),
        val courses: List<Course> = emptyList(),
        val courseWeeks: List<CourseWeek> = emptyList(),

        // 2. 作息方案相关表数据
        val timeTables: List<TimeTable> = emptyList(),
        val timeSlots: List<TimeSlot> = emptyList(),
        val timeTableCombos: List<TimeTableCombo> = emptyList(),
        val timeTableComboRules: List<TimeTableComboRule> = emptyList()
    )

    // =========================================================================
    // 单表 JSON 导入/导出数据结构（用于 CourseConversionRepository）

    // 用于 JSON 导入和导出的配置模型
    @Serializable
    data class CourseConfigJsonModel(
        val semesterStartDate: String? = null,
        val semesterTotalWeeks: Int = 20,
        val defaultClassDuration: Int = 45, // 基准作息的默认单节时长
        val defaultBreakDuration: Int = 10, // 基准作息的默认休息时长
        val firstDayOfWeek: Int = 1
    )

    // 导入时使用的 JSON 模型
    @Serializable
    data class CourseTableImportModel(
        val courses: List<ImportCourseJsonModel>,
        val timeSlots: List<TimeSlotJsonModel>? = emptyList(), // 跟随/基准作息
        val config: CourseConfigJsonModel? = null,
        val comboSchedule: ComboScheduleJsonModel? = null       // 组合作息扩展（可选）
    )

    @Serializable
    data class ImportCourseJsonModel(
        val name: String,
        val teacher: String,
        val position: String,
        val day: Int,
        val startSection: Int? = null,
        val endSection: Int? = null,
        val weeks: List<Int> = emptyList(),
        val isCustomTime: Boolean = false,
        val customStartTime: String? = null,
        val customEndTime: String? = null,
        val color: Int? = null,
        val remark: String? = null,
        val credit: Float? = null
    )

    // 导出时使用的 JSON 模型
    @Serializable
    data class CourseTableExportModel(
        val courses: List<ExportCourseJsonModel>,
        val timeSlots: List<TimeSlotJsonModel>,             // 跟随/基准作息
        val config: CourseConfigJsonModel,
        val comboSchedule: ComboScheduleJsonModel? = null   // 组合作息扩展（可选）
    )

    @Serializable
    data class ExportCourseJsonModel(
        val name: String,
        val teacher: String,
        val position: String,
        val day: Int,
        val startSection: Int? = null,
        val endSection: Int? = null,
        val weeks: List<Int>,
        val isCustomTime: Boolean = false,
        val customStartTime: String? = null,
        val customEndTime: String? = null,
        val color: Int,
        val remark: String? = null,
        val credit: Float? = null
    )

    // 导入和导出都通用的时间段模型
    @Serializable
    data class TimeSlotJsonModel(
        val number: Int,
        val startTime: String,
        val endTime: String,
        val alias: String? = null
    )

    // 组合作息配置模型
    @Serializable
    data class ComboScheduleJsonModel(
        val name: String? = null, // 组合作息方案名称
        val publicSchedules: List<PublicScheduleTemplateJsonModel> = emptyList() // 公共作息模板规则列表
    )

    // 组合作息下辖的公共作息模板模型
    @Serializable
    data class PublicScheduleTemplateJsonModel(
        val name: String,                   // 公共作息名称（如"夏季作息"）
        val startDate: String,              // 生效起始日期 "2026-05-01"
        val endDate: String,                // 生效结束日期 "2026-09-30"
        val defaultClassDuration: Int = 45, // 该公共作息下的单节时长
        val defaultBreakDuration: Int = 10, // 该公共作息下的休息时长
        val timeSlots: List<TimeSlotJsonModel> // 该公共作息对应的节次时间点
    )
}