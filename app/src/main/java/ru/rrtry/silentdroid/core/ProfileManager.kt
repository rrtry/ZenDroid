package ru.rrtry.silentdroid.core

import android.app.NotificationManager
import android.app.NotificationManager.*
import android.app.NotificationManager.Policy.PRIORITY_CATEGORY_CALLS
import android.app.NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS
import android.content.Context
import android.media.AudioManager
import android.content.Context.*
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
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_ALARM
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_MANUAL
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.entities.CurrentAlarmInstance
import ru.rrtry.silentdroid.eventBus.EventBus
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

        if (profile.streamsUnlinked) {
            if (isRinging()) {
                setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
            } else {
                setRingerMode(STREAM_NOTIFICATION, profile.notificationVolume, profile.notificationMode)
            }
        } else {
            setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
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

        val currentAlarmInstance: CurrentAlarmInstance? = scheduleManager.getCurrentAlarmInstance(alarms)
        val alarm: Alarm? = currentAlarmInstance?.relation?.alarm

        if (alarm != null) {
            if (scheduleManager.hasPreviouslyFired(alarm)) {
                if (scheduleManager.isAlarmValid(alarm)) {
                    setProfile(currentAlarmInstance.profile!!, TRIGGER_TYPE_ALARM, alarm)
                } else {
                    setProfile(currentAlarmInstance.profile!!, TRIGGER_TYPE_MANUAL, null)
                }
                notificationHelper.updateNotification(currentAlarmInstance.profile, currentAlarmInstance)
            } else {
                notificationHelper.updateNotification(preferencesManager.getProfile(), currentAlarmInstance)
            }
        } else {
            preferencesManager.getProfile()?.let { currentProfile ->
                setProfile(currentProfile, TRIGGER_TYPE_MANUAL, null)
                notificationHelper.updateNotification(currentProfile, null)
            }
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
            "New profile",
                R.drawable.ic_baseline_do_not_disturb_on_24,
                audioManager.getStreamVolume(STREAM_MUSIC),
                audioManager.getStreamVolume(STREAM_VOICE_CALL),
                audioManager.getStreamVolume(STREAM_NOTIFICATION),
                audioManager.getStreamVolume(STREAM_RING),
                audioManager.getStreamVolume(STREAM_ALARM),
                getDefaultRingtoneUri(TYPE_RINGTONE),
                getDefaultRingtoneUri(TYPE_NOTIFICATION),
                getDefaultRingtoneUri(TYPE_ALARM),
                false,
                notificationManager.currentInterruptionFilter,
                audioManager.ringerMode,
                audioManager.ringerMode,
                Settings.System.getInt(context.contentResolver, VIBRATE_WHEN_RINGING),
                notificationManager.notificationPolicy.priorityCategories,
                notificationManager.notificationPolicy.priorityCallSenders,
                notificationManager.notificationPolicy.priorityMessageSenders,
                0, 0
        ).apply {
            if (Build.VERSION_CODES.N <= Build.VERSION.SDK_INT) {
                suppressedVisualEffects = notificationManager.notificationPolicy.suppressedVisualEffects
            }
            if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
                primaryConversationSenders = notificationManager.notificationPolicy.priorityConversationSenders
            }
        }
    }

    private fun adjustUnmuteStream(streamType: Int) {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_UNMUTE, 0)
        }
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

    @Suppress("newApi")
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
        return getActualDefaultRingtoneUri(context, type)
    }

    private fun setVibrateWhenRingingBehavior(state: Int) {
        try {
            Settings.System.putInt(context.contentResolver, VIBRATE_WHEN_RINGING, state)
        } catch (e: SecurityException) {
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
            if (streamType == STREAM_VOICE_CALL || streamType == STREAM_ALARM) {
                return 1
            }
            return 0
        }

        @JvmStatic
        fun getStreamMaxVolume(context: Context, streamType: Int): Int {
            val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            return audioManager.getStreamMaxVolume(streamType) - getStreamMinVolume(context, streamType)
        }
    }
}