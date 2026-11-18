package com.xingheyuzhuan.shiguangschedule

import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.DesktopWebViewController
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.SaveComboSchedulePayload
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.SaveConfigPayload
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.SaveCoursesPayload
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.SaveTimeSlotsPayload
import com.xingheyuzhuan.shiguangschedule.ui.schoolselection.web.bridgeJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DesktopWebBridgeManualImportTest {

    @Test
    fun testParseManualImportPayloadFromCourseArray() {
        val rawCoursesJson = """
            [
                {
                    "name": "计算机网络",
                    "teacher": "刘教授",
                    "position": "理教201",
                    "day": 3,
                    "startSection": 1,
                    "endSection": 2,
                    "weeks": [1, 2, 3, 4, 5, 6, 7, 8]
                }
            ]
        """.trimIndent()

        val messages = DesktopWebViewController.parseManualImportPayload(rawCoursesJson)
        assertEquals(2, messages.size)
        assertEquals("saveImportedCourses", messages[0].action)
        assertEquals("manual_courses", messages[0].callbackId)
        assertNotNull(messages[0].payload)
        val decodedPayload = bridgeJson.decodeFromString<SaveCoursesPayload>(messages[0].payload!!)
        assertTrue(decodedPayload.coursesJsonString.contains("计算机网络"))
        assertEquals("notifyTaskCompletion", messages[1].action)
    }

    @Test
    fun testParseManualImportPayloadFromFullExportedBundle() {
        val bundleJson = """
            {
                "courses": [
                    {
                        "name": "操作系统",
                        "teacher": "陈老师",
                        "position": "工学院302",
                        "day": 2,
                        "startSection": 3,
                        "endSection": 4,
                        "weeks": [1, 3, 5, 7]
                    }
                ],
                "timeSlots": [
                    {
                        "number": 1,
                        "startTime": "08:00",
                        "endTime": "08:45"
                    }
                ],
                "config": {
                    "semesterStartDate": "2026-09-01",
                    "semesterTotalWeeks": 18,
                    "defaultClassDuration": 45,
                    "defaultBreakDuration": 10,
                    "firstDayOfWeek": 1
                },
                "comboSchedule": {
                    "name": "夏令作息",
                    "publicSchedules": []
                }
            }
        """.trimIndent()

        val messages = DesktopWebViewController.parseManualImportPayload(bundleJson)
        // 严格顺序：savePresetTimeSlots -> saveComboSchedule -> saveCourseConfig -> saveImportedCourses -> notifyTaskCompletion
        assertEquals(
            listOf(
                "savePresetTimeSlots",
                "saveComboSchedule",
                "saveCourseConfig",
                "saveImportedCourses",
                "notifyTaskCompletion"
            ),
            messages.map { it.action }
        )

        val slotsPayload = bridgeJson.decodeFromString<SaveTimeSlotsPayload>(messages[0].payload!!)
        assertTrue(slotsPayload.timeSlotsJsonString.contains("08:00"))

        val comboPayload = bridgeJson.decodeFromString<SaveComboSchedulePayload>(messages[1].payload!!)
        assertTrue(comboPayload.comboScheduleJsonString.contains("夏令作息"))

        val configPayload = bridgeJson.decodeFromString<SaveConfigPayload>(messages[2].payload!!)
        assertTrue(configPayload.configJsonString.contains("2026-09-01"))

        val coursesPayload = bridgeJson.decodeFromString<SaveCoursesPayload>(messages[3].payload!!)
        assertTrue(coursesPayload.coursesJsonString.contains("操作系统"))
    }

    @Test
    fun testParseManualImportPayloadFromBridgeMessagesAndEdgeCases() {
        val singleBridgeMsg = """{"action":"showToast","callbackId":null,"payload":"{\"message\":\"ok\"}"}"""
        val singleParsed = DesktopWebViewController.parseManualImportPayload(singleBridgeMsg)
        assertEquals(1, singleParsed.size)
        assertEquals("showToast", singleParsed.first().action)

        val arrayBridgeMsg = """[{"action":"showToast","payload":"{\"message\":\"step1\"}"},{"action":"notifyTaskCompletion"}]"""
        val arrayParsed = DesktopWebViewController.parseManualImportPayload(arrayBridgeMsg)
        assertEquals(listOf("showToast", "notifyTaskCompletion"), arrayParsed.map { it.action })

        // 边界异常情况测试
        assertFailsWith<IllegalArgumentException> {
            DesktopWebViewController.parseManualImportPayload("   ")
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopWebViewController.parseManualImportPayload("{invalid_json")
        }
        assertFailsWith<IllegalArgumentException> {
            DesktopWebViewController.parseManualImportPayload("""{"foo":"bar"}""")
        }
    }
}
