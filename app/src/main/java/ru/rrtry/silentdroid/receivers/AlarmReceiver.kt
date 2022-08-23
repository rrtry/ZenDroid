package ru.rrtry.silentdroid.receivers

import android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import ru.rrtry.silentdroid.Application.Companion.ACTION_ALARM
import ru.rrtry.silentdroid.core.ProfileManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import ru.rrtry.silentdroid.core.ScheduleManager
import ru.rrtry.silentdroid.util.ParcelableUtil.Companion.getExtra
import ru.rrtry.silentdroid.util.WakeLock
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver: BroadcastReceiver() {

    @Inject lateinit var profileManager: ProfileManager

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            ACTION_ALARM -> {
                goAsync(context!!, GlobalScope, Dispatchers.Default) {
                    profileManager.onTimeTrigger(
                        getExtra(intent, EXTRA_ALARM),
                        getExtra(intent, EXTRA_START_PROFILE),
                        getExtra(intent, EXTRA_END_PROFILE)
                    )
                }
            }
            ACTION_TIMEZONE_CHANGED, ACTION_TIME_CHANGED, ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                goAsync(context!!, GlobalScope, Dispatchers.IO) {
                    profileManager.updateProfileAsync(true)
                }
            }
            BOOT_COMPLETED -> {
                goAsync(context!!, GlobalScope, Dispatchers.IO) {
                    profileManager.updateProfileAsync(false)
                }
            }
        }
    }

    companion object {

        fun BroadcastReceiver.goAsync(
            context: Context,
            coroutineScope: CoroutineScope,
            dispatcher: CoroutineDispatcher,
            block: suspend () -> Unit
        ) {
            WakeLock.acquire(context)
            val pendingResult: PendingResult = goAsync()
            coroutineScope.launch(dispatcher) {
                block()
                WakeLock.release()
                pendingResult.finish()
            }
        }

        private val BOOT_COMPLETED: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ACTION_LOCKED_BOOT_COMPLETED
        } else {
            ACTION_BOOT_COMPLETED
        }

        internal const val EXTRA_ALARM: String = "extra_alarm"
        internal const val EXTRA_START_PROFILE: String = "extra_start_profile"
        internal const val EXTRA_END_PROFILE: String = "extra_end_profile"
    }
}