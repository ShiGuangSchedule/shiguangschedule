import com.android.build.api.variant.FilterConfiguration

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.xingheyuzhuan.shiguangschedule"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.xingheyuzhuan.shiguangschedule"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 35
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    splits {
        abi {
            isEnable = true
            exclude("mips", "mips64", "armeabi", "riscv64", "x86")
            isUniversalApk = false
            include("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("zh", "zh-rCN", "zh-rTW", "en")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.remoteviews)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.androidx.datastore.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.annotations)
    debugImplementation(libs.compose.ui.tooling)
}

androidComponents {
    onVariants { variant ->
        val buildType = variant.buildType ?: ""
        val versionName = android.defaultConfig.versionName ?: ""

        variant.outputs.forEach { output ->
            val abiFilter = output.filters.find {
                it.filterType == FilterConfiguration.FilterType.ABI
            }?.identifier ?: "universal"

            // 动态设置输出的 APK 文件名
            output.outputFileName.set("shiguangschedule-v${versionName}-${abiFilter}-${buildType}.apk")
        }
    }
}