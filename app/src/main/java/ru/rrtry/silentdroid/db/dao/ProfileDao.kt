package ru.rrtry.silentdroid.db.dao

import androidx.room.*
import ru.rrtry.silentdroid.entities.Profile
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface ProfileDao {

    @Query("SELECT * FROM Profile")
    fun observeProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM Profile")
    suspend fun getProfiles(): List<Profile>

    @Query("SELECT * FROM Profile WHERE id=(:id)")
    suspend fun getProfile(id: UUID): Profile

    @Insert
    suspend fun addProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun removeProfile(profile: Profile)
}