package com.example.volumeprofiler.viewmodels

import android.app.NotificationManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.*
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.Profile
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.channels.*
import android.util.Log
import kotlinx.coroutines.flow.receiveAsFlow
import android.app.NotificationManager.Policy.*

class EditProfileViewModel: ViewModel() {

    private val repository = Repository.get()
    private var isSet: Boolean = false

    sealed class Event {
        object StoragePermissionRequestEvent: Event()
        object ShowPopupWindowEvent: Event()
        data class ChangeRingtoneEvent(val ringtoneType: Int): Event()
        data class ResetAlarmsEvent(val alarms: List<AlarmTrigger>, val profile: Profile): Event()
    }

    private val profileIdLiveData = MutableLiveData<UUID>()
    private val alarmsLiveData: LiveData<List<AlarmTrigger>?> = Transformations.switchMap(profileIdLiveData) { profileId -> repository.observeProfileWithScheduledAlarms(profileId).asLiveData() }

    val title: MutableLiveData<String> = MutableLiveData("New profile")
    val mediaVolume: MutableLiveData<Int> = MutableLiveData(0)
    val callVolume: MutableLiveData<Int> = MutableLiveData(1)
    val notificationVolume: MutableLiveData<Int> = MutableLiveData(0)
    val ringVolume: MutableLiveData<Int> = MutableLiveData(0)
    val alarmVolume: MutableLiveData<Int> = MutableLiveData(1)
    val ringerMode: MutableLiveData<Int> = MutableLiveData(AudioManager.RINGER_MODE_SILENT)
    val vibrateForCalls: MutableLiveData<Int> = MutableLiveData(0)
    val phoneRingtoneUri: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)
    val notificationSoundUri: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)
    val alarmSoundUri: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)
    val interruptionFilter: MutableLiveData<Int> = MutableLiveData(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    val priorityCategories: MutableLiveData<List<Int>> = MutableLiveData(listOf(PRIORITY_CATEGORY_MEDIA))

    /*
        Events channel
     */
    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val eventsFlow = eventChannel.receiveAsFlow()

    fun setArgs(profile: Profile, hasArguments: Boolean): Unit {
        if (!isSet) {
            isSet = true
            setBindings(profile)
            if (hasArguments) {
                setProfileID(profile.id)
            }
        }
    }

    fun onSilentModeLayoutClick(): Unit {
        if (ringerMode.value == AudioManager.RINGER_MODE_SILENT) {
            ringerMode.value = AudioManager.RINGER_MODE_NORMAL
        } else {
            ringerMode.value = AudioManager.RINGER_MODE_SILENT
        }
        Log.i("EditProfileViewModel", "onSilentModeLayoutClick")
    }

    private fun setBindings(profile: Profile): Unit {
        title.value = profile.title
        mediaVolume.value = profile.mediaVolume
        callVolume.value = profile.callVolume
        notificationVolume.value = profile.notificationVolume
        ringVolume.value = profile.ringVolume
        alarmVolume.value = profile.alarmVolume
        phoneRingtoneUri.value = profile.phoneRingtoneUri
        notificationSoundUri.value = profile.notificationSoundUri
        alarmSoundUri.value = profile.alarmSoundUri
        ringerMode.value = profile.ringerMode
        vibrateForCalls.value = profile.isVibrateForCallsActive
    }

    private fun setProfileID(id : UUID) {
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

    fun onVibrateForCallsLayoutClick(): Unit {
        if (vibrateForCalls.value == 1) {
            vibrateForCalls.value = 0
        } else {
            vibrateForCalls.value = 1
        }
    }

    fun onNotificationSoundLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ChangeRingtoneEvent(RingtoneManager.TYPE_NOTIFICATION))
        }
    }

    fun onRingtoneLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ChangeRingtoneEvent(RingtoneManager.TYPE_RINGTONE))
        }
    }

    fun onAlarmSoundLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ChangeRingtoneEvent(RingtoneManager.TYPE_ALARM))
        }
    }

    fun onInterruptionFilterLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ShowPopupWindowEvent)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("EditProfileActivity", "onCleared")
    }

}