package com.xingheyuzhuan.shiguangschedule.data.db.widget

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import okio.Path
import org.koin.mp.KoinPlatform

actual fun createWidgetDatabase(databasesDir: Path): WidgetDatabase {
    val context = KoinPlatform.getKoin().get<Context>()
    val dbPath = (databasesDir / "widget_database").toString()
    return Room.databaseBuilder<WidgetDatabase>(
        context = context,
        name = dbPath,
        factory = { WidgetDatabaseConstructor.initialize() }
    )
        .fallbackToDestructiveMigration(true)
        .setDriver(AndroidSQLiteDriver())
        .build()
}
