package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.compositionLocalOf

/**
 * 隐式下发当前项卡片形状的 Local。
 * 默认值为 16.dp 大圆角（用于单独调用或没有外层容器包裹的场景）。
 */
val LocalCardShape = compositionLocalOf { RoundedCornerShape(SettingsDefaults.OuterCornerRadius) }