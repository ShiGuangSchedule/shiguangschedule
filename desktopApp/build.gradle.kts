import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.nio.file.Files
import java.nio.file.StandardCopyOption

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    jvmToolchain(21)
}

val appVersion = "2.1.0"
val appBaseName = "shiguangschedule"

dependencies {
    implementation(project(":shared"))
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.annotations)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.compose.ui.tooling.preview)
}

compose.desktop {
    application {
        mainClass = "com.xingheyuzhuan.shiguangschedule.MainKt"

        buildTypes.release.proguard {
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(
                TargetFormat.Exe, TargetFormat.Msi,
                TargetFormat.Dmg, TargetFormat.Pkg,
                TargetFormat.Deb, TargetFormat.Rpm
            )
            packageName = "ShiguangSchedule"
            packageVersion = appVersion

            modules("jdk.unsupported")
        }
    }
}

tasks.register("renameDesktopPackage") {
    val osName = System.getProperty("os.name").lowercase()
    val osArch = System.getProperty("os.arch").lowercase()
    val version = appVersion
    val baseName = appBaseName

    when {
        osName.contains("win") -> {
            dependsOn("packageReleaseExe", "packageReleaseMsi")
        }
        osName.contains("mac") -> {
            dependsOn("packageReleaseDmg", "packageReleasePkg")
        }
        osName.contains("linux") -> {
            dependsOn("packageReleaseDeb", "packageReleaseRpm")
        }
    }

    doLast {
        val binariesRoot = listOf("main-release", "main")
            .map { layout.buildDirectory.dir("compose/binaries/$it").get().asFile }
            .firstOrNull { it.isDirectory }
            ?: layout.buildDirectory.dir("compose/binaries/main").get().asFile

        if (!binariesRoot.isDirectory) {
            logger.warn("renameDesktopPackage: 未找到 binaries 目录，跳过重命名。")
            return@doLast
        }

        val osTag = when {
            osName.contains("win")   -> "windows"
            osName.contains("mac")   -> "macos"
            osName.contains("linux") -> "linux"
            else -> "unknown"
        }

        val arch = when (osArch) {
            "amd64", "x86_64" -> "x64"
            "aarch64", "arm64" -> "arm64"
            else -> osArch
        }

        val buildType = "release"
        val subDirs = listOf("exe", "msi", "dmg", "pkg", "deb", "rpm")
        val knownExts = setOf("exe", "msi", "dmg", "pkg", "deb", "rpm")

        var renamedCount = 0

        subDirs.forEach { sub ->
            val dir = binariesRoot.resolve(sub)
            if (!dir.isDirectory) return@forEach

            dir.listFiles()?.forEach { original ->
                if (!original.isFile) return@forEach

                val ext = original.extension.lowercase()
                if (ext !in knownExts) return@forEach

                val prefix = "${baseName}-v${version}-${osTag}-${arch}-${buildType}."
                if (original.name.startsWith(prefix)) return@forEach

                val targetName = "${baseName}-v${version}-${osTag}-${arch}-${buildType}.${ext}"
                val targetFile = dir.resolve(targetName)

                try {
                    Files.move(
                        original.toPath(),
                        targetFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    renamedCount++
                    logger.lifecycle("重命名: ${original.name} -> $targetName")
                } catch (e: Exception) {
                    logger.warn("重命名失败: ${original.absolutePath} -> ${targetFile.absolutePath} (${e.message})")
                }
            }
        }

        logger.lifecycle("renameDesktopPackage 完成：共重命名 $renamedCount 个文件，根目录 = $binariesRoot")
    }
}