package com.example.volumeprofiler.core

import android.app.NotificationManager
import android.app.NotificationManager.*
import android.content.Context
import android.media.AudioManager
import android.content.Context.*
import android.content.Intent
import com.example.volumeprofiler.entities.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.media.AudioManager.*
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.media.RingtoneManager.*
import android.provider.Settings.System.VIBRATE_WHEN_RINGING
import android.telephony.TelephonyManager.CALL_STATE_RINGING
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.util.ID_SCHEDULER
import com.example.volumeprofiler.util.createAlarmAlertNotification
import com.example.volumeprofiler.util.postNotification
import java.util.*
import kotlin.math.E

@Singleton
@Suppress("deprecation")
class ProfileManager @Inject constructor (@ApplicationContext private val context: Context) {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var scheduleManager: ScheduleManager

    private val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
    private val notificationManager: NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val telephonyManager: TelephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

    fun setProfile(profile: Profile) {

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

        preferencesManager.setEnabledProfile(profile)
        eventBus.onProfileChanged(profile.id)
    }

    private fun isRinging(): Boolean {
        return telephonyManager.callState == CALL_STATE_RINGING
    }

    fun setDefaultProfile() {
        setProfile(getDefaultProfile())
        preferencesManager.clearPreferences()
    }

    fun setScheduledProfile(alarms: List<AlarmRelation>) {
        scheduleManager.getRecentAlarm(alarms)?.let {

            setProfile(it.profile)

            postNotification(
                context,
                createAlarmAlertNotification(
                    context,
                    it.alarm.title,
                    it.profile.title,
                    it.time.toLocalTime()
                ), ID_SCHEDULER)
        }
    }

    fun setRingerMode(streamType: Int, streamVolume: Int, mode: Int, flags: Int = 0) {
        when (mode) {
            RINGER_MODE_NORMAL -> setStreamVolume(streamType, streamVolume, flags)
            RINGER_MODE_VIBRATE -> setVibrateMode(streamType, flags)
            RINGER_MODE_SILENT -> setSilentMode(streamType, flags)
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
        adjustUnmuteStream(streamType)
        audioManager.setStreamVolume(streamType, index, flags)
    }

    fun getDefaultProfile(): Profile {
        return Profile(
                UUID.randomUUID(),
            "New profile",
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

    fun isVolumeValid(streamType: Int, index: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return index >= audioManager.getStreamMinVolume(streamType)
        }
        if (streamType == STREAM_ALARM || streamType == STREAM_VOICE_CALL) {
            return index >= 1
        }
        return index >= 0
    }

    fun getStreamMinVolume(streamType: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return audioManager.getStreamMinVolume(streamType)
        }
        if (streamType == STREAM_ALARM || streamType == STREAM_VOICE_CALL) {
            return 1
        }
        return 0
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