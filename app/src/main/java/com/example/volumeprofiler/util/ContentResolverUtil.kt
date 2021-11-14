package com.example.volumeprofiler.util

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import java.lang.StringBuilder
import javax.inject.Inject

@ViewModelScoped
class ContentResolverUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun queryStarredContacts(): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED) {
            return "Contacts permission required"
        }
        val contentResolver: ContentResolver = context.contentResolver
        val projection: Array<String> = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val cursor: Cursor? = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, "${ContactsContract.Data.STARRED} = 1", null, "${ContactsContract.Contacts.DISPLAY_NAME} ASC")
        val stringBuilder: StringBuilder = StringBuilder()
        cursor?.use {
            if (it.count > 0) {
                while (it.moveToNext() && it.position <= 2) {
                    stringBuilder.append(it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)) + if (it.position == 2) "and ${it.count} others" else ", ")
                }
            } else {
                stringBuilder.append("None")
            }
        }
        return stringBuilder.toString()
    }

    fun getRingtoneTitle(uri: Uri, type: Int): String {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_DENIED) {
            return "Storage permission required"
        }
        val actualUri: Uri = if (uri == Uri.EMPTY) RingtoneManager.getActualDefaultRingtoneUri(context, type) else uri
        val contentResolver: ContentResolver = context.contentResolver
        val projection: Array<String> = arrayOf(MediaStore.MediaColumns.TITLE)
        var title: String = "Unknown"
        try {
            val cursor: Cursor? = contentResolver.query(actualUri, projection, null, null, null)
            cursor?.use {
                if (cursor.moveToFirst()) {
                    title = cursor.getString(0)
                }
            }
        } catch (exception: IllegalArgumentException) {
            Log.e("ContentResolverUtil", "Unknown column for query", exception)
        }
        return title
    }
}