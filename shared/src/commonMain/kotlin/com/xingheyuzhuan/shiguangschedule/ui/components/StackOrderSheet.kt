package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.action_done
import shiguangschedule.shared.generated.resources.desc_stack_order_sheet
import shiguangschedule.shared.generated.resources.filter_list_24px
import shiguangschedule.shared.generated.resources.hint_stack_order_drag
import shiguangschedule.shared.generated.resources.text_no_course
import shiguangschedule.shared.generated.resources.title_stack_order
import kotlin.math.roundToInt

/**
 * “堆叠显示顺序”列表使用的展示模型。
 */
data class StackOrderEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val color: Color
)

/**
 * 单个冲突格的堆叠顺序编辑内容。
 *
 * @param entries 当前冲突格内的课程
 * @param orderIds 该冲突格已保存的顺序；未出现的条目按原始顺序追加
 * @param onOrderChange 拖动结束后回写新的格内顺序
 */
@Composable
fun StackOrderSheetContent(
    entries: List<StackOrderEntry>,
    orderIds: List<String>,
    onOrderChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var orderedEntries by remember(entries, orderIds) {
        mutableStateOf(sortEntries(entries, orderIds))
    }

    var draggingId by remember { mutableStateOf<String?>(null) }
    var dragDelta by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(Res.string.title_stack_order),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(Res.string.desc_stack_order_sheet),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (orderedEntries.isEmpty()) {
            Text(
                text = stringResource(Res.string.text_no_course),
                modifier = Modifier.padding(vertical = 32.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            orderedEntries.forEach { entry ->
                val isDragging = draggingId == entry.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .graphicsLayer { translationY = if (isDragging) dragDelta else 0f }
                        .onSizeChanged { rowHeightPx = it.height.toFloat() }
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isDragging) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color.Transparent
                            }
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(entry.color, CircleShape)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entry.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .pointerInput(entry.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingId = entry.id
                                        dragDelta = 0f
                                    },
                                    onDragCancel = {
                                        draggingId = null
                                        dragDelta = 0f
                                    },
                                    onDragEnd = {
                                        val from = orderedEntries.indexOfFirst { it.id == entry.id }
                                        if (from >= 0 && rowHeightPx > 0f) {
                                            val to = (from + (dragDelta / rowHeightPx).roundToInt())
                                                .coerceIn(0, orderedEntries.lastIndex)
                                            if (to != from) {
                                                val reordered = orderedEntries.toMutableList()
                                                val moved = reordered.removeAt(from)
                                                reordered.add(to, moved)
                                                orderedEntries = reordered
                                                onOrderChange(reordered.map { it.id })
                                            }
                                        }
                                        draggingId = null
                                        dragDelta = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragDelta += dragAmount.y
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.filter_list_24px),
                            contentDescription = stringResource(Res.string.hint_stack_order_drag),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp)
        ) {
            Text(stringResource(Res.string.action_done))
        }
    }
}

private fun sortEntries(
    entries: List<StackOrderEntry>,
    orderIds: List<String>
): List<StackOrderEntry> {
    val indexById = orderIds.withIndex().associate { (index, id) -> id to index }
    return entries.sortedWith(
        compareBy<StackOrderEntry> { indexById[it.id] ?: Int.MAX_VALUE }
    )
}