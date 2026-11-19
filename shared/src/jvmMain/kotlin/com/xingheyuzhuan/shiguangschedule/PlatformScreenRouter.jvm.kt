package com.xingheyuzhuan.shiguangschedule

import androidx.compose.runtime.Composable
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.extension.ExtensionBridgeSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsScreen

actual object PlatformScreenRouter {
    @Composable
    actual fun Content(
        destination: Destination,
        onNavigate: (Destination) -> Unit,
        onBack: () -> Unit
    ): Boolean {
        if (destination !is Destination.DesktopDestination) return false

        val content: @Composable () -> Unit = when (destination) {
            is Destination.DesktopNotificationSettings -> {
                { NotificationSettingsScreen(onBack = onBack) }
            }
            is Destination.DesktopExtensionBridgeSettings -> {
                {
                    ExtensionBridgeSettingsScreen(
                        onBack = onBack,
                        schoolId = destination.schoolId,
                        adapterId = destination.adapterId
                    )
                }
            }
        }
        content()
        return true
    }
}
