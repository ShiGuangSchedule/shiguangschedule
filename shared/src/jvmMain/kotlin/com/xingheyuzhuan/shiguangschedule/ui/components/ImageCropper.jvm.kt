package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * 计算边界安全的裁剪框参数 [safeLeft, safeTop, safeWidth, safeHeight]
 */
internal fun computeSafeCropBounds(
    sourceWidth: Int,
    sourceHeight: Int,
    srcLeft: Int,
    srcTop: Int,
    cropWidth: Int,
    cropHeight: Int
): IntArray {
    val safeLeft = srcLeft.coerceIn(0, (sourceWidth - 1).coerceAtLeast(0))
    val safeTop = srcTop.coerceIn(0, (sourceHeight - 1).coerceAtLeast(0))
    val safeWidth = cropWidth.coerceAtLeast(1).coerceAtMost((sourceWidth - safeLeft).coerceAtLeast(1))
    val safeHeight = cropHeight.coerceAtLeast(1).coerceAtMost((sourceHeight - safeTop).coerceAtLeast(1))
    return intArrayOf(safeLeft, safeTop, safeWidth, safeHeight)
}

/**
 * 纯 JVM BufferedImage 区域裁剪与 JPEG 编码（供无 Skiko Native 环境或测试环境直接调用）
 */
internal fun cropBufferedImageToJpeg(
    source: BufferedImage,
    srcLeft: Int,
    srcTop: Int,
    cropWidth: Int,
    cropHeight: Int
): ByteArray {
    val (safeLeft, safeTop, safeWidth, safeHeight) = computeSafeCropBounds(
        source.width,
        source.height,
        srcLeft,
        srcTop,
        cropWidth,
        cropHeight
    )
    val subImage = source.getSubimage(safeLeft, safeTop, safeWidth, safeHeight)
    val rgbImage = BufferedImage(safeWidth, safeHeight, BufferedImage.TYPE_INT_RGB)
    val g = rgbImage.createGraphics()
    try {
        g.drawImage(subImage, 0, 0, null)
    } finally {
        g.dispose()
    }
    return ByteArrayOutputStream().use { out ->
        ImageIO.write(rgbImage, "jpg", out)
        out.toByteArray()
    }
}

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
    val (safeLeft, safeTop, safeWidth, safeHeight) = computeSafeCropBounds(
        skiaBitmap.width,
        skiaBitmap.height,
        srcLeft,
        srcTop,
        cropWidth,
        cropHeight
    )

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