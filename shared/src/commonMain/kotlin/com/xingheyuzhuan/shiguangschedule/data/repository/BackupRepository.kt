package com.xingheyuzhuan.shiguangschedule.data.repository

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfigDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBindingDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeekDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlotDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableComboDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableDao
import com.xingheyuzhuan.shiguangschedule.data.model.CourseImportExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import shiguangschedule.shared.generated.resources.Res
import shiguangschedule.shared.generated.resources.backup_err_corrupted
import shiguangschedule.shared.generated.resources.backup_err_empty
import shiguangschedule.shared.generated.resources.backup_err_version_too_new
import shiguangschedule.shared.generated.resources.backup_err_version_too_old
import kotlin.time.Clock

/**
 * 模块化备份定义
 */
enum class BackupModule(val key: String) {
    COURSE("course"),
    STYLE("style")
}

/**
 * 各模块的物理隔离载体
 */
data class AppBackupPackage(
    val meta: BackupMeta,
    val payloadMap: Map<String, ByteArray>
)

@Serializable
data class BackupMeta(
    val backupTimestamp: Long,
    val appVersionCode: Int,
    val appVersionName: String,
    val modules: List<ModuleInfo>
)

@Serializable
data class ModuleInfo(
    val key: String,
    val schemaVersion: Int
)

/**
 * 备份与恢复的中央总仓库（KMP 共享层）
 */
@Single
class BackupRepository(
    @Named("AppVersionCode") private val appVersionCode: Int,
    @Named("AppVersionName") private val appVersionName: String,
    private val appSettingsRepository: AppSettingsRepository,
    private val styleSettingsRepository: StyleSettingsRepository,
    private val courseTableDao: CourseTableDao,
    private val courseTableConfigDao: CourseTableConfigDao,
    private val courseDao: CourseDao,
    private val courseWeekDao: CourseWeekDao,
    private val courseTimeBindingDao: CourseTimeBindingDao,
    private val timeTableDao: TimeTableDao,
    private val timeSlotDao: TimeSlotDao,
    private val timeTableComboDao: TimeTableComboDao
) {

    /**
     * 构建全软件多模块统一内存备份包
     */
    suspend fun createFullSoftwareBackup(modules: List<BackupModule>): AppBackupPackage? = withContext(Dispatchers.IO) {
        try {
            val payloadMap = mutableMapOf<String, ByteArray>()
            val moduleInfos = mutableListOf<ModuleInfo>()

            modules.forEach { module ->
                when (module) {
                    BackupModule.COURSE -> {
                        exportAllCourseTablesCbor()?.let {
                            payloadMap[module.key] = it
                            moduleInfos.add(ModuleInfo(module.key, CourseImportExport.COURSE_SCHEMA_VERSION))
                        }
                    }
                    BackupModule.STYLE -> {
                        exportAppStyleBytes()?.let {
                            payloadMap[module.key] = it
                            moduleInfos.add(ModuleInfo(module.key, StyleSettingsRepository.STYLE_SCHEMA_VERSION))
                        }
                    }
                }
            }

            if (payloadMap.isEmpty()) return@withContext null

            AppBackupPackage(
                meta = BackupMeta(
                    backupTimestamp = Clock.System.now().toEpochMilliseconds(),
                    appVersionCode = appVersionCode,
                    appVersionName = appVersionName,
                    modules = moduleInfos
                ),
                payloadMap = payloadMap
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 原子化分发恢复网关
     */
    suspend fun restoreFullSoftwareBackup(backupPackage: AppBackupPackage): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            backupPackage.meta.modules.forEach { info ->
                when (info.key) {
                    BackupModule.COURSE.key -> {
                        if (info.schemaVersion > CourseImportExport.COURSE_SCHEMA_VERSION) {
                            return@withContext Result.failure(IllegalStateException(getString(Res.string.backup_err_version_too_new)))
                        }
                        if (info.schemaVersion < CourseImportExport.COURSE_SCHEMA_VERSION) {
                            return@withContext Result.failure(IllegalStateException(getString(Res.string.backup_err_version_too_old)))
                        }
                    }
                    BackupModule.STYLE.key -> {
                        if (info.schemaVersion > StyleSettingsRepository.STYLE_SCHEMA_VERSION) {
                            return@withContext Result.failure(IllegalStateException(getString(Res.string.backup_err_version_too_new)))
                        }
                    }
                }
            }
            backupPackage.meta.modules.forEach { info ->
                val data = backupPackage.payloadMap[info.key] ?: return@forEach
                val result = when (info.key) {
                    BackupModule.COURSE.key -> restoreAllCourseTablesCbor(data)
                    BackupModule.STYLE.key -> restoreAppStyleBytes(data)
                    else -> Result.success(Unit)
                }
                if (result.isFailure) return@withContext result
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 1. 课表核心业务通道（基于结构化 DAO 读写）

    /**
     * 导出所有课表数据并序列化为 CBOR 字节数组
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun exportAllCourseTablesCbor(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val appSettings = appSettingsRepository.getAppSettingsOnce()

            // 使用 CourseImportExport 中定义的总信封及实体载体
            val envelope = CourseImportExport.TotalAppBackupEnvelope(
                backupTimestamp = Clock.System.now().toEpochMilliseconds(),
                appVersionCode = CourseImportExport.COURSE_SCHEMA_VERSION,
                currentCourseTableId = appSettings.currentCourseTableId,
                databasePayload = CourseImportExport.CourseDatabasePayload(
                    courseTables = courseTableDao.getAll(),
                    courseTableConfigs = courseTableConfigDao.getAll(),
                    courses = courseDao.getAll(),
                    courseWeeks = courseWeekDao.getAll(),
                    courseTimeBindings = courseTimeBindingDao.getAll(),
                    timeTables = timeTableDao.getAll(),
                    timeSlots = timeSlotDao.getAll(),
                    timeTableCombos = timeTableComboDao.getAll(),
                    timeTableComboRules = timeTableComboDao.getAllRules()
                )
            )

            CourseImportExport.cbor.encodeToByteArray(CourseImportExport.TotalAppBackupEnvelope.serializer(), envelope)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析 CBOR 字节数组并通过 DAO 全量恢复课表数据
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun restoreAllCourseTablesCbor(cborBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (cborBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException(getString(Res.string.backup_err_empty)))
            }

            val envelope = try {
                CourseImportExport.cbor.decodeFromByteArray(CourseImportExport.TotalAppBackupEnvelope.serializer(), cborBytes)
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalStateException(getString(Res.string.backup_err_corrupted)))
            }

            if (envelope.appVersionCode > CourseImportExport.COURSE_SCHEMA_VERSION) {
                return@withContext Result.failure(IllegalStateException(getString(Res.string.backup_err_version_too_new)))
            }

            if (envelope.appVersionCode < CourseImportExport.COURSE_SCHEMA_VERSION) {
                return@withContext Result.failure(IllegalStateException(getString(Res.string.backup_err_version_too_old)))
            }

            val payload = envelope.databasePayload

            // 清空旧数据（依外键从属倒序清理）
            courseWeekDao.clearAll()
            courseDao.clearAll()
            courseTableConfigDao.clearAll()
            courseTimeBindingDao.clearAll()
            timeTableComboDao.clearAllRules()
            timeTableComboDao.clearAll()
            timeSlotDao.clearAll()
            timeTableDao.clearAll()
            courseTableDao.clearAll()

            // 写入新数据（依主从关系正序写入）
            if (payload.courseTables.isNotEmpty()) {
                payload.courseTables.forEach { courseTableDao.insert(it) }
            }
            if (payload.timeTables.isNotEmpty()) {
                payload.timeTables.forEach { timeTableDao.insert(it) }
            }
            if (payload.timeSlots.isNotEmpty()) {
                timeSlotDao.insertAll(payload.timeSlots)
            }
            if (payload.courseTables.isNotEmpty()) {
                payload.courseTables.forEach { table ->
                    payload.courseTableConfigs.find { it.courseTableId == table.id }?.let {
                        courseTableConfigDao.insertOrUpdate(it)
                    }
                }
            }
            if (payload.courses.isNotEmpty()) {
                courseDao.insertAll(payload.courses)
            }
            if (payload.courseWeeks.isNotEmpty()) {
                courseWeekDao.insertAll(payload.courseWeeks)
            }
            if (payload.courseTimeBindings.isNotEmpty()) {
                payload.courseTimeBindings.forEach { courseTimeBindingDao.insertOrUpdate(it) }
            }
            if (payload.timeTableCombos.isNotEmpty()) {
                payload.timeTableCombos.forEach { timeTableComboDao.insertCombo(it) }
            }
            if (payload.timeTableComboRules.isNotEmpty()) {
                timeTableComboDao.insertRules(payload.timeTableComboRules)
            }

            val currentSettings = appSettingsRepository.getAppSettingsOnce()
            envelope.currentCourseTableId.let { appSettingsRepository.insertOrUpdateAppSettings(currentSettings.copy(currentCourseTableId = it)) }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. 个性化样式核心业务通道

    /**
     * 导出样式独立通道
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun exportAppStyleBytes(): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val rawProtoBytes = styleSettingsRepository.exportRawStyleBytes()
            val envelope = StyleBackupEnvelope(
                backupTimestamp = Clock.System.now().toEpochMilliseconds(),
                appVersionCode = StyleSettingsRepository.STYLE_SCHEMA_VERSION,
                styleProtoBytes = rawProtoBytes
            )
            CourseImportExport.cbor.encodeToByteArray(StyleBackupEnvelope.serializer(), envelope)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 样式恢复独立通道
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend fun restoreAppStyleBytes(styleBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (styleBytes.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException(getString(Res.string.backup_err_empty)))
            }

            val envelope = try {
                CourseImportExport.cbor.decodeFromByteArray(StyleBackupEnvelope.serializer(), styleBytes)
            } catch (_: Exception) {
                return@withContext Result.failure(IllegalStateException(getString(Res.string.backup_err_corrupted)))
            }

            if (envelope.appVersionCode > StyleSettingsRepository.STYLE_SCHEMA_VERSION) {
                return@withContext Result.failure(IllegalStateException(getString(Res.string.backup_err_version_too_new)))
            }

            val migratedProtoBytes = if (envelope.appVersionCode < StyleSettingsRepository.STYLE_SCHEMA_VERSION) {
                envelope.styleProtoBytes
            } else {
                envelope.styleProtoBytes
            }
            styleSettingsRepository.restoreRawStyleBytes(migratedProtoBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}