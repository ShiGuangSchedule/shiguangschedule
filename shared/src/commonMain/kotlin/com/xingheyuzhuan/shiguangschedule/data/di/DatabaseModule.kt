package com.xingheyuzhuan.shiguangschedule.data.di

import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableConfigDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTableDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseTimeBindingDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWeekDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.MainAppDatabase
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlotDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableComboDao
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeTableDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetAppSettingsDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetCourseDao
import com.xingheyuzhuan.shiguangschedule.data.db.widget.WidgetDatabase
import okio.Path
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single

@Module
@Suppress("unused")
class DatabaseModule {

    @Single
    fun provideMainDatabase(@Named("DatabasesDir") databasesDir: Path): MainAppDatabase {
        return MainAppDatabase.getDatabase(databasesDir)
    }

    @Single
    fun provideWidgetDatabase(@Named("DatabasesDir") databasesDir: Path): WidgetDatabase {
        return WidgetDatabase.getDatabase(databasesDir)
    }

    @Factory
    fun provideCourseTableConfigDao(db: MainAppDatabase): CourseTableConfigDao = db.courseTableConfigDao()

    @Factory
    fun provideTimeSlotDao(db: MainAppDatabase): TimeSlotDao = db.timeSlotDao()

    @Factory
    fun provideCourseDao(db: MainAppDatabase): CourseDao = db.courseDao()

    @Factory
    fun provideCourseTableDao(db: MainAppDatabase): CourseTableDao = db.courseTableDao()

    @Factory
    fun provideCourseWeekDao(db: MainAppDatabase): CourseWeekDao = db.courseWeekDao()

    @Factory
    fun provideTimeTableDao(db: MainAppDatabase): TimeTableDao = db.timeTableDao()

    @Factory
    fun provideCourseTimeBindingDao(db: MainAppDatabase): CourseTimeBindingDao = db.courseTimeBindingDao()

    @Factory
    fun provideTimeTableComboDao(db: MainAppDatabase): TimeTableComboDao = db.timeTableComboDao()

    // Widget 数据库 DAO
    @Factory
    fun provideWidgetCourseDao(db: WidgetDatabase): WidgetCourseDao = db.widgetCourseDao()

    @Factory
    fun provideWidgetAppSettingsDao(db: WidgetDatabase): WidgetAppSettingsDao = db.widgetAppSettingsDao()
}
