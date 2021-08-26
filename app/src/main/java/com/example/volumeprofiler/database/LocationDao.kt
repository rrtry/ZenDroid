package com.example.volumeprofiler.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.volumeprofiler.models.Location
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface LocationDao {

    /*

    @Query("SELECT * FROM Location")
    suspend fun observeLocations(): Flow<List<Location>>

    @Query("SELECT * FROM Location WHERE Location.id = (:id)")
    suspend fun observeLocation(id: UUID): LiveData<Location>

    @Insert
    suspend fun insertLocation(location: Location): Unit

    @Update
    suspend fun updateLocation(location: Location): Unit

    @Delete
    suspend fun deleteLocation(location: Location): Unit
     */

}