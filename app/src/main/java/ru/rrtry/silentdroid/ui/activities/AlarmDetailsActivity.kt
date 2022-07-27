package ru.rrtry.silentdroid.ui.activities

import android.app.Instrumentation
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings.System.TIME_12_24
import android.provider.Settings.System.getUriFor
import android.transition.*
import android.view.Gravity
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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
import java.time.LocalDateTime
import java.time.LocalTime
import ru.rrtry.silentdroid.viewmodels.AlarmDetailsViewModel.DialogType.*
import kotlinx.coroutines.flow.map
import ru.rrtry.silentdroid.databinding.CreateAlarmActivityBinding
import ru.rrtry.silentdroid.ui.fragments.TimePickerFragment
import ru.rrtry.silentdroid.ui.fragments.WeekDaysPickerDialog
import ru.rrtry.silentdroid.util.ContentUtil
import ru.rrtry.silentdroid.util.TimeFormatChangeObserver

@AndroidEntryPoint
class AlarmDetailsActivity: AppCompatActivity(), DetailsViewContract<Alarm> {

    private val viewModel: AlarmDetailsViewModel by viewModels()
    private var elapsedTime: Long = 0L

    private lateinit var binding: CreateAlarmActivityBinding
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var contentUtil: ContentUtil
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var preferencesManager: PreferencesManager

    // Intended for non-recurring events
    private var start: LocalDateTime? = null
    private var end: LocalDateTime? = null
    private var scheduledAlarms: List<AlarmRelation>? = null
    private var timeFormatChangeObserver: TimeFormatChangeObserver? = null

    private fun onApply(alarm: Alarm, update: Boolean) {
        lifecycleScope.launch {

            alarm.startDateTime = start
            alarm.endDateTime = end

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
            profileManager.updateScheduledProfile(viewModel.getEnabledAlarms())
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
        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        with(window) {

            sharedElementEnterTransition = ChangeBounds()
            sharedElementExitTransition = ChangeBounds()

            TransitionSet().also {

                it.ordering = TransitionSet.ORDERING_TOGETHER
                it.duration = 350
                it.addTransition(Fade())
                it.addTransition(Slide(Gravity.BOTTOM))

                it.excludeTarget(android.R.id.statusBarBackground, true)
                it.excludeTarget(android.R.id.navigationBarBackground, true)

                enterTransition = it
                exitTransition = it
            }
            allowEnterTransitionOverlap = true
        }
        binding = CreateAlarmActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setEntity()
        registerTimeFormatChangeObserver()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.startAndEndDate.collect {
                        start = it?.first
                        end = it?.second
                    }
                }
                launch {
                    viewModel.eventsFlow.collect {
                        when (it) {
                            is ShowDialogEvent -> showDialog(it.dialogType)
                            is OnCreateAlarmEvent -> onInsert(it.alarm)
                            is OnUpdateAlarmEvent -> onUpdate(it.alarm)
                            is OnCancelChangesEvent -> onFinish(false)
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
                        if (profiles.isNotEmpty()) viewModel.setProfiles(profiles)
                    }
                }
            }
        }
        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {

        }
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isFinishing) {
            Instrumentation().callActivityOnSaveInstanceState(this, Bundle())
        }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        phonePermissionLauncher.unregister()
        unregisterTimeFormatChangeObserver()
    }

    private fun registerTimeFormatChangeObserver() {
        timeFormatChangeObserver = TimeFormatChangeObserver(
            Handler(Looper.getMainLooper()))
        {
            binding.invalidateAll()
        }
        contentResolver.registerContentObserver(getUriFor(TIME_12_24), true, timeFormatChangeObserver!!)
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

    override fun onBackPressed() {
        if (elapsedTime + DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
            onFinish(false)
        } else {
            showSnackbar(binding.root, "Press back button again to dismiss changes", LENGTH_LONG)
        }
        elapsedTime = System.currentTimeMillis()
    }

    companion object {

        internal const val EXTRA_ALARM_PROFILE_RELATION: String = "extra_alarm_relation"
    }
}