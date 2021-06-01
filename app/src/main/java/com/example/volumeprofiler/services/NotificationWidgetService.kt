package com.example.volumeprofiler.services

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.MainActivity
import com.example.volumeprofiler.receivers.NotificationActionReceiver
import java.util.*

class NotificationWidgetService: Service() {

    private lateinit var profilesUUIDArray: Array<UUID>

    override fun onCreate() {
        super.onCreate()
    }

    private fun createNotification(contentIntent: PendingIntent): Notification {
        val pendingIntent: PendingIntent = Intent(this, NotificationActionReceiver::class.java).let {
            PendingIntent.getActivity(this, BROADCAST_REQUEST_CODE, it, 0)
        }
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Select profile")
                .setSmallIcon(R.drawable.baseline_volume_down_deep_purple_300_24dp)
                .setOngoing(true)
                .addAction(R.drawable.baseline_navigate_before_black_24dp, "Previous", pendingIntent)
                .addAction(R.drawable.baseline_navigate_next_black_24dp, "Next", pendingIntent)
                .setContentIntent(contentIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel().also {
                builder.setChannelId(it.id)
            }
        }
        return builder.build()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): NotificationChannel {
        val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "onStartCommand()")
        if (intent != null) {
            val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let {
                PendingIntent.getActivity(this, ACTIVITY_REQUEST_CODE, it, 0)
            }
            if (intent.extras != null) {

            }
            else {

            }
            startForeground(SERVICE_ID, createNotification(pendingIntent))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {

        const val EXTRA_PROFILES: String = "extra_profiles"
        private const val LOG_TAG: String = "ProfileSelectService"
        private const val NOTIFICATION_CHANNEL_ID: String = "channel_172"
        private const val NOTIFICATION_CHANNEL_NAME: String = "Profile selector"
        private const val SERVICE_ID: Int = 172
        private const val ACTIVITY_REQUEST_CODE: Int = 0
        private const val BROADCAST_REQUEST_CODE: Int = 1
    }
}