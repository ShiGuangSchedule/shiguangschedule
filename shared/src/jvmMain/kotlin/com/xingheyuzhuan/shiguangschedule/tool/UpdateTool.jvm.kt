package com.xingheyuzhuan.shiguangschedule.tool

import java.awt.Desktop
import java.net.URI

actual object PlatformUpdateStrategy {
    actual val isUpdateSupported: Boolean = true

    /**
     * JVM / 桌面端根据当前操作系统与 CPU 架构自动从 Release Assets 中匹配最合适的安装包直链。
     */
    actual fun parseTargetUrl(response: ApiReleaseResponse): String? {
        return selectDesktopAssetUrl(
            assets = response.assets,
            osName = System.getProperty("os.name").orEmpty(),
            osArch = System.getProperty("os.arch").orEmpty()
        )
    }

    internal fun selectDesktopAssetUrl(
        assets: List<ApiAsset>,
        osName: String,
        osArch: String
    ): String? {
        if (assets.isEmpty()) return null

        val normalizedOs = osName.lowercase()
        val normalizedArch = osArch.lowercase()
        val isArm64 = normalizedArch.contains("aarch64") || normalizedArch.contains("arm64")

        val preferredExtensions: List<String>
        val osKeywords: List<String>
        val foreignOsKeywords: List<String>

        when {
            normalizedOs.contains("win") -> {
                preferredExtensions = listOf(".exe", ".msi", ".zip")
                osKeywords = listOf("windows", "win")
                foreignOsKeywords = listOf("macos", "mac", "osx", "darwin", "linux", "ubuntu", "debian", "android", "ios")
            }
            normalizedOs.contains("mac") || normalizedOs.contains("darwin") -> {
                preferredExtensions = listOf(".dmg", ".pkg")
                osKeywords = listOf("macos", "mac", "osx", "darwin")
                foreignOsKeywords = listOf("windows", "win", "linux", "ubuntu", "debian", "android", "ios")
            }
            normalizedOs.contains("linux") -> {
                preferredExtensions = listOf(".deb", ".appimage", ".rpm")
                osKeywords = listOf("linux", "ubuntu", "debian")
                foreignOsKeywords = listOf("windows", "win", "macos", "mac", "osx", "darwin", "android", "ios")
            }
            else -> return null
        }

        val archKeywords = if (isArm64) {
            listOf("arm64", "aarch64")
        } else {
            listOf("x64", "x86_64", "amd64")
        }
        val oppositeArchKeywords = if (isArm64) {
            listOf("x64", "x86_64", "amd64")
        } else {
            listOf("arm64", "aarch64")
        }

        val osCandidates = assets.filter { asset ->
            val lower = asset.name.lowercase()
            !lower.endsWith(".apk") &&
                    preferredExtensions.any { lower.endsWith(it) } &&
                    foreignOsKeywords.none { lower.contains(it) }
        }
        if (osCandidates.isEmpty()) return null

        // 优先匹配：扩展名优先级 + OS 关键字匹配 + 架构精准匹配
        for (ext in preferredExtensions) {
            val exactOsAndArchMatch = osCandidates.firstOrNull { asset ->
                val lower = asset.name.lowercase()
                lower.endsWith(ext) &&
                        osKeywords.any { lower.contains(it) } &&
                        archKeywords.any { lower.contains(it) }
            }
            if (exactOsAndArchMatch != null) return exactOsAndArchMatch.downloadUrl
        }

        // 次优匹配 1：扩展名优先级 + 架构精准匹配
        for (ext in preferredExtensions) {
            val exactArchMatch = osCandidates.firstOrNull { asset ->
                val lower = asset.name.lowercase()
                lower.endsWith(ext) && archKeywords.any { lower.contains(it) }
            }
            if (exactArchMatch != null) return exactArchMatch.downloadUrl
        }

        // 次优匹配 2：扩展名匹配 + OS 关键字匹配且不包含相反架构关键字
        for (ext in preferredExtensions) {
            val compatibleMatch = osCandidates.firstOrNull { asset ->
                val lower = asset.name.lowercase()
                lower.endsWith(ext) &&
                        oppositeArchKeywords.none { lower.contains(it) } &&
                        osKeywords.any { lower.contains(it) }
            }
            if (compatibleMatch != null) return compatibleMatch.downloadUrl
        }

        return osCandidates.firstOrNull { asset ->
            val lower = asset.name.lowercase()
            oppositeArchKeywords.none { lower.contains(it) }
        }?.downloadUrl ?: osCandidates.first().downloadUrl
    }

    /**
     * 调用系统默认浏览器打开网页
     */
    actual fun openUrl(url: String) {
        try {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(URI(url))
                    return
                }
            }
        } catch (_: Exception) {
        }

        try {
            val os = System.getProperty("os.name").orEmpty().lowercase()
            val command = when {
                os.contains("win") -> listOf("cmd", "/c", "start", "", url)
                os.contains("mac") -> listOf("open", url)
                else -> listOf("xdg-open", url)
            }
            ProcessBuilder(command).start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}