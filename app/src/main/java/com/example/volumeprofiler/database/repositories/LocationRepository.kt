package com.example.volumeprofiler.database.repositories

import com.example.volumeprofiler.database.dao.LocationDao
import com.example.volumeprofiler.database.dao.LocationRelationDao
import com.example.volumeprofiler.models.Location
import com.example.volumeprofiler.models.LocationRelation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
        private val locationDao: LocationDao,
        private val locationRelationDao: LocationRelationDao
) {

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