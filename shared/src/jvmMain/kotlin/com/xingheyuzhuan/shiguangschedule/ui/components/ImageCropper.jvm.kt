package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Image

/**
 * JVM / Desktop 平台实现：使用 Skia Bitmap.extractSubset 按裁剪坐标进行精确物理区域提取并编码为 JPEG ByteArray
 */
actual suspend fun cropImageBitmapNative(
    source: ImageBitmap,
    srcLeft: Int,
    srcTop: Int,
    cropWidth: Int,
    cropHeight: Int
): ByteArray = withContext(Dispatchers.IO) {
    val skiaBitmap = source.asSkiaBitmap()

    val safeLeft = srcLeft.coerceIn(0, (skiaBitmap.width - 1).coerceAtLeast(0))
    val safeTop = srcTop.coerceIn(0, (skiaBitmap.height - 1).coerceAtLeast(0))
    val safeWidth = cropWidth.coerceAtLeast(1).coerceAtMost((skiaBitmap.width - safeLeft).coerceAtLeast(1))
    val safeHeight = cropHeight.coerceAtLeast(1).coerceAtMost((skiaBitmap.height - safeTop).coerceAtLeast(1))

    val subsetBitmap = Bitmap()
    val extracted = skiaBitmap.extractSubset(
        subsetBitmap,
        IRect.makeXYWH(safeLeft, safeTop, safeWidth, safeHeight)
    )
    val targetBitmap = if (extracted && !subsetBitmap.isEmpty) subsetBitmap else skiaBitmap

    val image = Image.makeFromBitmap(targetBitmap)
    val data = image.encodeToData(EncodedImageFormat.JPEG, 90)

    data?.bytes ?: throw IllegalStateException("Desktop image encoding failed")
}