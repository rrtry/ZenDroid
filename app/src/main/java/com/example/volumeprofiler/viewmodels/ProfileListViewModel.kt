package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.collection.ArrayMap
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.LocationTrigger
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import kotlinx.coroutines.launch
import java.util.*

class ProfileListViewModel: ViewModel() {

    private val sharedPreferencesUtil: SharedPreferencesUtil = SharedPreferencesUtil.getInstance()
    private val repository: Repository = Repository.get()

    val alarmsToRemoveLiveData = MutableLiveData<List<AlarmTrigger>?>()
    val locationsToRemoveLiveData = MutableLiveData<List<LocationTrigger>>()

    var lastActiveProfileIndex: Int = -1

    fun applyProfileSettings(profile: Profile): Unit {
        val profileUtil = ProfileUtil.getInstance()
        profileUtil.applyAudioSettings(profile)
    }

    fun clearActiveProfileRecord(currentProfile: Profile): Unit {
        if (sharedPreferencesUtil.getActiveProfileId() == currentProfile.id.toString()) {
            sharedPreferencesUtil.clearActiveProfileRecord(currentProfile.id)
        }
    }

    fun removeProfile(profile: Profile, position: Int, positionMap: ArrayMap<UUID, Int>): Unit {
        if (position == lastActiveProfileIndex) {
            lastActiveProfileIndex = -1
        }
        val id: UUID = profile.id
        if (sharedPreferencesUtil.getActiveProfileId() == id.toString()) {
            sharedPreferencesUtil.clearActiveProfileRecord(id)
        }
        positionMap.remove(id)
        viewModelScope.launch {
            alarmsToRemoveLiveData.value = repository.getAlarmsByProfileId(id)
            repository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        Log.i("ProfileListViewModel", "onCleared()")
    }
}