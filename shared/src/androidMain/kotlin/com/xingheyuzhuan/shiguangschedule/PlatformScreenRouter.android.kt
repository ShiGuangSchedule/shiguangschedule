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
        if (destination !is Destination.AndroidDestination) return false

        val content: @Composable () -> Unit = when (destination) {
            Destination.AndroidNotificationSettings -> {
                { NotificationSettingsScreen(onBack = onBack) }
            }
        }
        content()
        return true
    }
}
