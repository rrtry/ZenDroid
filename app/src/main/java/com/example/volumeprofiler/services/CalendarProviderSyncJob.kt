package com.example.volumeprofiler.services

import android.annotation.TargetApi
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CalendarProviderSyncJob: JobService() {

    private var runningParams: JobParameters? = null

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val worker = Runnable {
        scheduleJob(this)
        Log.i("CalendarProviderJob", "performing heavy lifting ...")
        jobFinished(runningParams, false)
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i("CalendarProviderJob", "onStartJob()")
        runningParams = params
        handler.postDelayed(worker, 10*1000L)
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i("CalendarProviderJob", "onStopJob()")
        handler.removeCallbacks(worker)
        return false
    }

    companion object {

        private const val JOB_ID: Int = 3

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