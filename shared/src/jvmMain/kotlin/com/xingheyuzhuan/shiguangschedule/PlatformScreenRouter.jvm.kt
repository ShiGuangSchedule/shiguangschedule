package com.xingheyuzhuan.shiguangschedule

import androidx.compose.runtime.Composable
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsScreen

actual object PlatformScreenRouter {
    @Composable
    actual fun Content(
        destination: Destination,
        onNavigate: (Destination) -> Unit,
        onBack: () -> Unit
    ): Boolean {
        return when (destination) {
            is Destination.DesktopDestination -> {
                when (destination) {
                    Destination.DesktopNotificationSettings -> {
                        NotificationSettingsScreen(onBack = onBack)
                        true
                    }
                    else -> false
                }
            }
            else -> false
        }
    }
}