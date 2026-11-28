@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.ui.components.settingsgroup.items.SettingsIcon
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.check_24px

/** 菜单高度上限。 */
private val MenuMaxHeight = 360.dp

/** 选中项的对勾图标。 */
@Composable
private fun MenuCheckIcon() {
    SettingsIcon(imageVector = vectorResource(Res.drawable.check_24px))
}

/**
 * M3 Expressive 垂直选择菜单。
 *
 * 内容超过 [MenuMaxHeight] 时滚动，打开时自动滚到当前选中项。
 *
 * @param values 候选项
 * @param selectedValue 当前选中项
 * @param itemLabel 候选项文字
 * @param onConfirm 选中回调
 * @param onDismiss 关闭回调
 */
@Composable
fun <T> VerticalSelectionMenu(
    expanded: Boolean,
    values: List<T>,
    selectedValue: T,
    onDismiss: () -> Unit,
    onConfirm: (T) -> Unit,
    itemLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    DropdownMenuPopup(
        expanded = expanded,
        onDismissRequest = onDismiss,
        popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
            MenuAnchorPosition.Below
        )
    ) {
        val scrollState = rememberScrollState()
        var selectedItemTop by remember { mutableIntStateOf(-1) }

        LaunchedEffect(selectedItemTop) {
            if (selectedItemTop >= 0) scrollState.scrollTo(selectedItemTop)
        }

        DropdownMenuGroup(
            shapes = MenuDefaults.groupShape(0, 1),
            modifier = modifier.heightIn(max = MenuMaxHeight).verticalScroll(scrollState)
        ) {
            values.forEachIndexed { index, value ->
                DropdownMenuItem(
                    selected = value == selectedValue,
                    onClick = {
                        onConfirm(value)
                        onDismiss()
                    },
                    text = { Text(itemLabel(value)) },
                    shapes = MenuDefaults.itemShape(index, values.size),
                    trailingIcon = { if (value == selectedValue) MenuCheckIcon() },
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        if (value == selectedValue) {
                            selectedItemTop = coordinates.positionInParent().y.toInt()
                        }
                    }
                )
            }
        }
    }
}
