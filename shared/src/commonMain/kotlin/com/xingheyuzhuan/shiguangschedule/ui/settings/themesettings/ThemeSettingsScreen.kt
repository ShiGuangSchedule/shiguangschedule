package com.xingheyuzhuan.shiguangschedule.ui.settings.themesettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
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
import com.xingheyuzhuan.shiguangschedule.data.model.AppThemeMode
import com.xingheyuzhuan.shiguangschedule.ui.components.AdvancedColorPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.ColorPickerConfig
import com.xingheyuzhuan.shiguangschedule.ui.settings.SettingsViewModel
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import com.xingheyuzhuan.shiguangschedule.ui.theme.supportsDynamicColor
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.action_reset
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.custom_color_title
import shiguangschedule.shared.generated.resources.dark_primary_color
import shiguangschedule.shared.generated.resources.dynamic_color_desc
import shiguangschedule.shared.generated.resources.dynamic_color_title
import shiguangschedule.shared.generated.resources.light_primary_color
import shiguangschedule.shared.generated.resources.refresh_24px
import shiguangschedule.shared.generated.resources.theme_color_hint
import shiguangschedule.shared.generated.resources.theme_mode_label
import shiguangschedule.shared.generated.resources.theme_settings_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val settings = uiState.appSettings

    val surfaceColor = MaterialTheme.colorScheme.surface

    Scaffold(
        containerColor = surfaceColor,
        topBar = {
            Surface(
                color = surfaceColor,
                tonalElevation = 0.dp
            ) {
                TopAppBar(
                    title = { Text(stringResource(Res.string.theme_settings_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                vectorResource(Res.drawable.arrow_back_24px),
                                contentDescription = stringResource(Res.string.a11y_back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 主题模式选择
            SectionHeader(stringResource(Res.string.theme_mode_label))
            ThemeModeSelector(
                selectedMode = settings.themeMode,
                onModeSelected = { viewModel.onThemeModeChanged(it) }
            )

            // 动态取色 (利用跨平台 supportsDynamicColor 统一处理)
            if (supportsDynamicColor) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DynamicColorToggle(
                    enabled = settings.useDynamicColor,
                    onEnabledChange = { viewModel.onUseDynamicColorChanged(it) }
                )
            }

            // 自定义主色调选择
            val showColorPicker = !supportsDynamicColor || !settings.useDynamicColor

            AnimatedVisibility(
                visible = showColorPicker,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    SectionHeader(stringResource(Res.string.custom_color_title))

                    val isDark = LocalIsDarkTheme.current

                    if (isDark) {
                        ColorPickerItem(
                            label = stringResource(Res.string.dark_primary_color),
                            currentColor = Color(settings.customPrimaryColor.dark),
                            onColorChanged = { viewModel.onCustomDarkPrimaryChanged(it) },
                            onReset = { viewModel.onCustomDarkPrimaryChanged() }
                        )
                    } else {
                        ColorPickerItem(
                            label = stringResource(Res.string.light_primary_color),
                            currentColor = Color(settings.customPrimaryColor.light),
                            onColorChanged = { viewModel.onCustomLightPrimaryChanged(it) },
                            onReset = { viewModel.onCustomLightPrimaryChanged() }
                        )
                    }

                    Text(
                        text = stringResource(Res.string.theme_color_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerItem(
    label: String,
    currentColor: Color,
    onColorChanged: (Color) -> Unit,
    onReset: () -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Surface(
        onClick = { showSheet = true },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(currentColor)
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
                    Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                    TextButton(onClick = onReset) {
                        Icon(vectorResource(Res.drawable.refresh_24px), contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.action_reset))
                    }
                }
                AdvancedColorPicker(
                    initialColor = currentColor,
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

@Composable
private fun ThemeModeSelector(
    selectedMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit
) {
    val modes = AppThemeMode.entries

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size)
            ) {
                Text(
                    text = stringResource(mode.labelRes),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun DynamicColorToggle(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onEnabledChange(!enabled) },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(Res.string.dynamic_color_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(Res.string.dynamic_color_desc), style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = enabled, onCheckedChange = onEnabledChange)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}