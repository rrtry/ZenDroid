package com.example.volumeprofiler.database.repositories

import androidx.collection.ArrayMap
import com.example.volumeprofiler.database.dao.ProfileDao
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.util.SharedPreferencesUtil
import com.example.volumeprofiler.util.restoreChangedPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
        private val profileDao: ProfileDao,
        private val sharedPreferencesUtil: SharedPreferencesUtil
) {

    suspend fun addProfile(profile: Profile) {
        withContext(Dispatchers.IO) {
            profileDao.addProfile(profile)
        }
    }

    suspend fun updateProfile(profile: Profile) {
        withContext(Dispatchers.IO) {
            profileDao.updateProfile(profile)
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
}