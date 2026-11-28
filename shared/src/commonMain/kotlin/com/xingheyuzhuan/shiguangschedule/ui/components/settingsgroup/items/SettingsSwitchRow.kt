@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.LocalItemShapes

/** 带开关的设置行，点击整行可切换开关。 */
@Composable
fun SettingsSwitchRow(
    modifier: Modifier = Modifier,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    shapes: ListItemShapes? = null,
) {
    val itemShapes = shapes ?: LocalItemShapes.current
        ?: ListItemDefaults.segmentedShapes(index = 0, count = 1)
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = itemShapes,
        modifier = modifier,
        enabled = enabled,
        leadingContent = leading,
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
        supportingContent = subtitle?.let { { Text(text = it) } },
        verticalAlignment = Alignment.CenterVertically,
        colors = ListItemDefaults.segmentedColors(),
        elevation = ListItemDefaults.elevation(elevation = 2.dp),
        content = { Text(text = title) }
    )
}
