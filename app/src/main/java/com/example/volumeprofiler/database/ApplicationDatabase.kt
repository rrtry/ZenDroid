package com.example.volumeprofiler.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.volumeprofiler.database.dao.*
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Location
import com.example.volumeprofiler.models.Profile

@Database(entities = [Profile::class, Alarm::class, Location::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ApplicationDatabase: RoomDatabase() {

    abstract fun getProfileDao(): ProfileDao

    abstract fun getAlarmDao(): AlarmDao

    abstract fun getAlarmRelationDao(): AlarmRelationDao

    abstract fun getLocationDao(): LocationDao

    abstract fun getLocationRelationDao(): LocationRelationDao
}