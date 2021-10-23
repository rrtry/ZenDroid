package com.example.volumeprofiler.services

import android.app.*
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

    private val job: Job = Job()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    @Inject lateinit var repository: AlarmRepository
    @Inject lateinit var alarmUtil: AlarmUtil

    private suspend fun scheduleAlarms(): Unit {
        val toSchedule: List<AlarmRelation>? = repository.getEnabledAlarms()
        if (toSchedule != null && toSchedule.isNotEmpty()) {
            alarmUtil.setAlarms(toSchedule)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground(SERVICE_ID, createSchedulerNotification(this))

        scope.launch {
            val request = launch {
                scheduleAlarms()
            }
            request.join()
            stopService()
        }
        return START_STICKY
    }

    private fun stopService(): Unit {
        stopForeground(true)
        stopSelf(SERVICE_ID)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {

        private const val SERVICE_ID: Int = 162
    }
}