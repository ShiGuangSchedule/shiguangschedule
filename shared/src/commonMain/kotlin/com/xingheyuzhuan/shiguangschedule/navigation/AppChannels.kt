// com/xingheyuzhuan/shiguangschedule/navigation/AppChannels.kt
package com.xingheyuzhuan.shiguangschedule.navigation

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow


/**
 * 传递课程编辑/添加页面的预设数据。
 * 所有参数均为可空类型，支持选择性预填充。
 */
data class PresetCourseData(
    // --- 基础公共信息 ---
    val name: String? = null,
    val teacher: String? = null,
    val position: String? = null,
    val credit: Float? = null,          // 学分预设
    val remark: String? = null,         // 备注信息的预设
    val day: Int? = null,               // 周几 (1=周一, 7=周日)

    // 课节模式所需字段
    val startSection: Int? = null,      // 预设开始节次
    val endSection: Int? = null,        // 预设结束节次

    // 自定义时间模式所需字段
    val isCustomTime: Boolean = false,  // 自定义时间开关
    val customStartTime: String? = null,// 预设起始时间，格式 "HH:MM"
    val customEndTime: String? = null,  // 预设结束时间，格式 "HH:MM"

    val presetWeeks: Set<Int>? = null,  // 预设周次
    val colorIndex: Int? = null         // 预设颜色
)

/**
 * 安全传递一次性数据的通道。
 */
object AddEditCourseChannel {
    private val channel = Channel<PresetCourseData>(Channel.CONFLATED)
    fun sendEvent(data: PresetCourseData) {
        channel.trySend(data)
    }
    val presetDataFlow = channel.receiveAsFlow()
}