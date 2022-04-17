package com.example.volumeprofiler.util

import android.content.Context
import android.os.PowerManager

object WakeLock {

    private const val TAG: String = "volumeprofiler:PROFILE_STATE_UPDATE_WAKELOCK"
    private const val DEFAULT_TIMEOUT: Long = 1000L * 60

    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire(context: Context): Unit {
        if (wakeLock != null) {
            return
        }
        val powerManager: PowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        wakeLock?.acquire(DEFAULT_TIMEOUT)
    }

    fun release(): Unit {
        wakeLock?.release()
        wakeLock = null
    }
}