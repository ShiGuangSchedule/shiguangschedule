package com.xingheyuzhuan.shiguangschedule.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.a11y_switch_color_mode
import shiguangschedule.shared.generated.resources.color_picker_label_alpha
import shiguangschedule.shared.generated.resources.color_picker_label_hue
import shiguangschedule.shared.generated.resources.color_picker_label_saturation
import shiguangschedule.shared.generated.resources.color_picker_label_value
import shiguangschedule.shared.generated.resources.color_picker_mode_input
import shiguangschedule.shared.generated.resources.color_picker_mode_visual
import shiguangschedule.shared.generated.resources.color_picker_title_edit
import shiguangschedule.shared.generated.resources.color_picker_title_precise
import shiguangschedule.shared.generated.resources.edit_24px
import shiguangschedule.shared.generated.resources.tune_24px
import kotlin.time.Duration.Companion.milliseconds

/**
 * 颜色选择器功能配置
 * 默认全开，按需关闭
 */
data class ColorPickerConfig(
    val showHue: Boolean = true,        // 色相
    val showSaturation: Boolean = true, // 饱和度
    val showValue: Boolean = true,      // 明度
    val showAlpha: Boolean = true,      // 不透明度
    val showHex: Boolean = true,        // 十六进制代码显示
    val showInputMode: Boolean = true   // 是否允许切换到数字输入模式
)

private object ColorInternalUtils {
    fun hsvToColor(h: Float, s: Float, v: Float, a: Float = 1f): Color {
        // 对 h 做 0..360 规范化，防止边界越界
        val safeH = (h % 360f + 360f) % 360f
        val c = v * s
        val x = c * (1 - kotlin.math.abs((safeH / 60f) % 2 - 1))
        val m = v - c

        val (rPrime, gPrime, bPrime) = when {
            safeH < 60f -> Triple(c, x, 0f)
            safeH < 120f -> Triple(x, c, 0f)
            safeH < 180f -> Triple(0f, c, x)
            safeH < 240f -> Triple(0f, x, c)
            safeH < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            red = rPrime + m,
            green = gPrime + m,
            blue = bPrime + m,
            alpha = a
        )
    }

    fun colorToHsv(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue

        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min

        var h = 0f
        if (delta != 0f) {
            h = when (max) {
                r -> ((g - b) / delta) % 6f
                g -> ((b - r) / delta) + 2f
                else -> ((r - g) / delta) + 4f
            } * 60f
            if (h < 0) h += 360f
        }

        val s = if (max == 0f) 0f else delta / max
        val v = max

        return floatArrayOf(h, s, v)
    }

    fun colorToHex(color: Color): String {
        val argb = color.toArgb()
        val alpha = ((argb shr 24) and 0xFF).toString(16).padStart(2, '0').uppercase()
        val red = ((argb shr 16) and 0xFF).toString(16).padStart(2, '0').uppercase()
        val green = ((argb shr 8) and 0xFF).toString(16).padStart(2, '0').uppercase()
        val blue = (argb and 0xFF).toString(16).padStart(2, '0').uppercase()
        return "#$alpha$red$green$blue"
    }
}

@Composable
fun AdvancedColorPicker(
    initialColor: Color,
    onColorChanged: (Color) -> Unit,
    config: ColorPickerConfig = ColorPickerConfig(),
    previewContent: @Composable (() -> Unit)? = null
) {
    val initialHsv = remember(initialColor) { ColorInternalUtils.colorToHsv(initialColor) }
    var h by remember { mutableFloatStateOf(initialHsv[0]) }
    var s by remember { mutableFloatStateOf(initialHsv[1]) }
    var v by remember { mutableFloatStateOf(initialHsv[2]) }
    var a by remember { mutableFloatStateOf(initialColor.alpha) }

    var isUserInteracting by remember { mutableStateOf(false) }

    LaunchedEffect(initialColor) {
        if (!isUserInteracting) {
            val currentLocalColor = ColorInternalUtils.hsvToColor(h, s, v, a)
            if (initialColor.toArgb() != currentLocalColor.toArgb()) {
                val hsv = ColorInternalUtils.colorToHsv(initialColor)

                if (hsv[2] > 0.01f) {
                    h = hsv[0]
                    s = hsv[1]
                }
                v = hsv[2]
                a = initialColor.alpha
            }
        }
    }

    var isInputMode by remember { mutableStateOf(false) }
    val currentColor = remember(h, s, v, a) { ColorInternalUtils.hsvToColor(h, s, v, a) }

    LaunchedEffect(currentColor) {
        if (isUserInteracting) {
            onColorChanged(currentColor)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        previewContent?.let { Box(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) { it() } }

        // 标题与切换按钮
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Column {
                Text(
                    text = if (isInputMode) stringResource(Res.string.color_picker_title_precise)
                    else stringResource(Res.string.color_picker_title_edit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isInputMode) stringResource(Res.string.color_picker_mode_input)
                    else stringResource(Res.string.color_picker_mode_visual),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (config.showInputMode) {
                IconButton(onClick = { isInputMode = !isInputMode }) {
                    Icon(
                        imageVector = if (isInputMode) vectorResource(Res.drawable.tune_24px) else vectorResource(Res.drawable.edit_24px),
                        contentDescription = stringResource(Res.string.a11y_switch_color_mode)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!isInputMode) {
            val updateHsv = { nh: Float, ns: Float, nv: Float, na: Float ->
                isUserInteracting = true
                h = nh; s = ns; v = nv; a = na
            }
            LaunchedEffect(isUserInteracting) {
                if (isUserInteracting) {
                    delay(500.milliseconds)
                    isUserInteracting = false
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (config.showHue) {
                    ColorLabel(stringResource(Res.string.color_picker_label_hue), "${h.toInt()}°")
                    InternalGradientSlider(h, { updateHsv(it, s, v, a) }, 0f..359f, Brush.horizontalGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)))
                }
                if (config.showSaturation) {
                    ColorLabel(stringResource(Res.string.color_picker_label_saturation), "${(s * 100).toInt()}%")
                    InternalGradientSlider(s, { updateHsv(h, it, v, a) }, 0f..1f, Brush.horizontalGradient(listOf(ColorInternalUtils.hsvToColor(h, 0f, v), ColorInternalUtils.hsvToColor(h, 1f, v))))
                }
                if (config.showValue) {
                    ColorLabel(stringResource(Res.string.color_picker_label_value), "${(v * 100).toInt()}%")
                    InternalGradientSlider(v, { updateHsv(h, s, it, a) }, 0f..1f, Brush.horizontalGradient(listOf(Color.Black, ColorInternalUtils.hsvToColor(h, s, 1f))))
                }
                if (config.showAlpha) {
                    ColorLabel(stringResource(Res.string.color_picker_label_alpha), "${(a * 100).toInt()}%")
                    InternalGradientSlider(a, { updateHsv(h, s, v, it) }, 0f..1f, Brush.horizontalGradient(listOf(Color.Transparent, ColorInternalUtils.hsvToColor(h, s, v, 1f))))
                }
            }
        } else {
            RgbInputSection(currentColor, config.showAlpha) { newColor ->
                isUserInteracting = true
                val newHsv = ColorInternalUtils.colorToHsv(newColor)
                h = newHsv[0]; s = newHsv[1]; v = newHsv[2]
                a = newColor.alpha
            }
        }

        if (config.showHex) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                    Text(text = ColorInternalUtils.colorToHex(currentColor), modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun InternalGradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val thumbRadiusOuter = with(density) { 12.dp.toPx() }
    val thumbRadiusInner = with(density) { 10.dp.toPx() }
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val horizontalPaddingPx = with(density) { 12.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(32.dp)
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(gradient)
                .pointerInput(range, widthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val updateValue = { offset: Offset ->
                            val ratio = (offset.x / widthPx).coerceIn(0f, 1f)
                            val newValue = ratio * (range.endInclusive - range.start) + range.start
                            onValueChange(newValue)
                        }
                        updateValue(down.position)
                        drag(down.id) { change ->
                            updateValue(change.position)
                            if (change.positionChange() != Offset.Zero) change.consume()
                        }
                    }
                }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            val thumbX = (fraction * widthPx).coerceIn(horizontalPaddingPx, widthPx - horizontalPaddingPx)
            val centerY = heightPx / 2
            drawCircle(Color.Black.copy(alpha = 0.2f), radius = thumbRadiusOuter, center = Offset(thumbX, centerY))
            drawCircle(Color.White, radius = thumbRadiusInner, center = Offset(thumbX, centerY))
            drawCircle(Color.Gray.copy(alpha = 0.8f), radius = thumbRadiusInner, center = Offset(thumbX, centerY), style = Stroke(width = strokeWidthPx))
        }
    }
}

@Composable
private fun RgbInputSection(color: Color, showAlpha: Boolean, onColorChanged: (Color) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        RgbInputField("R", (color.red * 255).toInt(), Modifier.weight(1f)) { onColorChanged(color.copy(red = it / 255f)) }
        RgbInputField("G", (color.green * 255).toInt(), Modifier.weight(1f)) { onColorChanged(color.copy(green = it / 255f)) }
        RgbInputField("B", (color.blue * 255).toInt(), Modifier.weight(1f)) { onColorChanged(color.copy(blue = it / 255f)) }
        if (showAlpha) {
            RgbInputField("A", (color.alpha * 255).toInt(), Modifier.weight(1f)) { onColorChanged(color.copy(alpha = it / 255f)) }
        }
    }
}

@Composable
private fun RgbInputField(
    label: String,
    value: Int,
    modifier: Modifier,
    onValueChange: (Int) -> Unit
) {
    var inputText by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) {
        if (value.toString() != inputText) {
            inputText = value.toString()
        }
    }

    OutlinedTextField(
        value = inputText,
        onValueChange = { text ->
            if (text.isEmpty()) {
                inputText = ""
                onValueChange(0)
            } else if (text.all { it.isDigit() }) {
                val parsed = text.toIntOrNull()
                if (parsed != null && parsed in 0..255) {
                    inputText = text
                    onValueChange(parsed)
                }
            }
        },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@Composable
private fun ColorLabel(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}