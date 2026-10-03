package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm_crop
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 各平台是否启用裁剪预览窗口开关
 */
expect val isCropWindowEnabled: Boolean

/**
 * 跨平台通用图片裁剪组件
 */
@Composable
fun ImageCropper(
    imageBitmap: ImageBitmap?,
    aspectRatio: Float,
    onCropConfirmed: (ByteArray) -> Unit,
    onDismiss: () -> Unit
) {
    if (imageBitmap == null) return

    val coroutineScope = rememberCoroutineScope()

    // 若当前平台关闭了预览窗口（如 Desktop），后台异步静默转为 ByteArray 并直接回调
    LaunchedEffect(imageBitmap) {
        if (!isCropWindowEnabled) {
            val bytes = cropImageBitmapNative(
                source = imageBitmap,
                srcLeft = 0,
                srcTop = 0,
                cropWidth = imageBitmap.width,
                cropHeight = imageBitmap.height
            )
            onCropConfirmed(bytes)
            return@LaunchedEffect
        }
    }

    if (!isCropWindowEnabled) return

    var containerSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    var isCropping by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .zIndex(10f),
        contentAlignment = Alignment.Center
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { containerSize = it.size.toSize() }
        ) {
            val cw = constraints.maxWidth.toFloat()
            val ch = constraints.maxHeight.toFloat()

            val buttonAreaHeight = with(density) { 120.dp.toPx() }
            val topPadding = with(density) { 60.dp.toPx() }
            val availableHeight = ch - buttonAreaHeight - topPadding

            val cropWidthCandidate = cw * 0.75f
            val cropHeightCandidate = cropWidthCandidate / aspectRatio

            val (cropWidth, cropHeight) = if (cropHeightCandidate > availableHeight) {
                val h = availableHeight * 0.85f
                Pair(h * aspectRatio, h)
            } else {
                Pair(cropWidthCandidate, cropHeightCandidate)
            }

            val cropRect = Rect(
                left = (cw - cropWidth) / 2,
                top = topPadding + (availableHeight - cropHeight) / 2,
                right = (cw + cropWidth) / 2,
                bottom = topPadding + (availableHeight + cropHeight) / 2
            )

            LaunchedEffect(imageBitmap, containerSize) {
                if (containerSize != Size.Zero) {
                    val imgW = imageBitmap.width.toFloat()
                    val imgH = imageBitmap.height.toFloat()
                    scale = max(cropWidth / imgW, cropHeight / imgH)
                    val centerX = cropRect.left + cropWidth / 2
                    val centerY = cropRect.top + cropHeight / 2
                    offset = Offset(centerX - cw / 2, centerY - ch / 2)
                }
            }

            fun constrainOffset(newOffset: Offset, newScale: Float): Offset {
                val imgW = imageBitmap.width * newScale
                val imgH = imageBitmap.height * newScale
                val minX = cropRect.right - (cw + imgW) / 2
                val maxX = cropRect.left - (cw - imgW) / 2
                val minY = cropRect.bottom - (ch + imgH) / 2
                val maxY = cropRect.top - (ch - imgH) / 2
                return Offset(
                    newOffset.x.coerceIn(min(minX, maxX), max(minX, maxX)),
                    newOffset.y.coerceIn(min(minY, maxY), max(minY, maxY))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (!isCropping) {
                                val minScale = max(cropWidth / imageBitmap.width, cropHeight / imageBitmap.height)
                                val newScale = (scale * zoom).coerceAtLeast(minScale)
                                scale = newScale
                                offset = constrainOffset(offset + pan, newScale)
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val imgW = imageBitmap.width * scale
                    val imgH = imageBitmap.height * scale
                    val startX = (cw - imgW) / 2 + offset.x
                    val startY = (ch - imgH) / 2 + offset.y

                    drawImage(
                        image = imageBitmap,
                        dstOffset = IntOffset(startX.roundToInt(), startY.roundToInt()),
                        dstSize = IntSize(imgW.roundToInt(), imgH.roundToInt())
                    )
                }

                val scrimColor = MaterialTheme.colorScheme.scrim

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply { addRect(cropRect) }
                    clipPath(path, clipOp = ClipOp.Difference) {
                        drawRect(scrimColor.copy(alpha = 0.75f))
                    }
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(cropRect.left.roundToInt(), cropRect.top.roundToInt()) }
                        .size(with(density) { cropWidth.toDp() }, with(density) { cropHeight.toDp() })
                        .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 取消按钮
                Button(
                    enabled = !isCropping,
                    onClick = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }

                // 确认按钮
                Button(
                    enabled = !isCropping,
                    onClick = {
                        coroutineScope.launch {
                            isCropping = true

                            val imgW = imageBitmap.width * scale
                            val imgH = imageBitmap.height * scale
                            val imgStartX = (cw - imgW) / 2 + offset.x
                            val imgStartY = (ch - imgH) / 2 + offset.y

                            val srcLeft = max(0, ((cropRect.left - imgStartX) / scale).roundToInt())
                            val srcTop = max(0, ((cropRect.top - imgStartY) / scale).roundToInt())
                            val srcW = (cropWidth / scale).roundToInt()
                            val srcH = (cropHeight / scale).roundToInt()

                            val croppedBytes = cropImageBitmapNative(
                                source = imageBitmap,
                                srcLeft = srcLeft,
                                srcTop = srcTop,
                                cropWidth = srcW,
                                cropHeight = srcH
                            )

                            isCropping = false
                            onCropConfirmed(croppedBytes)
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (isCropping) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(Res.string.action_confirm_crop))
                    }
                }
            }
        }
    }
}

/**
 * Native 裁剪接口直接返回包含图片数据（JPEG/PNG）的 ByteArray
 */
expect suspend fun cropImageBitmapNative(
    source: ImageBitmap,
    srcLeft: Int,
    srcTop: Int,
    cropWidth: Int,
    cropHeight: Int
): ByteArray