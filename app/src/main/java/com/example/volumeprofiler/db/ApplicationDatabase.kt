package com.example.volumeprofiler.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.volumeprofiler.db.dao.*
import com.example.volumeprofiler.entities.*

@Database(entities = [Profile::class, Alarm::class, Location::class, Event::class, LocationSuggestion::class], version = 26, exportSchema = true)
@TypeConverters(Converters::class)
abstract class ApplicationDatabase: RoomDatabase() {

    abstract fun getSuggestionsDao(): LocationSuggestionsDao

    abstract fun getProfileDao(): ProfileDao

    abstract fun getAlarmDao(): AlarmDao

    abstract fun getAlarmRelationDao(): AlarmRelationDao

    abstract fun getLocationDao(): LocationDao

    abstract fun getLocationRelationDao(): LocationRelationDao

    abstract fun getEventDao(): EventDao

    abstract fun getEventRelationDao(): EventRelationDao
}