package com.xingheyuzhuan.shiguangschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.xingheyuzhuan.shiguangschedule.data.model.SchoolHistoryModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.FileSystem
import okio.Path
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import school_index.Adapter
import school_index.AdapterCategory
import school_index.School
import school_index.SchoolIndex
import shiguangschedule.shared.generated.resources.Res

/**
 * 学校数据仓库。
 * 职责：处理内部存储中 Protobuf 学校索引文件的读取、解析与响应式流派发。
 */
@Single
class SchoolRepository(
    private val fileSystem: FileSystem,
    @Named("FilesDir") private val filesDir: Path
) {

    // 定义需要在一级菜单中显示的教务类别
    private val RELEVANT_MENU_CATEGORIES = setOf(
        AdapterCategory.BACHELOR_AND_ASSOCIATE,
        AdapterCategory.POSTGRADUATE,
        AdapterCategory.GENERAL_TOOL
    )

    // 用于触发文件重新加载信号的通知流
    private val _reloadSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * 手动触发仓库重新解析 Protobuf（例如更新仓库文件成功后调用）
     */
    fun refresh() {
        _reloadSignal.tryEmit(Unit)
    }

    /**
     * 核心加载函数：读取本地 Protobuf 索引，并始终叠加 App 内置学校索引片段。
     *
     * 叠加方式：`files/builtin_schools.pb` 只包含 `SchoolIndex.schools`（字段 3）的编码，
     * 直接追加到本地索引字节之后即可 —— 按 Protobuf 规范，repeated 字段重复出现视为追加，
     * 因此两份数据会被解码成同一份学校列表，而本地索引的 protocol_version / version_id
     * （字段 1/2）保持为本地值，不会影响 GitUpdater 的版本比较。
     *
     * 这样即使适配仓库的索引把本地索引整份覆盖，内置学校（如西北农林科技大学）也不会丢失。
     */
    private suspend fun loadIndex(): SchoolIndex? {
        return withContext(Dispatchers.IO) {
            val internalPath = filesDir / "repo/index/school_index.pb"

            val localBytes = if (fileSystem.exists(internalPath)) {
                runCatching { fileSystem.read(internalPath) { readByteArray() } }.getOrNull()
            } else {
                null
            }

            val builtinBytes = readBuiltinSchoolFragment()

            if (localBytes == null && builtinBytes == null) {
                println("错误：Protobuf 索引文件未找到: $internalPath")
                return@withContext null
            }

            try {
                val mergedSource = Buffer()
                localBytes?.let { mergedSource.write(it) }
                builtinBytes?.let { mergedSource.write(it) }
                return@withContext SchoolIndex.ADAPTER.decode(mergedSource)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 读取 App 内置的学校索引片段（由 tools/gen_builtin_schools.py 生成）。
     * 读取失败不影响适配仓库索引的正常使用。
     */
    @OptIn(ExperimentalResourceApi::class)
    private suspend fun readBuiltinSchoolFragment(): ByteArray? {
        return runCatching { Res.readBytes("files/builtin_schools.pb") }
            .onFailure { println("内置学校索引片段读取失败: ${it.message}") }
            .getOrNull()
    }

    /**
     * 【响应式核心】提供学校列表的观察 Flow。
     * 当收到 `refresh()` 信号或初始化时，自动读取本地文件并发射最新学校数据。
     */
    fun getSchoolsFlow(): Flow<List<School>> {
        return _reloadSignal
            .onStart { emit(Unit) } // 首次订阅时立即触发一次加载
            .map {
                getSchools()
            }
            .distinctUntilChanged()
    }

    /**
     * 获取经过类别过滤与排序的学校列表（同步/单次获取）。
     */
    suspend fun getSchools(): List<School> {
        val index = loadIndex() ?: return emptyList()

        // 内置片段追加在本地索引之后，若适配仓库里已有同 id 学校则以仓库那份为准（去重保留先出现的）
        val filteredSchools = index.schools
            .distinctBy { it.id }
            .filter { school ->
                school.adapters.any { adapter ->
                    adapter.category in RELEVANT_MENU_CATEGORIES
                }
            }

        return filteredSchools.sortedBy { it.initial.uppercase() + it.name }
    }

    /**
     * 【二级页面数据】根据学校 ID 获取其所有的适配器列表。
     */
    suspend fun getAdaptersForSchool(schoolId: String): List<Adapter> {
        return withContext(Dispatchers.IO) {
            val index = loadIndex()
            val school = index?.schools?.find { it.id == schoolId }
            return@withContext school?.adapters ?: emptyList()
        }
    }

    /**
     * 辅助方法：通过 ID 获取单个学校对象
     */
    suspend fun getSchoolById(id: String): School? {
        return withContext(Dispatchers.IO) {
            val index = loadIndex()
            return@withContext index?.schools?.find { it.id == id }
        }
    }
}

/**
 * 用户记录仓库
 */
@Single
class SchoolHistoryRepository(
    @Named("SchoolHistory") private val dataStore: DataStore<Preferences>
) {
    val historyFlow: Flow<SchoolHistoryModel> = dataStore.data.map { prefs ->
        SchoolHistoryModel.fromPreferences(prefs)
    }

    /**
     * 保存上次选择的学校
     */
    suspend fun saveLastSchool(category: AdapterCategory, school: School) {
        dataStore.edit { prefs ->
            val keys = SchoolHistoryModel.getKeysForCategory(category)
            prefs[keys.first] = school.id
            prefs[keys.second] = school.name
            prefs[keys.third] = school.resource_folder
        }
    }

    /**
     * 清除历史记录
     */
    suspend fun clearHistory(category: AdapterCategory) {
        dataStore.edit { prefs ->
            val keys = SchoolHistoryModel.getKeysForCategory(category)
            prefs.remove(keys.first)
            prefs.remove(keys.second)
            prefs.remove(keys.third)
        }
    }
}