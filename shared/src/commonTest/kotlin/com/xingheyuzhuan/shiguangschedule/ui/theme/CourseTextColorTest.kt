package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.StackClusterOrder
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.CourseTextColorModeProto
import com.xingheyuzhuan.shiguangschedule.data.model.toCompose
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CourseTextColorTest {

    private val lightCards = listOf(
        Color(0xFFFFCC99),
        Color(0xFFFFE699),
        Color(0xFFE6FF99),
        Color(0xFF99FFFF),
        Color(0xFFB399FF),
        Color(0xFFFF99E6)
    )

    private val darkCards = listOf(
        Color(0xFF663300),
        Color(0xFF664D00),
        Color(0xFF336600),
        Color(0xFF006666),
        Color(0xFF1A0066),
        Color(0xFF66004D)
    )

    @Test
    fun tonalTextMeetsAccessibleContrastOnLightCards() {
        lightCards.forEach { background ->
            val colors = CourseTextColorUtils.tonalColors(background)
            assertTrue(
                CourseTextColorUtils.contrastRatio(colors.primary, background) >= 4.5f,
                "primary contrast failed for $background"
            )
            assertTrue(
                CourseTextColorUtils.contrastRatio(colors.secondary, background) >= 4.5f,
                "secondary contrast failed for $background"
            )
        }
    }

    @Test
    fun tonalTextMeetsAccessibleContrastOnDarkCards() {
        darkCards.forEach { background ->
            val colors = CourseTextColorUtils.tonalColors(background)
            assertTrue(
                CourseTextColorUtils.contrastRatio(colors.primary, background) >= 4.5f,
                "primary contrast failed for $background"
            )
            assertTrue(
                CourseTextColorUtils.contrastRatio(colors.secondary, background) >= 4.5f,
                "secondary contrast failed for $background"
            )
        }
    }

    @Test
    fun lightCardsUseDarkTextAndDarkCardsUseLightText() {
        lightCards.forEach { background ->
            val primary = CourseTextColorUtils.tonalColors(background).primary
            assertTrue(
                CourseTextColorUtils.relativeLuminance(primary) <
                    CourseTextColorUtils.relativeLuminance(background),
                "expected dark text on $background"
            )
        }
        darkCards.forEach { background ->
            val primary = CourseTextColorUtils.tonalColors(background).primary
            assertTrue(
                CourseTextColorUtils.relativeLuminance(primary) >
                    CourseTextColorUtils.relativeLuminance(background),
                "expected light text on $background"
            )
        }
    }

    @Test
    fun translucentCardKeepsCourseHue() {
        val source = Color(0xFFFFCC99)
        val paleBackground = source.copy(alpha = 0.18f).compositeOver(Color.White)
        val colors = CourseTextColorUtils.tonalColors(
            background = paleBackground,
            tintStrength = 0.8f,
            contrastTarget = 6f,
            sourceColor = source
        )
        assertTrue(
            CourseTextColorUtils.contrastRatio(colors.primary, paleBackground) >= 4.5f,
            "primary contrast failed on translucent card"
        )
        // 橙色课程即使在接近白色的背景上也应得到偏橙的文字，而不是灰色
        assertTrue(colors.primary.red > colors.primary.blue + 0.15f)
    }

    @Test
    fun globalModeKeepsConfiguredColor() {
        val global = Color(0xFF123456)
        val colors = CourseTextColorUtils.resolve(
            background = Color.White,
            mode = CourseTextColorModeProto.COURSE_TEXT_COLOR_GLOBAL,
            globalColor = global,
            fallback = Color.Black
        )
        assertEquals(global, colors.primary)
        assertEquals(0.8f, colors.secondary.alpha)
    }

    @Test
    fun textColorModeRoundTripsThroughProto() {
        val style = ScheduleGridStyle.DEFAULT.copy(
            courseTextColorMode = CourseTextColorModeProto.COURSE_TEXT_COLOR_TONAL
        )
        val restored = style.toProto().toCompose()
        assertEquals(CourseTextColorModeProto.COURSE_TEXT_COLOR_TONAL, restored.courseTextColorMode)
    }

    @Test
    fun clusterOrdersRoundTripThroughProto() {
        val style = ScheduleGridStyle.DEFAULT.copy(
            stackConflictCourses = true,
            stackClusterOrders = listOf(
                StackClusterOrder(clusterKey = "1|a,b", courseIds = listOf("b", "a"))
            )
        )
        val restored = style.toProto().toCompose()
        assertTrue(restored.stackConflictCourses)
        assertEquals(style.stackClusterOrders, restored.stackClusterOrders)
    }

    @Test
    fun zeroTintProducesNeutralText() {
        val primary = CourseTextColorUtils.tonalColors(
            background = Color(0xFFFFCC99),
            tintStrength = 0f
        ).primary
        val maxChannel = maxOf(primary.red, primary.green, primary.blue)
        val minChannel = minOf(primary.red, primary.green, primary.blue)
        assertTrue(maxChannel - minChannel < 0.02f, "tint 0 should be neutral")
    }

    @Test
    fun tintStrengthIsApplied() {
        val neutral = CourseTextColorUtils.tonalColors(Color(0xFFFFCC99), tintStrength = 0f).primary
        val tinted = CourseTextColorUtils.tonalColors(Color(0xFFFFCC99), tintStrength = 1f).primary
        val neutralSpread = maxOf(neutral.red, neutral.green, neutral.blue) - minOf(neutral.red, neutral.green, neutral.blue)
        val tintedSpread = maxOf(tinted.red, tinted.green, tinted.blue) - minOf(tinted.red, tinted.green, tinted.blue)
        assertTrue(tintedSpread > neutralSpread)
    }

    @Test
    fun higherContrastTargetIsRespectedWhenPossible() {
        val background = Color(0xFFFFCC99)
        val primary = CourseTextColorUtils.tonalColors(
            background = background,
            tintStrength = 0.8f,
            contrastTarget = 8f
        ).primary
        assertTrue(CourseTextColorUtils.contrastRatio(primary, background) >= 7.5f)
    }

    @Test
    fun tintParametersRoundTripThroughProto() {
        val style = ScheduleGridStyle.DEFAULT.copy(
            courseTextTintStrength = 0.35f,
            courseTextContrastRatio = 7.5f
        )
        val restored = style.toProto().toCompose()
        assertEquals(0.35f, restored.courseTextTintStrength)
        assertEquals(7.5f, restored.courseTextContrastRatio)
    }
}
