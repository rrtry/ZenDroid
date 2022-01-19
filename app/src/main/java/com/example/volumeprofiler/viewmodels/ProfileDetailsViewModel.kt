package com.example.volumeprofiler.viewmodels

import android.net.Uri
import androidx.lifecycle.*
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.channels.*
import android.app.NotificationManager.Policy.*
import android.media.AudioManager.*
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.util.ContentUtil
import com.example.volumeprofiler.util.interruptionPolicy.interruptionPolicyAllowsRingerStream
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import android.app.NotificationManager.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import android.media.RingtoneManager.*
import com.example.volumeprofiler.activities.ProfileDetailsActivity.Companion.TAG_PROFILE_FRAGMENT
import com.example.volumeprofiler.entities.Profile.Companion.STREAM_ALARM_DEFAULT_VOLUME
import com.example.volumeprofiler.entities.Profile.Companion.STREAM_MUSIC_DEFAULT_VOLUME
import com.example.volumeprofiler.entities.Profile.Companion.STREAM_NOTIFICATION_DEFAULT_VOLUME
import com.example.volumeprofiler.entities.Profile.Companion.STREAM_RING_DEFAULT_VOLUME
import com.example.volumeprofiler.entities.Profile.Companion.STREAM_VOICE_CALL_DEFAULT_VOLUME
import com.example.volumeprofiler.util.interruptionPolicy.interruptionPolicyAllowsAlarmsStream
import com.example.volumeprofiler.util.interruptionPolicy.interruptionPolicyAllowsMediaStream
import com.example.volumeprofiler.util.interruptionPolicy.interruptionPolicyAllowsNotificationStream
import kotlinx.coroutines.FlowPreview

@HiltViewModel
class ProfileDetailsViewModel @Inject constructor(
        private val alarmRepository: AlarmRepository,
        private val contentUtil: ContentUtil
): ViewModel() {

    private val activityEventChannel: Channel<Event> = Channel(Channel.BUFFERED)
    private val eventChannel: Channel<Event> = Channel(Channel.BUFFERED)

    val fragmentEventsFlow: Flow<Event> = eventChannel.receiveAsFlow()
    val activityEventsFlow: Flow<Event> = activityEventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val ringtoneType: List<Int> = listOf(TYPE_ALARM, TYPE_NOTIFICATION, TYPE_RINGTONE)
            for (i in ringtoneType) {
                eventChannel.send(Event.GetDefaultRingtoneUri(i))
            }
        }
    }

    private var areArgsSet: Boolean = false
    private var previousRingerVolume: Int = STREAM_RING_DEFAULT_VOLUME
    private var previousNotificationVolume: Int = STREAM_NOTIFICATION_DEFAULT_VOLUME

    var ringtoneUri: Uri = Uri.EMPTY
    var alarmUri: Uri = Uri.EMPTY
    var notificationUri: Uri = Uri.EMPTY

    sealed class Event {

        object NavigateToNextFragment: Event()
        object StoragePermissionRequestEvent: Event()
        object NotificationPolicyRequestEvent: Event()
        object PhonePermissionRequestEvent: Event()
        object ShowPopupWindowEvent: Event()
        object StartContactsActivity: Event()
        object WriteSystemSettingsRequestEvent: Event()

        data class StartRingtonePlayback(val streamType: Int, val volume: Int): Event()
        data class StopRingtonePlayback(val streamType: Int): Event()
        data class ApplyChangesEvent(val profile: Profile, val shouldUpdate: Boolean): Event()
        data class ShowDialogFragment(val dialogType: DialogType): Event()
        data class ChangeRingerMode(val streamType: Int, val showToast: Boolean, val vibrate: Boolean): Event()
        data class GetDefaultRingtoneUri(val type: Int): Event()
        data class ChangeRingtoneEvent(val ringtoneType: Int): Event()
        data class ShowPopupWindow(val category: Int): Event()
        data class AlarmStreamVolumeChanged(val streamType: Int, val volume: Int): Event()
        data class StreamVolumeChanged(val streamType: Int, val volume: Int): Event()
    }

    enum class DialogType {
        PRIORITY,
        SUPPRESSED_EFFECTS_ON,
        SUPPRESSED_EFFECTS_OFF,
        TITLE
    }

    val isNew: MutableStateFlow<Boolean> = MutableStateFlow(false)

    private val profileUUID = MutableStateFlow<UUID?>(null)

    @FlowPreview
    val alarmsFlow: Flow<List<AlarmRelation>?> = profileUUID.flatMapConcat {
        if (it != null) {
            alarmRepository.observeAlarmsByProfileId(it)
        } else {
            flowOf(listOf())
        }
    }

    val title: MutableStateFlow<String> = MutableStateFlow("New profile")
    val currentFragmentTag: MutableStateFlow<String> = MutableStateFlow(TAG_PROFILE_FRAGMENT)
    val mediaVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_MUSIC_DEFAULT_VOLUME)
    val callVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_VOICE_CALL_DEFAULT_VOLUME)
    val notificationVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_NOTIFICATION_DEFAULT_VOLUME)
    val ringVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_RING_DEFAULT_VOLUME)
    val alarmVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_ALARM_DEFAULT_VOLUME)
    val vibrateForCalls: MutableStateFlow<Int> = MutableStateFlow(1)
    val phoneRingtoneUri: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)
    val notificationSoundUri: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)
    val alarmSoundUri: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)

    val phoneRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val phoneRingtoneTitle: StateFlow<String> = phoneRingtoneUri.map { uri ->  contentUtil.getRingtoneTitle(uri, TYPE_RINGTONE) }
        .stateIn(viewModelScope, WhileSubscribed(1000), "")

    val notificationRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val notificationRingtoneTitle: StateFlow<String> = notificationSoundUri.map { uri -> contentUtil.getRingtoneTitle(uri, TYPE_NOTIFICATION) }
        .stateIn(viewModelScope, WhileSubscribed(1000), "")

    val alarmRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val alarmRingtoneTitle: StateFlow<String> = alarmSoundUri.map { uri -> contentUtil.getRingtoneTitle(uri, TYPE_ALARM) }
        .stateIn(viewModelScope, WhileSubscribed(1000), "")

    val voiceCallRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val musicRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val streamsUnlinked: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val interruptionFilter: MutableStateFlow<Int> = MutableStateFlow(INTERRUPTION_FILTER_PRIORITY)
    val priorityCategories: MutableStateFlow<Int> = MutableStateFlow(0)
    val priorityCallSenders: MutableStateFlow<Int> = MutableStateFlow(PRIORITY_SENDERS_ANY)
    val priorityMessageSenders: MutableStateFlow<Int> = MutableStateFlow(PRIORITY_SENDERS_ANY)
    val suppressedVisualEffects: MutableStateFlow<Int> = MutableStateFlow(0)
    val primaryConversationSenders: MutableStateFlow<Int> = MutableStateFlow(CONVERSATION_SENDERS_ANYONE)

    val storagePermissionGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val phonePermissionGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val notificationPolicyAccessGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val canWriteSettings: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val ringerMode: MutableStateFlow<Int> = MutableStateFlow(RINGER_MODE_NORMAL)
    val notificationMode: MutableStateFlow<Int> = MutableStateFlow(RINGER_MODE_SILENT)

    val policyAllowsMediaStream: Flow<Boolean> = combine(
        interruptionFilter,
        priorityCategories,
        notificationPolicyAccessGranted) {
        filter, categories, granted -> interruptionPolicyAllowsMediaStream(filter, categories, granted)
    }

    val policyAllowsAlarmStream: Flow<Boolean> = combine(
        interruptionFilter,
        priorityCategories,
        notificationPolicyAccessGranted) {
            filter, categories, granted -> interruptionPolicyAllowsAlarmsStream(filter, categories, granted)
    }

    val policyAllowsRingerStream: Flow<Boolean> = combine(
        interruptionFilter,
        priorityCategories,
        notificationPolicyAccessGranted,
        streamsUnlinked) {
            filter, categories, granted, unlinked -> interruptionPolicyAllowsRingerStream(filter, categories, granted, unlinked)
    }

    val policyAllowsNotificationsStream: Flow<Boolean> = combine(
        interruptionFilter,
        priorityCategories,
        notificationPolicyAccessGranted,
        streamsUnlinked) {
            filter, categories, granted, unlinked -> interruptionPolicyAllowsNotificationStream(filter, categories, granted, unlinked)
    }

    private fun setBindings(profile: Profile): Unit {

        title.value = profile.title

        mediaVolume.value = profile.mediaVolume
        callVolume.value = profile.callVolume
        notificationVolume.value = profile.notificationVolume
        ringVolume.value = profile.ringVolume
        alarmVolume.value = profile.alarmVolume

        previousNotificationVolume = profile.notificationVolume
        previousRingerVolume = profile.ringVolume

        phoneRingtoneUri.value = profile.phoneRingtoneUri
        notificationSoundUri.value = profile.notificationSoundUri
        alarmSoundUri.value = profile.alarmSoundUri

        ringtoneUri = profile.phoneRingtoneUri
        notificationUri = profile.notificationSoundUri
        alarmUri = profile.alarmSoundUri

        streamsUnlinked.value = profile.streamsUnlinked

        interruptionFilter.value = profile.interruptionFilter
        ringerMode.value = profile.ringerMode
        notificationMode.value = profile.notificationMode
        vibrateForCalls.value = profile.isVibrateForCallsActive

        priorityCategories.value = profile.priorityCategories
        priorityCallSenders.value = profile.priorityCallSenders
        priorityMessageSenders.value = profile.priorityMessageSenders
        suppressedVisualEffects.value = profile.suppressedVisualEffects
        primaryConversationSenders.value = profile.primaryConversationSenders
    }

    fun setArgs(profile: Profile, hasExtras: Boolean): Unit {
        if (!areArgsSet) {
            setBindings(profile)
            if (hasExtras) {
                setProfileUUID(profile.id)
            } else {
                isNew.value = true
            }
            areArgsSet = true
        }
    }

    fun getProfile(): Profile {
        return Profile(
            if (profileUUID.value == null) UUID.randomUUID() else profileUUID.value!!,
            title.value,
            mediaVolume.value,
            callVolume.value,
            notificationVolume.value,
            ringVolume.value,
            alarmVolume.value,
            phoneRingtoneUri.value,
            notificationSoundUri.value,
            alarmSoundUri.value,
            streamsUnlinked.value,
            interruptionFilter.value,
            ringerMode.value,
            notificationMode.value,
            vibrateForCalls.value,
            priorityCategories.value,
            priorityCallSenders.value,
            priorityMessageSenders.value,
            suppressedVisualEffects.value,
            primaryConversationSenders.value
        )
    }

    private fun updateProfile(): Boolean {
        return profileUUID.value != null
    }

    fun usesUnlinkedStreams(): Boolean {
        return streamsUnlinked.value
    }

    private fun setProfileUUID(uuid : UUID) {
        profileUUID.value = uuid
    }

    fun onSaveChangesButtonClick(): Unit {
        viewModelScope.launch {
            activityEventChannel.send(Event.ApplyChangesEvent(getProfile(),updateProfile()))
        }
    }

    fun onVibrateForCallsLayoutClick(): Unit {
        if (canWriteSettings.value) {
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
            if (storagePermissionGranted.value) {
                eventChannel.send(Event.ChangeRingtoneEvent(TYPE_NOTIFICATION))
            } else {
                eventChannel.send(Event.StoragePermissionRequestEvent)
            }
        }
    }

    private fun isRingtonePlaying(streamType: Int): Boolean {
        return when (streamType) {
            STREAM_MUSIC -> {
                musicRingtonePlaying.value
            }
            STREAM_VOICE_CALL -> {
                voiceCallRingtonePlaying.value
            }
            STREAM_NOTIFICATION -> {
                notificationRingtonePlaying.value
            }
            STREAM_RING -> {
                phoneRingtonePlaying.value
            }
            STREAM_ALARM -> {
                alarmRingtonePlaying.value
            }
            else -> false
        }
    }

    fun getStreamVolume(streamType: Int): Int {
        return when (streamType) {
            STREAM_MUSIC -> {
                mediaVolume.value
            }
            STREAM_VOICE_CALL -> {
                callVolume.value
            }
            STREAM_NOTIFICATION -> {
                notificationVolume.value
            }
            STREAM_RING -> {
                ringVolume.value
            }
            STREAM_ALARM -> {
                alarmVolume.value
            }
            else -> -1
        }
    }

    fun setPlaybackState(streamType: Int, playing: Boolean): Unit {
        when (streamType) {
            STREAM_MUSIC -> musicRingtonePlaying.value = playing
            STREAM_VOICE_CALL -> voiceCallRingtonePlaying.value = playing
            STREAM_NOTIFICATION -> notificationRingtonePlaying.value = playing
            STREAM_RING -> phoneRingtonePlaying.value = playing
            STREAM_ALARM -> alarmRingtonePlaying.value = playing
        }
    }

    private fun getPlayingRingtone(): Int {
        val properties: Map<Int, Boolean> = mapOf(
            STREAM_MUSIC to musicRingtonePlaying.value,
            STREAM_VOICE_CALL to voiceCallRingtonePlaying.value,
            STREAM_NOTIFICATION to notificationRingtonePlaying.value,
            STREAM_RING to phoneRingtonePlaying.value,
            STREAM_ALARM to alarmRingtonePlaying.value
        )
        for (i in properties.entries) {
            if (i.value) {
                return i.key
            }
        }
        return -1
    }

    fun onPlayRingtoneButtonClick(streamType: Int): Unit {
        if (isRingtonePlaying(streamType)) {
            viewModelScope.launch {
                eventChannel.send(Event.StopRingtonePlayback(streamType))
            }
        } else {
            viewModelScope.launch {
                eventChannel.send(Event.StopRingtonePlayback(getPlayingRingtone()))
                eventChannel.send(Event.StartRingtonePlayback(streamType, getStreamVolume(streamType)))
            }
        }
    }

    fun onRingtoneLayoutClick(): Unit {
        viewModelScope.launch {
            if (storagePermissionGranted.value) {
                eventChannel.send(Event.ChangeRingtoneEvent(TYPE_RINGTONE))
            } else {
                eventChannel.send(Event.StoragePermissionRequestEvent)
            }
        }
    }

    fun onUnlinkStreamsLayoutClick(): Unit {
        viewModelScope.launch {
            if (phonePermissionGranted.value) {
                streamsUnlinked.value = !streamsUnlinked.value
            } else {
                eventChannel.send(Event.PhonePermissionRequestEvent)
            }
        }
    }

    fun onAlarmSoundLayoutClick(): Unit {
        viewModelScope.launch {
            if (storagePermissionGranted.value) {
                eventChannel.send(Event.ChangeRingtoneEvent(TYPE_ALARM))
            } else {
                eventChannel.send(Event.StoragePermissionRequestEvent)
            }
        }
    }

    fun onMediaStreamVolumeChanged(index: Int, fromUser: Boolean): Unit {
        if (fromUser) {
            viewModelScope.launch {
                eventChannel.send(Event.StreamVolumeChanged(STREAM_MUSIC, index))
            }
        }
        mediaVolume.value = index
    }

    fun onAlarmStreamVolumeChanged(index: Int, fromUser: Boolean): Unit {
        if (fromUser) {
            viewModelScope.launch {
                eventChannel.send(Event.AlarmStreamVolumeChanged(STREAM_ALARM, index))
            }
        }
    }

    fun onVoiceCallStreamVolumeChanged(index: Int, fromUser: Boolean): Unit {
        if (fromUser) {
            viewModelScope.launch {
                eventChannel.send(Event.AlarmStreamVolumeChanged(STREAM_VOICE_CALL, index))
            }
        }
    }

    fun onInterruptionFilterLayoutClick(): Unit {
        viewModelScope.launch {
            if (notificationPolicyAccessGranted.value) {
                eventChannel.send(Event.ShowPopupWindowEvent)
            } else {
                eventChannel.send(Event.NotificationPolicyRequestEvent)
            }
        }
    }

    fun onRingerIconClick(): Unit {
        if (ringerStreamAllowed()) {
            if (ringerMode.value == RINGER_MODE_SILENT) {
                if (previousRingerVolume <= 0) {
                    onStreamMuted(STREAM_RING, showToast = false, vibrate = true)
                }
                else {
                    ringerMode.value = RINGER_MODE_NORMAL
                }
                ringVolume.value = previousRingerVolume
            }
            else {
                previousRingerVolume = ringVolume.value
                ringVolume.value = 0
                ringerMode.value = RINGER_MODE_SILENT
            }
        }
    }

    fun onNotificationIconClick(): Unit {
        if (notificationsStreamAllowed()) {
            if (notificationMode.value == RINGER_MODE_SILENT) {
                if (previousNotificationVolume <= 0) {
                    onStreamMuted(STREAM_NOTIFICATION, showToast = false, vibrate = true)
                } else {
                    notificationMode.value = RINGER_MODE_NORMAL
                }
                notificationVolume.value = previousNotificationVolume
            } else {
                previousNotificationVolume = notificationVolume.value
                notificationVolume.value = 0
                notificationMode.value = RINGER_MODE_SILENT
            }
        }
    }

    fun onPreferencesLayoutClick(): Unit {
        viewModelScope.launch {
            if (notificationPolicyAccessGranted.value) {
                eventChannel.send(Event.NavigateToNextFragment)
            } else {
                eventChannel.send(Event.NotificationPolicyRequestEvent)
            }
        }
    }

    private fun onStreamMuted(streamType: Int, showToast: Boolean, vibrate: Boolean): Unit {
        when (streamType) {
            STREAM_NOTIFICATION -> notificationVolume.value = 0
            STREAM_RING -> ringVolume.value = 0
        }
        viewModelScope.launch {
            eventChannel.send(Event.ChangeRingerMode(streamType, showToast, vibrate))
        }
    }

    fun onAlertStreamVolumeChanged(value: Int, fromUser: Boolean, streamType: Int): Unit {
        if (fromUser) {
            when {
                value == 0 -> {
                    onStreamMuted(streamType, showToast = false, vibrate = true)
                }
                streamType == STREAM_NOTIFICATION -> {
                    notificationVolume.value = value
                    notificationMode.value = RINGER_MODE_NORMAL
                }
                streamType == STREAM_RING -> {
                    ringVolume.value = value
                    ringerMode.value = RINGER_MODE_NORMAL
                }
            }
            when (streamType) {
                STREAM_NOTIFICATION -> previousNotificationVolume = value
                STREAM_RING -> previousRingerVolume = value
            }
            viewModelScope.launch {
                eventChannel.send(Event.StreamVolumeChanged(streamType, value))
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

    fun getRingtoneUri(type: Int): Uri {
        return when (type) {
            TYPE_RINGTONE -> {
                phoneRingtoneUri.value
            }
            TYPE_NOTIFICATION -> {
                notificationSoundUri.value
            }
            TYPE_ALARM -> {
                alarmSoundUri.value
            }
            else -> Uri.EMPTY
        }
    }

    fun onStarredContactsLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.StartContactsActivity)
        }
    }

    fun onConversationsLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(Event.ShowPopupWindow(PRIORITY_CATEGORY_CONVERSATIONS))
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

    private fun containsPriorityCategory(category: Int): Boolean {
        return (priorityCategories.value and category) != 0
    }

    private fun containsSuppressedEffect(effect: Int): Boolean {
        return suppressedVisualEffects.value and effect != 0
    }

    private fun addSuppressedEffect(effect: Int): Unit {
        suppressedVisualEffects.value = suppressedVisualEffects.value or effect
    }

    private fun removeSuppressedEffect(effect: Int): Unit {
        suppressedVisualEffects.value = suppressedVisualEffects.value and effect.inv()
    }

    fun addPriorityCategory(category: Int): Unit {
        priorityCategories.value = priorityCategories.value or category
    }

    fun removePriorityCategory(category: Int): Unit {
        priorityCategories.value = priorityCategories.value and category.inv()
    }

    fun setAllowedSenders(category: Int, senders: Int): Unit {
        when (category) {
            PRIORITY_CATEGORY_MESSAGES -> priorityMessageSenders.value = senders
            PRIORITY_CATEGORY_CALLS -> priorityCallSenders.value = senders
        }
    }

    private fun notificationsStreamAllowed(): Boolean {
        return interruptionPolicyAllowsNotificationStream(
            interruptionFilter.value,
            priorityCategories.value,
            notificationPolicyAccessGranted.value,
            streamsUnlinked.value
        )
    }

    private fun ringerStreamAllowed(): Boolean {
        return interruptionPolicyAllowsRingerStream(
            interruptionFilter.value,
            priorityCategories.value,
            notificationPolicyAccessGranted.value,
            streamsUnlinked.value
        )
    }

    override fun onCleared() {
        super.onCleared()
        activityEventChannel.close()
        eventChannel.close()
    }
}