# ==============================================================================
# 拾光课程表 (ShiGuangSchedule) Desktop Release ProGuard 规则
# 目标：在保证 Compose / Room3 / Koin / Kotlinx Serialization / Wire / Nucleus
#       反射与 JNI 正常工作的同时，最大化剔除未使用的第三方库字节码以减小安装包体积。
# ==============================================================================

-dontobfuscate
-dontoptimize
-ignorewarnings
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# 入口主类
-keep class com.xingheyuzhuan.shiguangschedule.MainKt { *; }
-keep class com.xingheyuzhuan.shiguangschedule.** { *; }
-keep class shiguangschedule.** { *; }
-keep class school_index.** { *; }
-keep class schedule_style.** { *; }
-keep class widget_snapshot.** { *; }

# Kotlin 元数据与协程
-keep class kotlin.Metadata { *; }
-keep class kotlinx.coroutines.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*,InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.xingheyuzhuan.shiguangschedule.**$$serializer { *; }
-keepclassmembers class com.xingheyuzhuan.shiguangschedule.** {
    *** Companion;
}
-keepclasseswithmembers class com.xingheyuzhuan.shiguangschedule.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room 3 & SQLite Bundled JNI
-keep class * extends androidx.room3.RoomDatabase { *; }
-keep @androidx.room3.Entity class * { *; }
-keep @androidx.room3.Dao class * { *; }
-keep class androidx.sqlite.** { *; }

# DataStore & Protobuf / Wire
-keep class androidx.datastore.** { *; }
-keep class * extends com.squareup.wire.Message { *; }
-keep class * extends com.squareup.wire.WireEnum { *; }
-keep class com.squareup.wire.** { *; }

# Koin 依赖注入
-keep class org.koin.** { *; }

# Compose Multiplatform & Skiko / Skia JNI
-keep class org.jetbrains.skia.** { *; }
-keep class org.jetbrains.skiko.** { *; }
-keep class androidx.compose.** { *; }

# Nucleus Desktop Framework & Native Tray / Notification / Tao Window
-keep class dev.nucleusframework.** { *; }
-keep class com.sun.jna.** { *; }

# FileKit
-keep class io.github.vinceglb.filekit.** { *; }

# Java Logging & Crypto
-keep class java.util.logging.** { *; }
