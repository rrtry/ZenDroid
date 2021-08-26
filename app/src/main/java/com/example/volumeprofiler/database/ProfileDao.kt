package com.example.volumeprofiler.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface ProfileDao {

    @Query("SELECT * FROM Profile")
    fun observeProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM Profile")
    suspend fun getProfiles(): List<Profile>

    @Query("SELECT * FROM Profile WHERE id=(:id)")
    fun observeProfile(id: UUID): LiveData<Profile?>

    @Query("SELECT * FROM Profile WHERE id=(:id)")
    suspend fun getProfile(id: UUID): Profile

    @Insert
    suspend fun addProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun removeProfile(profile: Profile)
}