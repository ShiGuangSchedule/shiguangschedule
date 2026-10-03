package com.xingheyuzhuan.shiguangschedule.data.api.date

import com.xingheyuzhuan.shiguangschedule.data.model.Holiday
import com.xingheyuzhuan.shiguangschedule.data.repository.AppSettingsRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock

@Serializable
data class ApiResponse(
    @SerialName("code")
    val code: Int = 0,
    @SerialName("holiday")
    val holidays: Map<String, HolidayInfo> = emptyMap()
)

@Serializable
data class HolidayInfo(
    @SerialName("name")
    val name: String,
    @SerialName("holiday")
    val isHoliday: Boolean,
    @SerialName("date")
    val date: String
)

/**
 * API 节假日导入工具，基于 Ktor 3.0 实现。
 */
object ApiDateImporter {
    private const val BASE_URL = "https://timor.tech/api/holiday/year/"

    private val jsonInstance = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(Logging) {
            level = LogLevel.INFO
            logger = Logger.DEFAULT
        }

        install(ContentNegotiation) {
            json(jsonInstance)
        }

        defaultRequest {
            header("User-Agent", "Mozilla/5.0 (Linux; Android 10; SM-G973F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36")
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 15000
        }
    }

    /**
     * 请求指定年份的节假日数据
     */
    private suspend fun fetchHolidayDataForYear(year: Int): Map<String, HolidayInfo> {
        return try {
            val url = "$BASE_URL$year"
            val response: ApiResponse = client.get(url).body()
            if (response.code == 0) {
                response.holidays
            } else {
                emptyMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * 从 API 获取节假日/调休数据，按名称聚合后保存到 AppSettingsRepository 中。
     */
    suspend fun importAndSaveSkippedDates(appSettingsRepository: AppSettingsRepository) {
        try {
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val currentYear = today.year
            val currentMonth = today.month.number

            val combinedHolidayMap = fetchHolidayDataForYear(currentYear).toMutableMap()

            if (currentMonth >= 11) {
                val nextYearHolidays = fetchHolidayDataForYear(currentYear + 1)
                combinedHolidayMap.putAll(nextYearHolidays)
            }

            if (combinedHolidayMap.isEmpty()) {
                return
            }

            val groupedHolidays: List<Holiday> = combinedHolidayMap.values
                .groupBy { info -> Pair(info.name, info.isHoliday) }
                .mapNotNull { (key, infoList) ->
                    val (name, isHoliday) = key
                    val datesSet = infoList.mapNotNull { info ->
                        try {
                            LocalDate.parse(info.date)
                        } catch (_: Exception) {
                            null
                        }
                    }.toSet()

                    if (datesSet.isNotEmpty()) {
                        Holiday(
                            name = name,
                            isHoliday = isHoliday,
                            dates = datesSet
                        )
                    } else {
                        null
                    }
                }

            val currentSettings = appSettingsRepository.getAppSettings().first()
            val updatedSettings = currentSettings.copy(holidays = groupedHolidays)
            appSettingsRepository.insertOrUpdateAppSettings(updatedSettings)
        } catch (_: Exception) {
        }
    }

    fun close() = client.close()
}