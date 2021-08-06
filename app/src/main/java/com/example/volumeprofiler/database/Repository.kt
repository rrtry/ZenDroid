package com.example.volumeprofiler.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import kotlinx.coroutines.*
import java.util.UUID

class Repository private constructor(context: Context) {

    private val database: ApplicationDatabase = Room.databaseBuilder(
        context,
        ApplicationDatabase::class.java,
            DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    private val profileDao: ProfileDao = database.profileDao()
    private val alarmDao: AlarmDao = database.alarmDao()

    suspend fun addAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmDao.addAlarm(alarm)
        }
    }

    suspend fun updateAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmDao.updateAlarm(alarm)
        }
    }

    suspend fun removeAlarm(alarm: Alarm) {
        withContext(Dispatchers.IO) {
            alarmDao.removeAlarm(alarm)
        }
    }

    suspend fun getProfilesWithScheduledAlarms(): List<AlarmTrigger>? {
        return withContext(Dispatchers.IO) {
            profileDao.getProfilesWithScheduledAlarms()
        }
    }

    suspend fun getAlarmsByProfileId(id: UUID): List<AlarmTrigger>? {
        return withContext(Dispatchers.IO) {
            alarmDao.getAlarmsByProfileId(id)
        }
    }

    fun observeProfilesWithAlarms(): LiveData<List<AlarmTrigger>> = profileDao.observeProfilesWithAlarms()

    fun observeProfileWithScheduledAlarms(id: UUID): LiveData<List<AlarmTrigger>?> = profileDao.observeProfileWithScheduledAlarms(id)

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

    suspend fun getProfiles(): List<Profile> {
        return withContext(Dispatchers.IO) {
            profileDao.getProfiles()
        }
    }

    fun observeProfiles(): LiveData<List<Profile>> = profileDao.observeProfiles()

    companion object {

        const val DATABASE_NAME = "volumeprofiler-database"
        
        private var INSTANCE: Repository? = null
        private const val LOG_TAG: String = "Repository"

        fun initialize(context: Context) {

            if (INSTANCE == null) {
                INSTANCE = Repository(context)
            }
        }

        fun get(): Repository {
            return INSTANCE
                    ?:
            throw IllegalStateException("Repository must be initialized")
        }
    }
}