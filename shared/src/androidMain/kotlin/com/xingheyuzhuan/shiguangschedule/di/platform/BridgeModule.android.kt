package com.xingheyuzhuan.shiguangschedule.di.platform

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

/** Android 支持的能力在本包注册；不支持的能力无需声明。 */
@Module
@ComponentScan(
    "com.xingheyuzhuan.shiguangschedule.di.platform",
    "com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web"
)
actual class PlatformBridgeModule

/** Android 12 (API 31) 起支持系统动态取色。 */
@Singleton
class AndroidDynamicColorSupport : DynamicColorSupport {
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    override val supported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
