package com.example.volumeprofiler.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.volumeprofiler.models.LocationRelation
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface LocationRelationDao {

    @Transaction
    @Query("SELECT * FROM Location")
    fun observeLocations(): Flow<List<LocationRelation>>

    @Transaction
    @Query("SELECT * FROM Location WHERE Location.onExitProfileId = (:id) OR Location.onEnterProfileId = (:id)")
    fun getLocationsByProfileId(id: UUID): List<LocationRelation>

    @Transaction
    @Query("SELECT * FROM Location")
    fun getLocations(): List<LocationRelation>
}