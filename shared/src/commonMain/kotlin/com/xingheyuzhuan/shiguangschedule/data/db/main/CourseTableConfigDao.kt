package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseTableConfigDao {

    /**
     * 获取所有课表配置（用于全量备份）
     */
    @Query("SELECT * FROM course_table_config")
    suspend fun getAll(): List<CourseTableConfig>

    /**
     * 清空所有课表配置（用于全量恢复）
     */
    @Query("DELETE FROM course_table_config")
    suspend fun clearAll()

    /**
     * 插入或更新课表配置。
     * 当配置已存在时，进行替换（更新）。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: CourseTableConfig)

    /**
     * 根据课表ID获取配置，以 Flow 形式返回，用于监听变化。
     */
    @Query("SELECT * FROM course_table_config WHERE courseTableId = :courseTableId")
    fun getConfigById(courseTableId: String): Flow<CourseTableConfig?>

    /**
     * 一次性获取配置，用于计算或不需要监听的场景。
     */
    @Query("SELECT * FROM course_table_config WHERE courseTableId = :courseTableId LIMIT 1")
    suspend fun getConfigOnce(courseTableId: String): CourseTableConfig?
}