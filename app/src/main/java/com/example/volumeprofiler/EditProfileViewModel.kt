package com.example.volumeprofiler

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import java.util.*

class EditProfileViewModel: ViewModel() {

    private val repository = Repository.get()
    private val profileIdLiveData = MutableLiveData<UUID>()
    var profileLiveData: LiveData<Profile?> = Transformations.switchMap(profileIdLiveData) { profileId -> repository.observeProfile(profileId) }
    var profileAndEventLiveData: LiveData<List<ProfileAndEvent>?> = Transformations.switchMap(profileIdLiveData) { profileId -> repository.observeProfileWithScheduledEvents(profileId) }
    var mutableProfile: Profile? = null

    fun loadProfile(id : UUID) {
        profileIdLiveData.value = id
    }

    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }

    fun addProfile(profile: Profile) {
        viewModelScope.launch {
            repository.addProfile(profile)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("EditProfileViewModel", "onCleared")
    }
}