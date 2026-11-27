package com.xingheyuzhuan.shiguangschedule.data.db.main

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import okio.Path
import org.koin.mp.KoinPlatform

actual fun createMainDatabase(databasesDir: Path): MainAppDatabase {
    val context = KoinPlatform.getKoin().get<Context>()
    val dbPath = (databasesDir / "main_app_database").toString()
    return Room.databaseBuilder<MainAppDatabase>(
        context = context,
        name = dbPath,
        factory = { MainAppDatabaseConstructor.initialize() }
    )
        .addMigrations(*ALL_MIGRATIONS)
        .setDriver(AndroidSQLiteDriver())
        .build()
}
