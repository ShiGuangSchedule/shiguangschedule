package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

@Composable
actual fun rememberSystemDark(): Boolean {
    return isSystemInDarkTheme()
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
    SideEffect {
        val uiStyle = when (themeMode) {
            AppThemeMode.FOLLOW_SYSTEM -> UIUserInterfaceStyle.UIUserInterfaceStyleUnspecified
            AppThemeMode.LIGHT -> UIUserInterfaceStyle.UIUserInterfaceStyleLight
            AppThemeMode.DARK -> UIUserInterfaceStyle.UIUserInterfaceStyleDark
        }

        UIApplication.sharedApplication.connectedScenes.forEach { scene ->
            (scene as? UIWindowScene)?.windows?.forEach { window ->
                (window as? UIWindow)?.let { win ->
                    win.overrideUserInterfaceStyle = uiStyle
                    win.rootViewController?.setNeedsStatusBarAppearanceUpdate()
                }
            }
        }
    }
}

actual val supportsDynamicColor: Boolean = false