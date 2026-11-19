package com.xingheyuzhuan.shiguangschedule

import com.xingheyuzhuan.shiguangschedule.tool.ApiAsset
import com.xingheyuzhuan.shiguangschedule.tool.DesktopExportTracker
import com.xingheyuzhuan.shiguangschedule.tool.PlatformUpdateStrategy
import com.xingheyuzhuan.shiguangschedule.ui.components.cropBufferedImageToJpeg
import com.xingheyuzhuan.shiguangschedule.ui.components.resolveDesktopShareTargetFile
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.mapDayToDisplayIndex
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.mapDisplayIndexToDay
import com.xingheyuzhuan.shiguangschedule.ui.schedule.components.rearrangeDays
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.DesktopWebViewController
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.buildJsCallbackScript
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DesktopFeaturesTest {

    @Test
    fun testSelectDesktopAssetUrlMatchesOsAndArch() {
        val assets = listOf(
            ApiAsset("shiguangschedule-v2.1.0-arm64-v8a.apk", "https://example.com/android-arm64.apk"),
            ApiAsset("拾光课程表-v2.1.0-windows-arm64.exe", "https://example.com/win-arm64.exe"),
            ApiAsset("拾光课程表-v2.1.0-windows-x64.exe", "https://example.com/win-x64.exe"),
            ApiAsset("拾光课程表-v2.1.0-windows-x64.zip", "https://example.com/win-x64.zip"),
            ApiAsset("拾光课程表-v2.1.0-macos-arm64.dmg", "https://example.com/mac-arm64.dmg"),
            ApiAsset("拾光课程表-v2.1.0-macos-x64.dmg", "https://example.com/mac-x64.dmg"),
            ApiAsset("拾光课程表-v2.1.0-linux-x64.deb", "https://example.com/linux-x64.deb")
        )

        assertEquals(
            "https://example.com/win-x64.exe",
            PlatformUpdateStrategy.selectDesktopAssetUrl(assets, "Windows 11", "amd64")
        )
        assertEquals(
            "https://example.com/win-arm64.exe",
            PlatformUpdateStrategy.selectDesktopAssetUrl(assets, "Windows 11", "aarch64")
        )
        assertEquals(
            "https://example.com/mac-arm64.dmg",
            PlatformUpdateStrategy.selectDesktopAssetUrl(assets, "Mac OS X", "aarch64")
        )
        assertEquals(
            "https://example.com/mac-x64.dmg",
            PlatformUpdateStrategy.selectDesktopAssetUrl(assets, "Mac OS X", "x86_64")
        )
        assertEquals(
            "https://example.com/linux-x64.deb",
            PlatformUpdateStrategy.selectDesktopAssetUrl(assets, "Linux", "x86_64")
        )
        assertNull(
            PlatformUpdateStrategy.selectDesktopAssetUrl(emptyList(), "Windows 11", "amd64")
        )
        assertNull(
            PlatformUpdateStrategy.selectDesktopAssetUrl(assets, "FreeBSD", "amd64")
        )

        // 验证 Windows 下不会误匹配仅含 macos/linux 标识或未知的 .zip 文件
        val crossOsZips = listOf(
            ApiAsset("shiguangschedule-v2.1.0-macos-x64.zip", "https://example.com/macos-x64.zip"),
            ApiAsset("offline_schools.zip", "https://example.com/offline_schools.zip"),
            ApiAsset("拾光课程表-v2.1.0-windows-x64.zip", "https://example.com/win-x64.zip")
        )
        assertEquals(
            "https://example.com/win-x64.zip",
            PlatformUpdateStrategy.selectDesktopAssetUrl(crossOsZips, "Windows 11", "amd64")
        )
        assertNull(
            PlatformUpdateStrategy.selectDesktopAssetUrl(
                listOf(ApiAsset("shiguangschedule-v2.1.0-macos-x64.zip", "https://example.com/macos-x64.zip")),
                "Windows 11",
                "amd64"
            )
        )
    }

    @Test
    fun testResolveDesktopShareTargetFileRedirectsShareTempToActualExportedFile() {
        val savedFile = File.createTempFile("shiguang_export_test_", ".json")
        savedFile.writeText("{}")
        val previous = DesktopExportTracker.lastExportedFilePath
        try {
            DesktopExportTracker.lastExportedFilePath = savedFile.absolutePath
            val resolved = resolveDesktopShareTargetFile(
                "C:/Users/test/AppData/Local/cache/share_temp/shiguangschedule_123.json"
            )
            assertEquals(savedFile.absolutePath, resolved.absolutePath)
        } finally {
            DesktopExportTracker.lastExportedFilePath = previous
            savedFile.delete()
        }
    }

    @Test
    fun testCropImageExtractsExactSubregionAndClampsBounds() {
        val source = BufferedImage(200, 120, BufferedImage.TYPE_INT_RGB)

        // 1. 正常子区域裁剪
        val croppedBytes = cropBufferedImageToJpeg(
            source = source,
            srcLeft = 30,
            srcTop = 20,
            cropWidth = 80,
            cropHeight = 50
        )
        assertTrue(croppedBytes.isNotEmpty())
        val decoded = ImageIO.read(ByteArrayInputStream(croppedBytes))
        assertEquals(80, decoded.width)
        assertEquals(50, decoded.height)

        // 2. 越界参数边界安全裁剪（负数起点与超出原图尺寸）
        val clampedBytes = cropBufferedImageToJpeg(
            source = source,
            srcLeft = -50,
            srcTop = -30,
            cropWidth = 500,
            cropHeight = 400
        )
        val decodedClamped = ImageIO.read(ByteArrayInputStream(clampedBytes))
        assertEquals(200, decodedClamped.width)
        assertEquals(120, decodedClamped.height)
    }

    @Test
    fun testDesktopWebViewBridgeProtocolAndServerLifecycle() {
        val successScript = buildJsCallbackScript("cb_1_100", true, "\"ok_value\"")
        val parsedSuccess = DesktopWebViewController.parseNativeCallbackScript(successScript)
        assertNotNull(parsedSuccess)
        assertEquals("cb_1_100", parsedSuccess.first)
        assertEquals(true, parsedSuccess.second)
        assertEquals("\"ok_value\"", parsedSuccess.third)

        val failScript = buildJsCallbackScript("cb_2_200", false, "\"err_msg\"")
        val parsedFail = DesktopWebViewController.parseNativeCallbackScript(failScript)
        assertNotNull(parsedFail)
        assertEquals("cb_2_200", parsedFail.first)
        assertEquals(false, parsedFail.second)
        assertEquals("\"err_msg\"", parsedFail.third)

        assertNull(DesktopWebViewController.parseNativeCallbackScript("console.log('noop')"))

        val controller = DesktopWebViewController()
        controller.startLocalBridgeServer()
        try {
            assertTrue(controller.bridgePort > 0, "Bridge server port should be > 0")
            val generated = DesktopWebViewController.buildDesktopBrowserImportScript(
                wrappedAdapterJs = "window.shiguangBridge.notifyTaskCompletion();",
                port = controller.bridgePort
            )
            assertTrue(generated.contains("http://127.0.0.1:${controller.bridgePort}/bridge"))
            assertTrue(generated.contains("window.shiguangBridgePromise"))
        } finally {
            controller.stopLocalBridgeServer()
            assertEquals(0, controller.bridgePort)
        }
    }

    @Test
    fun testScheduleGridDayMappingRoundTrip() {
        val days = listOf("一", "二", "三", "四", "五", "六", "日")
        assertEquals(listOf("一", "二", "三", "四", "五", "六", "日"), rearrangeDays(days, 1))
        assertEquals(listOf("日", "一", "二", "三", "四", "五", "六"), rearrangeDays(days, 7))

        for (firstDay in listOf(1, 7)) {
            for (courseDay in 1..7) {
                val displayIdx = mapDayToDisplayIndex(courseDay, firstDay, showWeekends = true)
                assertTrue(displayIdx in 0..6)
                val restoredDay = mapDisplayIndexToDay(displayIdx, firstDay)
                assertEquals(courseDay, restoredDay)
            }
        }
        // 不显示周末时：周六(6)与周日(7)在周一为起始日时应返回 -1
        assertEquals(-1, mapDayToDisplayIndex(6, firstDayOfWeek = 1, showWeekends = false))
        assertEquals(-1, mapDayToDisplayIndex(7, firstDayOfWeek = 1, showWeekends = false))
    }
}
