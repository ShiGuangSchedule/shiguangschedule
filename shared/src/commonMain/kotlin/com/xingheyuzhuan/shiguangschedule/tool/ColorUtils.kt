package com.xingheyuzhuan.shiguangschedule.tool

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.dynamicColorScheme
import com.materialkolor.PaletteStyle

// M3 基线紫默认种子色
val DEFAULT_SEED_COLOR = Color(0xFF6750A4)

/**
 * 纯 Kotlin 动态配色生成函数
 * 供小组件快照构建、后台 Worker、iOS 共享层等无 Compose 上下文的模块直接调用
 */
fun createMaterialKolorScheme(
    darkTheme: Boolean,
    seedColor: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot
): ColorScheme {
    return dynamicColorScheme(
        seedColor = seedColor ?: DEFAULT_SEED_COLOR,
        isDark = darkTheme,
        style = style
    )
}

/**
 * 亮色/暗色配色对的数据类
 */
data class WidgetColorSchemePair(
    val light: ColorScheme,
    val dark: ColorScheme
)

/**
 * 一次性计算生成亮色与暗色两套 ColorScheme
 */
fun generateWidgetColorSchemePair(
    seedColor: Color? = null,
    style: PaletteStyle = PaletteStyle.TonalSpot
): WidgetColorSchemePair {
    return WidgetColorSchemePair(
        light = createMaterialKolorScheme(darkTheme = false, seedColor = seedColor, style = style),
        dark = createMaterialKolorScheme(darkTheme = true, seedColor = seedColor, style = style)
    )
}