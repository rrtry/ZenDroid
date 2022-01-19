package com.example.volumeprofiler.database.repositories

import com.example.volumeprofiler.database.dao.EventDao
import com.example.volumeprofiler.database.dao.EventRelationDao
import com.example.volumeprofiler.entities.Event
import com.example.volumeprofiler.entities.EventRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val eventRelationDao: EventRelationDao,
) {

    suspend fun getEvents(): List<EventRelation> {
        return withContext(Dispatchers.IO) {
            eventRelationDao.getEvents()
        }
    }

    suspend fun getEventsByState(enabled: Boolean): List<EventRelation> {
        return withContext(Dispatchers.IO) {
            eventRelationDao.getEventsByState(enabled)
        }
    }

    fun observeEvents(): Flow<List<EventRelation>> {
        return eventRelationDao.observeEvents()
    }

    suspend fun insertEvent(event: Event): Unit {
        withContext(Dispatchers.IO) {
            eventDao.insertEvent(event)
        }
    }

    suspend fun updateEvent(event: Event): Unit {
        withContext(Dispatchers.IO) {
            eventDao.updateEvent(event)
        }
    }

    suspend fun deleteEvent(event: Event): Unit {
        withContext(Dispatchers.IO) {
            eventDao.deleteEvent(event)
        }
    }
}