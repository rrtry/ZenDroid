package com.example.volumeprofiler.database.dao

import androidx.room.*
import com.example.volumeprofiler.entities.LocationSuggestion

@Dao
interface LocationSuggestionsDao {

    @Query("SELECT * FROM LocationSuggestion")
    suspend fun getAllSuggestions(): List<LocationSuggestion>

    @Query("SELECT * FROM LocationSuggestion WHERE address LIKE '%' || :query || '%' COLLATE utf8_general_ci")
    suspend fun getSuggestionsByAddress(query: String): List<LocationSuggestion>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSuggestion(suggestion: LocationSuggestion)

    @Delete
    suspend fun deleteSuggestion(suggestion: LocationSuggestion)
}