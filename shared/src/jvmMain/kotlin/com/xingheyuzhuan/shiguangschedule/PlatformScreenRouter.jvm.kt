package com.xingheyuzhuan.shiguangschedule

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.extension.ExtensionBridgeSettingsScreen
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsScreen

/** 只接受 Desktop 页面类型的注册辅助函数，传错平台类型将编译失败。 */
private inline fun <reified K : Destination.DesktopDestination> EntryProviderScope<Destination>.desktopEntry(
    noinline content: @Composable (K) -> Unit
) = entry<K>(content = content)

actual fun EntryProviderScope<Destination>.platformEntryBuilder(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit
) {
    desktopEntry<Destination.DesktopNotificationSettings> {
        NotificationSettingsScreen(onBack = onBack)
    }
    desktopEntry<Destination.DesktopExtensionBridgeSettings> { key ->
        ExtensionBridgeSettingsScreen(
            onBack = onBack,
            schoolId = key.schoolId,
            adapterId = key.adapterId
        )
    }
}
