package ru.rrtry.silentdroid.db.dao

import androidx.room.*
import ru.rrtry.silentdroid.entities.Event
import ru.rrtry.silentdroid.entities.Location
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface EventDao {

    @Query("SELECT * FROM Location")
    fun observeEvents(): Flow<List<Location>>

    @Query("SELECT * FROM Event WHERE Event.id = (:id)")
    suspend fun getEvent(id: UUID): Event

    @Insert
    suspend fun insertEvent(event: Event): Unit

    @Update
    suspend fun updateEvent(event: Event): Unit

    @Delete
    suspend fun deleteEvent(event: Event): Unit
}