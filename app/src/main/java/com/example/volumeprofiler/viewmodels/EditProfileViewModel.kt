package com.example.volumeprofiler.viewmodels

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import kotlinx.coroutines.launch
import java.util.*

class EditProfileViewModel: ViewModel() {

    private val repository = Repository.get()
    private val profileIdLiveData = MutableLiveData<UUID>()
    var alarmsLiveData: LiveData<List<AlarmTrigger>?> = Transformations.switchMap(profileIdLiveData) { profileId -> repository.observeProfileWithScheduledAlarms(profileId).asLiveData() }
    var mutableProfile: Profile? = null

    private fun setAlarm(alarmTrigger: AlarmTrigger, newProfile: Profile): Unit {
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        alarmUtil.setAlarm(alarmTrigger.alarm, newProfile, false)
    }

    fun setMultipleAlarms(triggers: List<AlarmTrigger>, newProfile: Profile): Unit {
        for (i in triggers) {
            setAlarm(i, newProfile)
        }
    }

    fun applyAudioSettingsIfActive(): Unit {
        val sharedPreferencesUtil = SharedPreferencesUtil.getInstance()
        if (sharedPreferencesUtil.getActiveProfileId()
                == mutableProfile!!.id.toString()) {
            val profileUtil: ProfileUtil = ProfileUtil.getInstance()
            profileUtil.applyAudioSettings(mutableProfile!!)
        }
    }

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