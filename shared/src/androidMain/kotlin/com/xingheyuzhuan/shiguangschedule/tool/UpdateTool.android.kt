package com.xingheyuzhuan.shiguangschedule.tool

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual object PlatformUpdateStrategy : KoinComponent {
    actual val isUpdateSupported: Boolean = true

    private val SUPPORTED_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86", "universal")

    private fun getDeviceAbi(): String {
        val deviceAbis = Build.SUPPORTED_ABIS
        val knownSplits = setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        return deviceAbis.firstOrNull { it in knownSplits } ?: "universal"
    }

    actual fun parseTargetUrl(response: ApiReleaseResponse): String? {

        val abiUrlMap = mutableMapOf<String, String>()

        // 1. 精准匹配
        for ((name, downloadUrl) in response.assets) {
            val fileName = name.lowercase()
            if (!fileName.endsWith(".apk")) continue

            for (abi in SUPPORTED_ABIS) {
                if (fileName.contains("-$abi-") || fileName.endsWith("-$abi.apk")) {
                    abiUrlMap[abi] = downloadUrl
                    break
                }
            }
        }

        // 2. 宽松匹配
        if (abiUrlMap.isEmpty()) {
            for ((name, downloadUrl) in response.assets) {
                val fileName = name.lowercase()
                if (!fileName.endsWith(".apk")) continue

                for (abi in SUPPORTED_ABIS) {
                    if (fileName.contains(abi)) {
                        abiUrlMap[abi] = downloadUrl
                        break
                    }
                }
            }
        }

        val deviceAbi = getDeviceAbi()
        return abiUrlMap[deviceAbi]
            ?: abiUrlMap["universal"]
            ?: abiUrlMap.values.firstOrNull()
    }

    actual fun openUrl(url: String) {
        try {
            val context: Context = get()
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}