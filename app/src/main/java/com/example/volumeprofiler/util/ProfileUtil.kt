package com.example.volumeprofiler.util

import android.app.NotificationManager
import android.app.NotificationManager.*
import android.content.Context
import android.media.AudioManager
import com.example.volumeprofiler.models.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.media.AudioManager.*
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import com.example.volumeprofiler.util.interruptionPolicy.*

@Singleton
class ProfileUtil @Inject constructor (
        @ApplicationContext private val context: Context
        ) {

    @Inject lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    private val audioManager: AudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val telephonyManager: TelephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val phoneStateListener: PhoneStateListener = object : PhoneStateListener() {

        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            phoneState = state
        }
    }

    init {
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private var phoneState: Int = TelephonyManager.CALL_STATE_IDLE

    private fun createNotificationPolicy(profile: Profile): Policy? {
        if (Build.VERSION_CODES.N > Build.VERSION.SDK_INT) {
            return Policy (
                bitmaskOfListContents(profile.priorityCategories),
                profile.priorityCallSenders,
                profile.priorityMessageSenders
            )
        }
        else if (Build.VERSION_CODES.N <= Build.VERSION.SDK_INT && Build.VERSION_CODES.R > Build.VERSION.SDK_INT) {
             return Policy (
                bitmaskOfListContents(profile.priorityCategories),
                profile.priorityCallSenders,
                profile.priorityMessageSenders,
                bitmaskOfListContents(profile.screenOnVisualEffects + profile.screenOffVisualEffects)
            )
        }
        else if (Build.VERSION_CODES.R <= Build.VERSION.SDK_INT) {
            return Policy (
                bitmaskOfListContents(profile.priorityCategories),
                profile.priorityCallSenders,
                profile.priorityMessageSenders,
                bitmaskOfListContents(profile.screenOnVisualEffects + profile.screenOffVisualEffects),
                profile.primaryConversationSenders
            )
        }
        return null
    }

    private fun setNotificationPolicy(policy: Policy?): Unit {
        notificationManager.notificationPolicy = policy
    }

    private fun setInterruptionFilter(profile: Profile): Unit {
        if (profile.interruptionFilter == INTERRUPTION_FILTER_PRIORITY) {
            setNotificationPolicy(createNotificationPolicy(profile))
        }
        setInterruptionFilter(profile.interruptionFilter)
    }

    private fun setInterruptionFilter(interruptionFilter: Int): Unit {
        notificationManager.setInterruptionFilter(interruptionFilter)
    }

    fun setRingerMode(streamType: Int, streamVolume: Int, mode: Int): Unit {
        when (mode) {
            RINGER_MODE_NORMAL -> setStreamVolume(streamType, streamVolume, FLAG_ALLOW_RINGER_MODES or FLAG_SHOW_UI)
            RINGER_MODE_VIBRATE -> setVibrateMode(streamType)
            RINGER_MODE_SILENT -> setSilentMode(streamType)
        }
    }

    fun setProfile(profile: Profile) {
        setInterruptionFilter(profile)
        setStreamVolume(STREAM_MUSIC, profile.mediaVolume, FLAG_SHOW_UI)
        setStreamVolume(STREAM_VOICE_CALL, profile.callVolume, FLAG_SHOW_UI)
        setStreamVolume(STREAM_ALARM, profile.alarmVolume, FLAG_SHOW_UI)
        if (phoneState == TelephonyManager.CALL_STATE_RINGING) {
            setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
        } else {
            setRingerMode(STREAM_NOTIFICATION, profile.notificationVolume, profile.notificationMode)
        }
        sharedPreferencesUtil.writeCurrentProfileProperties(profile)
    }

    fun setDefaults(): Unit {
        setInterruptionFilter(INTERRUPTION_FILTER_ALL)
        setStreamVolume(STREAM_MUSIC, 1, FLAG_SHOW_UI)
        setStreamVolume(STREAM_VOICE_CALL, 1, FLAG_SHOW_UI)
        setStreamVolume(STREAM_ALARM, 1, FLAG_SHOW_UI)
        setStreamVolume(STREAM_NOTIFICATION, 3, FLAG_SHOW_UI)
    }

    private fun setSilentMode(streamType: Int): Unit {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_RAISE, 0)
        }
        audioManager.adjustStreamVolume(streamType, ADJUST_MUTE, FLAG_ALLOW_RINGER_MODES or FLAG_SHOW_UI)
    }

    private fun setVibrateMode(streamType: Int): Unit {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_RAISE, 0)
        }
        audioManager.setStreamVolume(streamType, 0, FLAG_ALLOW_RINGER_MODES or FLAG_SHOW_UI)
    }

    private fun setStreamVolume(streamType: Int, index: Int, flags: Int): Unit {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_UNMUTE, 0)
        }
        audioManager.setStreamVolume(streamType, index, flags)
    }

    companion object {

        private fun bitmaskOfListContents(list: List<Int>): Int {
            var temp: Int = 0
            for (i in list) {
                temp = temp or i
            }
            return temp
        }
    }
}