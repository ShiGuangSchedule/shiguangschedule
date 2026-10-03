package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作课程周数 (CourseWeek) 关联数据表。
 */
@Dao
interface CourseWeekDao {

    /**
     * 获取所有课程周数关联数据（用于全量备份）
     */
    @Query("SELECT * FROM course_weeks")
    suspend fun getAll(): List<CourseWeek>

    /**
     * 清空所有课程周数关联数据（用于全量恢复）
     */
    @Query("DELETE FROM course_weeks")
    suspend fun clearAll()

    /**
     * 获取指定课程的所有周数。
     */
    @Query("SELECT * FROM course_weeks WHERE courseId = :courseId ORDER BY weekNumber ASC")
    fun getWeeksByCourseId(courseId: String): Flow<List<CourseWeek>>

    /**
     * 批量插入课程周数。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(courseWeeks: List<CourseWeek>)

    /**
     * 更新指定课程的周数列表。
     * 这个方法在一个事务中执行，确保原子操作。
     * 核心逻辑：先删除该课程所有的旧周数，然后插入新的周数。
     */
    @Transaction
    suspend fun updateCourseWeeks(courseId: String, courseWeeks: List<CourseWeek>) {
        // 先清理旧的关系
        deleteByCourseId(courseId)
        // 插入新的关系
        insertAll(courseWeeks)
    }

    /**
     * 根据课程ID删除其所有的周数记录。
     */
    @Query("DELETE FROM course_weeks WHERE courseId = :courseId")
    suspend fun deleteByCourseId(courseId: String)

    /**
     * 根据课程ID列表和周次，批量删除周次记录。
     * 这是调课（TweakMode）场景下的核心方法：只切断特定周次的联系。
     */
    @Query("DELETE FROM course_weeks WHERE courseId IN (:courseIds) AND weekNumber = :weekNumber")
    suspend fun deleteCourseWeeksForCourseAndWeek(courseIds: List<String>, weekNumber: Int)
}