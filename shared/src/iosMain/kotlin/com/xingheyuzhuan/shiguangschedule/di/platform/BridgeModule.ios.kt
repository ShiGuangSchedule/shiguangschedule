package com.xingheyuzhuan.shiguangschedule.di.platform

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/** iOS 支持的能力在本包注册；不支持的能力无需声明。 */
@Module
@ComponentScan
actual class PlatformBridgeModule
