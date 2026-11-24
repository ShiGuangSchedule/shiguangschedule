package com.xingheyuzhuan.shiguangschedule.di.platform

import dev.nucleusframework.systemcolor.isSystemAccentColorSupported
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

/** JVM 支持的能力在本包注册；不支持的能力无需声明。 */
@Module
@ComponentScan
actual class PlatformBridgeModule

/** 桌面端在系统强调色可用时支持动态取色。 */
@Singleton
class JvmDynamicColorSupport : DynamicColorSupport {
    override val supported: Boolean
        get() = isSystemAccentColorSupported()
}
