package com.example.volumeprofiler.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile

@Database(entities = [Profile::class, Alarm::class], version = 2, exportSchema = true)
@TypeConverters(Conveters::class)
abstract class ApplicationDatabase: RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    abstract fun alarmDao(): AlarmDao

    abstract fun alarmTriggerDao(): AlarmTriggerDao

    abstract fun locationDao(): LocationDao

    abstract fun locationTriggerDao(): LocationTriggerDao
}