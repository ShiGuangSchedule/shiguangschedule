package com.xingheyuzhuan.shiguangschedule.data.di

import okio.Path

/**
 * 目标平台枚举
 */
enum class TargetPlatform {
    ANDROID,
    IOS,
    MACOS,
    WINDOWS,
    LINUX;

    companion object {
        /** 桌面平台集合 */
        val desktop: Set<TargetPlatform> = setOf(MACOS, WINDOWS, LINUX)

        /** 移动平台集合 */
        val mobile: Set<TargetPlatform> = setOf(ANDROID, IOS)
    }
}

/**
 * 平台环境信息抽象
 */
expect object PlatformEnvironment {
    val currentPlatform: TargetPlatform
}

/**
 * 判断当前运行平台是否在允许的平台或集合范围内
 */
fun isPlatformSupported(vararg allowedValues: Any): Boolean {
    val current = PlatformEnvironment.currentPlatform
    return allowedValues.any { value ->
        when (value) {
            is TargetPlatform -> current == value
            is Set<*> -> current in value
            else -> false
        }
    }
}

/**
 * 跨平台文件存储路径抽象
 */
interface AppStorage {
    val filesDir: Path
    val cacheDir: Path
    fun getDatabasePath(dbName: String): String
}