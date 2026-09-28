package com.xingheyuzhuan.shiguangschedule.tool

import com.xingheyuzhuan.shiguangschedule.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 接收到的外部文件封装数据模型
 */
data class ExternalFile(
    val fileName: String,
    val bytes: ByteArray,
    val targetDestination: Destination?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ExternalFile) return false
        return fileName == other.fileName &&
                bytes.contentEquals(other.bytes) &&
                targetDestination == other.targetDestination
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + (targetDestination?.hashCode() ?: 0)
        return result
    }
}

/**
 * 外部文件接收与路由调度管理器
 */
object ExternalFileManager {
    private val _pendingFile = MutableStateFlow<ExternalFile?>(null)
    val pendingFile = _pendingFile.asStateFlow()

    /**
     * 任意原生平台（Android / iOS / Desktop）捕获到外部文件后调用此方法投递
     */
    fun onFileReceived(fileName: String, bytes: ByteArray) {
        val destination = resolveDestination(fileName)
        _pendingFile.value = ExternalFile(fileName, bytes, destination)
    }

    /**
     * 消费并清除暂存的文件数据，防止重复触发
     */
    fun consumeFile(): ExternalFile? {
        val file = _pendingFile.value
        _pendingFile.value = null
        return file
    }

    /**
     * 根据文件名/后缀匹配目标 Destination（目前仅处理 .json）
     */
    private fun resolveDestination(fileName: String): Destination? {
        return if (fileName.endsWith(".json", ignoreCase = true)) {
            Destination.CourseTableConversion
        } else {
            null
        }
    }
}