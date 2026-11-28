@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemDefaults.SegmentedGap
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 通用分组数据模型。 */
data class SettingsGroup<T>(
    val title: String? = null,
    val items: List<T>
)

private sealed interface FlatUiItem<T> {
    data class Header<T>(val groupIndex: Int, val title: String) : FlatUiItem<T>

    data class RowItem<T>(
        val groupIndex: Int,
        val itemIndex: Int,
        val data: T,
        val groupSize: Int
    ) : FlatUiItem<T>
}

/**
 * 长列表设置分组容器。
 *
 * @param groups 分组数据
 * @param content 条目内容，shapes 为当前条目的形状
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
    content: @Composable (item: T, shapes: ListItemShapes) -> Unit
) {
    val flatItems = rememberFlattenedItems(groups)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = state,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(SegmentedGap)
    ) {
        leadingContent?.let { leading ->
            item(key = Triple(-2, -1, -1)) { leading() }
        }

        if (!title.isNullOrBlank()) {
            item(key = Triple(-1, -1, -1)) { headerContent(title) }
        }

        items(
            items = flatItems,
            key = { flatItem ->
                when (flatItem) {
                    is FlatUiItem.Header -> Triple(0, flatItem.groupIndex, -1)
                    is FlatUiItem.RowItem -> Triple(1, flatItem.groupIndex, flatItem.itemIndex)
                }
            }
        ) { flatItem ->
            when (flatItem) {
                is FlatUiItem.Header -> headerContent(flatItem.title)
                is FlatUiItem.RowItem -> {
                    val shapes = ListItemDefaults.segmentedShapes(
                        index = flatItem.itemIndex,
                        count = flatItem.groupSize
                    )
                    content(flatItem.data, shapes)
                }
            }
        }

        trailingContent?.let { trailing ->
            item(key = Triple(-3, -1, -1)) { trailing() }
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
                    groupSize = size
                )
            )
        }
    }
    return result
}
