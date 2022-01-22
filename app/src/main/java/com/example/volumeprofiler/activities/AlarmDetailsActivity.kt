package com.example.volumeprofiler.activities

import android.content.Intent
import android.Manifest.permission.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Bundle
import android.transition.*
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.*
import com.example.volumeprofiler.databinding.CreateAlarmLayoutBinding
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.fragments.TimePickerFragment.Companion.EXTRA_LOCAL_TIME
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel.DialogType
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.fragments.*
import com.example.volumeprofiler.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_CLOCK
import com.example.volumeprofiler.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SWITCH
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel.Event.*

@AndroidEntryPoint
class AlarmDetailsActivity: AppCompatActivity() {

    private val detailsViewModel: AlarmDetailsViewModel by viewModels()
    private var elapsedTime: Long = 0

    private lateinit var binding: CreateAlarmLayoutBinding
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var contentUtil: ContentUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    private val timeChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            detailsViewModel.weekDaysLocalTime.value = LocalTime.now()
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

                duration = 350
                ordering = TransitionSet.ORDERING_TOGETHER
                addTransition(Fade())
                addTransition(Slide(Gravity.BOTTOM))

                excludeTarget(R.id.action_bar_container, true)
                excludeTarget(android.R.id.statusBarBackground, true)
                excludeTarget(android.R.id.navigationBarBackground, true)
            }
            allowEnterTransitionOverlap = true
        }

        setBinding()
        setActionBar()
        setFragmentResultListeners()
        collectEventsFlow()

        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            when {
                it -> {
                    onSaveChangesItemClick()
                }
                shouldShowRequestPermissionRationale(READ_PHONE_STATE) -> {
                    Snackbar.make(binding.root, "Phone permission is required", Snackbar.LENGTH_LONG).apply {
                        setAction("Show explanation") {
                            ViewUtil.showPhoneStatePermissionExplanation(supportFragmentManager)
                        }
                    }.show()
                }
                else -> {
                    ViewUtil.showPhoneStatePermissionExplanation(supportFragmentManager)
                }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerReceiver(timeChangeReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
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

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val item: MenuItem = menu.findItem(R.id.saveChangesButton)
            return if (intent.extras != null) {
                ViewUtil.setActionMenuSaveIcon(this, item)
                true
            } else {
                ViewUtil.setActionMenuAddIcon(this, item)
                true
            }
        }
        return false
    }

    private fun getAlarmRelation(): AlarmRelation? {
        return intent.getParcelableExtra(EXTRA_ALARM_PROFILE_RELATION) as? AlarmRelation
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.saveChangesButton -> {
                onSaveChangesItemClick()
                true
            }
            else -> false
        }
    }

    private fun onSaveChangesItemClick(): Unit {
        val alarm: Alarm = detailsViewModel.getAlarm()
        val updateAlarm: Boolean = detailsViewModel.getAlarmId() != null
        val profile: Profile? = getAlarmRelation()?.profile
        if (updateAlarm && alarm.isScheduled == 1) {
            when {
                profileUtil.grantedRequiredPermissions(profile!!) -> {
                    updateAlarm()
                }
                profileUtil.shouldRequestPhonePermission(profile) -> {
                    phonePermissionLauncher.launch(READ_PHONE_STATE)
                }
                else -> {
                    sendSystemPreferencesAccessNotification(this, profileUtil)
                }
            }
        } else {
            updateAlarm()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        return if (menu != null) {
            menuInflater.inflate(R.menu.action_menu_scheduler, menu)
            true
        }
        else {
            false
        }
    }

    private fun setBinding(): Unit {
        binding = CreateAlarmLayoutBinding.inflate(layoutInflater)
        binding.viewModel = detailsViewModel
        binding.lifecycleOwner = this

        ViewCompat.setTransitionName(binding.clockView, SHARED_TRANSITION_CLOCK)
        ViewCompat.setTransitionName(binding.enableAlarmSwitch, SHARED_TRANSITION_SWITCH)

        binding.applyButton.setOnClickListener {
            onSaveChangesItemClick()
        }
        binding.cancelButton.setOnClickListener {
            ActivityCompat.finishAfterTransition(this)
        }
        setContentView(binding.root)
    }

    private fun setActionBar(): Unit {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (intent.extras == null) {
            supportActionBar?.title = "Create alarm"
        } else {
            supportActionBar?.title = "Edit alarm"
        }
    }

    private fun setFragmentResultListeners(): Unit {
        supportFragmentManager.setFragmentResultListener(
            PermissionExplanationDialog.PERMISSION_REQUEST_KEY, this,
            { requestKey, result ->
                if (result.getBoolean(PermissionExplanationDialog.EXTRA_RESULT_OK)) {
                    if (shouldShowRequestPermissionRationale(READ_PHONE_STATE)) {
                        phonePermissionLauncher.launch(READ_PHONE_STATE)
                    } else {
                        startActivity(getApplicationSettingsIntent(this))
                    }
                } else if (!shouldShowRequestPermissionRationale(READ_PHONE_STATE)) {
                    Snackbar.make(binding.root, "You can always grant permissions in settings", Snackbar.LENGTH_LONG).show()
                }
            })
        supportFragmentManager.setFragmentResultListener(TIME_REQUEST_KEY, this) { _, bundle ->
            val localTime: LocalTime? = bundle.getSerializable(EXTRA_LOCAL_TIME) as? LocalTime
            if (localTime != null) {
                detailsViewModel.localTime.value = localTime
                detailsViewModel.weekDaysLocalTime.value = localTime
            }
        }
        supportFragmentManager.setFragmentResultListener(SCHEDULED_DAYS_REQUEST_KEY, this) {_, bundle ->
            val scheduledDays: Int = bundle.getInt(BaseDialog.EXTRA_MASK, 0)
            detailsViewModel.scheduledDays.value = scheduledDays
        }
    }

    private fun collectEventsFlow(): Unit {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    detailsViewModel.eventsFlow.collect {
                        if (it is ShowDialogEvent) {
                            if (it.dialogType == DialogType.DAYS_SELECTION) {
                                showDaysPickerDialog()
                            } else if (it.dialogType == DialogType.TIME_SELECTION) {
                                showTimePickerDialog()
                            }
                        }
                    }
                }
                launch {
                    detailsViewModel.profilesStateFlow.collect {
                        if (it.isNotEmpty()) {
                            detailsViewModel.setArgs(getAlarmRelation(), it)
                        }
                        binding.enableAlarmSwitch.jumpDrawablesToCurrentState()
                    }
                }
                launch {
                    detailsViewModel.updateNextAlarmDay.collect {
                        if (it) {
                            registerReceiver(timeChangeReceiver, IntentFilter().apply {
                                addAction(Intent.ACTION_TIME_CHANGED)
                                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                                addAction(Intent.ACTION_LOCALE_CHANGED)
                                addAction(Intent.ACTION_TIME_TICK)
                            })
                        } else {
                            registerReceiver(timeChangeReceiver, IntentFilter().apply {
                                addAction(Intent.ACTION_TIME_CHANGED)
                                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                                addAction(Intent.ACTION_LOCALE_CHANGED)
                            })
                        }
                    }
                }
            }
        }
    }

    private fun showDaysPickerDialog(): Unit {
        val fragment: WeekDaysPickerDialog = WeekDaysPickerDialog.newInstance(detailsViewModel.scheduledDays.value)
        fragment.show(supportFragmentManager, null)
    }

    private fun showTimePickerDialog(): Unit {
        val fragment: TimePickerFragment = TimePickerFragment.newInstance(detailsViewModel.localTime.value)
        fragment.show(supportFragmentManager, null)
    }

    private fun updateAlarm(): Unit {
        lifecycleScope.launch {
            val alarm: Alarm = detailsViewModel.getAlarm()
            val update: Boolean = detailsViewModel.getAlarmId() != null
            val scheduled: Boolean = alarm.isScheduled == 1
            if (update) {
                detailsViewModel.updateAlarm(detailsViewModel.getAlarm())
            } else {
                detailsViewModel.addAlarm(detailsViewModel.getAlarm())
            }
            if (scheduled) {
                alarmUtil.scheduleAlarm(
                    alarm,
                    detailsViewModel.getProfile())
            }
        }.invokeOnCompletion {
            ActivityCompat.finishAfterTransition(this)
        }
    }

    override fun onBackPressed() {
        if (elapsedTime + ViewUtil.DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
            ActivityCompat.finishAfterTransition(this)
        } else {
            Toast.makeText(this, "Press back button again to dismiss changes", Toast.LENGTH_SHORT).show()
        }
        elapsedTime = System.currentTimeMillis()
    }

    companion object {

        private const val CURSOR_LOADER_ID: Int = 1
        private const val LOG_TAG: String = "EditEventActivity"

        const val TIME_REQUEST_KEY: String = "time_request_key"
        const val SCHEDULED_DAYS_REQUEST_KEY: String = "scheduled_days_request_key"
        const val EXTRA_ALARM_PROFILE_RELATION: String = "extra_alarm_relation"
    }
}