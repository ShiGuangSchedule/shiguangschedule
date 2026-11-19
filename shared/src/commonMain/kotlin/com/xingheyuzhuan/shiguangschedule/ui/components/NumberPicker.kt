package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.launch
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_state_not_selected
import shiguangschedule.shared.generated.resources.a11y_state_selected
import kotlin.math.abs

@Composable
fun <T> NativeNumberPicker(
    values: List<T>,
    selectedValue: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    visibleItemsCount: Int = 3,
    itemTextOffsetY: Dp = 0.dp,
    dividerColor: Color = MaterialTheme.colorScheme.primary,
    dividerSize: Dp = 1.dp,
) {
    // 校验可见项数量
    require(visibleItemsCount >= 3 && visibleItemsCount % 2 != 0) {
        "可见项数量必须是大于等于 3 的奇数"
    }

    val initialSelectedIndex = remember(values, selectedValue) {
        values.indexOf(selectedValue).coerceAtLeast(0)
    }

    val listState = rememberLazyListState(initialSelectedIndex)
    val coroutineScope = rememberCoroutineScope()
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    // 计算当前精确的滚动位置（用于线性插值）
    val currentScrollPosition by remember {
        derivedStateOf {
            if (itemHeightPx > 0) {
                listState.firstVisibleItemIndex + (listState.firstVisibleItemScrollOffset / itemHeightPx)
            } else initialSelectedIndex.toFloat()
        }
    }

    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) initialSelectedIndex
            else {
                // 计算视口中心线
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                // 寻找距离视口中心最近的 item
                visibleItems.minByOrNull { item ->
                    abs((item.offset + item.size / 2) - viewportCenter)
                }?.index ?: initialSelectedIndex
            }
        }
    }

    val stateSelected = stringResource(Res.string.a11y_state_selected)
    val stateNotSelected = stringResource(Res.string.a11y_state_not_selected)

    // 使用 SnapFlingBehavior 实现平滑吸附
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 逻辑部分：处理外部变更的平滑滚动
    LaunchedEffect(initialSelectedIndex) {
        // 只有在当前位置不一致且没有正在滚动时，才执行动画同步
        if (listState.firstVisibleItemIndex != initialSelectedIndex || listState.firstVisibleItemScrollOffset != 0) {
            if (!listState.isScrollInProgress) {
                listState.animateScrollToItem(initialSelectedIndex)
            }
        }
    }
    LaunchedEffect(centerIndex) {
        if (centerIndex in values.indices && values[centerIndex] != selectedValue) {
            onValueChange(values[centerIndex])
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItemsCount)
            .clipToBounds()
            .pointerInput(values.size) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Scroll && values.isNotEmpty() && !listState.isScrollInProgress) {
                            val deltaY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            if (abs(deltaY) > 0.05f) {
                                val step = if (deltaY > 0f) 1 else -1
                                val targetIndex = (centerIndex + step).coerceIn(0, values.lastIndex)
                                if (targetIndex != centerIndex) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(targetIndex)
                                    }
                                }
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
                }
            }
    ) {
        // 计算分隔线位置
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .align(Alignment.Center)
        ) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.TopCenter),
                color = dividerColor,
                thickness = dividerSize
            )
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomCenter),
                color = dividerColor,
                thickness = dividerSize
            )
        }

        LazyColumn(
            state = listState,
            flingBehavior = snapFlingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * visibleItemsCount),
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItemsCount / 2))
        ) {
            itemsIndexed(values) { index, item ->
                val isSelected = index == centerIndex
                val distance = abs(index - currentScrollPosition)

                // 线性插值计算样式
                val (fontSize, textColor) = when {
                    distance <= 1f -> {
                        lerp(30.sp, 25.sp, distance) to
                                lerp(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    distance
                                )
                    }
                    distance <= 2f -> {
                        lerp(25.sp, 20.sp, distance - 1f) to
                                lerp(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    distance - 1f
                                )
                    }
                    else -> 20.sp to MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                }

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        }
                        .semantics {
                            selected = isSelected
                            val stateText = if (isSelected) stateSelected else stateNotSelected
                            contentDescription = "$item $stateText"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.toString(),
                        fontSize = fontSize,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        style = TextStyle(
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Center,
                                trim = LineHeightStyle.Trim.None
                            )
                        ),
                        modifier = Modifier
                            .offset(y = itemTextOffsetY)
                    )
                }
            }
        }
    }
}