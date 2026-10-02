package com.xingheyuzhuan.shiguangschedule.ui.settings.style

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xingheyuzhuan.shiguangschedule.Destination
import com.xingheyuzhuan.shiguangschedule.tool.FileManagerCallbacks
import com.xingheyuzhuan.shiguangschedule.tool.rememberFileManager
import com.xingheyuzhuan.shiguangschedule.ui.components.AdvancedColorPicker
import com.xingheyuzhuan.shiguangschedule.ui.components.ColorPickerConfig
import com.xingheyuzhuan.shiguangschedule.ui.components.ImageCropper
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme
import com.xingheyuzhuan.shiguangschedule.ui.theme.rememberColorScheme
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_back
import shiguangschedule.shared.generated.resources.arrow_back_24px
import shiguangschedule.shared.generated.resources.contrast_24px
import shiguangschedule.shared.generated.resources.item_personalization

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StyleSettingsScreen(
    onNavigate: (Destination) -> Unit,
    onBack: () -> Unit,
    viewModel: StyleSettingsViewModel = koinViewModel()
) {
    val currentIsDark = LocalIsDarkTheme.current

    LaunchedEffect(Unit) {
        viewModel.initPreviewDark(currentIsDark)
    }

    // 收集状态数据
    val styleState by viewModel.styleState.collectAsStateWithLifecycle()
    val demoUiState by viewModel.demoUiState.collectAsStateWithLifecycle()
    val isPreviewDark by viewModel.isPreviewDark.collectAsStateWithLifecycle()
    val wallpaperPath by viewModel.wallpaperPathState.collectAsStateWithLifecycle()
    val appSettings by viewModel.appSettingsState.collectAsStateWithLifecycle()

    val containerSize = LocalWindowInfo.current.containerSize
    val isLandscape = containerSize.width > containerSize.height

    var showColorPicker by remember { mutableStateOf(false) }
    var isDarkTarget by remember { mutableStateOf(false) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }

    val sheetState = rememberModalBottomSheetState()

    var loadedBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showCropper by remember { mutableStateOf(false) }

    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val cardContainerColor = CardDefaults.cardColors().containerColor

    // 图片选择与裁剪管理器
    val fileManager = rememberFileManager(
        callbacks = FileManagerCallbacks(
            onImagePicked = { bitmap ->
                if (bitmap != null) {
                    loadedBitmap = bitmap
                    showCropper = true
                }
            }
        )
    )

    if (showCropper && loadedBitmap != null) {
        val screenAspectRatio = if (containerSize.height > 0) {
            containerSize.width.toFloat() / containerSize.height.toFloat()
        } else {
            1f
        }

        ImageCropper(
            imageBitmap = loadedBitmap,
            aspectRatio = screenAspectRatio,
            onCropConfirmed = { bytes ->
                viewModel.saveCroppedWallpaper(bytes, isPreviewDark)
                showCropper = false
                loadedBitmap = null
            },
            onDismiss = {
                showCropper = false
                loadedBitmap = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(Res.string.item_personalization), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(vectorResource(Res.drawable.arrow_back_24px), contentDescription = stringResource(Res.string.a11y_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        val currentStyle = styleState
        val currentAppSettings = appSettings

        // 确保非空后再渲染内容，防止空指针与 copy 报错
        if (currentStyle != null && currentAppSettings != null) {
            val contentModifier = Modifier.padding(paddingValues).fillMaxSize()

            // 预览区域内容
            val previewContent = @Composable { modifier: Modifier ->
                val density = LocalDensity.current
                val windowWidthDp = with(density) { containerSize.width.toDp() }

                val previewColorScheme = rememberColorScheme(
                    darkTheme = isPreviewDark,
                    dynamicColor = currentAppSettings.useDynamicColor,
                    customLightPrimary = Color(currentAppSettings.customPrimaryColor.light),
                    customDarkPrimary = Color(currentAppSettings.customPrimaryColor.dark)
                )

                CompositionLocalProvider(LocalIsDarkTheme provides isPreviewDark) {
                    MaterialTheme(colorScheme = previewColorScheme) {
                        Box(
                            modifier = modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(rememberScrollState())
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                awaitPointerEvent()
                                            }
                                        }
                                    }
                            ) {
                                Box(modifier = Modifier.requiredWidth(windowWidthDp)) {
                                    ScheduleGridContent(
                                        style = currentStyle,
                                        demoUiState = demoUiState,
                                        wallpaperPath = wallpaperPath
                                    )
                                }
                            }

                            // 切换深浅色预览悬浮按钮
                            FloatingActionButton(
                                onClick = { viewModel.setPreviewDark(!isPreviewDark) },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.contrast_24px),
                                    contentDescription = null,
                                    modifier = Modifier.graphicsLayer {
                                        if (isPreviewDark) {
                                            scaleX = -1f
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 响应式布局：横屏左右结构，竖屏上下结构
            if (isLandscape) {
                Row(modifier = contentModifier) {
                    previewContent(Modifier.fillMaxHeight().weight(0.4f))
                    Card(
                        modifier = Modifier.fillMaxHeight().weight(0.6f),
                        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                    ) {
                        SettingsListContent(
                            currentStyle = currentStyle,
                            viewModel = viewModel,
                            isPreviewDark = isPreviewDark,
                            wallpaperPath = wallpaperPath,
                            onWallpaperClick = { fileManager.pickImage() },
                            onNavigate = onNavigate
                        ) { isDark, idx ->
                            isDarkTarget = isDark
                            selectedColorIndex = idx
                            showColorPicker = true
                        }
                    }
                }
            } else {
                Column(modifier = contentModifier) {
                    previewContent(Modifier.fillMaxWidth().weight(0.45f))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.55f)
                            .drawWithContent {
                                drawContent()
                                val navBarPx = navigationBarHeight.toPx()
                                if (navBarPx > 0) {
                                    drawRect(
                                        color = cardContainerColor,
                                        topLeft = Offset(0f, size.height),
                                        size = Size(size.width, navBarPx)
                                    )
                                }
                            },
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    ) {
                        SettingsListContent(
                            currentStyle = currentStyle,
                            viewModel = viewModel,
                            isPreviewDark = isPreviewDark,
                            wallpaperPath = wallpaperPath,
                            onWallpaperClick = { fileManager.pickImage() },
                            onNavigate = onNavigate
                        ) { isDark, idx ->
                            isDarkTarget = isDark
                            selectedColorIndex = idx
                            showColorPicker = true
                        }
                    }
                }
            }

            // 颜色选择器底部弹窗
            if (showColorPicker) {
                ModalBottomSheet(onDismissRequest = { showColorPicker = false }, sheetState = sheetState) {
                    val initialColor = currentStyle.courseColorMaps.getOrNull(selectedColorIndex)?.let { pair ->
                        if (isDarkTarget) pair.dark else pair.light
                    } ?: Color.Gray

                    var currentColorInPicker by remember { mutableStateOf(initialColor) }

                    AdvancedColorPicker(
                        initialColor = initialColor,
                        config = ColorPickerConfig(showAlpha = false),
                        onColorChanged = { newColor ->
                            currentColorInPicker = newColor
                            viewModel.updatePrimaryColor(selectedColorIndex, newColor, isDarkTarget)
                        },
                        previewContent = {
                            ColorPreviewBox(currentColorInPicker, !isDarkTarget)
                        }
                    )
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        } else {
            // 加载中占位图
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}