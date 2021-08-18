package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.LocationTrigger
import kotlinx.coroutines.launch
import java.util.*

class ProfileListViewModel: ViewModel() {

    private val repository: Repository = Repository.get()

    val alarmsToRemoveLiveData = MutableLiveData<List<AlarmTrigger>?>()
    val geofencesToRemoveLiveData = MutableLiveData<List<LocationTrigger>>()

    var lastActiveProfileIndex: Int = -1

    fun removeProfile(profile: Profile): Unit {
        viewModelScope.launch {
            val id: UUID = profile.id
            alarmsToRemoveLiveData.value = repository.getAlarmsByProfileId(id)
            repository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        Log.i("ProfileListViewModel", "onCleared()")
    }
}