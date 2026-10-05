import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    jvmToolchain(21)
}

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
            targetFormats(TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.xingheyuzhuan.shiguangschedule"
            packageVersion = "2.1.0"
        }
    }
}