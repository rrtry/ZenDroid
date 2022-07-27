package ru.rrtry.silentdroid.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import ru.rrtry.silentdroid.entities.EventRelation
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface EventRelationDao {

    @Transaction
    @Query("SELECT * FROM Event WHERE Event.scheduled = (:scheduled)")
    fun getEventsByState(scheduled: Boolean): List<EventRelation>

    @Transaction
    @Query("SELECT * FROM Event")
    fun observeEvents(): Flow<List<EventRelation>>

    @Transaction
    @Query("SELECT * FROM Event WHERE Event.eventEndsProfileId = (:id) OR Event.eventStartsProfileId = (:id)")
    suspend fun getEventsByProfileId(id: UUID): List<EventRelation>

    @Transaction
    @Query("SELECT * FROM Event")
    suspend fun getEvents(): List<EventRelation>

}