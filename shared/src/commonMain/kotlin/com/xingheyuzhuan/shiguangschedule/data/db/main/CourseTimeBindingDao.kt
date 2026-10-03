package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作课表与作息方案的绑定关系 (CourseTimeBinding)。
 */
@Dao
interface CourseTimeBindingDao {

    /**
     * 获取所有课表作息绑定关系（用于全量备份）
     */
    @Query("SELECT * FROM course_time_bindings")
    suspend fun getAll(): List<CourseTimeBinding>

    /**
     * 清空所有课表作息绑定关系（用于全量恢复）
     */
    @Query("DELETE FROM course_time_bindings")
    suspend fun clearAll()

    /**
     * 获取指定课表的作息绑定关系
     */
    @Query("SELECT * FROM course_time_bindings WHERE courseTableId = :courseTableId LIMIT 1")
    suspend fun getBindingByCourseTableId(courseTableId: String): CourseTimeBinding?

    /**
     * 以 Flow 方式监听指定课表的作息绑定关系
     */
    @Query("SELECT * FROM course_time_bindings WHERE courseTableId = :courseTableId LIMIT 1")
    fun observeBindingByCourseTableId(courseTableId: String): Flow<CourseTimeBinding?>

    /**
     * 插入或更新绑定关系
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(binding: CourseTimeBinding)

    /**
     * 根据课表 ID 删除绑定关系
     */
    @Query("DELETE FROM course_time_bindings WHERE courseTableId = :courseTableId")
    suspend fun deleteByCourseTableId(courseTableId: String)

    /**
     * 根据目标作息/组合作息 ID 删除绑定关系（当公共作息或组合作息被删除时自动解除绑定）
     */
    @Query("DELETE FROM course_time_bindings WHERE targetId = :targetId")
    suspend fun deleteByTargetId(targetId: String)
}