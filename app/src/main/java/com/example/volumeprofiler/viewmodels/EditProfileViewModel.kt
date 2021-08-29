package com.example.volumeprofiler.viewmodels

import android.app.NotificationManager
import android.media.AudioManager
import android.net.Uri
import androidx.lifecycle.*
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.util.AlarmUtil
import kotlinx.coroutines.launch
import java.util.*
import android.util.Log

class EditProfileViewModel: ViewModel() {

    private val repository = Repository.get()

    private val profileIdLiveData = MutableLiveData<UUID>()
    var alarmsLiveData: LiveData<List<AlarmTrigger>?> = Transformations.switchMap(profileIdLiveData) { profileId -> repository.observeProfileWithScheduledAlarms(profileId).asLiveData() }

    val title: MutableLiveData<String> = MutableLiveData("PlaceHolder")
    val mediaVolume: MutableLiveData<Int> = MutableLiveData(0)
    val callVolume: MutableLiveData<Int> = MutableLiveData(1)
    val notificationVolume: MutableLiveData<Int> = MutableLiveData(0)
    val ringVolume: MutableLiveData<Int> = MutableLiveData(0)
    val alarmVolume: MutableLiveData<Int> = MutableLiveData(1)

    val phoneRingtoneUri: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)
    val notificationSoundUri: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)
    val alarmSoundUri: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)

    val ringerMode: MutableLiveData<Int> = MutableLiveData(AudioManager.RINGER_MODE_NORMAL)
    val vibrateForCalls: MutableLiveData<Int> = MutableLiveData(0)

    val interruptionFilter: MutableLiveData<Int> = MutableLiveData(NotificationManager.INTERRUPTION_FILTER_ALL)
    val priorityCategories: MutableLiveData<ArrayList<Int>> = MutableLiveData(arrayListOf(NotificationManager.Policy.PRIORITY_CATEGORY_CALLS, NotificationManager.Policy.PRIORITY_CATEGORY_MESSAGES))
    val priorityCallSenders: MutableLiveData<Int> = MutableLiveData(NotificationManager.Policy.PRIORITY_SENDERS_ANY)
    val priorityMessageSenders: MutableLiveData<Int> = MutableLiveData(NotificationManager.Policy.PRIORITY_SENDERS_ANY)
    val screenOnVisualEffects: MutableLiveData<ArrayList<Int>> = MutableLiveData(arrayListOf())
    val screenOffVisualEffects: MutableLiveData<ArrayList<Int>> = MutableLiveData(arrayListOf())
    val primaryConversationSenders: MutableLiveData<Int> = MutableLiveData(NotificationManager.Policy.CONVERSATION_SENDERS_ANYONE)

    private fun getProfile(id: UUID?): Profile {
        return Profile(
                title.value!!,
                if (id == null) UUID.randomUUID() else UUID.randomUUID(),
                mediaVolume.value!!,
                callVolume.value!!,
                notificationVolume.value!!,
                ringerMode.value!!,
                alarmVolume.value!!,
                phoneRingtoneUri.value!!,
                notificationSoundUri.value!!,
                alarmSoundUri.value!!,
                interruptionFilter.value!!,
                vibrateForCalls.value!!,
                ringerMode.value!!,
                vibrateForCalls.value!!,
                priorityCategories.value!!,
                priorityCallSenders.value!!,
                priorityMessageSenders.value!!,
                screenOnVisualEffects.value!!,
                screenOffVisualEffects.value!!,
                primaryConversationSenders.value!!
        )
    }

    private fun setAlarm(alarmTrigger: AlarmTrigger, newProfile: Profile): Unit {
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        alarmUtil.setAlarm(alarmTrigger.alarm, newProfile, false)
    }

    fun setMultipleAlarms(triggers: List<AlarmTrigger>, newProfile: Profile): Unit {
        for (i in triggers) {
            setAlarm(i, newProfile)
        }
    }

    /*
    fun applyAudioSettingsIfActive(): Unit {
        val sharedPreferencesUtil = SharedPreferencesUtil.getInstance()
        if (sharedPreferencesUtil.getActiveProfileId()
                == mutableProfile!!.id.toString()) {
            val profileUtil: ProfileUtil = ProfileUtil.getInstance()
            profileUtil.applyAudioSettings(mutableProfile!!)
        }
    }
     */

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