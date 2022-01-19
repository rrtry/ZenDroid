package com.example.volumeprofiler.util

import android.os.Handler
import android.os.HandlerThread

object DatabaseQueryHandler {

    private val sHandlerThread = HandlerThread("DatabaseHandler")
    private val sHandler: Handler

    init {
        sHandlerThread.start()
        sHandler = Handler(sHandlerThread.looper)
    }

    fun post(r: () -> Unit) {
        sHandler.post(r)
    }
}