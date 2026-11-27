package com.xingheyuzhuan.shiguangschedule.data.di

import android.content.Context
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.manualFileKitCoreInitialization
import io.github.vinceglb.filekit.path
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

actual object PlatformEnvironment {
    actual val currentPlatform: TargetPlatform = TargetPlatform.ANDROID
}

@Single
class AndroidAppStorage(private val context: Context) : AppStorage {
    // 需要手动初始化 FileKit
    init {
        FileKit.manualFileKitCoreInitialization(context)
    }

    override val filesDir: Path
        get() = FileKit.filesDir.path.toPath()

    override val cacheDir: Path
        get() = FileKit.cacheDir.path.toPath()

    override val databasesDir: Path
        get() = FileKit.databasesDir.path.toPath()
}

/** 安卓端应用版本名称提供 */
@Single
@Named("AppVersionName")
fun provideAppVersionName(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return packageInfo.versionName ?: "1.0.0"
}
