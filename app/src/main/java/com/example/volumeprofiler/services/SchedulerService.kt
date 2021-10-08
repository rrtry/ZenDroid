package com.example.volumeprofiler.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.example.volumeprofiler.database.repositories.AlarmRepository
import com.example.volumeprofiler.models.AlarmRelation
import com.example.volumeprofiler.util.AlarmUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import com.example.volumeprofiler.util.createSchedulerNotification

@AndroidEntryPoint
class SchedulerService: Service() {

    private val job: Job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    @Inject lateinit var repository: AlarmRepository
    @Inject lateinit var alarmUtil: AlarmUtil

    private suspend fun scheduleAlarms(): Unit {
        val toSchedule: List<AlarmRelation>? = repository.getEnabledAlarms()
        if (toSchedule != null && toSchedule.isNotEmpty()) {
            alarmUtil.setMultipleAlarms(toSchedule)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(SERVICE_ID, createSchedulerNotification(this))
        scope.launch {
            val request = launch {
                scheduleAlarms()
            }
            request.join()
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {

        const val NOTIFICATION_CHANNEL_ID: String = "channel_162"
        const val NOTIFICATION_CHANNEL_NAME: String = "Background Processing"
        const val SERVICE_ID: Int = 162
    }
}