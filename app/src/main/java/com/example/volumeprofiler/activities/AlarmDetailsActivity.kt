package com.example.volumeprofiler.activities

import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent.*
import android.content.IntentFilter
import android.os.Bundle
import android.transition.*
import android.view.Gravity
import android.view.Window
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.databinding.CreateAlarmActivityBinding
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel.DialogType
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.fragments.*
import com.example.volumeprofiler.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_CLOCK
import com.example.volumeprofiler.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SWITCH
import com.example.volumeprofiler.interfaces.DetailsViewContract
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.ViewUtil.Companion.showSnackbar
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel.ViewEvent.*
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class AlarmDetailsActivity: AppCompatActivity(), DetailsViewContract<Alarm> {

    private val viewModel: AlarmDetailsViewModel by viewModels()
    private var elapsedTime: Long = 0

    private lateinit var binding: CreateAlarmActivityBinding
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var contentUtil: ContentUtil
    @Inject lateinit var profileManager: ProfileManager

    override fun onUpdate(alarm: Alarm) {
        lifecycleScope.launch {
            viewModel.updateAlarm(alarm)
        }.invokeOnCompletion {
            if (alarm.isScheduled == 1) {
                scheduleManager.scheduleAlarm(
                    alarm,
                    viewModel.getProfile()
                )
            }
            ActivityCompat.finishAfterTransition(this)
        }
    }

    override fun onInsert(alarm: Alarm) {
        lifecycleScope.launch {
            viewModel.addAlarm(alarm)
        }.invokeOnCompletion {
            finish()
        }
    }

    override fun onCancel() {
        ActivityCompat.finishAfterTransition(this)
    }

    private fun setEntity(profiles: List<Profile>) {
        intent.getParcelableExtra<AlarmRelation>(EXTRA_ALARM_PROFILE_RELATION)?.also {
            viewModel.setEntity(it, profiles)
        }
    }

    private val timeChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.weekDaysLocalTime.value = LocalTime.now()
            val title: String = when (intent?.action) {
                ACTION_TIME_CHANGED -> "System time changed"
                ACTION_TIMEZONE_CHANGED -> "Timezone changed"
                ACTION_LOCALE_CHANGED -> "Locale changed"
                else -> ""
            }
            Snackbar.make(
                binding.root,
                title,
                Snackbar.LENGTH_SHORT
            ).show()
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
        ViewCompat.setTransitionName(binding.startClockView, SHARED_TRANSITION_CLOCK)
        ViewCompat.setTransitionName(binding.enableAlarmSwitch, SHARED_TRANSITION_SWITCH)
        setContentView(binding.root)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.eventsFlow.collect {
                        when (it) {
                            is ShowDialogEvent -> {
                                if (it.dialogType == DialogType.DAYS_SELECTION) {
                                    showDaysPickerDialog()
                                } else if (it.dialogType == DialogType.TIME_SELECTION) {
                                    showTimePickerDialog()
                                }
                            }
                            is OnCreateAlarmEvent -> {
                                onInsert(it.alarm)
                            }
                            is OnUpdateAlarmEvent -> {
                                onUpdate(it.alarm)
                            }
                            is OnCancelChangesEvent -> {
                                onCancel()
                            }
                        }
                    }
                }
                launch {
                    viewModel.profilesStateFlow.collect {
                        setEntity(it)
                    }
                }
            }
        }

        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {

        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerReceiver(timeChangeReceiver, IntentFilter().apply {
            addAction(ACTION_TIME_CHANGED)
            addAction(ACTION_TIMEZONE_CHANGED)
            addAction(ACTION_LOCALE_CHANGED)
        })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterReceiver(timeChangeReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        phonePermissionLauncher.unregister()
    }

    private fun showDaysPickerDialog() {
        WeekDaysPickerDialog.newInstance(viewModel.scheduledDays.value)
            .show(supportFragmentManager, null)
    }

    private fun showTimePickerDialog() {
        TimePickerFragment.newInstance(viewModel.localTime.value)
            .show(supportFragmentManager, null)
    }

    override fun onBackPressed() {
        if (elapsedTime + ViewUtil.DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
            ActivityCompat.finishAfterTransition(this)
        } else {
            showSnackbar(binding.root, "Press back button again to dismiss changes", LENGTH_LONG)
        }
        elapsedTime = System.currentTimeMillis()
    }

    companion object {

        internal const val EXTRA_ALARM_PROFILE_RELATION: String = "extra_alarm_relation"
    }
}