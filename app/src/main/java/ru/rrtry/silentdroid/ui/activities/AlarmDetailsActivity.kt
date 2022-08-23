package ru.rrtry.silentdroid.ui.activities

import android.Manifest.permission.SCHEDULE_EXACT_ALARM
import android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
import android.app.Instrumentation
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings.System.TIME_12_24
import android.provider.Settings.System.getUriFor
import android.view.Gravity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ru.rrtry.silentdroid.core.PreferencesManager
import ru.rrtry.silentdroid.core.ProfileManager
import ru.rrtry.silentdroid.core.ScheduleManager
import ru.rrtry.silentdroid.entities.Alarm
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel.DialogType
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.interfaces.DetailsViewContract
import ru.rrtry.silentdroid.util.ViewUtil.Companion.DISMISS_TIME_WINDOW
import ru.rrtry.silentdroid.util.ViewUtil.Companion.showSnackbar
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel.ViewEvent.*
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDateTime
import java.time.LocalTime
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel.DialogType.*
import kotlinx.coroutines.flow.map
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.databinding.CreateAlarmActivityBinding
import ru.rrtry.silentdroid.ui.fragments.TimePickerFragment
import ru.rrtry.silentdroid.ui.fragments.WeekDaysPickerDialog
import ru.rrtry.silentdroid.util.ContentUtil
import ru.rrtry.silentdroid.util.TimeFormatChangeObserver

@AndroidEntryPoint
class AlarmDetailsActivity: DetailsTransitionActivity(), DetailsViewContract<Alarm> {

    override val slideDirection: Int get() = Gravity.BOTTOM

    private val viewModel: AlarmDetailsViewModel by viewModels()
    private var elapsedTime: Long = 0L

    private lateinit var binding: CreateAlarmActivityBinding
    private lateinit var exactAlarmPermissionLauncher: ActivityResultLauncher<Intent>

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var contentUtil: ContentUtil
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var preferencesManager: PreferencesManager

    // Intended for non-recurring events
    private var start: LocalDateTime? = null
    private var end: LocalDateTime? = null
    private var scheduledAlarms: List<AlarmRelation>? = null
    private var timeFormatChangeObserver: TimeFormatChangeObserver? = null

    private val exactAlarmPermissionStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                viewModel.canScheduleExactAlarms = scheduleManager.canScheduleExactAlarms()
            }
        }
    }

    private fun onApply(alarm: Alarm, update: Boolean) {
        lifecycleScope.launch {

            alarm.startDateTime = start
            alarm.endDateTime = end
            alarm.title = alarm.title.trim().ifEmpty { resources.getString(R.string.no_title) }

            if (update) {
                viewModel.updateAlarm(alarm)
            } else {
                alarm.id = viewModel.addAlarm(alarm)
            }
            if (alarm.isScheduled) {
                scheduleManager.scheduleAlarm(
                    alarm,
                    viewModel.startProfile.value!!,
                    viewModel.endProfile.value!!
                )
            } else {
                scheduleManager.cancelAlarm(alarm)
            }
            profileManager.updateProfile(viewModel.getEnabledAlarms())
        }.invokeOnCompletion {
            onFinish(update)
        }
    }

    override fun onUpdate(alarm: Alarm) {
        onApply(alarm, true)
    }

    override fun onInsert(alarm: Alarm) {
        onApply(alarm, false)
    }

    override fun onFinish(result: Boolean) {
        ActivityCompat.finishAfterTransition(this)
    }

    private fun setEntity() {
        intent.getParcelableExtra<AlarmRelation>(EXTRA_ALARM_PROFILE_RELATION)?.let {
            viewModel.setEntity(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = CreateAlarmActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setContentView(binding.root)
        setEntity()
        registerTimeFormatChangeObserver()
        registerExactAlarmPermissionReceiver()
        registerExactAlarmPermissionLauncher()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.startAndEndDate.collect { dates ->
                        start = dates?.first
                        end = dates?.second
                    }
                }
                launch {
                    viewModel.eventsFlow.collect { event ->
                        when (event) {
                            is ShowDialogEvent -> showDialog(event.dialogType)
                            is OnCreateAlarmEvent -> onInsert(event.alarm)
                            is OnUpdateAlarmEvent -> onUpdate(event.alarm)
                            is OnCancelChangesEvent -> onFinish(false)
                            is OnRequestAlarmPermissionEvent -> scheduleManager.requestExactAlarmPermission(exactAlarmPermissionLauncher)
                        }
                    }
                }
                launch {
                    viewModel.alarms.map { alarms ->
                        alarms.filter { relation ->
                            relation.alarm.isScheduled
                        }
                    }.collect {
                        scheduledAlarms = it
                    }
                }
                launch {
                    viewModel.profilesStateFlow.collect { profiles ->
                        if (profiles.isNotEmpty()) {
                            viewModel.setProfiles(profiles)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.canScheduleExactAlarms = scheduleManager.canScheduleExactAlarms()
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isFinishing) {
            Instrumentation().callActivityOnSaveInstanceState(this, Bundle())
        }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterTimeFormatChangeObserver()
        unregisterReceiver(exactAlarmPermissionStateReceiver)
        unregisterExactAlarmPermissionLauncher()
    }

    private fun registerExactAlarmPermissionLauncher() {
        exactAlarmPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (!scheduleManager.canScheduleExactAlarms()) {
                showSnackbar(
                    binding.root,
                    resources.getString(R.string.snackbar_alarm_permission_explanation),
                    Snackbar.LENGTH_INDEFINITE,
                    resources.getString(R.string.open_settings)
                ) {
                    scheduleManager.requestExactAlarmPermission(exactAlarmPermissionLauncher)
                }
            }
        }
    }

    private fun registerExactAlarmPermissionReceiver() {
        registerReceiver(
            exactAlarmPermissionStateReceiver,
            IntentFilter(SCHEDULE_EXACT_ALARM)
        )
    }

    private fun registerTimeFormatChangeObserver() {
        timeFormatChangeObserver = TimeFormatChangeObserver(
            Handler(Looper.getMainLooper()))
        {
            binding.invalidateAll()
        }
        contentResolver.registerContentObserver(getUriFor(TIME_12_24), true, timeFormatChangeObserver!!)
    }

    private fun unregisterExactAlarmPermissionLauncher() {
        exactAlarmPermissionLauncher.unregister()
    }

    private fun unregisterTimeFormatChangeObserver() {
        timeFormatChangeObserver?.let { contentResolver.unregisterContentObserver(it) }
        timeFormatChangeObserver = null
    }

    private fun showDialog(dialogType: DialogType) {
        when (dialogType) {
            SCHEDULED_DAYS -> showDaysPickerDialog()
            START_TIME -> showTimePickerDialog(viewModel.startTime.value, START_TIME)
            END_TIME -> showTimePickerDialog(viewModel.endTime.value, END_TIME)
        }
    }

    private fun showDaysPickerDialog() {
        WeekDaysPickerDialog
            .newInstance(viewModel.scheduledDays.value)
            .show(supportFragmentManager, null)
    }

    private fun showTimePickerDialog(localTime: LocalTime, dialogType: DialogType) {
        TimePickerFragment
            .newInstance(localTime, dialogType)
            .show(supportFragmentManager, null)
    }

    override fun onBack() {
        if (elapsedTime + DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
            onFinish(false)
        } else {
            showSnackbar(binding.root, resources.getString(R.string.confirm_change_dismissal), LENGTH_LONG)
        }
        elapsedTime = System.currentTimeMillis()
    }

    companion object {

        internal const val EXTRA_ALARM_PROFILE_RELATION: String = "extra_alarm_relation"
    }
}