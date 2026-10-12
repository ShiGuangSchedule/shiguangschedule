package com.xingheyuzhuan.shiguangschedule.data.di

import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import java.io.File

@Single
class JvmAppStorage : AppStorage {

    private val appRootDir: File by lazy {
        val userHome = System.getProperty("user.home")
        val folderName = "shiguangschedule"

        // 结合 OperatingSystem 枚举进行类型安全的匹配
        val dir = when (OperatingSystem.current) {
            OperatingSystem.WINDOWS -> {
                val appData = System.getenv("APPDATA") ?: "$userHome/AppData/Roaming"
                File(appData, folderName)
            }
            OperatingSystem.MACOS -> {
                File(userHome, "Library/Application Support/$folderName")
            }
            OperatingSystem.LINUX -> {
                val configHome = System.getenv("XDG_CONFIG_HOME") ?: "$userHome/.config"
                File(configHome, folderName)
            }
            OperatingSystem.UNKNOWN -> {
                // 异常/极罕见系统兜底路径
                File(userHome, ".$folderName")
            }
        }

        if (!dir.exists()) dir.mkdirs()
        dir
    }

    override val filesDir: Path
        get() {
            val dir = File(appRootDir, "files")
            if (!dir.exists()) dir.mkdirs()
            return dir.absolutePath.toPath()
        }

    override val cacheDir: Path
        get() {
            val dir = File(appRootDir, "cache")
            if (!dir.exists()) dir.mkdirs()
            return dir.absolutePath.toPath()
        }

    override fun getDatabasePath(dbName: String): String {
        val dbDir = File(appRootDir, "databases")
        if (!dbDir.exists()) dbDir.mkdirs()
        return File(dbDir, dbName).absolutePath
    }
}

@Single
@Named("AppVersionCode")
fun provideAppVersionCode(): Int {
    return System.getProperty("app.version.code")?.toIntOrNull() ?: 1
}

@Single
@Named("AppVersionName")
fun provideAppVersionName(): String {
    return System.getProperty("app.version") ?: "1.0.0"
}