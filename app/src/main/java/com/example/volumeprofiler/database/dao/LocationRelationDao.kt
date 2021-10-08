package com.example.volumeprofiler.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.volumeprofiler.models.LocationRelation
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationRelationDao {

    @Transaction
    @Query("SELECT * FROM Location")
    fun observeLocationTriggers(): Flow<List<LocationRelation>>
}