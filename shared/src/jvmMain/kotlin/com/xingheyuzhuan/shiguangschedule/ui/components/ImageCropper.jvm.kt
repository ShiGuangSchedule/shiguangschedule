package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * JVM / Desktop 平台实现：不进行 UI 裁切，直接将原图编码导出为 JPEG ByteArray 字节流
 */
actual suspend fun cropImageBitmapNative(
    source: ImageBitmap,
    srcLeft: Int,
    srcTop: Int,
    cropWidth: Int,
    cropHeight: Int
): ByteArray = withContext(Dispatchers.IO) {
    val skiaBitmap = source.asSkiaBitmap()

    // 直接用原图创建 Image，不做任何坐标提取
    val image = Image.makeFromBitmap(skiaBitmap)
    val data = image.encodeToData(EncodedImageFormat.JPEG, 90)

    data?.bytes ?: throw IllegalStateException("Desktop image encoding failed")
}