package com.xingheyuzhuan.shiguangschedule

import androidx.compose.runtime.Composable

actual object PlatformScreenRouter {
    @Composable
    actual fun Content(
        destination: Destination,
        onNavigate: (Destination) -> Unit,
        onBack: () -> Unit
    ): Boolean {
        return when (destination) {
            is Destination.AndroidDestination -> {
                when (destination) {
                    // TODO: Android专属页面在这里实现
                    else -> false
                }
            }
            else -> false
        }
    }
}