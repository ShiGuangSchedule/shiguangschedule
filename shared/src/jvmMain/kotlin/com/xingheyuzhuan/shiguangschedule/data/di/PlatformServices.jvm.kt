package com.xingheyuzhuan.shiguangschedule.data.di

import dev.nucleusframework.core.runtime.NucleusApp
import okio.Path
import okio.Path.Companion.toOkioPath
import org.apache.commons.lang3.SystemUtils
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File

/**
 * JVM / 桌面端平台的 PlatformEnvironment 实现
 */
actual object PlatformEnvironment {
    actual val currentPlatform: TargetPlatform
        get() = when {
            SystemUtils.IS_OS_WINDOWS -> TargetPlatform.WINDOWS
            SystemUtils.IS_OS_MAC -> TargetPlatform.MACOS
            SystemUtils.IS_OS_LINUX -> TargetPlatform.LINUX
            else -> throw IllegalStateException("Unsupported operating system: ${System.getProperty("os.name")}")
        }
}

@Single
class JvmAppStorage : AppStorage {
    private val appDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val baseDir = when {
            SystemUtils.IS_OS_WINDOWS -> File(System.getenv("APPDATA") ?: "$userHome/AppData/Roaming")
            SystemUtils.IS_OS_MAC -> File("$userHome/Library/Application Support")
            else -> File(System.getenv("XDG_CONFIG_HOME") ?: "$userHome/.config")
        }
        File(baseDir, "shiguangschedule").apply { mkdirs() }
    }

    override val filesDir: Path
        get() = File(appDir, "files").apply { mkdirs() }.toPath().toOkioPath()

    override val cacheDir: Path
        get() = File(appDir, "cache").apply { mkdirs() }.toPath().toOkioPath()

    override fun getDatabasePath(dbName: String): String {
        return File(File(appDir, "databases").apply { mkdirs() }, dbName).absolutePath
    }
}


/**
 * 桌面端应用版本名称提供
 */
@Single
@Named("AppVersionName")
fun provideAppVersionName(): String {
    return NucleusApp.version ?: "1.0.0"
}