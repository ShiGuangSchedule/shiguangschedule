package com.xingheyuzhuan.shiguangschedule.ui.settings.style.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.WidgetThemeModeProto
import com.xingheyuzhuan.shiguangschedule.ui.components.AdvancedColorPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.ColorPickerConfig
import com.xingheyuzhuan.shiguangschedule.ui.components.SliderWithInputField
import com.xingheyuzhuan.shiguangschedule.ui.settings.style.StyleSwitchItem
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.action_cancel
import shiguangschedule.shared.generated.resources.action_confirm
import shiguangschedule.shared.generated.resources.action_reset
import shiguangschedule.shared.generated.resources.action_reset_style
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.dialog_reset_message
import shiguangschedule.shared.generated.resources.dialog_reset_title
import shiguangschedule.shared.generated.resources.label_font_scale
import shiguangschedule.shared.generated.resources.label_hide_date
import shiguangschedule.shared.generated.resources.label_hide_location
import shiguangschedule.shared.generated.resources.label_hide_teacher
import shiguangschedule.shared.generated.resources.label_opacity
import shiguangschedule.shared.generated.resources.refresh_24px
import shiguangschedule.shared.generated.resources.status_not_set
import shiguangschedule.shared.generated.resources.style_category_course_block
import shiguangschedule.shared.generated.resources.style_category_grid_size
import shiguangschedule.shared.generated.resources.theme_dark
import shiguangschedule.shared.generated.resources.theme_follow_system
import shiguangschedule.shared.generated.resources.theme_light
import shiguangschedule.shared.generated.resources.theme_settings_title
import shiguangschedule.shared.generated.resources.title_theme_seed_color
import shiguangschedule.shared.generated.resources.title_widget_style

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetStyleSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: WidgetStyleViewModel = koinViewModel()
) {
    val widgetStyle by viewModel.widgetStyle.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(Res.string.dialog_reset_title)) },
            text = { Text(stringResource(Res.string.dialog_reset_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetWidgetStyle()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(Res.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.title_widget_style),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.arrow_back_24px),
                            contentDescription = stringResource(Res.string.a11y_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 重置按钮
            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Text(stringResource(Res.string.action_reset_style))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 2. 主题与外观分类
            Text(
                text = stringResource(Res.string.theme_settings_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            WidgetThemeModeSelector(
                currentMode = widgetStyle.themeMode,
                onModeChange = { viewModel.updateThemeMode(it) }
            )

            WidgetSeedColorPickerItem(
                label = stringResource(Res.string.title_theme_seed_color),
                currentColor = widgetStyle.seedColor?.let { Color(it) },
                onColorChanged = { viewModel.updateSeedColor(it) },
                onReset = { viewModel.updateSeedColor(null) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 3. 尺寸与排版分类
            Text(
                text = stringResource(Res.string.style_category_grid_size),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            SliderWithInputField(
                label = stringResource(Res.string.label_font_scale),
                value = widgetStyle.fontScale,
                range = 0.5f..2.0f,
                stepValue = 0.1f,
                onValueChange = { viewModel.updateFontScale(it) }
            )

            SliderWithInputField(
                label = stringResource(Res.string.label_opacity),
                value = widgetStyle.backgroundAlpha,
                range = 0.0f..1.0f,
                stepValue = 0.05f,
                onValueChange = { viewModel.updateBackgroundAlpha(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            // 4. 显示控制分类
            Text(
                text = stringResource(Res.string.style_category_course_block),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            StyleSwitchItem(
                label = stringResource(Res.string.label_hide_teacher),
                checked = widgetStyle.hideTeacher,
                onCheckedChange = { viewModel.updateHideTeacher(it) }
            )

            StyleSwitchItem(
                label = stringResource(Res.string.label_hide_location),
                checked = widgetStyle.hideLocation,
                onCheckedChange = { viewModel.updateHideLocation(it) }
            )

            StyleSwitchItem(
                label = stringResource(Res.string.label_hide_date),
                checked = widgetStyle.hideDate,
                onCheckedChange = { viewModel.updateHideDate(it) }
            )
        }
    }
}

@Composable
private fun WidgetThemeModeSelector(
    currentMode: WidgetThemeModeProto,
    onModeChange: (WidgetThemeModeProto) -> Unit
) {
    val modes = listOf(
        WidgetThemeModeProto.WIDGET_THEME_FOLLOW_SYSTEM to stringResource(Res.string.theme_follow_system),
        WidgetThemeModeProto.WIDGET_THEME_LIGHT to stringResource(Res.string.theme_light),
        WidgetThemeModeProto.WIDGET_THEME_DARK to stringResource(Res.string.theme_dark)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = currentMode == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onModeChange(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetSeedColorPickerItem(
    label: String,
    currentColor: Color?,
    onColorChanged: (Color) -> Unit,
    onReset: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { showSheet = true }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        if (currentColor != null) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
            )
        } else {
            Text(
                text = stringResource(Res.string.status_not_set),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 40.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = {
                        onReset()
                        showSheet = false
                    }) {
                        Icon(
                            vectorResource(Res.drawable.refresh_24px),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.action_reset))
                    }
                }

                val pickerInitialColor = currentColor ?: MaterialTheme.colorScheme.primary

                AdvancedColorPicker(
                    initialColor = pickerInitialColor,
                    onColorChanged = onColorChanged,
                    config = ColorPickerConfig(
                        showAlpha = false,
                        showInputMode = true
                    )
                )
            }
        }
    }
}