package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.database.Repository
import kotlinx.coroutines.launch
import java.util.*

class EditProfileViewModel: ViewModel() {

    private val repository = Repository.get()
    var changesMade: Boolean = false
    private val profileIdLiveData = MutableLiveData<UUID>()
    var profileLiveData: LiveData<Profile?> = Transformations.switchMap(profileIdLiveData) { profileId -> repository.observeProfile(profileId) }
    var profileAndEventLiveData: LiveData<List<ProfileAndEvent>?> = Transformations.switchMap(profileIdLiveData) { profileId -> repository.observeProfileWithScheduledEvents(profileId) }
    var mutableProfile: Profile? = null

    fun setProfile(id : UUID) {
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