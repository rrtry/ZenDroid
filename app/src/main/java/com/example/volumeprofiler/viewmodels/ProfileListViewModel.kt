package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.Repository
import kotlinx.coroutines.launch
import java.util.*

class ProfileListViewModel: ViewModel() {

    private val repository: Repository = Repository.get()
    var associatedEventsLiveData = MutableLiveData<List<AlarmTrigger>?>()
    var lastActiveProfileIndex: Int = -1

    fun removeProfile(profile: Profile): Unit {
        viewModelScope.launch {
            val id: UUID = profile.id
            associatedEventsLiveData.value = repository.getAlarmsByProfileId(id)
            repository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        Log.i("ProfileListViewModel", "onCleared()")
    }
}