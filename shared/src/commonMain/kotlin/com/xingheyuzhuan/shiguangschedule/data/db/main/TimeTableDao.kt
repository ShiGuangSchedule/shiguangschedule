package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/**
 * 作息表 (TimeTable) 数据访问对象
 */
@Dao
interface TimeTableDao {

    /**
     * 获取所有作息表（用于全量备份）
     */
    @Query("SELECT * FROM time_tables")
    suspend fun getAll(): List<TimeTable>

    /**
     * 清空所有作息表数据（用于全量恢复）
     */
    @Query("DELETE FROM time_tables")
    suspend fun clearAll()

    /**
     * 根据 ID 获取作息表
     */
    @Query("SELECT * FROM time_tables WHERE id = :id LIMIT 1")
    suspend fun getTimeTableById(id: String): TimeTable?

    /**
     * 实时观察指定 ID 的作息表
     */
    @Query("SELECT * FROM time_tables WHERE id = :id LIMIT 1")
    fun observeTimeTableById(id: String): Flow<TimeTable?>

    /**
     * 获取所有公共作息表列表
     */
    @Query("SELECT * FROM time_tables WHERE name IS NOT NULL AND length(trim(name)) > 0 ORDER BY createdAt DESC")
    fun getAllPublicTimeTables(): Flow<List<TimeTable>>

    /**
     * 插入新作息表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(timeTable: TimeTable): Long

    /**
     * 更新作息表
     */
    @Update
    suspend fun update(timeTable: TimeTable): Int

    /**
     * 插入或更新作息表数据（先更新，失败则插入，避免 TRIGGER 异常）
     */
    suspend fun insertOrUpdate(timeTable: TimeTable) {
        val updatedRows = update(timeTable)
        if (updatedRows == 0) {
            insert(timeTable)
        }
    }

    /**
     * 根据 ID 删除作息表
     */
    @Query("DELETE FROM time_tables WHERE id = :id")
    suspend fun deleteTimeTableById(id: String)
}