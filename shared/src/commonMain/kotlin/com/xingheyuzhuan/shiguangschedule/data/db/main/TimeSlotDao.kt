package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作时间段 (TimeSlot) 数据表。
 */
@Dao
interface TimeSlotDao {

    /**
     * 获取所有时间段（用于全量备份）
     */
    @Query("SELECT * FROM time_slots")
    suspend fun getAll(): List<TimeSlot>

    /**
     * 清空所有时间段数据（用于全量恢复）
     */
    @Query("DELETE FROM time_slots")
    suspend fun clearAll()

    /**
     * 获取指定作息表 (TimeTable) 的所有时间段，并按节次编号升序排列（响应式 Flow）。
     */
    @Query("SELECT * FROM time_slots WHERE timeTableId = :timeTableId ORDER BY number ASC")
    fun getTimeSlotsByTimeTableId(timeTableId: String): Flow<List<TimeSlot>>

    /**
     * 一次性（suspend）查询指定作息表的所有时间段，按节次编号升序排列。
     */
    @Query("SELECT * FROM time_slots WHERE timeTableId = :timeTableId ORDER BY number ASC")
    suspend fun getTimeSlotsOnceByTimeTableId(timeTableId: String): List<TimeSlot>

    /**
     * 根据节次编号和作息表 ID 获取单个时间段。
     */
    @Query("SELECT * FROM time_slots WHERE number = :number AND timeTableId = :timeTableId LIMIT 1")
    suspend fun getTimeSlot(number: Int, timeTableId: String): TimeSlot?

    /**
     * 插入一个或多个时间段。如果发生主键冲突，则替换旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(timeSlots: List<TimeSlot>)

    /**
     * 更新一个现有时间段。
     */
    @Update
    suspend fun update(timeSlot: TimeSlot)

    /**
     * 删除一个时间段。
     */
    @Delete
    suspend fun delete(timeSlot: TimeSlot)

    /**
     * 根据作息表 ID 删除所有时间段。
     */
    @Query("DELETE FROM time_slots WHERE timeTableId = :timeTableId")
    suspend fun deleteAllTimeSlotsByTimeTableId(timeTableId: String)

    /**
     * 根据作息表 ID 删除节次编号大于 maxNumber 的时间段。
     */
    @Query("DELETE FROM time_slots WHERE timeTableId = :timeTableId AND number > :maxNumber")
    suspend fun deleteTimeSlotsGreaterThan(timeTableId: String, maxNumber: Int)
}