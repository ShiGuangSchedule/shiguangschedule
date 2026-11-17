package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Image


/**
 * iOS 平台实现：利用底层 Skia 引擎进行物理裁剪并导出为 JPEG ByteArray 字节流
 */
actual suspend fun cropImageBitmapNative(
    source: ImageBitmap,
    srcLeft: Int,
    srcTop: Int,
    cropWidth: Int,
    cropHeight: Int
): ByteArray = withContext(Dispatchers.IO) {
    val skiaBitmap = source.asSkiaBitmap()

    // 边界安全坐标计算，防止极值出界引发 CoreGraphics/Skia 崩溃
    val safeLeft = srcLeft.coerceIn(0, skiaBitmap.width - 1)
    val safeTop = srcTop.coerceIn(0, skiaBitmap.height - 1)
    val safeWidth = cropWidth.coerceAtLeast(1).coerceAtMost(skiaBitmap.width - safeLeft)
    val safeHeight = cropHeight.coerceAtLeast(1).coerceAtMost(skiaBitmap.height - safeTop)

    // 1. 提取像素子集 (Subset) 完成物理裁切
    val destBitmap = Bitmap()
    val cropRect = IRect.makeXYWH(safeLeft, safeTop, safeWidth, safeHeight)
    skiaBitmap.extractSubset(destBitmap, cropRect)

    // 2. 将裁切后的 Skia Bitmap 编码导出为 JPEG 字节数组
    val image = Image.makeFromBitmap(destBitmap)
    val data = image.encodeToData(EncodedImageFormat.JPEG, 90)

    data?.bytes ?: throw IllegalStateException("iOS image cropping/encoding failed")
}