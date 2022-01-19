package com.example.volumeprofiler.util

import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import androidx.loader.content.CursorLoader

class CalendarCursorLoader(context: Context): CursorLoader(context) {

    override fun loadInBackground(): Cursor? {
        val uri = CalendarContract.Calendars.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        )
        return context.contentResolver.query(
            uri, projection, null, null, null
        )
    }
}