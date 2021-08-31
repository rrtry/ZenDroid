package com.example.volumeprofiler.util.ui

import android.Manifest
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat

object BindingConverters {

    @JvmStatic
    fun getRingtoneTitle(context: Context, uri: Uri, type: Int): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        == PackageManager.PERMISSION_GRANTED) {
            Log.i("PermissionTest", "READ_EXTERNAL_STORAGE granted")
        } else {
            Log.i("PermissionTest", "READ_EXTERNAL_STORAGE denied")
        }
        val actualUri: Uri = if (uri == Uri.EMPTY) RingtoneManager.getActualDefaultRingtoneUri(context, type) else uri
        val contentResolver: ContentResolver = context.contentResolver
        val projection: Array<String> = arrayOf(MediaStore.MediaColumns.TITLE)
        var title: String = ""
        val cursor: Cursor? = contentResolver.query(actualUri, projection, null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                title = cursor.getString(0)
                cursor.close()
            }
        }
        return title
    }

    @JvmStatic
    fun interruptionFilterToString(interruptionFilterMode: Int): String {
        return when (interruptionFilterMode) {
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "Priority only"
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "Alarms only"
            NotificationManager.INTERRUPTION_FILTER_NONE -> "Total silence"
            NotificationManager.INTERRUPTION_FILTER_ALL -> "Allow everything"
            else -> "Unknown interruption filter"
        }
    }
}
