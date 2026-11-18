pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "shiguangschedule"

val localPropsFile = file("local.properties")
val sdkDirFromProps = if (localPropsFile.exists()) {
    java.util.Properties().apply { localPropsFile.inputStream().use { load(it) } }.getProperty("sdk.dir")
} else null
val androidSdkDir = sdkDirFromProps ?: System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
val hasAndroidSdk = !androidSdkDir.isNullOrBlank() && file(androidSdkDir).exists()

if (hasAndroidSdk) {
    include(":androidApp")
}
include(":shared")
include(":desktopApp")