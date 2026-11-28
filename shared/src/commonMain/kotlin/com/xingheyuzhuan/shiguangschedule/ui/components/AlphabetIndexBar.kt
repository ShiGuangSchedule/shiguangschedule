package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 索引条宽度，供调用方预留右侧留白。 */
val AlphabetIndexBarWidth = 24.dp

/**
 * 右侧字母索引条。
 *
 * 只负责绘制字母并处理点击 / 拖动，滚动由调用方通过 [onInitialSelected] 完成。
 *
 * @param initials 按顺序展示的首字母集合
 * @param onInitialSelected 选中某个首字母时回调
 */
@Composable
fun AlphabetIndexBar(
    initials: List<String>,
    modifier: Modifier = Modifier,
    onInitialSelected: (String) -> Unit
) {
    if (initials.isEmpty()) return

    var barHeight by remember { mutableIntStateOf(0) }

    val handleSelection: (Float) -> Unit = { yPosition ->
        if (barHeight > 0) {
            // 手指纵坐标换算成字母下标
            val index = (yPosition / barHeight * initials.size)
                .toInt()
                .coerceIn(0, initials.lastIndex)
            onInitialSelected(initials[index])
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(AlphabetIndexBarWidth)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .onGloballyPositioned { coordinates ->
                    barHeight = coordinates.size.height
                }
                .pointerInput(initials) {
                    detectTapGestures { offset -> handleSelection(offset.y) }
                }
                .pointerInput(initials) {
                    detectDragGestures { change, _ ->
                        handleSelection(change.position.y)
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            initials.forEach { initial ->
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
