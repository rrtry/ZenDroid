package com.example.volumeprofiler.database

import android.content.Context
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.models.Location
import com.example.volumeprofiler.util.SharedPreferencesUtil
import com.example.volumeprofiler.util.restoreChangedPosition
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

open class Repository private constructor(
        protected val context: Context) {

    private val database: ApplicationDatabase = Room.databaseBuilder(
        context,
        ApplicationDatabase::class.java,
            DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    private val sharedPreferencesUtil: SharedPreferencesUtil = SharedPreferencesUtil.getInstance()

    private val profileDao: ProfileDao = database.profileDao()
    private val alarmDao: AlarmDao = database.alarmDao()
    private val alarmTriggerDao: AlarmTriggerDao = database.alarmTriggerDao()
    private val locationDao: LocationDao = database.locationDao()
    private val locationTriggerDao: LocationTriggerDao = database.locationTriggerDao()

    /*
    suspend fun updateLocation(location: Location): Unit {
        withContext(Dispatchers.IO) {
            locationDao.updateLocation(location)
        }
    }

    suspend fun addLocation(location: Location): Unit {
        withContext(Dispatchers.IO) {
            locationDao.insertLocation(location)
        }
    }

    suspend fun removeLocation(location: Location): Unit {
        withContext(Dispatchers.IO) {
            locationDao.deleteLocation(location)
        }
    }

    suspend fun observeLocations(): Flow<List<Location>> = locationDao.observeLocations()

    suspend fun observeLocation(id: UUID): LiveData<Location> {
        return withContext(Dispatchers.IO) {
            locationDao.observeLocation(id)
        }
    }
     */

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
            alarmTriggerDao.getProfilesWithScheduledAlarms()
        }
    }

    suspend fun getAlarmsByProfileId(id: UUID): List<AlarmTrigger>? {
        return withContext(Dispatchers.IO) {
            alarmTriggerDao.getAlarmsByProfileId(id)
        }
    }

    fun observeProfilesWithAlarms(): Flow<List<AlarmTrigger>> = alarmTriggerDao.observeProfilesWithAlarms()

    fun observeProfileWithScheduledAlarms(id: UUID): Flow<List<AlarmTrigger>?> = alarmTriggerDao.observeProfileWithScheduledAlarms(id)

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

    suspend fun getProfile(id: UUID): Profile {
        return withContext(Dispatchers.IO) {
            profileDao.getProfile(id)
        }
    }

    suspend fun getProfiles(): List<Profile> {
        return withContext(Dispatchers.IO) {
            profileDao.getProfiles()
        }
    }

    fun observeProfiles(): Flow<List<Profile>> {
        val positionsMap: ArrayMap<UUID, Int>? = sharedPreferencesUtil.getRecyclerViewPositionsMap()
        return if (positionsMap != null) {
            profileDao.observeProfiles().map { restoreChangedPosition(it, positionsMap) }
        } else {
            profileDao.observeProfiles()
        }
    }

    companion object {

        const val DATABASE_NAME = "volumeprofiler-database"

        @Volatile
        private var INSTANCE: Repository? = null

        private const val LOG_TAG: String = "Repository"

        fun initialize(context: Context) {

            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = Repository(context)
                }
            }
        }

        fun get(): Repository {
            return INSTANCE ?: throw IllegalStateException("Repository must be initialized")
        }
    }
}