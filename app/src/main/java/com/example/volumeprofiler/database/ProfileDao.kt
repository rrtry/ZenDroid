package com.example.volumeprofiler.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import java.util.*

@Dao
interface ProfileDao {

    @Query("SELECT * FROM Profile")
    fun observeProfiles(): LiveData<List<Profile>>

    @Query("SELECT * FROM Profile WHERE id=(:id)")
    fun observeProfile(id: UUID): LiveData<Profile?>

    @Query("SELECT * FROM Profile")
    suspend fun getProfiles(): List<Profile>

    @Insert
    suspend fun addProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)

    @Delete
    suspend fun removeProfile(profile: Profile)

    @Query("SELECT * FROM Profile INNER JOIN Event ON profile.id = Event.profileUUID")
    fun observeProfilesWithEvents(): LiveData<List<ProfileAndEvent>>

    @Query("SELECT * FROM Profile INNER JOIN Event ON profile.id = Event.profileUUID WHERE profile.id = (:id) AND event.isScheduled = 1")
    fun observeProfileWithScheduledEvents(id: UUID): LiveData<List<ProfileAndEvent>?>

    @Query("SELECT * FROM Profile INNER JOIN Event ON profile.id = Event.profileUUID WHERE event.isScheduled = 1")
    suspend fun getProfilesWithScheduledEvents(): List<ProfileAndEvent>?
}