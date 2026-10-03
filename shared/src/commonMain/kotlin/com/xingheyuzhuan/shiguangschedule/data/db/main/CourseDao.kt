package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RoomWarnings
import androidx.room3.Transaction
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作课程 (Course) 数据表。
 * 已更新以支持标准节次和自定义时间字段的混合排序，并增加了精准更新支持。
 */
@Dao
interface CourseDao {

    /**
     * 获取所有课程（用于全量备份）
     */
    @Query("SELECT * FROM courses")
    suspend fun getAll(): List<Course>

    /**
     * 清空所有课程数据（用于全量恢复）
     */
    @Query("DELETE FROM courses")
    suspend fun clearAll()

    /**
     * 获取指定课表ID的所有课程。
     * 排序逻辑已调整，支持按节次和自定义时间字符串混合排序。
     */
    @Query(
        """
        SELECT * FROM courses 
        WHERE courseTableId = :courseTableId 
        ORDER BY 
            day ASC, 
            -- 如果是标准课，按 startSection 排序；否则给定一个大值 99
            CASE WHEN isCustomTime = 0 THEN startSection ELSE 99 END ASC,  
            -- 如果是自定义课，按 customStartTime 字符串排序；否则给定一个大值 '99:99'
            CASE WHEN isCustomTime = 1 THEN customStartTime ELSE '99:99' END ASC
        """
    )
    fun getCoursesByTableId(courseTableId: String): Flow<List<Course>>

    /**
     * 获取指定课表ID的所有课程，并包含其对应的周数。
     * 排序逻辑与上面保持一致，确保关联查询结果的顺序正确字段。
     */
    @Transaction
    @Query(
        """
        SELECT * FROM courses 
        WHERE courseTableId = :courseTableId 
        ORDER BY 
            day ASC, 
            CASE WHEN isCustomTime = 0 THEN startSection ELSE 99 END ASC,  
            CASE WHEN isCustomTime = 1 THEN customStartTime ELSE '99:99' END ASC
        """
    )
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    fun getCoursesWithWeeksByTableId(courseTableId: String): Flow<List<CourseWithWeeks>>

    /**
     * 检查指定 ID 的课程是否存在。
     * 用于 Repository 判断是执行插入(Insert)还是精准更新(Update)。
     */
    @Query("SELECT EXISTS(SELECT 1 FROM courses WHERE id = :courseId)")
    suspend fun exists(courseId: String): Boolean

    /**
     * 更新现有的课程信息。
     * 相比 REPLACE，使用 @Update 仅修改字段而不删除行，因此不会触发级联删除。
     * 适用于调课、修改颜色或名称等不希望触动其他周次数据的场景。
     */
    @Update
    suspend fun update(course: Course)

    /**
     * 插入一个或多个课程。
     * 策略调整为 ABORT，配合 Repository 的 exists 检查使用。
     * 确保只有在新课程时才执行插入，避免意外覆盖导致的级联数据丢失。
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(courses: List<Course>)

    /**
     * 删除一个或多个课程。
     */
    @Delete
    suspend fun delete(course: Course)

    /**
     * 根据课程ID删除单个课程。
     */
    @Query("DELETE FROM courses WHERE id = :courseId")
    suspend fun deleteById(courseId: String)

    /**
     * 批量删除指定的课程ID列表。
     * 依赖 ForeignKey.CASCADE，此操作会同时删除关联的 course_weeks 记录。
     */
    @Query("DELETE FROM courses WHERE id IN (:courseIds)")
    suspend fun deleteCoursesByIds(courseIds: List<String>)

    /**
     * 删除指定课表ID下的所有课程。
     * 这是支持覆盖导入功能的关键方法。
     */
    @Query("DELETE FROM courses WHERE courseTableId = :courseTableId")
    suspend fun deleteCoursesByTableId(courseTableId: String)

    /**
     * 获取指定课表ID下，在特定星期和周次的所有课程及其周数。
     * 排序逻辑已调整，以正确处理同一天内的自定义时间日程。
     *
     * @param courseTableId 课表ID。
     * @param day 星期几。
     * @param weekNumber 周次。
     */
    @Transaction
    @Query(
        """
        SELECT * FROM courses AS c
        INNER JOIN course_weeks AS cw ON c.id = cw.courseId
        WHERE c.courseTableId = :courseTableId
          AND c.day = :day
          AND cw.weekNumber = :weekNumber
        ORDER BY 
            CASE WHEN c.isCustomTime = 0 THEN c.startSection ELSE 99 END ASC,  
            CASE WHEN c.isCustomTime = 1 THEN c.customStartTime ELSE '99:99' END ASC
        """
    )
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    fun getCoursesWithWeeksByDayAndWeek(
        courseTableId: String,
        day: Int,
        weekNumber: Int
    ): Flow<List<CourseWithWeeks>>

    /**
     * 专门用于根据课程 ID 更新 colorInt 字段。
     * 用于将旧的 ARGB 值迁移到新的索引值。
     */
    @Query("UPDATE courses SET colorInt = :newColorInt WHERE id = :courseId")
    suspend fun updateCourseColorById(courseId: String, newColorInt: Int)

    /**
     * 批量删除指定课表下、指定名称的所有课程记录。
     * 启用级联删除后，此操作会自动删除关联的 course_weeks 记录。
     */
    @Query("DELETE FROM courses WHERE courseTableId = :tableId AND name IN (:courseNames)")
    suspend fun deleteCoursesByNames(tableId: String, courseNames: List<String>)

    /**
     * 获取指定课表在特定周次下的所有课程及其周数（整周数据）。
     * 用于主课表 Pager 页面的一次性数据加载。
     *
     * @param courseTableId 课表ID
     * @param weekNumber 目标周次
     */
    @Transaction
    @Query(
        """
        SELECT DISTINCT c.* FROM courses AS c
        INNER JOIN course_weeks AS cw ON c.id = cw.courseId
        WHERE c.courseTableId = :courseTableId
          AND cw.weekNumber = :weekNumber
        ORDER BY 
            c.day ASC,
            CASE WHEN c.isCustomTime = 0 THEN c.startSection ELSE 99 END ASC,  
            CASE WHEN c.isCustomTime = 1 THEN c.customStartTime ELSE '99:99' END ASC
        """
    )
    @Suppress(RoomWarnings.QUERY_MISMATCH)
    fun getCoursesWithWeeksByTableAndWeek(
        courseTableId: String,
        weekNumber: Int
    ): Flow<List<CourseWithWeeks>>
}