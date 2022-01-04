package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.Application

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Application.ACTION_ALARM_TRIGGER -> {

            }
            Application.ACTION_CALENDAR_EVENT_TRIGGER -> {

            }
        }
    }


}