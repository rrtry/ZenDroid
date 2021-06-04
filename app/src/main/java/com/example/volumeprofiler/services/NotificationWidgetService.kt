package com.example.volumeprofiler.services

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.volumeprofiler.Application.Companion.SHARED_PREFERENCES
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.MainActivity
import com.example.volumeprofiler.database.Repository
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.receivers.AlarmReceiver
import kotlinx.coroutines.*
import android.util.Log
import com.example.volumeprofiler.Application.Companion.ACTION_WIDGET_PROFILE_SELECTED
import com.example.volumeprofiler.receivers.NotificationActionReceiver
import com.example.volumeprofiler.util.ProfileUtil

class NotificationWidgetService: Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)
    private lateinit var profilesArray: List<Profile>
    private lateinit var sharedPreferences: SharedPreferences
    private var currentPosition: Int = -1

    override fun onCreate() {
        super.onCreate()
        val storageContext: Context = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            this.createDeviceProtectedStorageContext() else this
        sharedPreferences = storageContext.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)
    }

    private fun setActiveProfilePosition(): Unit {
        val id: String? = sharedPreferences.getString(AlarmReceiver.PREFS_PROFILE_ID, "")
        for ((index, i) in profilesArray.withIndex()) {
            if (i.id.toString() == id) {
                currentPosition = index
            }
        }
    }

    private fun getActiveProfileTitle(): String? = sharedPreferences.getString(AlarmReceiver.PREFS_PROFILE_TITLE, "Select profile")

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context): NotificationChannel {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return NotificationChannel(
                NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createPreviousSelectionIntent(prevPos: Int): PendingIntent {
        var prevPos: Int = prevPos
        return Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = ACTION_WIDGET_PROFILE_SELECTED
            if (prevPos == -1 || prevPos == 0) {
                prevPos = profilesArray.size - 1
            }
            else {
                prevPos -= 1
            }
            val profile: Profile = profilesArray[prevPos]
            this.putExtra(EXTRA_PROFILE_SETTINGS, ProfileUtil.getVolumeSettingsMapPair(profile))
            this.putExtra(EXTRA_PROFILE_ID, profile.id)
            this.putExtra(EXTRA_PROFILE_TITLE, profile.title)
        }.let {
            PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE_PREVIOUS_PROFILE, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun createNextSelectionIntent(nextPos: Int): PendingIntent {
        var nextPos: Int = nextPos
        return Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = ACTION_WIDGET_PROFILE_SELECTED
            if (nextPos == profilesArray.size - 1) {
                nextPos = 0
            }
            else {
                nextPos += 1
            }
            val profile: Profile = profilesArray[nextPos]
            this.putExtra(EXTRA_PROFILE_SETTINGS, ProfileUtil.getVolumeSettingsMapPair(profile))
            this.putExtra(EXTRA_PROFILE_ID, profile.id)
            this.putExtra(EXTRA_PROFILE_TITLE, profile.title)
        }.let {
            PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE_NEXT_PROFILE, it, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun createNotification(): Notification {
        val contentIntent: PendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, ACTIVITY_REQUEST_CODE, it, 0)
        }
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getActiveProfileTitle())
                .setSmallIcon(R.drawable.baseline_volume_down_deep_purple_300_24dp)
                .setOngoing(true)
                .setNotificationSilent()
                .addAction(R.drawable.baseline_navigate_before_black_24dp, "Previous", createPreviousSelectionIntent(currentPosition))
                .addAction(R.drawable.baseline_navigate_next_black_24dp, "Next", createNextSelectionIntent(currentPosition))
                .setContentIntent(contentIntent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this).also {
                builder.setChannelId(it.id)
            }
        }
        return builder.build()
    }

    @SuppressWarnings("unchecked")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.extras == null) {
            Log.i(LOG_TAG, "starting service and fetching data")
            scope.launch {
                val repository: Repository = Repository.get()
                profilesArray = async { repository.getProfiles() }.await()
                setActiveProfilePosition()
                startForeground(SERVICE_ID, createNotification())
            }
        }
        else if (intent.extras!!.getBoolean(EXTRA_UPDATE_NOTIFICATION)) {
            Log.i(LOG_TAG, "updating notification")
            setActiveProfilePosition()
            val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(SERVICE_ID, createNotification())
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
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
        const val EXTRA_PROFILE_SETTINGS: String = "extra_profile_settings"
        const val EXTRA_UPDATE_NOTIFICATION: String = "extra_update_notification"
        const val EXTRA_PROFILE_ID: String = "extra_profile_id"
        const val SERVICE_ID: Int = 172
        const val EXTRA_PROFILE_TITLE: String = "extra_profile_title"
    }
}