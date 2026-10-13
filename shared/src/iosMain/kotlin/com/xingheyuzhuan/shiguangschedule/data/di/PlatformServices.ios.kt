package com.xingheyuzhuan.shiguangschedule.data.di

import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import platform.Foundation.NSBundle
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

/**
 * iOS 平台的 PlatformEnvironment 实现
 */
actual object PlatformEnvironment {
    actual val currentPlatform: TargetPlatform = TargetPlatform.IOS
}

@Single
class IosAppStorage : AppStorage {
    override val filesDir: Path
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            val pathStr = paths.first() as? String ?: ""
            return pathStr.toPath()
        }

    override val cacheDir: Path
        get() {
            val paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true)
            val pathStr = paths.first() as? String ?: ""
            return pathStr.toPath()
        }

    override fun getDatabasePath(dbName: String): String {
        return "$filesDir/$dbName"
    }
}

/**
 * 通过 iOS NSBundle 提供应用版本号 (CFBundleVersion)
 */
@Single
@Named("AppVersionCode")
fun provideAppVersionCode(): Int {
    val buildStr = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
    return buildStr?.toIntOrNull() ?: 1
}

/**
 * 通过 iOS NSBundle 提供应用版本名称 (CFBundleShortVersionString)
 */
@Single
@Named("AppVersionName")
fun provideAppVersionName(): String {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0.0"
}