package ru.rrtry.silentdroid.core

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.RingtoneManager
import android.media.RingtoneManager.TYPE_RINGTONE
import android.media.RingtoneManager.TYPE_NOTIFICATION
import android.media.RingtoneManager.TYPE_ALARM
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.util.canWriteSettings
import ru.rrtry.silentdroid.util.checkPermission
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
        return try {
            RingtoneManager.getActualDefaultRingtoneUri(context, type)
        } catch (e: RuntimeException) {
            Uri.EMPTY
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

    suspend fun getRingtoneTitle(uri: Uri, type: Int): String {
        if (!context.checkPermission(READ_EXTERNAL_STORAGE)) {
            return context.resources.getString(R.string.grant_storage_permission)
        }
        if (!canWriteSettings(context)) {
            return context.resources.getString(R.string.grant_system_settings_access)
        }
        if (uri == Uri.EMPTY) {
            return context.getString(R.string.not_set)
        }

        val contentResolver: ContentResolver = context.contentResolver
        val projection: Array<String> = arrayOf(MediaStore.MediaColumns.TITLE)

        return withContext(Dispatchers.IO) {
            var title: String = context.getString(R.string.not_set)
            try {
                val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        title = cursor.getString(0)
                    }
                }
            } catch (exception: IllegalArgumentException) {
                Log.e("ContentResolverUtil", "Unknown column for query", exception)
            }
            title
        }
    }
}