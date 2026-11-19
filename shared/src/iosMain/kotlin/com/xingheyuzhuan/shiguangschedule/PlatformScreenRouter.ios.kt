package com.xingheyuzhuan.shiguangschedule

import androidx.compose.runtime.Composable

actual object PlatformScreenRouter {
    @Composable
    actual fun Content(
        destination: Destination,
        onNavigate: (Destination) -> Unit,
        onBack: () -> Unit
    ): Boolean {
        if (destination !is Destination.IosDestination) return false

        // TODO: iOS专属页面在这里实现
        return false
    }
}
