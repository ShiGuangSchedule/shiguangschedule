package com.xingheyuzhuan.shiguangschedule

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import com.xingheyuzhuan.shiguangschedule.ui.settings.notification.NotificationSettingsScreen

/** 只接受 Android 页面类型的注册辅助函数，传错平台类型将编译失败。 */
private inline fun <reified K : Destination.AndroidDestination> EntryProviderScope<Destination>.androidEntry(
    noinline content: @Composable (K) -> Unit
) = entry<K>(content = content)

actual fun EntryProviderScope<Destination>.platformEntryBuilder(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit
) {
    androidEntry<Destination.AndroidNotificationSettings> {
        NotificationSettingsScreen(onBack = onBack)
    }
}
