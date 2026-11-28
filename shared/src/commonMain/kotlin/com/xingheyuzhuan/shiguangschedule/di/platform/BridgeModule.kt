package com.xingheyuzhuan.shiguangschedule.di.platform

import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/** 各平台的能力注册模块，由对应 source set 提供 actual。 */
@Module
expect class PlatformBridgeModule

/** 平台桥接根模块，交由 Koin 自动挂载。 */
@Module(includes = [PlatformBridgeModule::class])
@Configuration
class BridgeModule
