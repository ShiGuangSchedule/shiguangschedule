package com.xingheyuzhuan.shiguangschedule.tool

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.di.AppStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import org.koin.core.context.GlobalContext
import java.awt.Desktop
import java.io.File

actual object CalendarAccountManager {
    /**
     * 桌面端日历同步实现：生成标准 .ics 日历文件并调用系统默认日历应用（如 Windows 日历 / Outlook / Apple Calendar）打开导入。
     */
    actual suspend fun syncCurrentTableToSystemCalendar(
        courses: List<CourseWithWeeks>,
        getTimeSlotsForDate: suspend (LocalDate) -> List<TimeSlot>,
        semesterStartDate: LocalDate,
        semesterTotalWeeks: Int,
        firstDayOfWeekInt: Int,
        alarmMinutes: Int?,
        isHolidayDate: suspend (LocalDate) -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val icsContent = IcsExportTool.generateIcsFileContent(
                courses = courses,
                getTimeSlotsForDate = getTimeSlotsForDate,
                semesterStartDate = semesterStartDate,
                semesterTotalWeeks = semesterTotalWeeks,
                firstDayOfWeekInt = firstDayOfWeekInt,
                alarmMinutes = alarmMinutes,
                isHolidayDate = isHolidayDate
            )

            val baseDir = runCatching {
                File(GlobalContext.get().get<AppStorage>().cacheDir.toString(), "calendar_sync")
            }.getOrElse {
                File(System.getProperty("java.io.tmpdir"), "shiguangschedule_calendar")
            }.apply { mkdirs() }

            val icsFile = File(baseDir, "shiguangschedule_sync.ics")
            icsFile.writeText(icsContent, Charsets.UTF_8)

            openWithSystemDefault(icsFile)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun openWithSystemDefault(file: File): Boolean {
        if (!file.exists()) return false
        try {
            if (Desktop.isDesktopSupported()) {
                val desktop = Desktop.getDesktop()
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(file)
                    return true
                }
            }
        } catch (_: Exception) {
        }

        return try {
            val osName = System.getProperty("os.name").orEmpty().lowercase()
            val command = when {
                osName.contains("win") -> listOf("cmd", "/c", "start", "", file.absolutePath)
                osName.contains("mac") -> listOf("open", file.absolutePath)
                else -> listOf("xdg-open", file.absolutePath)
            }
            ProcessBuilder(command).start()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}