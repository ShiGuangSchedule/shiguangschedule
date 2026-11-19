package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 通用分组数据模型。
 *
 * @param title 分组标题，为 null 时不显示分组头
 * @param items 分组内的子项
 */
data class SettingsGroup<T>(
    val title: String? = null,
    val items: List<T>
)

private sealed interface FlatUiItem<T> {
    data class Header<T>(
        val groupIndex: Int,
        val title: String
    ) : FlatUiItem<T>

    data class RowItem<T>(
        val groupIndex: Int,
        val itemIndex: Int,
        val data: T,
        val isFirstInGroup: Boolean,
        val isLastInGroup: Boolean
    ) : FlatUiItem<T>
}

/**
 * 高性能长列表分组容器（单根 LazyColumn）。
 * 负责数据扁平化、分组头渲染与组内首尾非对称圆角。
 *
 * @param groups 分组数据
 * @param state 滚动状态，供外部控制跳转
 * @param contentPadding 列表内边距
 * @param leadingContent 所有分组之前的可选内容
 * @param trailingContent 所有分组之后的可选内容
 * @param title 顶层可选大标题
 * @param headerContent 自定义分组标题渲染
 */
@Composable
fun <T> SettingsGroupLazyColumn(
    groups: List<SettingsGroup<T>>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    title: String? = null,
    headerContent: @Composable (title: String) -> Unit = { titleText ->
        Text(
            text = titleText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
        )
    },
    content: @Composable (item: T, shape: RoundedCornerShape) -> Unit
) {
    val flatItems = rememberFlattenedItems(groups)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(SettingsDefaults.CardSpacing)
    ) {
        leadingContent?.let { leading ->
            item(key = Triple(-2, -1, -1)) {
                leading()
            }
        }

        if (!title.isNullOrBlank()) {
            item(key = Triple(-1, -1, -1)) {
                headerContent(title)
            }
        }

        items(
            items = flatItems,
            key = { flatItem ->
                // (类型标记, 分组序号, 组内序号)
                when (flatItem) {
                    is FlatUiItem.Header -> Triple(0, flatItem.groupIndex, -1)
                    is FlatUiItem.RowItem -> Triple(1, flatItem.groupIndex, flatItem.itemIndex)
                }
            }
        ) { flatItem ->
            when (flatItem) {
                is FlatUiItem.Header -> {
                    headerContent(flatItem.title)
                }
                is FlatUiItem.RowItem -> {
                    val shape = when {
                        flatItem.isFirstInGroup && flatItem.isLastInGroup -> {
                            RoundedCornerShape(SettingsDefaults.OuterCornerRadius)
                        }
                        flatItem.isFirstInGroup -> {
                            RoundedCornerShape(
                                topStart = SettingsDefaults.OuterCornerRadius,
                                topEnd = SettingsDefaults.OuterCornerRadius,
                                bottomStart = SettingsDefaults.InnerCornerRadius,
                                bottomEnd = SettingsDefaults.InnerCornerRadius
                            )
                        }
                        flatItem.isLastInGroup -> {
                            RoundedCornerShape(
                                topStart = SettingsDefaults.InnerCornerRadius,
                                topEnd = SettingsDefaults.InnerCornerRadius,
                                bottomStart = SettingsDefaults.OuterCornerRadius,
                                bottomEnd = SettingsDefaults.OuterCornerRadius
                            )
                        }
                        else -> {
                            RoundedCornerShape(SettingsDefaults.InnerCornerRadius)
                        }
                    }

                    content(flatItem.data, shape)
                }
            }
        }

        trailingContent?.let { trailing ->
            item(key = Triple(-3, -1, -1)) {
                trailing()
            }
        }
    }
}

@Composable
private fun <T> rememberFlattenedItems(groups: List<SettingsGroup<T>>): List<FlatUiItem<T>> {
    val result = mutableListOf<FlatUiItem<T>>()
    groups.forEachIndexed { groupIndex, group ->
        if (group.items.isEmpty()) return@forEachIndexed
        if (!group.title.isNullOrBlank()) {
            result.add(FlatUiItem.Header(groupIndex = groupIndex, title = group.title))
        }
        val size = group.items.size
        group.items.forEachIndexed { itemIndex, item ->
            result.add(
                FlatUiItem.RowItem(
                    groupIndex = groupIndex,
                    itemIndex = itemIndex,
                    data = item,
                    isFirstInGroup = (itemIndex == 0),
                    isLastInGroup = (itemIndex == size - 1)
                )
            )
        }
    }
    return result
}