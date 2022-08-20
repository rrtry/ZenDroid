package ru.rrtry.silentdroid.core

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRingtoneManager @Inject constructor(@ApplicationContext private val context: Context) {

    val canWriteSettings: Boolean get() = Settings.System.canWrite(context)

    fun setRingtoneUri(uri: Uri, type: Int) {
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, type, uri)
        } catch (e: SecurityException) {
            Log.e("AppRingtoneManager", "Failed to set ringtone uri", e)
        }
    }

    fun getDefaultRingtoneUri(type: Int): Uri {
        return RingtoneManager.getActualDefaultRingtoneUri(context, type) ?: Uri.EMPTY
    }

    fun setVibrateWhenRingingState(state: Int) {
        try {
            Settings.System.putInt(context.contentResolver,
                Settings.System.VIBRATE_WHEN_RINGING, state)
        } catch (e: SecurityException) {
            Log.e("AppRingtoneManager", "Failed to change system settings", e)
        } catch (e: IllegalArgumentException) {
            Log.e("AppRingtoneManager", "Failed to change system settings", e)
        }
    }
}