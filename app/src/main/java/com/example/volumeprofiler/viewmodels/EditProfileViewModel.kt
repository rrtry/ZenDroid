package com.example.volumeprofiler.viewmodels

import android.app.NotificationManager
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.*
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.models.Profile
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.receiveAsFlow
import android.app.NotificationManager.Policy.*
import kotlin.collections.ArrayList
import android.media.AudioManager.*
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.database.repositories.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
        private val alarmRepository: AlarmRepository,
): ViewModel() {

    var areArgsSet: Boolean = false
    var hasArgs: Boolean = false

    sealed class Event {

        object NavigateToNextFragment: Event()
        object StoragePermissionRequestEvent: Event()
        object NotificationPolicyRequestEvent: Event()
        object ShowPopupWindowEvent: Event()
        object StartContactsActivity: Event()

        data class SaveChangesEvent(val profile: Profile, val shouldUpdate: Boolean): Event()
        data class ShowDialogFragment(val dialogType: DialogType): Event()
        data class ChangeRingerMode(val streamType: Int, val fromUser: Boolean): Event()
        data class ChangeRingtoneEvent(val ringtoneType: Int): Event()
        data class ShowPopupWindow(val category: Int): Event()
    }

    enum class DialogType {
        PRIORITY,
        SUPPRESSED_EFFECTS_ON,
        SUPPRESSED_EFFECTS_OFF,
        TITLE
    }

    private val profileIdLiveData = MutableLiveData<UUID>()
    private val alarmsLiveData: LiveData<List<AlarmTrigger>?> = Transformations.switchMap(profileIdLiveData) { profileId -> alarmRepository.observeAlarmTriggersByProfileId(profileId).asLiveData() }

    val title: MutableLiveData<String> = MutableLiveData("New profile")
    val currentFragmentTag: MutableLiveData<String> = MutableLiveData(EditProfileActivity.TAG_PROFILE_FRAGMENT)
    val mediaVolume: MutableLiveData<Int> = MutableLiveData(0)
    val callVolume: MutableLiveData<Int> = MutableLiveData(1)
    val notificationVolume: MutableLiveData<Int> = MutableLiveData(3)
    val ringVolume: MutableLiveData<Int> = MutableLiveData(3)
    val alarmVolume: MutableLiveData<Int> = MutableLiveData(1)
    val vibrateForCalls: MutableLiveData<Int> = MutableLiveData(0)
    val phoneRingtoneUri: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)
    val notificationSoundUri: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)
    val alarmSoundUri: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)

    val interruptionFilter: MutableLiveData<Int> = MutableLiveData(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    val priorityCategories: MutableLiveData<ArrayList<Int>> = MutableLiveData(arrayListOf(PRIORITY_CATEGORY_MESSAGES))
    val priorityCallSenders: MutableLiveData<Int> = MutableLiveData(0)
    val priorityMessageSenders: MutableLiveData<Int> = MutableLiveData(PRIORITY_SENDERS_CONTACTS)
    val screenOnVisualEffects: MutableLiveData<ArrayList<Int>> = MutableLiveData(arrayListOf())
    val screenOffVisualEffects: MutableLiveData<ArrayList<Int>> = MutableLiveData(arrayListOf())
    val primaryConversationSenders: MutableLiveData<Int> = MutableLiveData(CONVERSATION_SENDERS_ANYONE)

    val storagePermissionGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    val notificationPolicyAccessGranted: MutableLiveData<Boolean> = MutableLiveData(false)

    val ringerMode: MutableLiveData<Int> = MutableLiveData(RINGER_MODE_NORMAL)
    val notificationMode: MutableLiveData<Int> = MutableLiveData(RINGER_MODE_NORMAL)
    var previousStreamVolume = 3

    private val activityEventChannel: Channel<Event> = Channel(Channel.BUFFERED)
    private val eventChannel: Channel<Event> = Channel(Channel.BUFFERED)
    val fragmentEventsFlow: Flow<Event> = eventChannel.receiveAsFlow()
    val activityEventsFlow: Flow<Event> = activityEventChannel.receiveAsFlow()

    fun setArgs(profile: Profile, hasArguments: Boolean): Unit {
        if (!areArgsSet) {
            areArgsSet = true
            hasArgs = hasArguments
            setBindings(profile)
            if (hasArguments) {
                setProfileID(profile.id)
            }
        }
    }

    private fun getProfile(): Profile {
        return Profile(
                title.value!!,
                if (profileIdLiveData.value == null) UUID.randomUUID() else profileIdLiveData.value!!,
                mediaVolume.value!!,
                callVolume.value!!,
                notificationVolume.value!!,
                ringVolume.value!!,
                alarmVolume.value!!,
                phoneRingtoneUri.value!!,
                notificationSoundUri.value!!,
                alarmSoundUri.value!!,
                interruptionFilter.value!!,
                ringerMode.value!!,
                notificationMode.value!!,
                vibrateForCalls.value!!,
                priorityCategories.value!!,
                priorityCallSenders.value!!,
                priorityMessageSenders.value!!,
                screenOnVisualEffects.value!!,
                screenOffVisualEffects.value!!,
                primaryConversationSenders.value!!
        )
    }

    fun onSilentModeLayoutClick(): Unit {
        if (ringerMode.value == RINGER_MODE_SILENT) {
            if (previousStreamVolume == 0) {
                ringerMode.value = RINGER_MODE_VIBRATE
            } else {
                ringerMode.value = RINGER_MODE_NORMAL
            }
            ringVolume.value = previousStreamVolume
        } else {
            previousStreamVolume = ringVolume.value!!
            ringVolume.value = 0
            ringerMode.value = RINGER_MODE_SILENT
        }
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
        notificationMode.value = profile.notificationMode
        vibrateForCalls.value = profile.isVibrateForCallsActive
    }

    fun getAlarms(): List<AlarmTrigger>? {
        return alarmsLiveData.value
    }

    fun onSaveChangesButtonClick(): Unit {
        viewModelScope.launch {
            activityEventChannel.send(Event.SaveChangesEvent(getProfile(),profileIdLiveData.value != null))
        }
    }

    private fun setProfileID(id : UUID) {
        profileIdLiveData.value = id
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
            if (storagePermissionGranted.value!!) {
                eventChannel.send(Event.ChangeRingtoneEvent(RingtoneManager.TYPE_NOTIFICATION))
            } else {
                eventChannel.send(Event.StoragePermissionRequestEvent)
            }
        }
    }

    fun onRingtoneLayoutClick(): Unit {
        viewModelScope.launch {
            if (storagePermissionGranted.value!!) {
                eventChannel.send(Event.ChangeRingtoneEvent(RingtoneManager.TYPE_RINGTONE))
            } else {
                eventChannel.send(Event.StoragePermissionRequestEvent)
            }
        }
    }

    fun onAlarmSoundLayoutClick(): Unit {
        viewModelScope.launch {
            if (storagePermissionGranted.value!!) {
                eventChannel.send(Event.ChangeRingtoneEvent(RingtoneManager.TYPE_ALARM))
            } else {
                eventChannel.send(Event.StoragePermissionRequestEvent)
            }
        }
    }

    fun onInterruptionFilterLayoutClick(): Unit {
        viewModelScope.launch {
            if (notificationPolicyAccessGranted.value!!) {
                eventChannel.send(Event.ShowPopupWindowEvent)
            } else {
                eventChannel.send(Event.NotificationPolicyRequestEvent)
            }
        }
    }

    fun onPreferencesLayoutClick(): Unit {
        viewModelScope.launch {
            if (notificationPolicyAccessGranted.value!!) {
                eventChannel.send(Event.NavigateToNextFragment)
            } else {
                eventChannel.send(Event.NotificationPolicyRequestEvent)
            }
        }
    }

    private fun onStreamMuted(streamType: Int, fromUser: Boolean): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ChangeRingerMode(streamType, fromUser))
        }
    }

    fun onStreamVolumeChanged(value: Int, fromUser: Boolean, streamType: Int): Unit {
        if (fromUser) {
            if (value == 0) {
                onStreamMuted(streamType, fromUser)
            } else if (streamType == STREAM_NOTIFICATION && notificationMode.value != RINGER_MODE_NORMAL) {
                notificationMode.value = RINGER_MODE_NORMAL
            } else if (streamType == STREAM_RING && ringerMode.value != RINGER_MODE_NORMAL) {
                ringerMode.value = RINGER_MODE_NORMAL
            }
        }
    }

    fun onCallsLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ShowPopupWindow(PRIORITY_CATEGORY_CALLS))
        }
    }

    fun onRepetitiveCallersLayoutClick(): Unit {
        if (containsPriorityCategory(PRIORITY_CATEGORY_REPEAT_CALLERS)) {
            removePriorityCategory(PRIORITY_CATEGORY_REPEAT_CALLERS)
        } else {
            addPriorityCategory(PRIORITY_CATEGORY_REPEAT_CALLERS)
        }
    }

    fun onStarredContactsLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.StartContactsActivity)
        }
    }

    fun onMessagesLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ShowPopupWindow(PRIORITY_CATEGORY_MESSAGES))
        }
    }

    fun onPriorityInterruptionsLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ShowDialogFragment(DialogType.PRIORITY))
        }
    }

    fun onSuppressedEffectsOnLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ShowDialogFragment(DialogType.SUPPRESSED_EFFECTS_ON))
        }
    }

    fun onChangeTitleButtonClick(): Unit {
        viewModelScope.launch {
            activityEventChannel.send(Event.ShowDialogFragment(DialogType.TITLE))
        }
    }

    fun onSuppressedEffectsOffLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ShowDialogFragment(DialogType.SUPPRESSED_EFFECTS_OFF))
        }
    }

    fun onSuppressedEffectLayoutClick(effect: Int): Unit {
        if (containsSuppressedEffect(effect)) {
            removeSuppressedEffect(effect)
        } else {
            addSuppressedEffect(effect)
        }
    }

    fun updateSoundUris(): Unit {
        phoneRingtoneUri.value = phoneRingtoneUri.value
        notificationSoundUri.value = notificationSoundUri.value
        alarmSoundUri.value = alarmSoundUri.value
    }

    fun containsPriorityCategory(category: Int): Boolean = priorityCategories.value!!.contains(category)

    private fun containsSuppressedEffect(effect: Int): Boolean = if (effect == SUPPRESSED_EFFECT_SCREEN_ON) screenOnVisualEffects.value!!.contains(effect) else screenOffVisualEffects.value!!.contains(effect)

    private fun addSuppressedEffect(effect: Int): Unit {
        if (!containsSuppressedEffect(effect)) {
            if (effect == SUPPRESSED_EFFECT_SCREEN_ON) {
                screenOnVisualEffects.value!!.add(effect)
                screenOnVisualEffects.value = screenOnVisualEffects.value!!
            } else {
                screenOffVisualEffects.value!!.add(effect)
                screenOffVisualEffects.value = screenOffVisualEffects.value
            }
        }
    }

    private fun removeSuppressedEffect(effect: Int): Unit {
        if (effect == SUPPRESSED_EFFECT_SCREEN_ON) {
            screenOnVisualEffects.value?.remove(effect)
            screenOnVisualEffects.value = screenOnVisualEffects.value
        } else {
            screenOffVisualEffects.value?.remove(effect)
            screenOffVisualEffects.value = screenOffVisualEffects.value
        }
    }

    fun addPriorityCategory(category: Int): Unit {
        if (!containsPriorityCategory(category)) {
            priorityCategories.value!!.add(category)
            priorityCategories.value = priorityCategories.value
        }
    }

    fun removePriorityCategory(category: Int): Unit {
        priorityCategories.value!!.remove(category)
        priorityCategories.value = priorityCategories.value
    }

    override fun onCleared() {
        super.onCleared()
        activityEventChannel.close()
        eventChannel.close()
    }
}