package com.example.volumeprofiler.util

import android.annotation.TargetApi
import android.app.NotificationManager
import android.app.NotificationManager.*
import android.content.Context
import android.media.AudioManager
import android.content.Context.*
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
import java.lang.IllegalArgumentException
import android.media.RingtoneManager.*
import android.telephony.TelephonyManager.CALL_STATE_RINGING
import com.example.volumeprofiler.eventBus.EventBus
import java.util.*

@Singleton
class ProfileManager @Inject constructor (@ApplicationContext private val context: Context) {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var eventBus: EventBus

    private val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
    private val notificationManager: NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val telephonyManager: TelephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

    @Suppress("deprecation")
    private val vibrator: Vibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator

    @Suppress("deprecation")
    private fun isRinging(): Boolean {
        return telephonyManager.callState == CALL_STATE_RINGING
    }

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

        setInterruptionFilter(profile)

        setRingtoneUri(profile.phoneRingtoneUri, TYPE_RINGTONE)
        setRingtoneUri(profile.notificationSoundUri, TYPE_NOTIFICATION)
        setRingtoneUri(profile.alarmSoundUri, TYPE_ALARM)
        setVibrateWhenRingingBehavior(profile.isVibrateForCallsActive)

        preferencesManager.writeCurrentProfileProperties(profile)
        eventBus.onProfileSet(profile.id)
    }

    fun setRingerMode(streamType: Int, streamVolume: Int, mode: Int, flags: Int = 0): Unit {
        when (mode) {
            RINGER_MODE_NORMAL -> setStreamVolume(streamType, streamVolume, flags)
            RINGER_MODE_VIBRATE -> setVibrateMode(streamType, flags)
            RINGER_MODE_SILENT -> setSilentMode(streamType, flags)
        }
    }

    private fun setSilentMode(streamType: Int, flags: Int = 0): Unit {
        if (isVibrateHardwarePresent()) {
            adjustUnmuteStream(streamType)
        }
        audioManager.adjustStreamVolume(streamType, ADJUST_MUTE, flags)
    }

    private fun setVibrateMode(streamType: Int, flags: Int = 0) {
        if (isVibrateHardwarePresent()) {
            adjustUnmuteStream(streamType)
            audioManager.setStreamVolume(streamType, 0, flags)
        } else {
            Log.i("ProfileManager", "The device does not have a vibrator")
        }
    }

    fun setStreamVolume(streamType: Int, index: Int, flags: Int): Unit {
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
                Settings.System.getInt(context.contentResolver, Settings.System.VIBRATE_WHEN_RINGING),
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

    private fun adjustUnmuteStream(streamType: Int): Unit {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_UNMUTE, 0)
        }
    }

    private fun isVibrateHardwarePresent(): Boolean {
        return vibrator.hasVibrator()
    }

    fun isNotificationPolicyAccessGranted(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    @Suppress("newApi")
    private fun createNotificationPolicy(profile: Profile): Policy {
        return when {
            Build.VERSION_CODES.N > Build.VERSION.SDK_INT -> {
                Policy (
                    profile.priorityCategories,
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders
                )
            }
            Build.VERSION_CODES.N <= Build.VERSION.SDK_INT && Build.VERSION_CODES.R > Build.VERSION.SDK_INT -> {
                Policy (
                    profile.priorityCategories,
                    profile.priorityCallSenders,
                    profile.priorityMessageSenders,
                    profile.suppressedVisualEffects
                )
            }
            else -> {
                Policy (
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
        } else {
            Log.w("ProfileManager", "Failed to set notification policy")
        }
    }

    private fun setInterruptionFilter(profile: Profile) {
        if (profile.interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
            setNotificationPolicy(createNotificationPolicy(profile))
        }
        setInterruptionFilter(profile.interruptionFilter)
    }

    private fun setInterruptionFilter(interruptionFilter: Int) {
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(interruptionFilter)
        }
    }

    private fun setRingtoneUri(uri: Uri, type: Int) {
        if (Settings.System.canWrite(context)) {
            setActualDefaultRingtoneUri(context, type, uri)
        } else {
            Log.w("ProfileManager", "Failed to set ringtone uri")
        }
    }

    private fun getDefaultRingtoneUri(type: Int): Uri {
        return getActualDefaultRingtoneUri(context, type)
    }

    private fun setVibrateWhenRingingBehavior(state: Int) {
        if (Settings.System.canWrite(context)) {
            try {
                Settings.System.putInt(context.contentResolver, Settings.System.VIBRATE_WHEN_RINGING, state)
            } catch (e: IllegalArgumentException) {
                Log.e("ProfileManager", "Failed to change system settings", e)
            }
        } else {
            Log.e("ProfileManager", "Not allowed to modify system settings", SecurityException())
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    companion object {

        @JvmStatic
        fun getStreamMaxVolume(context: Context, streamType: Int): Int {
            val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            return audioManager.getStreamMaxVolume(streamType)
        }
    }
}