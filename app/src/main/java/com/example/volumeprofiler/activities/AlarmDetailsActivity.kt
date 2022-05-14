package com.example.volumeprofiler.activities

import android.os.Bundle
import android.transition.*
import android.util.Log
import android.view.Gravity
import android.view.Window
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.databinding.CreateAlarmActivityBinding
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel.DialogType
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.fragments.*
import com.example.volumeprofiler.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_END_TIME
import com.example.volumeprofiler.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SEPARATOR
import com.example.volumeprofiler.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_START_TIME
import com.example.volumeprofiler.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SWITCH
import com.example.volumeprofiler.interfaces.DetailsViewContract
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.ViewUtil.Companion.DISMISS_TIME_WINDOW
import com.example.volumeprofiler.util.ViewUtil.Companion.showSnackbar
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel.ViewEvent.*
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import java.time.LocalDateTime
import java.time.LocalTime
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel.DialogType.*
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class AlarmDetailsActivity: AppCompatActivity(), DetailsViewContract<Alarm> {

    private val viewModel: AlarmDetailsViewModel by viewModels()
    private var elapsedTime: Long = 0L

    private lateinit var binding: CreateAlarmActivityBinding
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var contentUtil: ContentUtil
    @Inject lateinit var profileManager: ProfileManager

    // Intended for non-recurring events
    private var start: LocalDateTime? = null
    private var end: LocalDateTime? = null

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
                    viewModel.getStartProfile(),
                    viewModel.getEndProfile()
                )
            }
            profileManager.setScheduledProfile(viewModel.getEnabledAlarms())
        }.invokeOnCompletion {
            ActivityCompat.finishAfterTransition(this)
        }
    }

    override fun onUpdate(alarm: Alarm) {
        onApply(alarm, true)
    }

    override fun onInsert(alarm: Alarm) {
        onApply(alarm, false)
    }

    override fun onCancel() {
        ActivityCompat.finishAfterTransition(this)
    }

    private fun setEntity(profiles: List<Profile>) {
        intent.getParcelableExtra<AlarmRelation>(EXTRA_ALARM_PROFILE_RELATION)?.also {
            viewModel.setEntity(it, profiles)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        with(window) {
            sharedElementEnterTransition = TransitionSet().apply {
                addTransition(ChangeBounds())
            }
            enterTransition = TransitionSet().apply {

                ordering = TransitionSet.ORDERING_TOGETHER
                duration = 350
                addTransition(Fade())
                addTransition(Slide(Gravity.BOTTOM))

                excludeTarget(android.R.id.statusBarBackground, true)
                excludeTarget(android.R.id.navigationBarBackground, true)
            }
            allowEnterTransitionOverlap = true
        }

        binding = CreateAlarmActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        ViewCompat.setTransitionName(binding.startTime, SHARED_TRANSITION_START_TIME)
        ViewCompat.setTransitionName(binding.endTime, SHARED_TRANSITION_END_TIME)
        ViewCompat.setTransitionName(binding.enableAlarmSwitch, SHARED_TRANSITION_SWITCH)
        ViewCompat.setTransitionName(binding.clockViewSeparator, SHARED_TRANSITION_SEPARATOR)

        setContentView(binding.root)

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
                            is OnCancelChangesEvent -> onCancel()
                        }
                    }
                }
                launch {
                    viewModel.profilesStateFlow.collectLatest {
                        if (it.isNotEmpty()) {
                            setEntity(it)
                        }
                    }
                }
            }
        }
        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        phonePermissionLauncher.unregister()
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
            onCancel()
        } else {
            showSnackbar(binding.root, "Press back button again to dismiss changes", LENGTH_LONG)
        }
        elapsedTime = System.currentTimeMillis()
    }

    companion object {

        internal const val EXTRA_ALARM_PROFILE_RELATION: String = "extra_alarm_relation"
    }
}