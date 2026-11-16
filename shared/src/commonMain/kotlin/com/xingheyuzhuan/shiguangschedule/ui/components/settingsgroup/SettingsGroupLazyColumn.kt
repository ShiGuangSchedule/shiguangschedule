package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 通用分组数据模型（用于长列表，如语言选择、城市选择等）。
 *
 * @param title 分组标题（可空，若为 null 则该分组不显示头部 Header）
 * @param items 该组下的子项数据列表
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
 * 高性能长列表分组容器（底层使用单根 LazyColumn）。
 * 自动处理数据扁平化、字母分组头、以及组内首尾非对称圆角。
 * 内部基于顺序索引管理 Item Key，完全不受业务数据重复或同名标题影响。
 *
 * @param groups 分组数据列表
 * @param title 整个长列表的顶层可选大标题（可空）
 * @param headerContent 自定义分组标题渲染
 */
@Composable
fun <T> SettingsGroupLazyColumn(
    groups: List<SettingsGroup<T>>,
    modifier: Modifier = Modifier,
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
        verticalArrangement = Arrangement.spacedBy(SettingsDefaults.CardSpacing)
    ) {
        if (!title.isNullOrBlank()) {
            item(key = Triple(-1, -1, -1)) {
                headerContent(title)
            }
        }

        items(
            items = flatItems,
            key = { flatItem ->
                // 纯内部顺序索引作为唯一键：(类型标记, 分组序号, 组内序号)
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