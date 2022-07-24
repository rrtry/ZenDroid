package ru.rrtry.silentdroid.db.repositories

import ru.rrtry.silentdroid.db.dao.EventDao
import ru.rrtry.silentdroid.db.dao.EventRelationDao
import ru.rrtry.silentdroid.entities.Event
import ru.rrtry.silentdroid.entities.EventRelation
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