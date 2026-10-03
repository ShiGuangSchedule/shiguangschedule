package com.xingheyuzhuan.shiguangschedule.tool

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import androidx.core.graphics.toColorInt
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.app_name
import shiguangschedule.shared.generated.resources.course_teacher_prefix

actual object CalendarAccountManager : KoinComponent {

    private const val ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL

    // 通过 Koin 动态注入全局 Application Context
    private val context: Context by inject()

    private suspend fun getOrCreateCalendarId(): Long {
        val contentResolver = context.contentResolver
        val accountName = "${context.packageName}.account"

        val projection = arrayOf(Calendars._ID)
        val selection = "${Calendars.ACCOUNT_NAME} = ? AND ${Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(accountName, ACCOUNT_TYPE)

        val cursor: Cursor? = contentResolver.query(Calendars.CONTENT_URI, projection, selection, selectionArgs, null)
        cursor?.use {
            if (it.moveToFirst()) return it.getLong(0)
        }

        val calendarDisplayName = getString(Res.string.app_name)
        val values = ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, accountName)
            put(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            put(Calendars.NAME, accountName)
            put(Calendars.CALENDAR_DISPLAY_NAME, calendarDisplayName)
            put(Calendars.CALENDAR_COLOR, "#4285F4".toColorInt())
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, accountName)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
            put(Calendars.CALENDAR_TIME_ZONE, java.util.TimeZone.getDefault().id)
            put(Calendars.CAN_ORGANIZER_RESPOND, 1)
        }

        val uri: Uri = Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, accountName)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .build()

        return try {
            val resultUri = contentResolver.insert(uri, values)
            resultUri?.let { ContentUris.parseId(it) } ?: -1L
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    actual suspend fun syncCurrentTableToSystemCalendar(
        courses: List<CourseWithWeeks>,
        getTimeSlotsForDate: suspend (LocalDate) -> List<TimeSlot>,
        semesterStartDate: LocalDate,
        semesterTotalWeeks: Int,
        firstDayOfWeekInt: Int,
        alarmMinutes: Int?,
        isHolidayDate: suspend (LocalDate) -> Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val calendarId = getOrCreateCalendarId()
                if (calendarId == -1L) return@withContext false

                val resolver = context.contentResolver
                resolver.delete(
                    CalendarContract.Events.CONTENT_URI,
                    "${CalendarContract.Events.CALENDAR_ID} = ?",
                    arrayOf(calendarId.toString())
                )

                if (semesterTotalWeeks <= 0 || courses.isEmpty()) {
                    return@withContext true
                }

                val ops = ArrayList<ContentProviderOperation>()
                val timeZone = TimeZone.currentSystemDefault()

                IcsExportTool.processCourseInstances(
                    courses = courses,
                    getTimeSlotsForDate = getTimeSlotsForDate,
                    semesterStartDate = semesterStartDate,
                    semesterTotalWeeks = semesterTotalWeeks,
                    firstDayOfWeekInt = firstDayOfWeekInt,
                    isHolidayDate = isHolidayDate
                ) { course, start, end, _ ->
                    val startMillis = start.toInstant(timeZone).toEpochMilliseconds()
                    val endMillis = end.toInstant(timeZone).toEpochMilliseconds()
                    val eventOpIndex = ops.size

                    val teacherDescription = if (course.teacher.isNotBlank()) {
                        getString(Res.string.course_teacher_prefix, course.teacher)
                    } else ""

                    ops.add(ContentProviderOperation.newInsert(CalendarContract.Events.CONTENT_URI)
                        .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                        .withValue(CalendarContract.Events.TITLE, course.name)
                        .withValue(CalendarContract.Events.EVENT_LOCATION, course.position)
                        .withValue(CalendarContract.Events.DESCRIPTION, teacherDescription)
                        .withValue(CalendarContract.Events.DTSTART, startMillis)
                        .withValue(CalendarContract.Events.DTEND, endMillis)
                        .withValue(CalendarContract.Events.EVENT_TIMEZONE, timeZone.id)
                        .withValue(CalendarContract.Events.HAS_ALARM, if (alarmMinutes != null) 1 else 0)
                        .build())

                    if (alarmMinutes != null && alarmMinutes in 0..60) {
                        ops.add(ContentProviderOperation.newInsert(CalendarContract.Reminders.CONTENT_URI)
                            .withValueBackReference(CalendarContract.Reminders.EVENT_ID, eventOpIndex)
                            .withValue(CalendarContract.Reminders.MINUTES, alarmMinutes)
                            .withValue(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                            .build())
                    }
                }

                if (ops.isNotEmpty()) {
                    resolver.applyBatch(CalendarContract.AUTHORITY, ops)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}