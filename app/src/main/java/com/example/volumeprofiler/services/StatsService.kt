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
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.Application.Companion.ACTION_WIDGET_PROFILE_SELECTED
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.receivers.NotificationActionReceiver
import com.example.volumeprofiler.util.SharedPreferencesUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class StatsService: Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    private var profilesList: ArrayList<Profile> = arrayListOf()
    private val sharedPreferencesUtil = SharedPreferencesUtil.getInstance()
    private var currentPosition: Int = -1

    private fun setActiveProfilePosition(): Unit {
        val id: String? = sharedPreferencesUtil.getActiveProfileId()
        for ((index, i) in profilesList.withIndex()) {
            if (i.id.toString() == id) {
                currentPosition = index
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context): NotificationChannel {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPreviousSelectionIntent(prevPos: Int): PendingIntent {
        var prevPos: Int = prevPos
        return Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = ACTION_WIDGET_PROFILE_SELECTED
            if (prevPos == -1 || prevPos == 0) {
                prevPos = profilesList.size - 1
            }
            else {
                prevPos -= 1
            }
            val profile: Profile = profilesList[prevPos]
            this.putExtra(EXTRA_PROFILE, profile)
        }.let {
            PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE_PREVIOUS_PROFILE, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun createNextSelectionIntent(nextPos: Int): PendingIntent {
        var nextPos: Int = nextPos
        return Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = ACTION_WIDGET_PROFILE_SELECTED
            if (nextPos == profilesList.size - 1) {
                nextPos = 0
            }
            else {
                nextPos += 1
            }
            val profile: Profile = profilesList[nextPos]
            this.putExtra(EXTRA_PROFILE, profile)
        }.let {
            PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE_NEXT_PROFILE, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun createNotification(): Notification {
        val contentIntent: PendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, ACTIVITY_REQUEST_CODE, it, 0)
        }
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_volume_down_deep_purple_300_24dp)
                .setContentTitle(sharedPreferencesUtil.getActiveProfileTitle())
                .addAction(R.drawable.baseline_navigate_before_black_24dp, "Previous", createPreviousSelectionIntent(currentPosition))
                .addAction(R.drawable.baseline_navigate_next_black_24dp, "Next", createNextSelectionIntent(currentPosition))
                .setContentIntent(contentIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this).also {
                builder.setChannelId(it.id)
            }
        }
        val notification: Notification = builder.build()
        notification.flags = Notification.FLAG_ONLY_ALERT_ONCE
        return notification
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.extras == null) {
            val repository: Repository = Repository.get()
            scope.launch {
                repository.observeProfiles().collect {
                    profilesList = it as ArrayList<Profile>
                    if (profilesList.isNotEmpty()) {
                        setActiveProfilePosition()
                        startForeground(SERVICE_ID, createNotification())
                    }
                }
            }
        }
        else if (intent.extras!!.getBoolean(EXTRA_UPDATE_NOTIFICATION)) {
            setActiveProfilePosition()
            val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(SERVICE_ID, createNotification())
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {

        private const val LOG_TAG: String = "NotificationService"
        private const val NOTIFICATION_CHANNEL_ID: String = "channel_172"
        private const val NOTIFICATION_CHANNEL_NAME: String = "Profile selector"
        private const val ACTIVITY_REQUEST_CODE: Int = 0
        private const val BROADCAST_REQUEST_CODE_NEXT_PROFILE: Int = 1
        private const val BROADCAST_REQUEST_CODE_PREVIOUS_PROFILE: Int = 2
        const val EXTRA_PROFILE: String = "extra_profile_settings"
        const val EXTRA_UPDATE_NOTIFICATION: String = "extra_update_notification"
        const val SERVICE_ID: Int = 172
    }
}