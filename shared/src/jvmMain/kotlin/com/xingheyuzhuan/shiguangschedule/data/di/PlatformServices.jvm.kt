package com.xingheyuzhuan.shiguangschedule.data.di

import dev.nucleusframework.core.runtime.NucleusApp
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.databasesDir
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

/**
 * JVM / 桌面端平台的 PlatformEnvironment 实现
 */
actual object PlatformEnvironment {
    actual val currentPlatform: TargetPlatform
        get() {
            val system = System.getProperty("os.name").lowercase()
            return when {
                system.contains("win") -> TargetPlatform.WINDOWS
                system.contains("nix") || system.contains("nux") || system.contains("aix") -> TargetPlatform.LINUX
                system.contains("mac") -> TargetPlatform.MACOS
                else -> TargetPlatform.LINUX
            }
        }
}

@Single
class JvmAppStorage : AppStorage {
    // JVM 上必须先初始化 FileKit 才能读取应用目录
    init {
        FileKit.init("shiguangschedule")
    }

    override val filesDir: Path
        get() = FileKit.filesDir.path.toPath()

    override val cacheDir: Path
        get() = FileKit.cacheDir.path.toPath()

    override val databasesDir: Path
        get() = FileKit.databasesDir.also { it.createDirectories() }.path.toPath()
}


/**
 * 桌面端应用版本名称提供
 */
@Single
@Named("AppVersionName")
fun provideAppVersionName(): String {
    return NucleusApp.version ?: "1.0.0"
}