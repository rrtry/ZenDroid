package com.example.volumeprofiler.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.loader.content.CursorLoader
import java.time.LocalDateTime

class EventsCursorLoader(
    context: Context,
    private val input: String?,
    private val calendarId: Int
    ): CursorLoader(context) {

    override fun loadInBackground(): Cursor? {

        val projection: Array<String> = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.RRULE
        )

        val uri: Uri = CalendarContract.Events.CONTENT_URI
        val currentMillis: Long = System.currentTimeMillis()

        var query: String = ""
        var selectionArgs: Array<String>? = null
        if (input != null && input.isNotEmpty()) {
            query += "${CalendarContract.Events.TITLE} LIKE ? AND "
            selectionArgs = arrayOf("%$input%")
        }
        query += "${CalendarContract.Events.CALENDAR_ID} = $calendarId AND " +
                "(${CalendarContract.Events.RRULE} IS NOT NULL OR $currentMillis < ${CalendarContract.Events.DTEND})"
        return context.contentResolver.query(uri, projection, query, selectionArgs, "${CalendarContract.Events.DTSTART} ASC")
    }
}