package com.xingheyuzhuan.shiguangschedule

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope

/** 只接受 iOS 页面类型的注册辅助函数，传错平台类型将编译失败。 */
private inline fun <reified K : Destination.IosDestination> EntryProviderScope<Destination>.iosEntry(
    noinline content: @Composable (K) -> Unit
) = entry<K>(content = content)

actual fun EntryProviderScope<Destination>.platformEntryBuilder(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit
) {
    // TODO: iOS 专属页面在此通过 iosEntry<...> { ... } 注册
}
