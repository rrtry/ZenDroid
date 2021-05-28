package com.example.volumeprofiler.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.ProfileAndEvent
import java.util.*

@Dao
interface SchedulerDao {

    @Query("SELECT * FROM Event WHERE Event.eventId = (:id)")
    fun observeEvent(id: Long): LiveData<Event>

    @Query("SELECT * FROM Event WHERE Event.eventId = (:id)")
    fun getEvent(id: Long): Event

    @Insert
    suspend fun addEvent(event: Event)

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun removeEvent(event: Event)

    @Query("SELECT * FROM Profile INNER JOIN Event ON profile.id = Event.profileUUID WHERE profile.id = (:id) AND event.isScheduled = 1")
    suspend fun getEventsByProfileId(id: UUID): List<ProfileAndEvent>?

    @Query("SELECT * FROM Profile INNER JOIN Event ON Event.eventId = (:id) AND event.isScheduled = 1")
    fun observeScheduledEventWithProfile(id: Long): LiveData<ProfileAndEvent?>
}