package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.Repository
import kotlinx.coroutines.launch
import java.util.*

class ProfileListViewModel: ViewModel() {

    private val repository: Repository = Repository.get()
    var associatedEventsLiveData = MutableLiveData<List<ProfileAndEvent>?>()

    val profileListLiveData: LiveData<List<Profile>>
    get() {
        return repository.observeProfiles()
    }

    fun removeProfile(profile: Profile): Unit {
        viewModelScope.launch {
            val id: UUID = profile.id
            associatedEventsLiveData.value = repository.getProfileWithScheduledEvents(id)
            Log.i("ProfileListViewModel", associatedEventsLiveData.value?.size.toString())
            repository.removeProfile(profile)
        }
    }

    override fun onCleared(): Unit {
        Log.i("ProfileListViewModel", "onCleared()")
    }
}