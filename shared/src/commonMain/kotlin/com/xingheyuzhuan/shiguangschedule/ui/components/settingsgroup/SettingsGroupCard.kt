@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemDefaults.SegmentedGap
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class SettingsGroupScope {
    internal val items = mutableListOf<@Composable () -> Unit>()

    /** 在分组中添加一个条目。 */
    fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}

/**
 * 条目较少的设置分组卡片。
 *
 * @param title 分组标题，为空时不显示
 */
@Composable
fun SettingsGroupCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: SettingsGroupScope.() -> Unit
) {
    val scope = remember(content) {
        SettingsGroupScope().apply(content)
    }
    val itemCount = scope.items.size

    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(SegmentedGap)
        ) {
            scope.items.forEachIndexed { index, itemComposable ->
                val shapes = ListItemDefaults.segmentedShapes(index = index, count = itemCount)
                CompositionLocalProvider(LocalItemShapes provides shapes) {
                    itemComposable()
                }
            }
        }
    }
}
