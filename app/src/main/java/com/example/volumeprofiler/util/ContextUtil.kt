package com.example.volumeprofiler.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle

fun startService(context: Context, c: Class<*>): Unit {
    val intent: Intent = Intent(context, c)
    if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}