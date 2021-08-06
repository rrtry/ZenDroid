package com.example.volumeprofiler.viewmodels

import android.util.Log
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.Repository
import kotlinx.coroutines.launch
import java.util.*

class EditProfileViewModel: ViewModel() {

    private val repository = Repository.get()
    private val profileIdLiveData = MutableLiveData<UUID>()
    var alarmTriggerLiveData: LiveData<List<AlarmTrigger>?> = Transformations.switchMap(profileIdLiveData) { profileId -> repository.observeProfileWithScheduledAlarms(profileId) }
    var mutableProfile: Profile? = null

    fun setProfileID(id : UUID) {
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
        Log.d("EditProfileActivity", "onCleared")
    }
}