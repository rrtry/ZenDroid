package com.example.volumeprofiler.util

import android.annotation.TargetApi
import android.app.NotificationManager
import android.app.NotificationManager.*
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.Manifest.permission.*
import com.example.volumeprofiler.models.Profile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import android.media.AudioManager.*
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

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
            RINGER_MODE_NORMAL -> setStreamVolume(streamType, streamVolume, FLAG_ALLOW_RINGER_MODES)
            RINGER_MODE_VIBRATE -> setVibrateMode(streamType)
            RINGER_MODE_SILENT -> setSilentMode(streamType)
        }
    }

    private fun setRingtoneUri(uri: Uri, type: Int): Unit {
        RingtoneManager.setActualDefaultRingtoneUri(context, type, uri)
    }

    private fun setVibrateWhenRinging(state: Int): Unit {
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(context.contentResolver, Settings.System.VIBRATE_WHEN_RINGING, state)
        } else {
            Log.e("ProfileUtil", "Not allowed to modify system settings", SecurityException())
        }
    }

    fun setProfile(profile: Profile) {
        if (notificationManager.currentInterruptionFilter != INTERRUPTION_FILTER_ALL) {
            setInterruptionFilter(INTERRUPTION_FILTER_ALL)
        }
        setStreamVolume(STREAM_MUSIC, profile.mediaVolume, 0)
        setStreamVolume(STREAM_VOICE_CALL, profile.callVolume, 0)
        setStreamVolume(STREAM_ALARM, profile.alarmVolume, 0)
        if (phoneState == TelephonyManager.CALL_STATE_RINGING) {
            setRingerMode(STREAM_RING, profile.ringVolume, profile.ringerMode)
        } else {
            setRingerMode(STREAM_NOTIFICATION, profile.notificationVolume, profile.notificationMode)
        }
        setInterruptionFilter(profile)
        setRingtoneUri(profile.phoneRingtoneUri, RingtoneManager.TYPE_RINGTONE)
        setRingtoneUri(profile.notificationSoundUri, RingtoneManager.TYPE_NOTIFICATION)
        setRingtoneUri(profile.alarmSoundUri, RingtoneManager.TYPE_ALARM)
        setVibrateWhenRinging(profile.isVibrateForCallsActive)
        sharedPreferencesUtil.writeCurrentProfileProperties(profile)
    }

    fun canWriteSettings(): Boolean {
        return Settings.System.canWrite(context)
    }

    fun arePermissionsGranted(): Boolean {
        return checkSelfPermission(READ_EXTERNAL_STORAGE)
                && checkSelfPermission(READ_PHONE_STATE)
    }

    fun canSetProfile(): Boolean {
        return arePermissionsGranted() &&
                canWriteSettings() &&
                isNotificationPolicyAccessGranted()
    }

    fun getMissingPermissions(): List<String> {
        val list: ArrayList<String> = arrayListOf()
        if (!checkSelfPermission(READ_EXTERNAL_STORAGE)) {
            list.add(READ_EXTERNAL_STORAGE)
        }
        if (!checkSelfPermission(READ_PHONE_STATE)) {
            list.add(READ_PHONE_STATE)
        }
        return list
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun isNotificationPolicyAccessGranted(): Boolean {
        return notificationManager.isNotificationPolicyAccessGranted
    }

    private fun checkSelfPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun setDefaults(): Unit {
        setInterruptionFilter(INTERRUPTION_FILTER_ALL)
        setStreamVolume(STREAM_MUSIC, 1, 0)
        setStreamVolume(STREAM_VOICE_CALL, 1, 0)
        setStreamVolume(STREAM_ALARM, 1, 0)
        setStreamVolume(STREAM_NOTIFICATION, 3, 0)
        sharedPreferencesUtil.clearPreferences()
    }

    private fun setSilentMode(streamType: Int): Unit {
        audioManager.adjustStreamVolume(streamType, ADJUST_MUTE, FLAG_ALLOW_RINGER_MODES)
    }

    private fun setVibrateMode(streamType: Int): Unit {
        toggleMuteState(streamType)
        audioManager.setStreamVolume(streamType, 0, FLAG_ALLOW_RINGER_MODES)
    }

    private fun setStreamVolume(streamType: Int, index: Int, flags: Int): Unit {
        toggleMuteState(streamType)
        audioManager.setStreamVolume(streamType, index, flags)
    }

    private fun toggleMuteState(streamType: Int): Unit {
        if (audioManager.isStreamMute(streamType)) {
            audioManager.adjustStreamVolume(streamType, ADJUST_UNMUTE, 0)
        }
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