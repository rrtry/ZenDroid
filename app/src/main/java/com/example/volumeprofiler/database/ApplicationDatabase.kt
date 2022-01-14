package com.example.volumeprofiler.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.volumeprofiler.database.dao.*
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.Event
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.Profile

@Database(entities = [Profile::class, Alarm::class, Location::class, Event::class], version = 20, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ApplicationDatabase: RoomDatabase() {

    abstract fun getProfileDao(): ProfileDao

    abstract fun getAlarmDao(): AlarmDao

    abstract fun getAlarmRelationDao(): AlarmRelationDao

    abstract fun getLocationDao(): LocationDao

    abstract fun getLocationRelationDao(): LocationRelationDao

    abstract fun getEventDao(): EventDao

    abstract fun getEventRelationDao(): EventRelationDao
}