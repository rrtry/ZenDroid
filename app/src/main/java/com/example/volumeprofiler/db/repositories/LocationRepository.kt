package com.example.volumeprofiler.db.repositories

import com.example.volumeprofiler.db.dao.LocationDao
import com.example.volumeprofiler.db.dao.LocationRelationDao
import com.example.volumeprofiler.db.dao.LocationSuggestionsDao
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.LocationSuggestion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
        private val locationDao: LocationDao,
        private val locationRelationDao: LocationRelationDao,
        private val locationSuggestionsDao: LocationSuggestionsDao
) {

    suspend fun removeSuggestion(suggestion: LocationSuggestion) {
        withContext(Dispatchers.IO) {
            locationSuggestionsDao.deleteSuggestion(suggestion)
        }
    }

    suspend fun addSuggestion(suggestion: LocationSuggestion) {
        withContext(Dispatchers.IO) {
            locationSuggestionsDao.addSuggestion(suggestion)
        }
    }

    suspend fun getAllRecentSuggestions(): List<LocationSuggestion> {
        return withContext(Dispatchers.IO) {
            locationSuggestionsDao.getAllSuggestions()
        }
    }

    suspend fun getRecentLocationsByAddress(query: String): List<LocationSuggestion> {
        return withContext(Dispatchers.IO) {
            locationSuggestionsDao.getSuggestionsByAddress(query)
        }
    }

    suspend fun addLocation(location: Location): Unit {
        withContext(Dispatchers.IO) {
            locationDao.insertLocation(location)
        }
    }

    suspend fun removeLocation(location: Location): Unit {
        withContext(Dispatchers.IO) {
            locationDao.deleteLocation(location)
        }
    }

    suspend fun updateLocation(location: Location): Unit {
        withContext(Dispatchers.IO) {
            locationDao.updateLocation(location)
        }
    }

    fun observeLocations(): Flow<List<LocationRelation>> {
        return locationRelationDao.observeLocations()
    }

    fun observeLocationsByProfileId(id: UUID): Flow<List<LocationRelation>?> {
        return locationRelationDao.observeLocationsByProfileId(id)
    }

    suspend fun getLocationsByProfileId(id: UUID): List<LocationRelation> {
        return withContext(Dispatchers.IO) {
            locationRelationDao.getLocationsByProfileId(id)
        }
    }

    suspend fun getLocations(): List<LocationRelation> {
        return withContext(Dispatchers.IO) {
            locationRelationDao.getLocations()
        }
    }
}