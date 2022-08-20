package ru.rrtry.silentdroid.core

import android.app.NotificationManager
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Context.TELEPHONY_SERVICE
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.telephony.TelephonyManager
import android.telephony.TelephonyManager.CALL_STATE_RINGING
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.rrtry.silentdroid.entities.Profile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAudioManager @Inject constructor(@ApplicationContext private val context: Context) {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var notificationPolicyManager: NotificationPolicyManager

    private val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
    private val telephonyManager: TelephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

    val isVolumeFixed: Boolean get() = audioManager.isVolumeFixed
    val isRinging: Boolean get() = telephonyManager.callState == CALL_STATE_RINGING
    val isVoicePlatform: Boolean get() = telephonyManager.isVoiceCapable

    fun setRingerMode(streamType: Int, streamVolume: Int, mode: Int, flags: Int = 0) {
        try {
            when (mode) {
                AudioManager.RINGER_MODE_NORMAL -> setStreamVolume(streamType, streamVolume, flags)
                AudioManager.RINGER_MODE_VIBRATE -> setVibrateMode(streamType, flags)
                AudioManager.RINGER_MODE_SILENT -> setSilentMode(streamType, flags)
            }
        } catch (e: SecurityException) {
            Log.e("AppAudioManager", "Cannot change DND state", e)
        }
    }

    private fun setSilentMode(streamType: Int, flags: Int = 0) {
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            adjustUnmuteStream(streamType)
        }
        audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_MUTE, flags)
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
            Log.e("AppAudioManager", "Cannot change DND state", e)
        }
    }

    fun getStreamVolume(streamVolume: Int): Int {
        return audioManager.getStreamVolume(streamVolume)
    }

    private fun adjustUnmuteStream(streamType: Int) {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, AudioManager.ADJUST_UNMUTE, 0)
        }
    }

    private fun toggleMuteState(streamType: Int) {
        audioManager.adjustStreamVolume(
            streamType,
            AudioManager.ADJUST_TOGGLE_MUTE,
            0
        )
    }

    fun isRingerAudible(profile: Profile): Boolean {

        val interruptionFilter: Int = profile.interruptionFilter
        val priorityCategories: Int = profile.priorityCategories
        val streamsUnlinked: Boolean = profile.streamsUnlinked

        val callsPrioritized: Boolean = interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY &&
                ((priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS) != 0 ||
                        (priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_CALLS) != 0)

        return streamsUnlinked && (callsPrioritized || interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    fun isNotificationStreamIndependent(): Boolean {

        val streamType: Int = getNotificationStreamType()

        if (streamType != PreferencesManager.PREFS_STREAM_TYPE_NOT_SET) {
            return streamType == PreferencesManager.PREFS_STREAM_TYPE_INDEPENDENT
        }

        val interruptionFilter: Int = notificationPolicyManager.currentInterruptionFilter
        val notificationVol: Int = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val ringVol: Int = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        if (notificationVol != ringVol) {
            return true
        }
        if (!notificationPolicyManager.isPolicyAccessGranted) {
            return false
        }
        if (interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
            notificationPolicyManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }

        toggleMuteState(AudioManager.STREAM_NOTIFICATION)
        val independent: Boolean = audioManager.isStreamMute(AudioManager.STREAM_RING) != audioManager.isStreamMute(
            AudioManager.STREAM_NOTIFICATION
        )
        toggleMuteState(AudioManager.STREAM_NOTIFICATION)
        notificationPolicyManager.setInterruptionFilter(interruptionFilter)
        return independent
    }

    fun setNotificationStreamType(streamType: Int) {
        preferencesManager.setNotificationStreamType(streamType)
    }

    fun getNotificationStreamType(): Int {
        return preferencesManager.getNotificationStreamType()
    }

    companion object {

        @JvmStatic
        fun getStreamMinVolume(context: Context, streamType: Int): Int {
            val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return audioManager.getStreamMinVolume(streamType)
            }
            return if (streamType == AudioManager.STREAM_ALARM || streamType == AudioManager.STREAM_VOICE_CALL) 1 else 0
        }

        @JvmStatic
        fun getStreamMaxVolume(context: Context, streamType: Int): Int {
            val audioManager: AudioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            return audioManager.getStreamMaxVolume(streamType) - getStreamMinVolume(context, streamType)
        }
    }
}