package com.example.volumeprofiler.services

import android.annotation.TargetApi
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.CalendarContract
import com.example.volumeprofiler.database.repositories.EventRepository
import com.example.volumeprofiler.entities.Event
import com.example.volumeprofiler.entities.EventRelation
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ContentQueryHandler
import com.example.volumeprofiler.util.ContentUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class CalendarProviderSyncJob: JobService(), ContentQueryHandler.AsyncQueryCallback {

    private var runningParams: JobParameters? = null

    private val job: Job = Job()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + job)

    @Inject
    lateinit var eventRepository: EventRepository

    @Inject
    lateinit var contentUtil: ContentUtil

    @Inject
    lateinit var alarmUtil: AlarmUtil

    /*
    private val handler: Handler = Handler(Looper.getMainLooper())
    private val worker = Runnable {
        scheduleJob(this)
        Log.i("CalendarProviderJob", "performing heavy lifting ...")
        jobFinished(runningParams, false)
    }
     */

    override fun onQueryComplete(cursor: Cursor?, cookie: Any?, token: Int) {
        val eventRelation: EventRelation = cookie as EventRelation
        val event: Event = eventRelation.event
        var instanceValid: Boolean = false
        cursor?.use {
            if (it.moveToFirst()) {
                instanceValid = true

                val begin: Long = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.BEGIN))
                val end: Long = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.END))
                val rrule: String? = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.RRULE))

                event.title = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.TITLE))
                event.startTime = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.DTSTART))
                event.endTime = cursor.getLong(cursor.getColumnIndex(CalendarContract.Instances.DTEND))
                event.timezoneId = cursor.getString(cursor.getColumnIndex(CalendarContract.Instances.EVENT_TIMEZONE))
                event.instanceBeginTime = begin
                event.instanceEndTime = end
            }
        }

        scope.launch {
            if (instanceValid) {
                eventRepository.updateEvent(event)
                if (event.isInstanceObsolete(event.instanceBeginTime)) {
                    alarmUtil.scheduleAlarm(event, eventRelation.eventEndsProfile, Event.State.END)
                } else {
                    alarmUtil.scheduleAlarm(event, eventRelation.eventStartsProfile, Event.State.START)
                }
            } else {
                eventRepository.deleteEvent(event)
                alarmUtil.cancelAlarm(event)
            }
        }.ensureActive()
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        runningParams = params

        scheduleJob(this)
        scope.launch {
            val events: List<EventRelation> = eventRepository.getEvents()
            for (i in events) {
                contentUtil.queryEventNextInstances(i.event.id, TOKEN, i, this@CalendarProviderSyncJob)
            }
        }.invokeOnCompletion {
            onStopJob(runningParams)
        }
        /*
        runningParams = params
        scheduleJob(this)
        scope.launch {
            val events: List<EventRelation> = eventRepository.getEvents()
            for (i in events) {

            }
        }
        Log.i("CalendarProviderJob", "onStartJob()")
        runningParams = params
        handler.postDelayed(worker, 10*1000L)
         */
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        /*
        Log.i("CalendarProviderJob", "onStopJob()")
        handler.removeCallbacks(worker)

        job.cancel()
         */
        return false
    }

    companion object {

        private const val JOB_ID: Int = 3
        private const val TOKEN: Int = 6

        fun isScheduled(context: Context): Boolean {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val pendingJobs: List<JobInfo> = jobScheduler.allPendingJobs
            for (i in pendingJobs) {
                if (i.id == JOB_ID) {
                    return true
                }
            }
            return false
        }

        @TargetApi(Build.VERSION_CODES.N)
        private fun createJobInfo(context: Context): JobInfo {
            val componentName: ComponentName = ComponentName(context.packageName, CalendarProviderSyncJob::class.java.name)
            val builder: JobInfo.Builder = JobInfo.Builder(JOB_ID, componentName)
            builder.addTriggerContentUri(JobInfo.TriggerContentUri(
                CalendarContract.Events.CONTENT_URI,
                JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
            ))
            return builder.build()
        }

        fun scheduleJob(context: Context): Unit {
            val jobService = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobService.schedule(createJobInfo(context))
        }

        fun cancelJob(context: Context): Unit {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
        }
    }
}