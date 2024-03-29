package ru.rrtry.silentdroid.viewmodels

import android.Manifest.permission.*
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
import android.util.Log
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
import ru.rrtry.silentdroid.core.*

@HiltViewModel
class ProfileDetailsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val alarmRepository: AlarmRepository,
    private val locationRepository: LocationRepository,
    private val ringtoneManager: AppRingtoneManager
): ViewModel() {

    private val activityChannel: Channel<ViewEvent> = Channel(Channel.BUFFERED)
    private val fragmentChannel: Channel<ViewEvent> = Channel(Channel.BUFFERED)

    val fragmentEventsFlow: Flow<ViewEvent> = fragmentChannel.receiveAsFlow()
    val activityEventsFlow: Flow<ViewEvent> = activityChannel.receiveAsFlow()

    init {
        setDefaultSoundUri()
    }

    private var isEntitySet: Boolean = false
    private var previousRingerVolume: Int = STREAM_RING_DEFAULT_VOLUME
    private var previousNotificationVolume: Int = STREAM_NOTIFICATION_DEFAULT_VOLUME

    sealed class ViewEvent {

        object ShowInterruptionFilterFragment: ViewEvent()
        object ShowNotificationRestrictionsFragment: ViewEvent()
        object NotificationPolicyRequestEvent: ViewEvent()
        object ShowPopupWindowEvent: ViewEvent()
        object StartContactsActivity: ViewEvent()
        object PhonePermissionRequestEvent: ViewEvent()
        object StoragePermissionRequestEvent: ViewEvent()
        object WriteSystemSettingsRequestEvent: ViewEvent()
        object StartPermissionsActivity: ViewEvent()

        data class StartRingtonePlayback(val streamType: Int): ViewEvent()
        data class StopRingtonePlayback(val streamType: Int): ViewEvent()
        data class ResumeRingtonePlayback(val streamType: Int, val position: Int): ViewEvent()

        data class ShowDialogFragment(val dialogType: DialogType): ViewEvent()
        data class ChangeRingerMode(val streamType: Int, val hasSeparateNotificationStream: Boolean): ViewEvent()
        data class GetDefaultRingtoneUri(val type: Int): ViewEvent()
        data class ChangeRingtoneEvent(val ringtoneType: Int): ViewEvent()
        data class ShowPopupWindow(val category: Int): ViewEvent()
        data class ShowStreamMutedSnackbar(val streamType: Int): ViewEvent()
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
    val alarmsFlow: Flow<List<AlarmRelation>?> = profileUUID.flatMapConcat { uuid ->
        if (uuid != null) alarmRepository.observeScheduledAlarmsByProfileId(uuid) else flowOf(listOf())
    }
    @FlowPreview
    val geofencesFlow: Flow<List<LocationRelation>?> = profileUUID.flatMapConcat { uuid ->
        if (uuid != null) locationRepository.observeLocationsByProfileId(uuid) else flowOf(listOf())
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

    val alarmRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val notificationRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val phoneRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val voiceCallRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val musicRingtonePlaying: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val isVoiceCapable: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isVolumeFixed: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val streamsUnlinked: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val notificationStreamIndependent: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val interruptionFilter: MutableStateFlow<Int> = MutableStateFlow(INTERRUPTION_FILTER_PRIORITY)
    val priorityCategories: MutableStateFlow<Int> = MutableStateFlow(0)
    val priorityCallSenders: MutableStateFlow<Int> = MutableStateFlow(PRIORITY_SENDERS_ANY)
    val priorityMessageSenders: MutableStateFlow<Int> = MutableStateFlow(PRIORITY_SENDERS_ANY)
    val suppressedVisualEffects: MutableStateFlow<Int> = MutableStateFlow(0)
    val primaryConversationSenders: MutableStateFlow<Int> = MutableStateFlow(CONVERSATION_SENDERS_ANYONE)

    val phonePermissionGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val storagePermissionGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val notificationPolicyAccessGranted: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val canWriteSettings: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val ringerMode: MutableStateFlow<Int> = MutableStateFlow(RINGER_MODE_NORMAL)
    val notificationMode: MutableStateFlow<Int> = MutableStateFlow(RINGER_MODE_NORMAL)

    val phoneRingtoneTitle: StateFlow<String> = combine(
        phoneRingtoneUri,
        storagePermissionGranted,
        canWriteSettings)
    {
            uri, _, _ -> ringtoneManager.getRingtoneTitle(uri, TYPE_RINGTONE)
    }.stateIn(viewModelScope, WhileSubscribed(1000), "")

    val notificationRingtoneTitle: StateFlow<String> = combine(
        notificationSoundUri,
        storagePermissionGranted,
        canWriteSettings)
    {
            uri, _, _ -> ringtoneManager.getRingtoneTitle(uri, TYPE_NOTIFICATION)
    }.stateIn(viewModelScope, WhileSubscribed(1000), "")

    val alarmRingtoneTitle: StateFlow<String> = combine(
        alarmSoundUri,
        storagePermissionGranted,
        canWriteSettings)
    {
            uri, _, _ -> ringtoneManager.getRingtoneTitle(uri, TYPE_ALARM)
    }.stateIn(viewModelScope, WhileSubscribed(1000), "")

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

    suspend fun getScheduledAlarms(): List<AlarmRelation>? {
        return withContext(viewModelScope.coroutineContext) {
            if (profileUUID.value != null) {
                alarmRepository.getScheduledAlarmsByProfileId(profileUUID.value!!)
            }
            null
        }
    }

    suspend fun getRegisteredGeofences(): List<LocationRelation>? {
        return withContext(viewModelScope.coroutineContext) {
            if (profileUUID.value != null) {
                locationRepository.getLocationsByProfileId(profileUUID.value!!)
            }
            null
        }
    }

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

    private fun setDefaultSoundUri() {
        viewModelScope.launch {
            val ringtoneType: List<Int> = listOf(TYPE_ALARM, TYPE_NOTIFICATION, TYPE_RINGTONE)
            for (type in ringtoneType) {
                fragmentChannel.send(ViewEvent.GetDefaultRingtoneUri(type))
            }
        }
    }

    fun setDefaultNotificationSoundUri(uri: Uri?) {
        uri?.let {
            if (notificationSoundUri.value == Uri.EMPTY) {
                notificationSoundUri.value = uri
            }
        }
    }

    fun setDefaultAlarmSoundUri(uri: Uri?) {
        uri?.let {
            if (alarmSoundUri.value == Uri.EMPTY) {
                alarmSoundUri.value = uri
            }
        }
    }

    fun setDefaultRingtoneUri(uri: Uri?) {
        uri?.let {
            if (phoneRingtoneUri.value == Uri.EMPTY) {
                phoneRingtoneUri.value = uri
            }
        }
    }

    fun requestPermission(permission: String, redirectToSettings: Boolean) {
        viewModelScope.launch {
            if (redirectToSettings &&
                (permission == READ_EXTERNAL_STORAGE ||
                permission == READ_PHONE_STATE))
            {
                fragmentChannel.send(ViewEvent.StartPermissionsActivity)
            } else {
                when (permission) {
                    ACCESS_NOTIFICATION_POLICY -> fragmentChannel.send(ViewEvent.NotificationPolicyRequestEvent)
                    WRITE_SETTINGS -> fragmentChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
                    READ_PHONE_STATE -> fragmentChannel.send(ViewEvent.PhonePermissionRequestEvent)
                    READ_EXTERNAL_STORAGE -> fragmentChannel.send(ViewEvent.StoragePermissionRequestEvent)
                }
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
            if (!storagePermissionGranted.value) {
                fragmentChannel.send(ViewEvent.StoragePermissionRequestEvent)
            } else if (!canWriteSettings.value) {
                fragmentChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            } else {
                fragmentChannel.send(ViewEvent.ChangeRingtoneEvent(TYPE_NOTIFICATION))
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

    private fun isStreamMute(streamType: Int): Boolean {
        if (getStreamVolume(streamType) == 0) return true
        return !isStreamAllowed(streamType)
    }

    fun stopPlayback() {
        setPlaybackState(
            getPlayingStream(),
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

    fun isRingtonePlaying(): Boolean {
        return getPlayingStream() != -1
    }

    fun getPlayingStream(): Int {
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

    fun savePlayerPosition(position: Int) {
        playerPosition = position
        resumePlayback = true
    }

    fun resumeRingtonePlayback() {
        if (resumePlayback) {
            onResumeRingtonePlayback(currentStreamType, playerPosition)
        }
        resumePlayback = false
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
            if (!storagePermissionGranted.value) {
                fragmentChannel.send(ViewEvent.StoragePermissionRequestEvent)
            } else if (isStreamMute(streamType)) {
                fragmentChannel.send(ViewEvent.ShowStreamMutedSnackbar(streamType))
            } else {
                val event: ViewEvent = if (isRingtonePlaying(streamType)) {
                    ViewEvent.StopRingtonePlayback(streamType)
                } else {
                    ViewEvent.StartRingtonePlayback(streamType)
                }
                setPlaybackState(getPlayingStream(), false)
                fragmentChannel.send(event)
            }
        }
    }

    fun onRingtoneLayoutClick() {
        viewModelScope.launch {
            if (!storagePermissionGranted.value) {
                fragmentChannel.send(ViewEvent.StoragePermissionRequestEvent)
            } else if (!canWriteSettings.value) {
                fragmentChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            } else {
                fragmentChannel.send(ViewEvent.ChangeRingtoneEvent(TYPE_RINGTONE))
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
            if (!storagePermissionGranted.value) {
                fragmentChannel.send(ViewEvent.StoragePermissionRequestEvent)
            } else if (!canWriteSettings.value) {
                fragmentChannel.send(ViewEvent.WriteSystemSettingsRequestEvent)
            } else {
                fragmentChannel.send(ViewEvent.ChangeRingtoneEvent(TYPE_ALARM))
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

    private fun setRingVolume(value: Int = 4) {
        val prevRingerMode: Int = ringerMode.value

        ringVolume.value = value
        ringerMode.value = RINGER_MODE_NORMAL

        if (notificationMode.value != RINGER_MODE_NORMAL &&
            prevRingerMode != RINGER_MODE_NORMAL &&
            notificationStreamIndependent.value)
        {
            setNotificationVolume(value)
        }
    }

    private fun setNotificationVolume(value: Int) {
        notificationVolume.value = value
        notificationMode.value = RINGER_MODE_NORMAL
    }

    fun silenceRinger(mode: Int = RINGER_MODE_SILENT) {
        ringVolume.value = 0
        ringerMode.value = mode
        onStopRingtonePlayback(STREAM_RING)
        if (notificationMode.value != mode &&
            notificationStreamIndependent.value)
        {
            silenceNotifications(mode)
        }
    }

    fun silenceNotifications(mode: Int = RINGER_MODE_SILENT) {
        notificationVolume.value = 0
        notificationMode.value = mode
        onStopRingtonePlayback(STREAM_NOTIFICATION)
    }

    fun onRingerIconClick() {
        if (isRingStreamAllowed()) {
            if (ringerMode.value == RINGER_MODE_SILENT) {
                setRingVolume(STREAM_RING_DEFAULT_VOLUME)
            } else {
                silenceRinger()
            }
        }
    }

    fun onNotificationIconClick() {
        if (isNotificationStreamAllowed()) {
            if (notificationMode.value == RINGER_MODE_SILENT) {
                setNotificationVolume(STREAM_NOTIFICATION_DEFAULT_VOLUME)
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

    private fun onRingStreamMuted(streamType: Int) {
        viewModelScope.launch {
            if (streamType == STREAM_NOTIFICATION) {
                notificationVolume.value = 0
            } else if (streamType == STREAM_RING) {
                ringVolume.value = 0
            }
            fragmentChannel.send(ViewEvent.ChangeRingerMode(streamType, notificationStreamIndependent.value))
            onStopRingtonePlayback(streamType)
        }
    }

    fun onAlertStreamVolumeChanged(value: Int, fromUser: Boolean, streamType: Int) {
        viewModelScope.launch {
            if (fromUser) {
                when {
                    value == 0 -> onRingStreamMuted(streamType)
                    streamType == STREAM_NOTIFICATION -> setNotificationVolume(value)
                    streamType == STREAM_RING -> setRingVolume(value)
                }
                if (value != 0) fragmentChannel.send(ViewEvent.StreamVolumeChanged(streamType, value))
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

    fun onSuppressedEffectsOffLayoutClick() {
        viewModelScope.launch {
            fragmentChannel.send(ViewEvent.ShowDialogFragment(DialogType.SUPPRESSED_EFFECTS_OFF))
        }
    }

    fun onSuppressedEffectsLayoutClick(effect: Int) {
        if (suppressedVisualEffects.value and effect == 0) {
            suppressedVisualEffects.value = suppressedVisualEffects.value or effect
        } else {
            suppressedVisualEffects.value = suppressedVisualEffects.value and effect.inv()
        }
    }

    fun onChangeTitleButtonClick() {
        viewModelScope.launch {
            activityChannel.send(ViewEvent.ShowDialogFragment(DialogType.PROFILE_TITLE))
        }
    }

    private fun containsPriorityCategory(category: Int): Boolean {
        return (priorityCategories.value and category) != 0
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

    private fun isStreamAllowed(streamType: Int): Boolean {
        return when (streamType) {
            STREAM_MUSIC -> isMediaStreamAllowed()
            STREAM_RING -> isRingStreamAllowed()
            STREAM_NOTIFICATION -> isNotificationStreamAllowed()
            STREAM_ALARM -> isAlarmStreamAllowed()
            else -> true
        }
    }

    private fun isAlarmStreamAllowed(): Boolean {
        return interruptionPolicyAllowsAlarmsStream(
            interruptionFilter.value,
            priorityCategories.value,
            notificationPolicyAccessGranted.value
        ) && !isVolumeFixed.value
    }

    private fun isMediaStreamAllowed(): Boolean {
        return interruptionPolicyAllowsMediaStream(
            interruptionFilter.value,
            priorityCategories.value,
            notificationPolicyAccessGranted.value
        ) && !isVolumeFixed.value
    }

    private fun isNotificationStreamAllowed(): Boolean {
        return interruptionPolicyAllowsNotificationStream(
            interruptionFilter.value,
            priorityCategories.value,
            notificationPolicyAccessGranted.value,
            streamsUnlinked.value)
                && !ringerModeMutesNotifications(ringerMode.value, notificationStreamIndependent.value)
                && !isVolumeFixed.value
    }

    private fun isRingStreamAllowed(): Boolean {
        return interruptionPolicyAllowsRingerStream(
            interruptionFilter.value,
            priorityCategories.value,
            notificationPolicyAccessGranted.value,
            streamsUnlinked.value
        ) && !isVolumeFixed.value
    }

    override fun onCleared() {
        super.onCleared()
        activityChannel.close()
        fragmentChannel.close()
    }
}