package com.xingheyuzhuan.shiguangschedule.ui.components

import java.awt.Desktop
import java.io.File

actual fun platformShareFile(filePath: String, mimeType: String) {
    val file = File(filePath)
    val targetFile = if (file.exists()) file else file.parentFile ?: return

    try {
        if (Desktop.isDesktopSupported()) {
            val desktop = Desktop.getDesktop()
            if (file.isFile && desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                desktop.browseFileDirectory(file)
                return
            }
            val dirToOpen = if (targetFile.isDirectory) targetFile else targetFile.parentFile
            if (dirToOpen != null && desktop.isSupported(Desktop.Action.OPEN)) {
                desktop.open(dirToOpen)
                return
            }
        }
    } catch (_: Exception) {
    }

    try {
        val osName = System.getProperty("os.name").orEmpty().lowercase()
        val command = when {
            osName.contains("win") && file.isFile -> listOf("explorer.exe", "/select,${file.absolutePath}")
            osName.contains("win") -> listOf("explorer.exe", targetFile.absolutePath)
            osName.contains("mac") && file.isFile -> listOf("open", "-R", file.absolutePath)
            osName.contains("mac") -> listOf("open", targetFile.absolutePath)
            else -> listOf("xdg-open", (if (targetFile.isDirectory) targetFile else targetFile.parentFile ?: targetFile).absolutePath)
        }
        ProcessBuilder(command).start()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}