package ru.rrtry.silentdroid.core

import android.app.NotificationManager
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.*
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.content.Context.*
import android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
import android.content.pm.PackageManager.DONT_KILL_APP
import ru.rrtry.silentdroid.entities.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.media.AudioManager.*
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.media.RingtoneManager.*
import android.provider.Settings.System.VIBRATE_WHEN_RINGING
import android.telephony.TelephonyManager.CALL_STATE_RINGING
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.PREFS_STREAM_TYPE_INDEPENDENT
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.PREFS_STREAM_TYPE_NOT_SET
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_ALARM
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_MANUAL
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.entities.PreviousAndNextTrigger
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_ALARM_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_MUSIC_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_NOTIFICATION_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_RING_DEFAULT_VOLUME
import ru.rrtry.silentdroid.entities.Profile.Companion.STREAM_VOICE_CALL_DEFAULT_VOLUME
import ru.rrtry.silentdroid.event.EventBus
import ru.rrtry.silentdroid.receivers.PhoneStateReceiver
import java.time.LocalDateTime
import java.util.*

@Singleton
@Suppress("deprecation")
class ProfileManager @Inject constructor (@ApplicationContext private val context: Context) {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var notificationHelper: NotificationHelper

    private val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
    private val notificationManager: NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val telephonyManager: TelephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

    fun setProfile(profile: Profile, updatePreferences: Boolean = false) {

        if (notificationManager.currentInterruptionFilter != INTERRUPTION_FILTER_ALL) {
            setInterruptionFilter(INTERRUPTION_FILTER_ALL)
        }

        setStreamVolume(STREAM_MUSIC, profile.mediaVolume, 0)
        setStreamVolume(STREAM_VOICE_CALL, profile.callVolume, 0)
        setStreamVolume(STREAM_ALARM, profile.alarmVolume, 0)

        if (profile.streamsUnlinked &&
            !isNotificationStreamIndependent())
        {
            if (isRinging()) {
                setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
            } else {
                setRingerMode(STREAM_NOTIFICATION, profile.notificationVolume, profile.notificationMode)
            }
        } else {
            setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
            setStreamVolume(STREAM_NOTIFICATION, profile.notificationVolume, 0)
        }

        setNotificationPolicy(createNotificationPolicy(profile))
        setInterruptionFilter(profile.interruptionFilter)
        setRingtoneUri(profile.phoneRingtoneUri, TYPE_RINGTONE)
        setRingtoneUri(profile.notificationSoundUri, TYPE_NOTIFICATION)
        setRingtoneUri(profile.alarmSoundUri, TYPE_ALARM)
        setVibrateWhenRingingBehavior(profile.isVibrateForCallsActive)

        if (updatePreferences) {
            preferencesManager.setProfile(profile)
        }
    }

    fun <T> setProfile(profile: Profile, triggerType: Int, trigger: T?) {
        setProfile(profile, false)
        preferencesManager.setProfile(profile, triggerType, trigger)
        eventBus.onProfileChanged(profile.id)
    }

    private fun isRinging(): Boolean {
        return telephonyManager.callState == CALL_STATE_RINGING
    }

    fun updateScheduledProfile(alarms: List<AlarmRelation>?) {

        val previousAndNextTrigger: PreviousAndNextTrigger? = scheduleManager.getPreviousAndNextTrigger(alarms)
        val alarm: Alarm = previousAndNextTrigger?.relation?.alarm ?: return
        val profileDateTime: LocalDateTime? = preferencesManager.getLastProfileDateTime()

        var overrideCurrentProfile: Boolean = true
        if (profileDateTime != null) overrideCurrentProfile = previousAndNextTrigger.from!! >= profileDateTime

        if (scheduleManager.hasPreviouslyFired(alarm) &&
            overrideCurrentProfile)
        {
            if (scheduleManager.isAlarmValid(alarm)) {
                setProfile(previousAndNextTrigger.profile!!, TRIGGER_TYPE_ALARM, alarm)
            } else {
                setProfile(previousAndNextTrigger.profile!!, TRIGGER_TYPE_MANUAL, null)
            }
            notificationHelper.updateNotification(previousAndNextTrigger.profile, previousAndNextTrigger)
        } else {
            notificationHelper.updateNotification(preferencesManager.getProfile(), previousAndNextTrigger)
        }
    }

    fun setRingerMode(streamType: Int, streamVolume: Int, mode: Int, flags: Int = 0) {
        try {
            when (mode) {
                RINGER_MODE_NORMAL -> setStreamVolume(streamType, streamVolume, flags)
                RINGER_MODE_VIBRATE -> setVibrateMode(streamType, flags)
                RINGER_MODE_SILENT -> setSilentMode(streamType, flags)
            }
        } catch (e: SecurityException) {
            Log.e("ProfileManager", "setRingerMode: $e")
        }
    }

    private fun setSilentMode(streamType: Int, flags: Int = 0) {
        if (audioManager.ringerMode == RINGER_MODE_VIBRATE) {
            adjustUnmuteStream(streamType)
        }
        audioManager.adjustStreamVolume(streamType, ADJUST_MUTE, flags)
    }

    private fun setVibrateMode(streamType: Int, flags: Int = 0) {
        if (audioManager.isStreamMute(streamType)) {
            adjustUnmuteStream(streamType)
        }
        audioManager.setStreamVolume(streamType, 0, flags)
    }

    fun setStreamVolume(streamType: Int, index: Int, flags: Int) {
        try {
            audioManager.setStreamVolume(streamType, index, flags)
        } catch (e: SecurityException) {
            Log.e("ProfileManager", "setStreamVolume $e")
        }
    }

    fun getDefaultProfile(): Profile {
        return Profile(
                UUID.randomUUID(),
                context.resources.getString(R.string.new_profile_title),
                R.drawable.ic_baseline_do_not_disturb_on_total_silence_24,
                STREAM_MUSIC_DEFAULT_VOLUME,
                STREAM_VOICE_CALL_DEFAULT_VOLUME,
                STREAM_NOTIFICATION_DEFAULT_VOLUME,
                STREAM_RING_DEFAULT_VOLUME,
                STREAM_ALARM_DEFAULT_VOLUME,
                getDefaultRingtoneUri(TYPE_RINGTONE),
                getDefaultRingtoneUri(TYPE_NOTIFICATION),
                getDefaultRingtoneUri(TYPE_ALARM),
                isNotificationStreamIndependent(),
                INTERRUPTION_FILTER_ALL,
                RINGER_MODE_NORMAL,
                RINGER_MODE_NORMAL,
                Settings.System.getInt(context.contentResolver, VIBRATE_WHEN_RINGING),
                PRIORITY_CATEGORY_CALLS,
                PRIORITY_SENDERS_ANY,
                PRIORITY_SENDERS_ANY,
                0,
                CONVERSATION_SENDERS_NONE
        )
    }

    private fun adjustUnmuteStream(streamType: Int) {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_UNMUTE, 0)
        }
    }

    private fun toggleMuteState(streamType: Int) {
        audioManager.adjustStreamVolume(
            streamType,
            ADJUST_TOGGLE_MUTE,
            0
        )
    }

    fun disablePhoneStateReceiver() {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, PhoneStateReceiver::class.java),
            COMPONENT_ENABLED_STATE_DISABLED,
            DONT_KILL_APP
        )
    }

    fun isNotificationStreamIndependent(): Boolean {

        if (preferencesManager.getNotificationStreamType() != PREFS_STREAM_TYPE_NOT_SET) {
            return preferencesManager.getNotificationStreamType() == PREFS_STREAM_TYPE_INDEPENDENT
        }

        val interruptionFilter: Int = notificationManager.currentInterruptionFilter
        val notificationVol: Int = audioManager.getStreamVolume(STREAM_NOTIFICATION)
        val ringVol: Int = audioManager.getStreamVolume(STREAM_RING)

        if (notificationVol != ringVol) {
            return true
        }
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            return false
        }
        if (interruptionFilter != INTERRUPTION_FILTER_ALL) {
            notificationManager.setInterruptionFilter(INTERRUPTION_FILTER_ALL)
        }

        toggleMuteState(STREAM_NOTIFICATION)
        val independent: Boolean = audioManager.isStreamMute(STREAM_RING) != audioManager.isStreamMute(STREAM_NOTIFICATION)
        toggleMuteState(STREAM_NOTIFICATION)
        notificationManager.setInterruptionFilter(interruptionFilter)
        return independent
    }

    fun isRingerAudible(profile: Profile): Boolean {

        val interruptionFilter: Int = profile.interruptionFilter
        val priorityCategories: Int = profile.priorityCategories
        val streamsUnlinked: Boolean = profile.streamsUnlinked

        val callsPrioritized: Boolean = interruptionFilter == INTERRUPTION_FILTER_PRIORITY &&
                ((priorityCategories and PRIORITY_CATEGORY_REPEAT_CALLERS) != 0 ||
                (priorityCategories and PRIORITY_CATEGORY_CALLS) != 0)

        return streamsUnlinked && (callsPrioritized || interruptionFilter == INTERRUPTION_FILTER_ALL)
    }

    fun isNotificationPolicyAccessGranted(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    private fun createNotificationPolicy(profile: Profile): Policy {
        return when {
            Build.VERSION_CODES.N > Build.VERSION.SDK_INT -> {
                Policy(
                    profile.priorityCategories,
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders
                )
            }
            Build.VERSION_CODES.N <= Build.VERSION.SDK_INT && Build.VERSION_CODES.R > Build.VERSION.SDK_INT -> {
                Policy(
                    profile.priorityCategories,
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders,
                    profile.suppressedVisualEffects
                )
            }
            else -> {
                Policy(
                    profile.priorityCategories,
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders,
                    profile.suppressedVisualEffects,
                    profile.primaryConversationSenders
                )
            }
        }
    }

    private fun setNotificationPolicy(policy: Policy?) {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.notificationPolicy = policy
        }
    }

    private fun setInterruptionFilter(interruptionFilter: Int) {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(interruptionFilter)
        }
    }

    private fun setRingtoneUri(uri: Uri, type: Int) {
        try {
            setActualDefaultRingtoneUri(context, type, uri)
        } catch (e: SecurityException) {
            Log.e("ProfileManager", "Failed to set ringtone uri")
        }
    }

    private fun getDefaultRingtoneUri(type: Int): Uri {
        return getActualDefaultRingtoneUri(context, type) ?: Uri.EMPTY
    }

    private fun setVibrateWhenRingingBehavior(state: Int) {
        try {
            Settings.System.putInt(context.contentResolver, VIBRATE_WHEN_RINGING, state)
        } catch (e: SecurityException) {
            Log.e("ProfileManager", "Failed to change system settings", e)
        } catch (e: IllegalArgumentException) {
            Log.e("ProfileManager", "Failed to change system settings", e)
        }
    }

    companion object {

        @JvmStatic
        fun getStreamMinVolume(context: Context, streamType: Int): Int {
            val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return audioManager.getStreamMinVolume(streamType)
            }
            return if (streamType == STREAM_ALARM || streamType == STREAM_VOICE_CALL) 1 else 0
        }

        @JvmStatic
        fun getStreamMaxVolume(context: Context, streamType: Int): Int {
            val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            return audioManager.getStreamMaxVolume(streamType) - getStreamMinVolume(context, streamType)
        }
    }
}