package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import dev.nucleusframework.darkmodedetector.isSystemInDarkMode

@Composable
actual fun rememberSystemDark(): Boolean {
    return isSystemInDarkMode()
}

@Composable
actual fun rememberColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    customLightPrimary: Color,
    customDarkPrimary: Color
): ColorScheme {
    val seedColor = if (darkTheme) customDarkPrimary else customLightPrimary

    return rememberMaterialKolorScheme(
        darkTheme = darkTheme,
        seedColor = seedColor
    )
}

@Composable
actual fun SetupPlatformThemeEffects(
    colorScheme: ColorScheme,
    darkTheme: Boolean,
    themeMode: AppThemeMode
) {
    // Desktop 端窗口外观交由 Compose Desktop 的 Window 属性控制，此处无需额外处理
    // 如要实现自绘边框需要补充实现
}

actual val supportsDynamicColor: Boolean = false