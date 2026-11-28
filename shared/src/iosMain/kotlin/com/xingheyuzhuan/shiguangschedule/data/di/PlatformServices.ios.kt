package com.xingheyuzhuan.shiguangschedule.data.di

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import platform.Foundation.NSBundle

/** iOS 平台的 PlatformEnvironment 实现 */
actual object PlatformEnvironment {
    actual val currentPlatform: TargetPlatform = TargetPlatform.IOS
}

@Single
class IosAppStorage : AppStorage {
    override val filesDir: Path
        get() = FileKit.filesDir.path.toPath()

    override val cacheDir: Path
        get() = FileKit.cacheDir.path.toPath()

    override val databasesDir: Path
        get() = FileKit.databasesDir.path.toPath()
}

/** 通过 iOS NSBundle 提供应用版本名称 */
@Single
@Named("AppVersionName")
fun provideAppVersionName(): String {
    return NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0.0"
}
