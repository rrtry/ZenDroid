package com.example.volumeprofiler.util

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

class TimeFormatChangeObserver(
    handler: Handler,
    private val onChange: () -> Unit
): ContentObserver(handler) {

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        onChange()
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        onChange()
    }
}