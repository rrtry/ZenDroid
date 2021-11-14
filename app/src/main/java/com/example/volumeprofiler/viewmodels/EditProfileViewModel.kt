package com.example.volumeprofiler.viewmodels

import android.app.NotificationManager
import android.media.RingtoneManager
import android.net.Uri
import androidx.lifecycle.*
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.channels.*
import android.app.NotificationManager.Policy.*
import kotlin.collections.ArrayList
import android.media.AudioManager.*
import android.util.Log
import com.example.volumeprofiler.activities.EditProfileActivity
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.util.ContentResolverUtil
import com.example.volumeprofiler.util.interruptionPolicy.isNotificationStreamActive
import com.example.volumeprofiler.util.interruptionPolicy.isRingerStreamActive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
        private val alarmRepository: AlarmRepository,
        private val contentResolverUtil: ContentResolverUtil
): ViewModel() {

    var areArgsSet: Boolean = false

    sealed class Event {

        object NavigateToPreviousFragment: Event()
        object NavigateToNextFragment: Event()
        object StoragePermissionRequestEvent: Event()
        object NotificationPolicyRequestEvent: Event()
        object PhonePermissionRequestEvent: Event()
        object ShowPopupWindowEvent: Event()
        object StartContactsActivity: Event()
        object WriteSystemSettingsRequestEvent: Event()

        data class SaveChangesEvent(val profile: Profile, val shouldUpdate: Boolean): Event()
        data class ShowDialogFragment(val dialogType: DialogType): Event()
        data class ChangeRingerMode(val streamType: Int, val showToast: Boolean, val vibrate: Boolean): Event()
        data class ChangeRingtoneEvent(val ringtoneType: Int): Event()
        data class ShowPopupWindow(val category: Int): Event()
    }

    enum class DialogType {
        PRIORITY,
        SUPPRESSED_EFFECTS_ON,
        SUPPRESSED_EFFECTS_OFF,
        TITLE
    }

    val isNew: MutableLiveData<Boolean> = MutableLiveData(false)

    private val profileIdLiveData = MutableLiveData<UUID>()
    private val alarmsLiveData: LiveData<List<AlarmRelation>?> = Transformations.switchMap(profileIdLiveData) { profileId -> alarmRepository.observeAlarmsByProfileId(profileId).asLiveData() }

    val title: MutableLiveData<String> = MutableLiveData("New profile")
    val currentFragmentTag: MutableLiveData<String> = MutableLiveData(EditProfileActivity.TAG_PROFILE_FRAGMENT)
    val mediaVolume: MutableLiveData<Int> = MutableLiveData()
    val callVolume: MutableLiveData<Int> = MutableLiveData()
    val notificationVolume: MutableLiveData<Int> = MutableLiveData()
    val ringVolume: MutableLiveData<Int> = MutableLiveData()
    val alarmVolume: MutableLiveData<Int> = MutableLiveData()
    val vibrateForCalls: MutableLiveData<Int> = MutableLiveData()
    val phoneRingtoneUri: MutableLiveData<Uri> = MutableLiveData()
    val notificationSoundUri: MutableLiveData<Uri> = MutableLiveData()
    val alarmSoundUri: MutableLiveData<Uri> = MutableLiveData()

    val phoneRingtoneTitle: LiveData<String> = phoneRingtoneUri.map { uri -> contentResolverUtil.getRingtoneTitle(uri, RingtoneManager.TYPE_RINGTONE) }
    val notificationRingtoneTitle: LiveData<String> = notificationSoundUri.map { uri -> contentResolverUtil.getRingtoneTitle(uri, RingtoneManager.TYPE_NOTIFICATION) }
    val alarmRingtoneTitle: LiveData<String> = alarmSoundUri.map { uri -> contentResolverUtil.getRingtoneTitle(uri, RingtoneManager.TYPE_ALARM) }

    val streamsUnlinked: MutableLiveData<Boolean> = MutableLiveData(false)

    val interruptionFilter: MutableLiveData<Int> = MutableLiveData()
    val priorityCategories: MutableLiveData<ArrayList<Int>> = MutableLiveData()
    val priorityCallSenders: MutableLiveData<Int> = MutableLiveData()
    val priorityMessageSenders: MutableLiveData<Int> = MutableLiveData()
    val screenOnVisualEffects: MutableLiveData<ArrayList<Int>> = MutableLiveData()
    val screenOffVisualEffects: MutableLiveData<ArrayList<Int>> = MutableLiveData()
    val primaryConversationSenders: MutableLiveData<Int> = MutableLiveData()

    val storagePermissionGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    val phonePermissionGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    val notificationPolicyAccessGranted: MutableLiveData<Boolean> = MutableLiveData(false)
    val canWriteSettings: MutableLiveData<Boolean> = MutableLiveData(false)

    val ringerMode: MutableLiveData<Int> = MutableLiveData(RINGER_MODE_NORMAL)
    val notificationMode: MutableLiveData<Int> = MutableLiveData(RINGER_MODE_NORMAL)
    var previousStreamVolume = 3

    private val activityEventChannel: Channel<Event> = Channel(Channel.BUFFERED)
    private val eventChannel: Channel<Event> = Channel(Channel.BUFFERED)
    val fragmentEventsFlow: Flow<Event> = eventChannel.receiveAsFlow()
    val activityEventsFlow: Flow<Event> = activityEventChannel.receiveAsFlow()

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

        streamsUnlinked.value = profile.streamsUnlinked

        interruptionFilter.value = profile.interruptionFilter
        ringerMode.value = profile.ringerMode
        notificationMode.value = profile.notificationMode
        vibrateForCalls.value = profile.isVibrateForCallsActive

        priorityCategories.value = profile.priorityCategories
        priorityCallSenders.value = profile.priorityCallSenders
        priorityMessageSenders.value = profile.priorityMessageSenders
        screenOnVisualEffects.value = profile.screenOnVisualEffects
        screenOffVisualEffects.value = profile.screenOffVisualEffects
        primaryConversationSenders.value = profile.primaryConversationSenders
    }

    fun setArgs(profile: Profile, hasExtras: Boolean): Unit {
        if (!areArgsSet) {
            setBindings(profile)
            if (hasExtras) {
                setProfileID(profile.id)
            } else {
                isNew.value = true
            }
            areArgsSet = true
        }
    }

    private fun setActualRingerMode(): Unit {
        if (!isNotificationStreamActive(interruptionFilter.value!!, priorityCategories.value!!, notificationPolicyAccessGranted.value!!)) {
            notificationMode.value = RINGER_MODE_SILENT
        }
        if (!isRingerStreamActive(interruptionFilter.value!!, priorityCategories.value!!, notificationPolicyAccessGranted.value!!, streamsUnlinked.value!!)) {
            ringerMode.value = RINGER_MODE_SILENT
        }
    }

    fun getProfile(): Profile {
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
                streamsUnlinked.value!!,
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
                onStreamMuted(STREAM_RING, showToast = false, vibrate = false)
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

    fun getAlarms(): List<AlarmRelation>? {
        return alarmsLiveData.value
    }

    fun shouldUpdateProfile(): Boolean {
        return profileIdLiveData.value != null
    }

    fun usesUnlinkedStreams(): Boolean {
        return streamsUnlinked.value!!
    }

    fun onSaveChangesButtonClick(): Unit {
        setActualRingerMode()
        viewModelScope.launch {
            activityEventChannel.send(Event.SaveChangesEvent(getProfile(),profileIdLiveData.value != null))
        }
    }

    private fun setProfileID(id : UUID) {
        profileIdLiveData.value = id
    }

    fun onVibrateForCallsLayoutClick(): Unit {
        if (canWriteSettings.value!!) {
            if (vibrateForCalls.value == 1) {
                vibrateForCalls.value = 0
            } else {
                vibrateForCalls.value = 1
            }
        } else {
            viewModelScope.launch {
                eventChannel.send(Event.WriteSystemSettingsRequestEvent)
            }
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

    fun onUnlinkStreamsLayoutClick(): Unit {
        viewModelScope.launch {
            if (phonePermissionGranted.value!!) {
                streamsUnlinked.value = !streamsUnlinked.value!!
            } else {
                eventChannel.send(Event.PhonePermissionRequestEvent)
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

    private fun onStreamMuted(streamType: Int, showToast: Boolean, vibrate: Boolean): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ChangeRingerMode(streamType, showToast, vibrate))
        }
    }

    fun onStreamVolumeChanged(value: Int, fromUser: Boolean, streamType: Int): Unit {
        if (fromUser) {
            when {
                value == 0 -> {
                    if (streamType == STREAM_VOICE_CALL) {
                        callVolume.value = 1
                    }
                    else if (streamType == STREAM_ALARM) {
                        alarmVolume.value = 1
                    }
                    else {
                        onStreamMuted(streamType, showToast = true, vibrate = true)
                    }
                }
                streamType == STREAM_NOTIFICATION && notificationMode.value != RINGER_MODE_NORMAL -> {
                    notificationMode.value = RINGER_MODE_NORMAL
                }
                streamType == STREAM_RING && ringerMode.value != RINGER_MODE_NORMAL -> {
                    ringerMode.value = RINGER_MODE_NORMAL
                }
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

    fun containsPriorityCategory(category: Int): Boolean {
        return priorityCategories.value!!.contains(category)
    }

    private fun containsSuppressedEffect(effect: Int): Boolean {
        return if (effect == SUPPRESSED_EFFECT_SCREEN_ON) screenOnVisualEffects.value!!.contains(effect)
        else screenOffVisualEffects.value!!.contains(effect)
    }

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