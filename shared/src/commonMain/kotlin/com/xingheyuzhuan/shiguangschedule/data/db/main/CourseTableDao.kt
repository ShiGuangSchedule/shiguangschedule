package com.xingheyuzhuan.shiguangschedule.data.db.main

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Delete
import kotlinx.coroutines.flow.Flow

/**
 * Room 数据访问对象 (DAO)，用于操作课表 (CourseTable) 数据表。
 */
@Dao
interface CourseTableDao {

    /**
     * 获取所有课表（用于全量备份）
     */
    @Query("SELECT * FROM course_tables")
    suspend fun getAll(): List<CourseTable>

    /**
     * 清空所有课表数据（用于全量恢复）
     */
    @Query("DELETE FROM course_tables")
    suspend fun clearAll()

    /**
     * 获取所有课表，并按创建时间倒序排列。
     */
    @Query("SELECT * FROM course_tables ORDER BY createdAt DESC")
    fun getAllCourseTables(): Flow<List<CourseTable>>

    /**
     * 根据 ID 获取单个课表。
     */
    @Query("SELECT * FROM course_tables WHERE id = :tableId LIMIT 1")
    suspend fun getCourseTableById(tableId: String): CourseTable?

    /**
     * 插入一个新的课表。如果发生主键冲突，则替换旧数据。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(courseTable: CourseTable)

    /**
     * 更新一个现有课表。
     */
    @Update
    suspend fun update(courseTable: CourseTable)

    /**
     * 删除一个或多个课表。
     * 由于外键设置为 `onDelete = ForeignKey.CASCADE`，
     * 删除课表时，其下的所有课程也会被自动删除。
     */
    @Delete
    suspend fun delete(courseTable: CourseTable)

    /**
     * 根据课表ID删除单个课表。
     */
    @Query("DELETE FROM course_tables WHERE id = :tableId")
    suspend fun deleteById(tableId: String)


    /**
     * 获取最早创建的一张课表。
     * 用于在 DataStore 迁移或初始化时，确定默认显示的课表 ID。
     */
    @Query("SELECT * FROM course_tables ORDER BY createdAt ASC LIMIT 1")
    suspend fun getFirstTableOnce(): CourseTable?
}