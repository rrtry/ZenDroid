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
import com.example.volumeprofiler.database.repositories.ProfileRepository
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
        private val profileRepository: ProfileRepository,
        private val alarmRepository: AlarmRepository,
        private val contentUtil: ContentUtil
): ViewModel() {

    private val activityEventChannel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    private val eventChannel: Channel<ViewEvent> = Channel(Channel.BUFFERED)

    val fragmentEventsFlow: Flow<ViewEvent> = eventChannel.receiveAsFlow()
    val activityEventsFlow: Flow<ViewEvent> = activityEventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val ringtoneType: List<Int> = listOf(TYPE_ALARM, TYPE_NOTIFICATION, TYPE_RINGTONE)
            for (i in ringtoneType) {
                eventChannel.send(ViewEvent.GetDefaultRingtoneUri(i))
            }
        }
    }

    private var areArgsSet: Boolean = false
    private var previousRingerVolume: Int = STREAM_RING_DEFAULT_VOLUME
    private var previousNotificationVolume: Int = STREAM_NOTIFICATION_DEFAULT_VOLUME

    var ringtoneUri: Uri = Uri.EMPTY
    var alarmUri: Uri = Uri.EMPTY
    var notificationUri: Uri = Uri.EMPTY

    sealed class ViewEvent {

        object NavigateToNextFragment: ViewEvent()
        object StoragePermissionRequestEvent: ViewEvent()
        object NotificationPolicyRequestEvent: ViewEvent()
        object PhonePermissionRequestEvent: ViewEvent()
        object ShowPopupWindowEvent: ViewEvent()
        object StartContactsActivity: ViewEvent()
        object WriteSystemSettingsRequestEvent: ViewEvent()

        data class StartRingtonePlayback(val streamType: Int): ViewEvent()
        data class StopRingtonePlayback(val streamType: Int): ViewEvent()
        data class ResumeRingtonePlayback(val streamType: Int, val position: Int): ViewEvent()

        data class ApplyChangesEvent(val profile: Profile, val shouldUpdate: Boolean): ViewEvent()
        data class ShowDialogFragment(val dialogType: DialogType): ViewEvent()
        data class ChangeRingerMode(val streamType: Int, val showToast: Boolean, val vibrate: Boolean): ViewEvent()
        data class GetDefaultRingtoneUri(val type: Int): ViewEvent()
        data class ChangeRingtoneEvent(val ringtoneType: Int): ViewEvent()
        data class ShowPopupWindow(val category: Int): ViewEvent()
        data class AlarmStreamVolumeChanged(val streamType: Int, val volume: Int): ViewEvent()
        data class StreamVolumeChanged(val streamType: Int, val volume: Int): ViewEvent()

        data class OnUpdateProfileEvent(val profile: Profile): ViewEvent()
        data class OnInsertProfileEvent(val profile: Profile): ViewEvent()
        data class OnRemoveProfileEvent(val profile: Profile): ViewEvent()
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

    val storagePermissionGranted: MutableStateFlow<Boolean> = MutableStateFlow(true)
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

    var resumePlayback: Boolean = false
    var currentStreamType: Int = -1
    var currentMediaUri: Uri? = null
    var playerPosition: Int = -1

    fun addProfile(profile: Profile): Unit {
        viewModelScope.launch {
            profileRepository.addProfile(profile)
        }
    }

    fun updateProfile(profile: Profile): Unit {
        viewModelScope.launch {
            profileRepository.updateProfile(profile)
        }
    }

    private fun setProperties(profile: Profile): Unit {

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

    fun setEntity(profile: Profile, hasExtras: Boolean): Unit {
        if (!areArgsSet) {
            setProperties(profile)
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
            val profile: Profile = getProfile()
            activityEventChannel.send(
                if (updateProfile()) ViewEvent.OnUpdateProfileEvent(profile) else ViewEvent.OnInsertProfileEvent(profile)
            )
        }
    }

    fun onVibrateForCallsLayoutClick(): Unit {
        if (canWriteSettings.value) {
            vibrateForCalls.value = vibrateForCalls.value xor 1
        } else {
            viewModelScope.launch {
                eventChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            }
        }
    }

    fun onNotificationSoundLayoutClick(): Unit {
        viewModelScope.launch {
            if (canWriteSettings.value) {
                eventChannel.send(ViewEvent.ChangeRingtoneEvent(TYPE_NOTIFICATION))
            } else {
                eventChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
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

    fun getPlayingRingtone(): Int {
        val streams: Map<Int, Boolean> = mapOf(
            STREAM_MUSIC to musicRingtonePlaying.value,
            STREAM_VOICE_CALL to voiceCallRingtonePlaying.value,
            STREAM_NOTIFICATION to notificationRingtonePlaying.value,
            STREAM_RING to phoneRingtonePlaying.value,
            STREAM_ALARM to alarmRingtonePlaying.value
        )
        for (i in streams.entries) {
            if (i.value) {
                return i.key
            }
        }
        return -1
    }

    fun onStopRingtonePlayback(streamType: Int): Unit {
        viewModelScope.launch {
            if (isRingtonePlaying(streamType)) {
                eventChannel.send(ViewEvent.StopRingtonePlayback(streamType))
            }
        }
    }

    fun onStartRingtonePlayback(streamType: Int): Unit {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.StartRingtonePlayback(streamType))
        }
    }

    fun onResumeRingtonePlayback(streamType: Int, position: Int): Unit {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ResumeRingtonePlayback(streamType, position))
        }
    }

    fun onPlayRingtoneButtonClick(streamType: Int): Unit {
        viewModelScope.launch {
            val event: ViewEvent = if (isRingtonePlaying(streamType)) {
                ViewEvent.StopRingtonePlayback(streamType)
            } else {
                ViewEvent.StartRingtonePlayback(streamType)
            }
            setPlaybackState(getPlayingRingtone(), false)
            eventChannel.send(event)
        }
    }

    fun onRingtoneLayoutClick(): Unit {
        viewModelScope.launch {
            if (canWriteSettings.value) {
                eventChannel.send(ViewEvent.ChangeRingtoneEvent(TYPE_RINGTONE))
            } else {
                eventChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            }
        }
    }

    fun onUnlinkStreamsLayoutClick(): Unit {
        viewModelScope.launch {
            if (phonePermissionGranted.value) {
                streamsUnlinked.value = !streamsUnlinked.value
            } else {
                eventChannel.send(ViewEvent.PhonePermissionRequestEvent)
            }
        }
    }

    fun onAlarmSoundLayoutClick(): Unit {
        viewModelScope.launch {
            if (canWriteSettings.value) {
                eventChannel.send(ViewEvent.ChangeRingtoneEvent(TYPE_ALARM))
            } else {
                eventChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            }
        }
    }

    fun onMediaStreamVolumeChanged(index: Int, fromUser: Boolean): Unit {
        if (fromUser) {
            viewModelScope.launch {
                eventChannel.send(ViewEvent.StreamVolumeChanged(STREAM_MUSIC, index))
            }
        }
        mediaVolume.value = index
    }

    fun onAlarmStreamVolumeChanged(index: Int, fromUser: Boolean): Unit {
        if (fromUser) {
            viewModelScope.launch {
                eventChannel.send(ViewEvent.AlarmStreamVolumeChanged(STREAM_ALARM, index))
            }
        }
    }

    fun onVoiceCallStreamVolumeChanged(index: Int, fromUser: Boolean): Unit {
        if (fromUser) {
            viewModelScope.launch {
                eventChannel.send(ViewEvent.AlarmStreamVolumeChanged(STREAM_VOICE_CALL, index))
            }
        }
    }

    fun onInterruptionFilterLayoutClick(): Unit {
        viewModelScope.launch {
            if (notificationPolicyAccessGranted.value) {
                eventChannel.send(ViewEvent.ShowPopupWindowEvent)
            } else {
                eventChannel.send(ViewEvent.NotificationPolicyRequestEvent)
            }
        }
    }

    fun onRingerIconClick(): Unit {
        if (ringerStreamAllowed()) {
            if (ringerMode.value == RINGER_MODE_SILENT) {
                ringVolume.value = 4
                ringerMode.value = RINGER_MODE_NORMAL
            } else {
                ringVolume.value = 0
                ringerMode.value = RINGER_MODE_SILENT
                onStopRingtonePlayback(STREAM_RING)
            }
        }
    }

    fun onNotificationIconClick(): Unit {
        if (notificationsStreamAllowed()) {
            if (notificationMode.value == RINGER_MODE_SILENT) {
                notificationVolume.value = 4
                notificationMode.value = RINGER_MODE_NORMAL
            } else {
                notificationVolume.value = 0
                notificationMode.value = RINGER_MODE_SILENT
                onStopRingtonePlayback(STREAM_NOTIFICATION)
            }
        }
    }

    fun onPreferencesLayoutClick(): Unit {
        viewModelScope.launch {
            if (notificationPolicyAccessGranted.value) {
                eventChannel.send(ViewEvent.NavigateToNextFragment)
            } else {
                eventChannel.send(ViewEvent.NotificationPolicyRequestEvent)
            }
        }
    }

    private fun onStreamMuted(streamType: Int, showToast: Boolean, vibrate: Boolean): Unit {
        when (streamType) {
            STREAM_NOTIFICATION -> notificationVolume.value = 0
            STREAM_RING -> ringVolume.value = 0
        }
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ChangeRingerMode(streamType, showToast, vibrate))
            onStopRingtonePlayback(getPlayingRingtone())
        }
    }

    fun onAlertStreamVolumeChanged(value: Int, fromUser: Boolean, streamType: Int): Unit {
        if (fromUser) {
            when {
                value == 0 -> {
                    onStreamMuted(streamType, false, true)
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
            viewModelScope.launch {
                eventChannel.send(ViewEvent.StreamVolumeChanged(streamType, value))
            }
        }
    }

    fun onCallsLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ShowPopupWindow(PRIORITY_CATEGORY_CALLS))
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
            eventChannel.send(ViewEvent.StartContactsActivity)
        }
    }

    fun onConversationsLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ShowPopupWindow(PRIORITY_CATEGORY_CONVERSATIONS))
        }
    }

    fun onMessagesLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ShowPopupWindow(PRIORITY_CATEGORY_MESSAGES))
        }
    }

    fun onPriorityInterruptionsLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ShowDialogFragment(DialogType.PRIORITY))
        }
    }

    fun onSuppressedEffectsOnLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ShowDialogFragment(DialogType.SUPPRESSED_EFFECTS_ON))
        }
    }

    fun onChangeTitleButtonClick(): Unit {
        viewModelScope.launch {
            activityEventChannel.send(ViewEvent.ShowDialogFragment(DialogType.TITLE))
        }
    }

    fun onSuppressedEffectsOffLayoutClick(): Unit {
        viewModelScope.launch {
            eventChannel.send(ViewEvent.ShowDialogFragment(DialogType.SUPPRESSED_EFFECTS_OFF))
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