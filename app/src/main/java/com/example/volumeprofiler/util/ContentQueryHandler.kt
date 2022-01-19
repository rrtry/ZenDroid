package com.example.volumeprofiler.util

import android.content.AsyncQueryHandler
import android.content.ContentResolver
import android.database.Cursor
import java.lang.ref.WeakReference

class ContentQueryHandler(contentResolver: ContentResolver, callback: AsyncQueryCallback): AsyncQueryHandler(contentResolver) {

     private var listener: WeakReference<AsyncQueryCallback> = WeakReference(callback)

    interface AsyncQueryCallback {

        fun onQueryComplete(cursor: Cursor?, cookie: Any?, token: Int): Unit
    }

    override fun onQueryComplete(token: Int, cookie: Any?, cursor: Cursor?) {
        super.onQueryComplete(token, cookie, cursor)
        val callback: AsyncQueryCallback? = listener.get()
        callback?.onQueryComplete(cursor, cookie, token)
    }
}