import dev.nucleusframework.desktop.application.dsl.NativeImageOptimization
import dev.nucleusframework.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.nucleus)
}

kotlin {
    jvmToolchain(21)
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
    implementation(libs.compose.ui.tooling.preview)
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
        isEnabled.set(false)
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

        modules(
            "jdk.unsupported",
            "jdk.localedata"
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