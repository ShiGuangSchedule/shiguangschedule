package com.xingheyuzhuan.shiguangschedule.data.di

import android.content.Context
import android.os.Build
import okio.Path
import okio.Path.Companion.toOkioPath
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

actual object PlatformEnvironment {
    actual val currentPlatform: TargetPlatform = TargetPlatform.ANDROID
}

@Single
class AndroidAppStorage(private val context: Context) : AppStorage {
    override val filesDir: Path
        get() = context.filesDir.toOkioPath()

    override val cacheDir: Path
        get() = context.cacheDir.toOkioPath()

    override fun getDatabasePath(dbName: String): String {
        return context.getDatabasePath(dbName).absolutePath
    }
}

@Single
@Named("AppVersionCode")
fun provideAppVersionCode(context: Context): Int {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode.toInt()
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode
    }
}

@Single
@Named("AppVersionName")
fun provideAppVersionName(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    return packageInfo.versionName ?: "1.0.0"
}