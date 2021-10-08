package com.example.volumeprofiler.broadcastReceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class PackageDataClearedReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_PACKAGE_DATA_CLEARED
                && intent.getIntExtra(Intent.EXTRA_UID, -1) == G_SERVICES_UID) {

        }
    }

    companion object {

        private const val G_SERVICES_UID: Int = 10014
    }
}