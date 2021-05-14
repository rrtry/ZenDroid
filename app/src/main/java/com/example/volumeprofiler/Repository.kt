package com.example.volumeprofiler

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.example.volumeprofiler.database.ProfileDao
import com.example.volumeprofiler.database.SchedulerDao
import com.example.volumeprofiler.database.VolumeProfilerDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class Repository private constructor(context: Context) {

    private val database: VolumeProfilerDatabase = Room.databaseBuilder(
        context.applicationContext,
        VolumeProfilerDatabase::class.java,
        DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    private val profileDao: ProfileDao = database.profileDao()
    private val schedulerDao: SchedulerDao = database.schedulerDao()

    fun observeEvent(id: Long): LiveData<Event> = schedulerDao.observeEvent(id)

    suspend fun getEvent(id: Long): Event {
        return withContext(Dispatchers.IO) {
            schedulerDao.getEvent(id)
        }
    }

    suspend fun addEvent(event: Event) {
        withContext(Dispatchers.IO) {
            schedulerDao.addEvent(event)
        }
    }

    suspend fun updateEvent(event: Event) {
        withContext(Dispatchers.IO) {
            schedulerDao.updateEvent(event)
        }
    }

    suspend fun removeEvent(event: Event) {
        withContext(Dispatchers.IO) {
            schedulerDao.removeEvent(event)
        }
    }

    suspend fun getProfilesWithScheduledEvents(): List<ProfileAndEvent>? {
        return withContext(Dispatchers.IO) {
            profileDao.getProfilesWithScheduledEvents()
        }
    }

    suspend fun getProfileWithScheduledEvents(id: UUID): List<ProfileAndEvent>? {
        return withContext(Dispatchers.IO) {
            schedulerDao.getProfileWithScheduledEvents(id)
        }
    }

    fun observeProfilesWithEvents(): LiveData<List<ProfileAndEvent>> = profileDao.observeProfilesWithEvents()

    fun observeProfileWithScheduledEvents(id: UUID): LiveData<List<ProfileAndEvent>?> = profileDao.observeProfileWithScheduledEvents(id)

    suspend fun updateProfile(profile: Profile) {
        withContext(Dispatchers.IO) {
            profileDao.updateProfile(profile)
        }
    }

    suspend fun addProfile(profile: Profile) {
        withContext(Dispatchers.IO) {
            profileDao.addProfile(profile)
        }
    }

    suspend fun removeProfile(profile: Profile) {
        withContext(Dispatchers.IO) {
            profileDao.removeProfile(profile)
        }
    }

    fun observeProfile(uuid: UUID): LiveData<Profile?> = profileDao.observeProfile(uuid)

    fun observeProfiles(): LiveData<List<Profile>> = profileDao.observeProfiles()

    companion object {

        private const val DATABASE_NAME = "volumeprofiler-database"

        @Volatile
        private var INSTANCE: Repository? = null

        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = Repository(context)
            }
        }

        fun get(): Repository {
            return INSTANCE ?:
            throw IllegalStateException("Repository must be initialized")
        }
    }
}