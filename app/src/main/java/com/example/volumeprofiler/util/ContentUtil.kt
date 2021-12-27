package com.example.volumeprofiler.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.Manifest.permission.*
import android.content.ContentUris
import android.provider.CalendarContract
import androidx.activity.result.contract.ActivityResultContracts
import com.example.volumeprofiler.activities.CalendarEventDetailsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import javax.inject.Singleton

@Singleton
class ContentUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun queryEventInstances(id: Int, token: Int, cookie: Any?, callback: EventInstanceQueryHandler.AsyncQueryCallback): Unit {

        val now: LocalDateTime = LocalDateTime.now()
        val startMillis: Long = AlarmUtil.toEpochMilli(now)
        val endMillis: Long = AlarmUtil.toEpochMilli(now.plusYears(1))

        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val queryHandler = EventInstanceQueryHandler(context.contentResolver, callback)
        queryHandler.startQuery(
            token,
            cookie,
            builder.build(),
            arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END),
            "${CalendarContract.Instances.EVENT_ID} = $id",
            null, null)
    }

    suspend fun getEventNextInstanceTime(eventId: Int): Pair<Long, Long> {

        val projection: Array<String> = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
        )

        val now: LocalDateTime = LocalDateTime.now()
        val startMillis: Long = AlarmUtil.toEpochMilli(now)
        val endMillis: Long = AlarmUtil.toEpochMilli(now.plusYears(1))

        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        val query: String = "${CalendarContract.Instances.EVENT_ID} = $eventId"
        val cursor: Cursor? = context.contentResolver.query(
            builder.build(), projection, query, null, null
        )
        var startTime: Long = -1
        var endTime: Long = -1
        cursor?.use {
            while (it.moveToNext()) {
                val begin: Long = it.getLong(it.getColumnIndex(CalendarContract.Instances.BEGIN))
                val end: Long = it.getLong(it.getColumnIndex(CalendarContract.Instances.END))
                if (AlarmUtil.toEpochMilli(LocalDateTime.now()) < begin) {
                    startTime = begin
                    endTime = end
                    break
                }
            }
        }
        return Pair(startTime, endTime)
    }

    suspend fun getEventTitle(eventId: Int): String {
        val contentResolver: ContentResolver = context.contentResolver
        val projection: Array<String> = arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events._ID)
        val title: String = withContext(Dispatchers.IO) {
            try {
                val cursor: Cursor? = contentResolver.query(
                    CalendarContract.Events.CONTENT_URI, projection, "${CalendarContract.Events._ID} = $eventId", null, null)
                cursor?.use {
                    if (cursor.moveToFirst()) {
                        cursor.getString(0)
                    }
                }
                ""
            } catch (e: java.lang.IllegalArgumentException) {
                ""
            }
        }
        return title
    }

    suspend fun getRingtoneTitle(uri: Uri, type: Int): String {
        if (!checkSelfPermission(context, READ_EXTERNAL_STORAGE)) {
            return "Storage permission required"
        }
        val contentResolver: ContentResolver = context.contentResolver
        val projection: Array<String> = arrayOf(MediaStore.MediaColumns.TITLE)
        return withContext(Dispatchers.IO) {
            var title: String = "Unknown"
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