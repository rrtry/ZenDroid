package com.example.volumeprofiler.db.dao

import androidx.room.*
import com.example.volumeprofiler.entities.Location
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface LocationDao {

    @Query("SELECT * FROM Location")
    fun observeLocations(): Flow<List<Location>>

    @Query("SELECT * FROM Location WHERE Location.location_id = (:id)")
    suspend fun getLocation(id: UUID): Location

    @Insert
    suspend fun insertLocation(location: Location): Unit

    @Update
    suspend fun updateLocation(location: Location): Unit

    @Delete
    suspend fun deleteLocation(location: Location): Unit

}