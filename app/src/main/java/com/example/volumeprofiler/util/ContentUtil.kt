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
import androidx.core.content.ContextCompat
import java.lang.StringBuilder

fun queryStarredContacts(context: Context): String {
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

fun getRingtoneTitle(context: Context, uri: Uri, type: Int): String {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            == PackageManager.PERMISSION_DENIED) {
        return "Storage permission required"
    }
    val actualUri: Uri = if (uri == Uri.EMPTY) RingtoneManager.getActualDefaultRingtoneUri(context, type) else uri
    val contentResolver: ContentResolver = context.contentResolver
    val projection: Array<String> = arrayOf(MediaStore.MediaColumns.TITLE)
    var title: String = ""
    val cursor: Cursor? = contentResolver.query(actualUri, projection, null, null, null)
    cursor?.use {
        if (cursor.moveToFirst()) {
            title = cursor.getString(0)
        }
    }
    return title
}