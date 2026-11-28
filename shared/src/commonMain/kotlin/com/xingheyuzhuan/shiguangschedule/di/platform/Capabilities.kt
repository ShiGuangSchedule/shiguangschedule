package com.xingheyuzhuan.shiguangschedule.di.platform

import androidx.compose.runtime.Composable

/** 当前平台是否支持系统动态取色（Dynamic Color）。 */
interface DynamicColorSupport {
    val supported: Boolean
}

/** 系统文件分享能力。 */
interface ShareFileSupport {
    @Composable
    fun rememberShareAction(): (filePath: String) -> Unit
}