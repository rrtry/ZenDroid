package com.example.volumeprofiler

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch

class ProfileListViewModel: ViewModel() {

    private val repository: Repository = Repository.get()
    var associatedEventsLiveData = MutableLiveData<List<ProfileAndEvent>?>()

    val profileListLiveData: LiveData<List<Profile>>
    get() {
        return repository.observeProfiles()
    }

    fun removeProfile(profile: Profile): Unit {
        viewModelScope.launch {
            associatedEventsLiveData.value = repository.getProfileWithScheduledEvents(profile.id)
            Log.i("ProfileListViewModel", associatedEventsLiveData.value?.size.toString())
            repository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        Log.i("ProfileListViewModel", "onCleared()")
    }
}