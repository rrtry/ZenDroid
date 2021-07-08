package com.example.volumeprofiler.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.Profile

@Database(entities = [Profile::class, Event::class], version = 20, exportSchema = false)
@TypeConverters(Converters::class)
abstract class VolumeProfilerDatabase: RoomDatabase() {

    abstract fun profileDao(): ProfileDao

    abstract fun schedulerDao(): SchedulerDao
}