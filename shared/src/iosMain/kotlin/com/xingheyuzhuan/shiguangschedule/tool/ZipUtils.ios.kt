package com.xingheyuzhuan.shiguangschedule.tool

import no.synth.kmpzip.io.ByteArrayOutputStream
import no.synth.kmpzip.zip.ZipEntry
import no.synth.kmpzip.zip.ZipOutputStream

/**
 * iOS 端的 Zip 工具类实现，基于 kmp-zip 在内存中构建 ZIP 归档。
 */
actual object ZipUtils {
    actual fun createZip(entries: Map<String, ByteArray>): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { zip ->
            entries.forEach { (filename, bytes) ->
                zip.putNextEntry(ZipEntry(filename))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return buffer.toByteArray()
    }
}
