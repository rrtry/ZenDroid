package com.example.volumeprofiler.database.repositories

import com.example.volumeprofiler.database.dao.ProfileDao
import com.example.volumeprofiler.models.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
        private val profileDao: ProfileDao,
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

    /*
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
     */

    fun observeProfiles(): Flow<List<Profile>> {
        return profileDao.observeProfiles()
    }
}