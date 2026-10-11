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

val appBaseName = "shiguangschedule"

dependencies {

    implementation(project(":shared"))
    implementation(libs.compose.components.resources)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
    implementation(libs.koin.annotations)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.nucleus.core.runtime)
    implementation(libs.nucleus.application)
    implementation(libs.nucleus.decorated.window.core)
    implementation(libs.nucleus.decorated.window.tao)
}

nucleus.application {
    mainClass = "com.xingheyuzhuan.shiguangschedule.MainKt"

    buildTypes.release.proguard {
        isEnabled.set(false)
    }

    nativeDistributions {
        targetFormats(
            TargetFormat.Portable,
            TargetFormat.Exe, TargetFormat.Msi,
            TargetFormat.Dmg, TargetFormat.Pkg,
            TargetFormat.Deb, TargetFormat.Rpm
        )
        packageName = "ShiguangSchedule"
        packageVersion = appVersion

        homepage = "https://github.com/xingheyuzhuan/shiguangschedule"
        artifactName = "\${name}-v\${version}-\${os}-\${arch}.\${ext}"

        modules(
            "jdk.unsupported",
            "jdk.localedata"
        )
    }
}