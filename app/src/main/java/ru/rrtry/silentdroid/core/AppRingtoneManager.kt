package ru.rrtry.silentdroid.core

import android.content.Context
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_RINGTONE
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.media.RingtoneManager.TYPE_ALARM
import android.net.Uri
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRingtoneManager @Inject constructor(@ApplicationContext private val context: Context) {

    val canWriteSettings: Boolean get() = Settings.System.canWrite(context)

    private fun isDefaultRingtoneUri(uri: Uri, type: Int): Boolean {
        return when (type) {
            TYPE_RINGTONE -> uri == Settings.System.DEFAULT_RINGTONE_URI
            TYPE_NOTIFICATION -> uri == Settings.System.DEFAULT_NOTIFICATION_URI
            TYPE_ALARM -> uri == Settings.System.DEFAULT_ALARM_ALERT_URI
            else -> throw IllegalArgumentException("Unknown ringtone type")
        }
    }

    fun setRingtoneUri(uri: Uri, type: Int) {
        try {
            if (!isDefaultRingtoneUri(uri, type)) {
                RingtoneManager.setActualDefaultRingtoneUri(context, type, uri)
            }
        } catch (e: SecurityException) {
            Log.e("AppRingtoneManager", "Failed to set ringtone uri", e)
        }
    }

    fun getDefaultRingtoneUri(type: Int): Uri {
        return when (type) {
            TYPE_RINGTONE -> Settings.System.DEFAULT_RINGTONE_URI
            TYPE_NOTIFICATION -> Settings.System.DEFAULT_NOTIFICATION_URI
            TYPE_ALARM -> Settings.System.DEFAULT_ALARM_ALERT_URI
            else -> throw IllegalArgumentException("Unknown ringtone type")
        }
    }

    fun setVibrateWhenRingingState(state: Int) {
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.VIBRATE_WHEN_RINGING,
                state
            )
        } catch (e: SecurityException) {
            Log.e("AppRingtoneManager", "Failed to change system settings", e)
        } catch (e: IllegalArgumentException) {
            Log.e("AppRingtoneManager", "Failed to change system settings", e)
        }
    }
}