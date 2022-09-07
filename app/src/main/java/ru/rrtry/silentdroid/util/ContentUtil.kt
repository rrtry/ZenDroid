package ru.rrtry.silentdroid.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import android.annotation.SuppressLint
import android.content.ContentUris
import android.provider.CalendarContract
import java.time.*
import javax.inject.Singleton

@Singleton
class ContentUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun buildInstancesUri(startMillis: Long, endMillis: Long? = null): Uri.Builder {
        val builder: Uri.Builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        if (endMillis != null) {
            ContentUris.appendId(builder, endMillis)
        }
        return builder
    }

    fun queryCurrentEventInstance(
        id: Int, startMillis: Long,
        token: Int, cookie: Any?,
        callback: ContentQueryHandler.AsyncQueryCallback
    ) {

        val builder: Uri.Builder = buildInstancesUri(startMillis)
        val projection: Array<String> = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_TIMEZONE
        )
        val queryHandler = ContentQueryHandler(context.contentResolver, callback)
        queryHandler.startQuery(
            token,
            cookie,
            builder.build(),
            projection,
            "${CalendarContract.Instances.EVENT_ID} = $id",
            null, null)
    }

    fun queryMissedEventInstances(
        id: Int, startMillis: Long,
        token: Int, cookie: Any?,
        callback: ContentQueryHandler.AsyncQueryCallback
    ) {

        val builder: Uri.Builder = buildInstancesUri(startMillis, System.currentTimeMillis())
        val projection: Array<String> = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_TIMEZONE
        )
        val queryHandler = ContentQueryHandler(context.contentResolver, callback)
        queryHandler.startQuery(
            token,
            cookie,
            builder.build(),
            projection,
            "${CalendarContract.Instances.EVENT_ID} = $id",
            null, null)
    }

    fun queryEventNextInstances(id: Int, token: Int, cookie: Any?, callback: ContentQueryHandler.AsyncQueryCallback) {

        val now: LocalDateTime = LocalDateTime.now()
        val startMillis: Long = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis: Long = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val builder: Uri.Builder = buildInstancesUri(startMillis, endMillis)
        val projection: Array<String> = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.EVENT_TIMEZONE,
            CalendarContract.Instances.EVENT_END_TIMEZONE,
            CalendarContract.Instances.RRULE
        )
        val queryHandler = ContentQueryHandler(context.contentResolver, callback)
        queryHandler.startQuery(
            token,
            cookie,
            builder.build(),
            projection,
            "${CalendarContract.Instances.EVENT_ID} = $id",
            null, null)
    }

    @SuppressLint("Range")
    suspend fun getEventNextInstanceTime(eventId: Int): Pair<Long, Long> {

        val projection: Array<String> = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
        )

        val now: LocalDateTime = LocalDateTime.now()
        val startMillis: Long = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis: Long = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

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
                if (System.currentTimeMillis() < begin) {
                    startTime = begin
                    endTime = end
                    break
                }
            }
        }
        return Pair(startTime, endTime)
    }
}