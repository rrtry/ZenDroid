package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.collection.ArrayMap
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.database.Repository
import kotlinx.coroutines.launch
import java.util.*
import androidx.collection.arrayMapOf

class ProfileListViewModel: ViewModel() {

    private val repository: Repository = Repository.get()
    var associatedEventsLiveData = MutableLiveData<List<ProfileAndEvent>?>()
    var positionMap: ArrayMap<UUID, Int> = arrayMapOf()
    var lastActiveProfileIndex: Int = -1
    val profileListLiveData: LiveData<List<Profile>>
    get() {
        return repository.observeProfiles()
    }

    fun removeProfile(profile: Profile): Unit {
        viewModelScope.launch {
            val id: UUID = profile.id
            associatedEventsLiveData.value = repository.getEventsByProfileId(id)
            repository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        Log.i("ProfileListViewModel", "onCleared()")
    }
}