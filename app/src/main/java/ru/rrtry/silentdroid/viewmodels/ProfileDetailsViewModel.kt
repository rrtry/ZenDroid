package ru.rrtry.silentdroid.viewmodels

import android.net.Uri
import androidx.lifecycle.*
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.entities.Profile
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.coroutines.channels.*
import android.app.NotificationManager.Policy.*
import android.media.AudioManager.*
import ru.rrtry.silentdroid.db.repositories.AlarmRepository
import ru.rrtry.silentdroid.util.ContentUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import android.app.NotificationManager.*
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import android.media.RingtoneManager.*
import ru.rrtry.silentdroid.db.repositories.LocationRepository
import ru.rrtry.silentdroid.db.repositories.ProfileRepository
import ru.rrtry.silentdroid.entities.LocationRelation
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_ALARM_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_MUSIC_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_NOTIFICATION_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_RING_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_VOICE_CALL_DEFAULT_VOLUME
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.withContext
import ru.rrtry.silentdroid.core.interruptionPolicyAllowsAlarmsStream
import ru.rrtry.silentdroid.core.interruptionPolicyAllowsMediaStream
import ru.rrtry.silentdroid.core.interruptionPolicyAllowsNotificationStream
import ru.rrtry.silentdroid.core.interruptionPolicyAllowsRingerStream

@HiltViewModel
class ProfileDetailsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val alarmRepository: AlarmRepository,
    private val locationRepository: LocationRepository,
    private val contentUtil: ContentUtil
): ViewModel() {

    private val activityChannel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    private val fragmentChannel: Channel<ViewEvent> = Channel(Channel.BUFFERED)

    val fragmentEventsFlow: Flow<ViewEvent> = fragmentChannel.receiveAsFlow()
    val activityEventsFlow: Flow<ViewEvent> = activityChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val ringtoneType: List<Int> = listOf(TYPE_ALARM, TYPE_NOTIFICATION, TYPE_RINGTONE)
            for (i in ringtoneType) {
                fragmentChannel.send(ViewEvent.GetDefaultRingtoneUri(i))
            }
        }
    }

    private var isEntitySet: Boolean = false
    private var previousRingerVolume: Int = STREAM_RING_DEFAULT_VOLUME
    private var previousNotificationVolume: Int = STREAM_NOTIFICATION_DEFAULT_VOLUME

    sealed class ViewEvent {

        object ShowInterruptionFilterFragment: ViewEvent()
        object ShowNotificationRestrictionsFragment: ViewEvent()
        object NotificationPolicyRequestEvent: ViewEvent()
        object PhonePermissionRequestEvent: ViewEvent()
        object ShowPopupWindowEvent: ViewEvent()
        object StartContactsActivity: ViewEvent()
        object WriteSystemSettingsRequestEvent: ViewEvent()
        object ToggleFloatingActionMenu: ViewEvent()

        data class StartRingtonePlayback(val streamType: Int): ViewEvent()
        data class StopRingtonePlayback(val streamType: Int): ViewEvent()
        data class ResumeRingtonePlayback(val streamType: Int, val position: Int): ViewEvent()

        data class ShowDialogFragment(val dialogType: DialogType): ViewEvent()
        data class ChangeRingerMode(val streamType: Int, val showSnackbar: Boolean, val vibrate: Boolean): ViewEvent()
        data class GetDefaultRingtoneUri(val type: Int): ViewEvent()
        data class ChangeRingtoneEvent(val ringtoneType: Int): ViewEvent()
        data class ShowPopupWindow(val category: Int): ViewEvent()
        data class StreamVolumeChanged(val streamType: Int, val volume: Int): ViewEvent()

        data class OnUpdateProfileEvent(val profile: Profile): ViewEvent()
        data class OnInsertProfileEvent(val profile: Profile): ViewEvent()
        data class OnRemoveProfileEvent(val profile: Profile): ViewEvent()
    }

    enum class DialogType {
        PRIORITY_CATEGORIES,
        SUPPRESSED_EFFECTS_ON,
        SUPPRESSED_EFFECTS_OFF,
        PROFILE_TITLE,
        PROFILE_IMAGE
    }

    private val isNew: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val profileUUID = MutableStateFlow<UUID?>(null)

    @FlowPreview
    val alarmsFlow: Flow<List<AlarmRelation>?> = profileUUID.flatMapConcat {
        if (it != null) alarmRepository.observeScheduledAlarmsByProfileId(it) else flowOf(listOf())
    }
    @FlowPreview
    val geofencesFlow: Flow<List<LocationRelation>?> = profileUUID.flatMapConcat {
        if (it != null) locationRepository.observeLocationsByProfileId(it) else flowOf(listOf())
    }

    val title: MutableStateFlow<String> = MutableStateFlow("My profile")
    val iconRes: MutableStateFlow<Int> = MutableStateFlow(-1)
    val mediaVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_MUSIC_DEFAULT_VOLUME)
    val callVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_VOICE_CALL_DEFAULT_VOLUME)
    val notificationVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_NOTIFICATION_DEFAULT_VOLUME)
    val ringVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_RING_DEFAULT_VOLUME)
    val alarmVolume: MutableStateFlow<Int> = MutableStateFlow(STREAM_ALARM_DEFAULT_VOLUME)
    val vibrateForCalls: MutableStateFlow<Int> = MutableStateFlow(1)
    val phoneRingtoneUri: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)
    val notificationSoundUri: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)
    val alarmSoundUri: MutableStateFlow<Uri> = MutableStateFlow(Uri.EMPTY)

    val phoneRingtoneTitle: StateFlow<String> = phoneRingtoneUri.map { uri ->  contentUtil.getRingtoneTitle(uri, TYPE_RINGTONE) }
        .stateIn(viewModelScope, WhileSubscribed(1000), "Not set")

    val notificationRingtoneTitle: StateFlow<String> = notificationSoundUri.map { uri -> contentUtil.getRingtoneTitle(uri, TYPE_NOTIFICATION) }
        .stateIn(viewModelScope, WhileSubscribed(1000), "Not set")

    val alarmRingtoneTitle: StateFlow<String> = alarmSoundUri.map { uri -> contentUtil.getRingtoneTitle(uri, TYPE_ALARM) }
        .stateIn(viewModelScope, WhileSubscribed(1000), "Not set")

    val alarmRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val notificationRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val phoneRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val voiceCallRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val musicRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val streamsUnlinked: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val interruptionFilter: MutableStateFlow<Int> = MutableStateFlow(INTERRUPTION_FILTER_PRIORITY)
    val priorityCategories: MutableStateFlow<Int> = MutableStateFlow(0)
    val priorityCallSenders: MutableStateFlow<Int> = MutableStateFlow(PRIORITY_SENDERS_ANY)
    val priorityMessageSenders: MutableStateFlow<Int> = MutableStateFlow(PRIORITY_SENDERS_ANY)
    val suppressedVisualEffects: MutableStateFlow<Int> = MutableStateFlow(0)
    val primaryConversationSenders: MutableStateFlow<Int> = MutableStateFlow(CONVERSATION_SENDERS_ANYONE)

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

    suspend fun addProfile(profile: Profile) {
        withContext(viewModelScope.coroutineContext) {
            profileRepository.addProfile(profile)
        }
    }

    suspend fun updateProfile(profile: Profile) {
        withContext(viewModelScope.coroutineContext) {
            profileRepository.updateProfile(profile)
        }
    }

    private fun setProfile(profile: Profile) {

        title.value = profile.title
        iconRes.value = profile.iconRes

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

    fun setEntity(profile: Profile, hasExtras: Boolean) {
        if (isEntitySet) return
        setProfile(profile)
        if (hasExtras) setProfileUUID(profile.id) else isNew.value = true
        isEntitySet = true
    }

    fun getProfile(): Profile {
        return Profile(
            if (profileUUID.value == null) UUID.randomUUID() else profileUUID.value!!,
            title.value,
            iconRes.value,
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

    private fun setProfileUUID(uuid : UUID) {
        profileUUID.value = uuid
    }

    fun setNotificationSoundUri(uri: Uri?) {
        uri?.let {
            if (notificationSoundUri.value == Uri.EMPTY) {
                notificationSoundUri.value = uri
            }
        }
    }

    fun setAlarmSoundUri(uri: Uri?) {
        uri?.let {
            if (alarmSoundUri.value == Uri.EMPTY) {
                alarmSoundUri.value = uri
            }
        }
    }

    fun setPhoneSoundUri(uri: Uri?) {
        uri?.let {
            if (phoneRingtoneUri.value == Uri.EMPTY) {
                phoneRingtoneUri.value = uri
            }
        }
    }

    fun onSaveChangesButtonClick() {
        viewModelScope.launch {
            getProfile().let {
                if (updateProfile()) {
                    activityChannel.send(ViewEvent.OnUpdateProfileEvent(it))
                } else {
                    activityChannel.send(ViewEvent.OnInsertProfileEvent(it))
                }
            }
        }
    }

    fun onVibrateForCallsLayoutClick() {
        if (canWriteSettings.value) {
            vibrateForCalls.value = vibrateForCalls.value xor 1
        } else {
            viewModelScope.launch {
                fragmentChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            }
        }
    }

    fun onNotificationSoundLayoutClick() {
        viewModelScope.launch {
            if (canWriteSettings.value) {
                fragmentChannel.send(ViewEvent.ChangeRingtoneEvent(TYPE_NOTIFICATION))
            } else {
                fragmentChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            }
        }
    }

    private fun isRingtonePlaying(streamType: Int): Boolean {
        return when (streamType) {
            STREAM_MUSIC -> musicRingtonePlaying.value
            STREAM_VOICE_CALL -> voiceCallRingtonePlaying.value
            STREAM_NOTIFICATION -> notificationRingtonePlaying.value
            STREAM_RING -> phoneRingtonePlaying.value
            STREAM_ALARM -> alarmRingtonePlaying.value
            else -> false
        }
    }

    fun getStreamVolume(streamType: Int): Int {
        return when (streamType) {
            STREAM_MUSIC -> mediaVolume.value
            STREAM_VOICE_CALL -> callVolume.value
            STREAM_NOTIFICATION -> notificationVolume.value
            STREAM_RING -> ringVolume.value
            STREAM_ALARM -> alarmVolume.value
            else -> -1
        }
    }

    fun stopPlayback() {
        setPlaybackState(
            getPlayingRingtone(),
            false
        )
    }

    fun setPlaybackState(streamType: Int, playing: Boolean) {
        when (streamType) {
            STREAM_MUSIC -> musicRingtonePlaying.value = playing
            STREAM_VOICE_CALL -> voiceCallRingtonePlaying.value = playing
            STREAM_NOTIFICATION -> notificationRingtonePlaying.value = playing
            STREAM_RING -> phoneRingtonePlaying.value = playing
            STREAM_ALARM -> alarmRingtonePlaying.value = playing
        }
    }

    fun isMediaPlaying(): Boolean {
        return getPlayingRingtone() != -1
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

    fun onStopRingtonePlayback(streamType: Int) {
        viewModelScope.launch {
            if (isRingtonePlaying(streamType)) {
                fragmentChannel.send(ViewEvent.StopRingtonePlayback(streamType))
            }
        }
    }

    fun onResumeRingtonePlayback(streamType: Int, position: Int) {
        viewModelScope.launch {
            fragmentChannel.send(ViewEvent.ResumeRingtonePlayback(streamType, position))
        }
    }

    fun onPlayRingtoneButtonClick(streamType: Int) {
        viewModelScope.launch {
            val event: ViewEvent = if (isRingtonePlaying(streamType)) {
                ViewEvent.StopRingtonePlayback(streamType)
            } else {
                ViewEvent.StartRingtonePlayback(streamType)
            }
            setPlaybackState(getPlayingRingtone(), false)
            fragmentChannel.send(event)
        }
    }

    fun onRingtoneLayoutClick() {
        viewModelScope.launch {
            if (canWriteSettings.value) {
                fragmentChannel.send(ViewEvent.ChangeRingtoneEvent(TYPE_RINGTONE))
            } else {
                fragmentChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            }
        }
    }

    fun onUnlinkStreamsLayoutClick() {
        viewModelScope.launch {
            if (phonePermissionGranted.value) {
                streamsUnlinked.value = !streamsUnlinked.value
            } else {
                fragmentChannel.send(ViewEvent.PhonePermissionRequestEvent)
            }
        }
    }

    fun onAlarmSoundLayoutClick() {
        viewModelScope.launch {
            if (canWriteSettings.value) {
                fragmentChannel.send(ViewEvent.ChangeRingtoneEvent(TYPE_ALARM))
            } else {
                fragmentChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            }
        }
    }

    fun onMediaStreamVolumeChanged(index: Int, fromUser: Boolean) {
        if (fromUser) {
            viewModelScope.launch {
                fragmentChannel.send(ViewEvent.StreamVolumeChanged(STREAM_MUSIC, index))
            }
        }
        mediaVolume.value = index
    }

    fun onAlarmStreamVolumeChanged(index: Int, fromUser: Boolean, streamMinVolume: Int) {
        val volume: Int = index + streamMinVolume
        if (fromUser) {
            viewModelScope.launch {
                fragmentChannel.send(ViewEvent.StreamVolumeChanged(STREAM_ALARM, volume))
            }
        }
        alarmVolume.value = volume
    }

    fun onVoiceCallStreamVolumeChanged(index: Int, fromUser: Boolean, streamMinVolume: Int) {
        val volume: Int = index + streamMinVolume
        if (fromUser) {
            viewModelScope.launch {
                fragmentChannel.send(ViewEvent.StreamVolumeChanged(STREAM_VOICE_CALL, volume))
            }
        }
        callVolume.value = volume
    }

    fun onInterruptionFilterLayoutClick() {
        viewModelScope.launch {
            if (notificationPolicyAccessGranted.value) {
                fragmentChannel.send(ViewEvent.ShowPopupWindowEvent)
            } else {
                fragmentChannel.send(ViewEvent.NotificationPolicyRequestEvent)
            }
        }
    }

    private fun restoreRingerMode(initialValue: Int = 4) {
        ringVolume.value = initialValue
        ringerMode.value = RINGER_MODE_NORMAL
    }

    private fun silenceRinger() {
        ringVolume.value = 0
        ringerMode.value = RINGER_MODE_SILENT
        onStopRingtonePlayback(STREAM_RING)
    }

    private fun restoreNotificationMode(initialValue: Int) {
        notificationVolume.value = initialValue
        notificationMode.value = RINGER_MODE_NORMAL
    }

    private fun silenceNotifications() {
        notificationVolume.value = 0
        notificationMode.value = RINGER_MODE_SILENT
        onStopRingtonePlayback(STREAM_NOTIFICATION)
    }

    fun onRingerIconClick() {
        if (ringerStreamAllowed()) {
            if (ringerMode.value == RINGER_MODE_SILENT) {
                restoreRingerMode(4)
            } else {
                silenceRinger()
            }
        }
    }

    fun onNotificationIconClick() {
        if (notificationsStreamAllowed()) {
            if (notificationMode.value == RINGER_MODE_SILENT) {
                restoreNotificationMode(4)
            } else {
                silenceNotifications()
            }
        }
    }

    fun onPreferencesLayoutClick() {
        viewModelScope.launch {
            if (notificationPolicyAccessGranted.value) {
                fragmentChannel.send(ViewEvent.ShowInterruptionFilterFragment)
            } else {
                fragmentChannel.send(ViewEvent.NotificationPolicyRequestEvent)
            }
        }
    }

    private fun onStreamMuted(streamType: Int, showSnackbar: Boolean = false, vibrate: Boolean = false) {
        viewModelScope.launch {
            if (streamType == STREAM_NOTIFICATION) {
                notificationVolume.value = 0
            } else if (streamType == STREAM_RING) {
                ringVolume.value = 0
            }
            fragmentChannel.send(ViewEvent.ChangeRingerMode(streamType, showSnackbar, vibrate))
            onStopRingtonePlayback(streamType)
        }
    }

    fun onAlertStreamVolumeChanged(value: Int, fromUser: Boolean, streamType: Int) {
        viewModelScope.launch {
            if (fromUser) {
                val isMute: Boolean = value == 0
                when {
                    isMute -> onStreamMuted(streamType, showSnackbar = true, vibrate = true)
                    streamType == STREAM_NOTIFICATION -> restoreNotificationMode(value)
                    streamType == STREAM_RING -> restoreRingerMode(value)
                }
                if (!isMute) {
                    fragmentChannel.send(ViewEvent.StreamVolumeChanged(streamType, value))
                }
            }
        }
    }

    fun onCallsLayoutClick() {
        viewModelScope.launch {
            fragmentChannel.send(ViewEvent.ShowPopupWindow(PRIORITY_CATEGORY_CALLS))
        }
    }

    fun onRepetitiveCallersLayoutClick() {
        if (containsPriorityCategory(PRIORITY_CATEGORY_REPEAT_CALLERS)) {
            removePriorityCategory(PRIORITY_CATEGORY_REPEAT_CALLERS)
        } else {
            addPriorityCategory(PRIORITY_CATEGORY_REPEAT_CALLERS)
        }
    }

    fun getRingtoneUri(type: Int): Uri {
        return when (type) {
            TYPE_RINGTONE -> phoneRingtoneUri.value
            TYPE_NOTIFICATION -> notificationSoundUri.value
            TYPE_ALARM -> alarmSoundUri.value
            else -> Uri.EMPTY
        }
    }

    fun onProfileImageViewClick() {
        viewModelScope.launch {
            activityChannel.send(ViewEvent.ShowDialogFragment(DialogType.PROFILE_IMAGE))
        }
    }

    fun onConversationsLayoutClick() {
        viewModelScope.launch {
            fragmentChannel.send(ViewEvent.ShowPopupWindow(PRIORITY_CATEGORY_CONVERSATIONS))
        }
    }

    fun onMessagesLayoutClick() {
        viewModelScope.launch {
            fragmentChannel.send(ViewEvent.ShowPopupWindow(PRIORITY_CATEGORY_MESSAGES))
        }
    }

    fun onPriorityInterruptionsLayoutClick() {
        viewModelScope.launch {
            fragmentChannel.send(ViewEvent.ShowDialogFragment(DialogType.PRIORITY_CATEGORIES))
        }
    }

    fun onSuppressedEffectsOnLayoutClick() {
        viewModelScope.launch {
            fragmentChannel.send(ViewEvent.ShowDialogFragment(DialogType.SUPPRESSED_EFFECTS_ON))
        }
    }

    fun onChangeTitleButtonClick() {
        viewModelScope.launch {
            activityChannel.send(ViewEvent.ShowDialogFragment(DialogType.PROFILE_TITLE))
        }
    }

    fun onSuppressedEffectsOffLayoutClick() {
        viewModelScope.launch {
            fragmentChannel.send(ViewEvent.ShowDialogFragment(DialogType.SUPPRESSED_EFFECTS_OFF))
        }
    }

    private fun containsPriorityCategory(category: Int): Boolean {
        return (priorityCategories.value and category) != 0
    }

    private fun containsSuppressedEffect(effect: Int): Boolean {
        return suppressedVisualEffects.value and effect != 0
    }

    private fun addSuppressedEffect(effect: Int) {
        suppressedVisualEffects.value = suppressedVisualEffects.value or effect
    }

    private fun removeSuppressedEffect(effect: Int) {
        suppressedVisualEffects.value = suppressedVisualEffects.value and effect.inv()
    }

    fun addPriorityCategory(category: Int) {
        priorityCategories.value = priorityCategories.value or category
    }

    fun removePriorityCategory(category: Int) {
        priorityCategories.value = priorityCategories.value and category.inv()
    }

    fun setAllowedSenders(category: Int, senders: Int) {
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
        activityChannel.close()
        fragmentChannel.close()
    }
}