import dev.nucleusframework.desktop.application.dsl.NativeImageOptimization
import dev.nucleusframework.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.nucleus)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

val appVersion = "2.1.0"

dependencies {

    implementation(project(":shared"))
    implementation(libs.compose.components.resources)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.annotations)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.compose.material3)
    compileOnly(libs.compose.ui.tooling.preview)
    implementation(libs.nucleus.core.runtime)
    implementation(libs.nucleus.application)
    implementation(libs.nucleus.decorated.window.core)
    implementation(libs.nucleus.decorated.window.tao)
    implementation(libs.nucleus.composenativetray)
    implementation(libs.nucleus.notification.common)
    implementation(libs.nucleus.autolaunch)
}

nucleus.application {
    mainClass = "com.xingheyuzhuan.shiguangschedule.MainKt"

    graalvm {
        isEnabled = true
        imageName = "shiguangschedule"

        toolchain {
            autoDownload = true
            version = "25.0.1"  //macos x64 最后支持版本
        }

        optimization = NativeImageOptimization.SIZE

        windows {
            bundleCRuntime = true
        }
    }

    buildTypes.release.proguard {
        version.set("7.7.0")
        isEnabled.set(project.findProperty("enableProguard")?.toString()?.toBoolean() ?: false)
        optimize.set(false)
        obfuscate.set(false)
        configurationFiles.from(project.file("proguard-rules.pro"))
    }

    nativeDistributions {
        targetFormats(
            // Windows
            TargetFormat.Nsis,
            TargetFormat.Portable,

            // macOS
            TargetFormat.Dmg,
            TargetFormat.Pkg,

            // Linux
            TargetFormat.Deb,
            TargetFormat.Rpm,
            TargetFormat.AppImage,
        )
        appName = "拾光课程表"
        packageName = "xingheyuzhuan.shiguangschedule"
        packageVersion = appVersion

        homepage = "https://github.com/ShiGuangSchedule/shiguangschedule"
        artifactName = "\${name}-v\${version}-\${os}-\${arch}.\${ext}"

        // 移除体积庞大的 jdk.localedata (~30MB)，仅保留实际需要的最小模块集
        modules(
            "java.prefs",
            "java.logging",
            "jdk.crypto.ec",
            "jdk.httpserver",
            "jdk.unsupported"
        )

        jvmArgs(
            "-Dfile.encoding=UTF-8",
            "-Dsun.stdout.encoding=UTF-8",
            "-Dsun.stderr.encoding=UTF-8",
            "-XX:+IgnoreUnrecognizedVMOptions",
            "-XX:+UseCompactObjectHeaders",
            "-Xshare:auto",
            "-XX:+UseG1GC",
            "-XX:+UseStringDeduplication",
            "-XX:MinHeapFreeRatio=15",
            "-XX:MaxHeapFreeRatio=30",
            "-Xms32m",
            "-Xmx256m"
        )

        macOS {
            iconFile.set(project.file("icons/ic_launcher.icns"))
            dockName = "拾光课程表"
        }

        windows {
            iconFile.set(project.file("icons/ic_launcher.ico"))
            nsis {
                oneClick = false
                allowElevation = true
                perMachine = false
                allowToChangeInstallationDirectory = true
                createDesktopShortcut = true
                runAfterFinish = true
                multiLanguageInstaller = true
                installerLanguages = listOf("en_US", "zh_CN","zh_TW")
            }
        }

        linux {
            iconFile.set(project.file("icons/ic_launcher.png"))
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    systemProperty("file.encoding", "UTF-8")
    systemProperty("sun.stdout.encoding", "UTF-8")
    systemProperty("sun.stderr.encoding", "UTF-8")
}

object DesktopDistributionSlimmer {
    /**
     * 判断 JAR 中的条目是否为非当前平台或非当前 CPU 架构的冗余原生动态库
     */
    fun isForeignNativeJarEntry(
        entryName: String,
        isDirectory: Boolean,
        currentOsPrefix: String,
        currentArchSuffix: String
    ): Boolean {
        if (isDirectory) return false
        val normalized = entryName.lowercase()
        val keepNativeDirPrefix = "natives/${currentOsPrefix}_${currentArchSuffix}/"
        if (normalized.startsWith("natives/")) {
            return !normalized.startsWith(keepNativeDirPrefix)
        }
        val isNativeBinary = normalized.endsWith(".dll") ||
                normalized.endsWith(".dylib") ||
                normalized.endsWith(".jnilib") ||
                normalized.endsWith(".so")
        if (!isNativeBinary) return false

        val expectedExt = when (currentOsPrefix) {
            "windows" -> ".dll"
            "osx" -> ".dylib"
            else -> ".so"
        }
        if (!normalized.endsWith(expectedExt) && !(currentOsPrefix == "osx" && normalized.endsWith(".jnilib"))) {
            return true
        }

        val foreignOsTokens = when (currentOsPrefix) {
            "windows" -> listOf("/osx", "/macos", "/darwin", "/linux", "/android", "osx_", "macos_", "linux_", "android_")
            "osx" -> listOf("/windows", "/win32", "/linux", "/android", "windows_", "win32_", "linux_", "android_")
            else -> listOf("/windows", "/win32", "/osx", "/macos", "/darwin", "/android", "windows_", "win32_", "osx_", "macos_", "android_")
        }
        if (foreignOsTokens.any { normalized.contains(it) }) return true

        val foreignArchTokens = if (currentArchSuffix == "arm64") {
            listOf("x86_64", "amd64", "_x64", "-x64", "/x64/", "/x86/")
        } else {
            listOf("aarch64", "arm64", "armv7", "/x86/")
        }
        return foreignArchTokens.any { normalized.contains(it) }
    }

    /**
     * 对分发目录执行原生库裁剪、JRE 运行时重压缩与冗余文件清理
     */
    fun slimDistributionDirectory(distDir: File, logger: org.gradle.api.logging.Logger) {
        if (!distDir.isDirectory) return

        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        val currentOsPrefix = when {
            osName.contains("win") -> "windows"
            osName.contains("mac") -> "osx"
            else -> "linux"
        }
        val currentArchSuffix = when {
            osArch.contains("aarch64") || osArch.contains("arm64") -> "arm64"
            else -> "x64"
        }
        val keepNativeDirPrefix = "natives/${currentOsPrefix}_${currentArchSuffix}/"

        val appDir = distDir.resolve("app")
        if (appDir.isDirectory) {
            appDir.listFiles { f -> f.isFile && f.extension.equals("jar", ignoreCase = true) }?.forEach { jarFile ->
                var hasForeignNatives = false
                ZipFile(jarFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (isForeignNativeJarEntry(entry.name, entry.isDirectory, currentOsPrefix, currentArchSuffix)) {
                            hasForeignNatives = true
                            break
                        }
                    }
                }
                if (hasForeignNatives) {
                    val tempJar = File(jarFile.parentFile, "${jarFile.name}.slim.tmp")
                    ZipFile(jarFile).use { zipIn ->
                        ZipOutputStream(FileOutputStream(tempJar)).use { zipOut ->
                            zipOut.setLevel(Deflater.BEST_COMPRESSION)
                            val entries = zipIn.entries()
                            while (entries.hasMoreElements()) {
                                val entry = entries.nextElement()
                                if (isForeignNativeJarEntry(entry.name, entry.isDirectory, currentOsPrefix, currentArchSuffix)) {
                                    continue
                                }
                                val newEntry = ZipEntry(entry.name)
                                newEntry.time = entry.time
                                zipOut.putNextEntry(newEntry)
                                if (!entry.isDirectory) {
                                    zipIn.getInputStream(entry).use { it.copyTo(zipOut) }
                                }
                                zipOut.closeEntry()
                            }
                        }
                    }
                    if (jarFile.delete()) {
                        tempJar.renameTo(jarFile)
                        logger.lifecycle("已剔除非当前平台($keepNativeDirPrefix)原生库: ${jarFile.name}")
                    } else {
                        tempJar.delete()
                    }
                }
            }
        }

        val runtimeDir = distDir.resolve("runtime")
        if (runtimeDir.isDirectory) {
            // 查找本机带 jmods 的完整 JDK，用 jlink --compress=zip-6 --strip-debug 重建精简运行时
            val candidateJdks = listOfNotNull(
                System.getProperty("java.home")?.let(::File),
                System.getenv("JDK_HOME")?.let(::File),
                System.getenv("JAVA_HOME")?.let(::File),
                File("D:/appdata/jdk-27.0.1")
            )
            val jdkWithJmods = candidateJdks.firstOrNull {
                it.resolve("jmods").isDirectory &&
                        (it.resolve("bin/jlink.exe").isFile || it.resolve("bin/jlink").isFile)
            }
            if (jdkWithJmods != null) {
                val jlinkExe = if (osName.contains("win")) jdkWithJmods.resolve("bin/jlink.exe") else jdkWithJmods.resolve("bin/jlink")
                val jmodsDir = jdkWithJmods.resolve("jmods")
                val tempRuntime = File(distDir, "runtime_slim_tmp")
                if (tempRuntime.exists()) tempRuntime.deleteRecursively()

                val modulesList = "java.base,java.datatransfer,java.desktop,java.logging,java.prefs,jdk.crypto.ec,jdk.httpserver,jdk.unsupported"
                val proc = ProcessBuilder(
                    jlinkExe.absolutePath,
                    "--module-path", jmodsDir.absolutePath,
                    "--add-modules", modulesList,
                    "--strip-debug",
                    "--no-man-pages",
                    "--no-header-files",
                    "--compress=zip-6",
                    "--output", tempRuntime.absolutePath
                ).redirectErrorStream(true).start()
                val output = proc.inputStream.bufferedReader().readText()
                if (proc.waitFor() == 0 && tempRuntime.isDirectory) {
                    runtimeDir.deleteRecursively()
                    tempRuntime.renameTo(runtimeDir)
                    logger.lifecycle("已使用 jlink (zip-6 + strip-debug) 重建精简运行时: ${runtimeDir.absolutePath}")
                } else {
                    tempRuntime.deleteRecursively()
                    logger.warn("jlink 精简跳过: $output")
                }
            }

            // 清理运行时中的静态导入库、调试符号和冗余许可目录
            runtimeDir.resolve("lib/jvm.lib").takeIf { it.exists() }?.delete()
            runtimeDir.resolve("legal").takeIf { it.exists() }?.deleteRecursively()
            runtimeDir.walkTopDown().filter { it.isFile && (it.extension == "pdb" || it.extension == "diz") }.forEach { it.delete() }
        }
    }
}

val slimDesktopDistributable = tasks.register("slimDesktopDistributable") {
    group = "nucleus"
    description = "裁剪桌面端分发目录中的跨平台冗余原生库并压缩 JRE 运行时体积"
    mustRunAfter("createDistributable", "createReleaseDistributable")
    val binariesDirProvider = layout.buildDirectory.dir("compose/binaries")

    doLast {
        val log = org.gradle.api.logging.Logging.getLogger("slimDesktopDistributable")
        val binariesDir = binariesDirProvider.get().asFile
        if (!binariesDir.isDirectory) return@doLast
        binariesDir.walkTopDown()
            .maxDepth(4)
            .filter { it.isDirectory && it.resolve("app").isDirectory && it.resolve("runtime").isDirectory }
            .forEach { distRoot ->
                val beforeBytes = distRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                DesktopDistributionSlimmer.slimDistributionDirectory(distRoot, log)
                val afterBytes = distRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }
                log.lifecycle(
                    "精简完成 [${distRoot.name}]: %.2f MB -> %.2f MB (减少 %.2f MB)".format(
                        beforeBytes / 1048576.0,
                        afterBytes / 1048576.0,
                        (beforeBytes - afterBytes) / 1048576.0
                    )
                )
            }
    }
}

tasks.matching { it.name == "createDistributable" || it.name == "createReleaseDistributable" }.configureEach {
    finalizedBy(slimDesktopDistributable)
}

tasks.register("packageCompactDesktop") {
    group = "nucleus"
    description = "构建并生成极致精简的桌面端便携压缩包 (.zip)"
    dependsOn("createDistributable", slimDesktopDistributable)
    val binariesDirProvider = layout.buildDirectory.dir("compose/binaries")
    val compactDirProvider = layout.buildDirectory.dir("compact")
    val targetVersion = appVersion

    doLast {
        val log = org.gradle.api.logging.Logging.getLogger("packageCompactDesktop")
        val binariesDir = binariesDirProvider.get().asFile
        val distRoot = binariesDir.walkTopDown()
            .maxDepth(4)
            .firstOrNull { it.isDirectory && it.resolve("app").isDirectory && it.resolve("runtime").isDirectory }
            ?: error("未找到已生成的桌面端分发目录，请确认 createDistributable 是否成功。")

        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        val osTag = when {
            osName.contains("win") -> "windows"
            osName.contains("mac") -> "macos"
            else -> "linux"
        }
        val archTag = if (osArch.contains("aarch64") || osArch.contains("arm64")) "arm64" else "x64"

        val compactDir = compactDirProvider.get().asFile.apply { mkdirs() }
        val zipFile = compactDir.resolve("shiguangschedule-v${targetVersion}-${osTag}-${archTag}-compact.zip")
        if (zipFile.exists()) zipFile.delete()

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            zipOut.setLevel(Deflater.BEST_COMPRESSION)
            distRoot.walkTopDown().filter { it.isFile }.forEach { file ->
                val relPath = "ShiguangSchedule/" + file.relativeTo(distRoot).invariantSeparatorsPath
                val entry = ZipEntry(relPath)
                entry.time = file.lastModified()
                zipOut.putNextEntry(entry)
                file.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }

        val unpackedMb = distRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1048576.0
        val zipMb = zipFile.length() / 1048576.0
        log.lifecycle("==========================================================")
        log.lifecycle("精简桌面端打包完成:")
        log.lifecycle("  解压后总目录体积: %.2f MB (原版基准: 142.42 MB)".format(unpackedMb))
        log.lifecycle("  便携安装包体积  : %.2f MB -> ${zipFile.absolutePath}".format(zipMb))
        log.lifecycle("==========================================================")
    }
}

tasks.register("renameDesktopPackage") {
    group = "nucleus"
    description = "CI 兼容打包与产物重命名任务"
    val osName = System.getProperty("os.name").lowercase()
    when {
        osName.contains("win") -> dependsOn("packageReleaseNsis", "packageReleasePortable")
        osName.contains("mac") -> dependsOn("packageReleaseDmg", "packageReleasePkg")
        osName.contains("linux") -> dependsOn("packageReleaseDeb", "packageReleaseRpm", "packageReleaseAppImage")
    }
}