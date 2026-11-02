package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.CourseTextColorModeProto
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 课程卡片文字配色结果。
 * primary 用于课程名/老师/地点，secondary 用于开始时间等次级信息。
 */
data class CourseTextColors(
    val primary: Color,
    val secondary: Color
)

/**
 * Compose 层缓存入口：同一背景/参数只计算一次。
 */
@Composable
fun rememberCourseTextColors(
    background: Color,
    mode: CourseTextColorModeProto,
    globalColor: Color?,
    tintStrength: Float,
    contrastTarget: Float,
    fallback: Color = MaterialTheme.colorScheme.onSurface,
    sourceColor: Color = background
): CourseTextColors {
    return remember(background, mode, globalColor, tintStrength, contrastTarget, fallback, sourceColor) {
        CourseTextColorUtils.resolve(
            background = background,
            mode = mode,
            globalColor = globalColor,
            fallback = fallback,
            tintStrength = tintStrength,
            contrastTarget = contrastTarget,
            sourceColor = sourceColor
        )
    }
}

/**
 * 基于 OKLCH 的同色系文字配色。
 *
 * 思路：
 * 1. 保留课程原色（或卡片背景）的色相，降低彩度，避免文字过艳；
 * 2. 根据背景选择深色或浅色文字；
 * 3. 用 WCAG 相对亮度调整 OKLCH 的 L，在达到目标对比度的前提下取最彩色结果；
 * 4. 深色/浅色模式共用同一算法，只由实际卡片背景亮度决定。
 *
 * OKLab/OKLCH 转换公式参考 Björn Ottosson:
 * https://bottosson.github.io/posts/oklab/
 */
object CourseTextColorUtils {

    private const val DEFAULT_PRIMARY_TARGET = 6.0f
    private const val SECONDARY_TARGET = 4.5f
    private const val NEUTRAL_CHROMA = 0.02f

    fun resolve(
        background: Color,
        mode: CourseTextColorModeProto,
        globalColor: Color?,
        fallback: Color,
        tintStrength: Float = 0.8f,
        contrastTarget: Float = DEFAULT_PRIMARY_TARGET,
        sourceColor: Color = background
    ): CourseTextColors {
        if (mode == CourseTextColorModeProto.COURSE_TEXT_COLOR_GLOBAL) {
            val primary = globalColor ?: fallback
            return CourseTextColors(
                primary = primary,
                secondary = primary.copy(alpha = 0.8f)
            )
        }
        return tonalColors(background, tintStrength, contrastTarget, sourceColor)
    }

    /**
     * 根据背景生成同色系文字。
     *
     * @param tintStrength 0 为中性黑白，1 为保留课程原色色相的最大浓度
     * @param contrastTarget 主文字目标对比度，最低 4.5
     * @param sourceColor 提取色相与彩度的课程原色，默认取背景色
     */
    fun tonalColors(
        background: Color,
        tintStrength: Float = 0.8f,
        contrastTarget: Float = DEFAULT_PRIMARY_TARGET,
        sourceColor: Color = background
    ): CourseTextColors {
        val bg = background.copy(alpha = 1f)
        val darkTextPotential = contrastRatio(Color.Black, bg)
        val lightTextPotential = contrastRatio(Color.White, bg)
        val useDarkText = darkTextPotential >= lightTextPotential

        val bgOklch = bg.toOklch()
        // 色相/彩度优先取未叠加透明度的课程原色：
        // 这样即使卡片透明度很低、背景接近白色，文字仍会保留课程颜色而不是发灰。
        val sourceOklch = sourceColor.copy(alpha = 1f).toOklch()
        val hueSource = if (sourceOklch.chroma >= NEUTRAL_CHROMA) sourceOklch else bgOklch
        val tint = tintStrength.coerceIn(0f, 1f)
        val chroma = when {
            hueSource.chroma < NEUTRAL_CHROMA || tint <= 0f -> 0f
            // 在保证可读性的前提下尽量保留色相，避免文字发闷发黑
            useDarkText -> min(hueSource.chroma * tint * 1.45f, 0.16f)
            else -> min(hueSource.chroma * tint * 1.1f, 0.13f)
        }
        val targetContrast = contrastTarget.coerceIn(4.5f, 12f)
        val secondaryTarget = max(SECONDARY_TARGET, targetContrast - 1.5f)

        // 从靠近背景的一侧开始向高对比方向搜索，取“刚好达标”的最彩色结果，
        // 而不是一味压到接近纯黑/纯白。
        val primaryStart = if (useDarkText) 0.82f else 0.42f
        val secondaryStart = if (useDarkText) 0.88f else 0.36f

        val primary = tuneLightness(
            startLightness = primaryStart,
            chroma = chroma,
            hueRadians = hueSource.hueRadians,
            background = bg,
            targetContrast = targetContrast,
            darken = useDarkText
        )
        val secondary = tuneLightness(
            startLightness = secondaryStart,
            chroma = chroma * 0.85f,
            hueRadians = hueSource.hueRadians,
            background = bg,
            targetContrast = secondaryTarget,
            darken = useDarkText
        )
        return CourseTextColors(primary = primary, secondary = secondary)
    }

    fun contrastRatio(foreground: Color, background: Color): Float {
        val l1 = relativeLuminance(foreground)
        val l2 = relativeLuminance(background)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    fun relativeLuminance(color: Color): Float {
        val r = srgbToLinear(color.red)
        val g = srgbToLinear(color.green)
        val b = srgbToLinear(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    private fun tuneLightness(
        startLightness: Float,
        chroma: Float,
        hueRadians: Float,
        background: Color,
        targetContrast: Float,
        darken: Boolean
    ): Color {
        var lightness = startLightness
        val step = if (darken) -0.004f else 0.004f
        var best = if (darken) Color.Black else Color.White
        var bestContrast = contrastRatio(best, background)

        repeat(250) {
            val candidate = oklchToColor(lightness, chroma, hueRadians)
            val contrast = contrastRatio(candidate, background)
            if (contrast > bestContrast) {
                best = candidate
                bestContrast = contrast
            }
            if (contrast >= targetContrast) return candidate
            if (lightness <= 0f || lightness >= 1f) return best
            lightness = (lightness + step).coerceIn(0f, 1f)
        }
        return best
    }

    private data class Oklch(val chroma: Float, val hueRadians: Float)

    private fun Color.toOklch(): Oklch {
        val r = srgbToLinear(red)
        val g = srgbToLinear(green)
        val b = srgbToLinear(blue)

        val l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b
        val m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b
        val s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b

        val lCbrt = cbrt(l.toDouble()).toFloat()
        val mCbrt = cbrt(m.toDouble()).toFloat()
        val sCbrt = cbrt(s.toDouble()).toFloat()

        val okA = 1.9779984951f * lCbrt - 2.4285922050f * mCbrt + 0.4505937099f * sCbrt
        val okB = 0.0259040371f * lCbrt + 0.7827717662f * mCbrt - 0.8086757660f * sCbrt

        return Oklch(
            chroma = sqrt(okA * okA + okB * okB),
            hueRadians = atan2(okB, okA)
        )
    }

    private fun oklchToColor(
        lightness: Float,
        chroma: Float,
        hueRadians: Float
    ): Color {
        var safeChroma = chroma
        repeat(24) {
            val linear = oklchToLinear(lightness, safeChroma, hueRadians)
            if (linear.all { it in -0.0005f..1.0005f }) {
                return Color(
                    red = linearToSrgb(linear[0].coerceIn(0f, 1f)),
                    green = linearToSrgb(linear[1].coerceIn(0f, 1f)),
                    blue = linearToSrgb(linear[2].coerceIn(0f, 1f)),
                    alpha = 1f
                )
            }
            safeChroma *= 0.9f
        }
        val linear = oklchToLinear(lightness, safeChroma, hueRadians)
        return Color(
            red = linearToSrgb(linear[0].coerceIn(0f, 1f)),
            green = linearToSrgb(linear[1].coerceIn(0f, 1f)),
            blue = linearToSrgb(linear[2].coerceIn(0f, 1f)),
            alpha = 1f
        )
    }

    private fun oklchToLinear(
        lightness: Float,
        chroma: Float,
        hueRadians: Float
    ): FloatArray {
        val a = chroma * cos(hueRadians)
        val b = chroma * sin(hueRadians)

        val lCbrt = lightness + 0.3963377774f * a + 0.2158037573f * b
        val mCbrt = lightness - 0.1055613458f * a - 0.0638541728f * b
        val sCbrt = lightness - 0.0894841775f * a - 1.2914855480f * b

        val l = lCbrt * lCbrt * lCbrt
        val m = mCbrt * mCbrt * mCbrt
        val s = sCbrt * sCbrt * sCbrt

        return floatArrayOf(
            4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
            -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
            -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s
        )
    }

    private fun srgbToLinear(channel: Float): Float {
        return if (channel <= 0.04045f) {
            channel / 12.92f
        } else {
            ((channel + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    private fun linearToSrgb(channel: Float): Float {
        return if (channel <= 0.0031308f) {
            12.92f * channel
        } else {
            1.055f * channel.pow(1f / 2.4f) - 0.055f
        }
    }
}
