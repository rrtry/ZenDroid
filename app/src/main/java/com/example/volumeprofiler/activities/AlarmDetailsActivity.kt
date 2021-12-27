package com.example.volumeprofiler.activities

import android.app.Activity
import android.content.Intent
import android.Manifest.permission.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.LogPrinter
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.*
import com.example.volumeprofiler.databinding.CreateAlarmActivityBinding
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.fragments.TimePickerFragment
import com.example.volumeprofiler.fragments.TimePickerFragment.Companion.EXTRA_LOCAL_TIME
import com.example.volumeprofiler.fragments.dialogs.PermissionExplanationDialog
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScheduledDaysPickerDialog
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel.DialogType
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import kotlin.collections.ArrayList
import com.example.volumeprofiler.viewmodels.AlarmDetailsViewModel.Event.*
@AndroidEntryPoint
class AlarmDetailsActivity: AppCompatActivity() {

    private val detailsViewModel: AlarmDetailsViewModel by viewModels()
    private var elapsedTime: Long = 0

    private lateinit var binding: CreateAlarmActivityBinding
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>
    private lateinit var calendarPermissionLauncher: ActivityResultLauncher<String>
    private var runnableInQueue: Boolean = false

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var contentUtil: ContentUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val localTimeUpdateRunnable: Runnable = Runnable {
        detailsViewModel.weekDaysLocalTime.value = LocalTime.now()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        calendarPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            detailsViewModel.readCalendarPermissionGranted.value = it
        }
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
        setBinding()
        setActionBar()
        setFragmentResultListeners()
        collectEventsFlow()
    }

    override fun onDestroy() {
        super.onDestroy()
        phonePermissionLauncher.unregister()
        calendarPermissionLauncher.unregister()
        handler.removeCallbacks(localTimeUpdateRunnable)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val item: MenuItem = menu.findItem(R.id.saveChangesButton)
            return if (intent.extras != null) {
                setSaveIcon(item)
                true
            } else {
                setAddIcon(item)
                true
            }
        }
        return false
    }

    private fun getAlarmRelation(): AlarmRelation? {
        return intent.getParcelableExtra(EXTRA_ALARM_PROFILE_RELATION) as? AlarmRelation
    }

    private fun setAddIcon(item: MenuItem): Unit {
        val drawable = ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_add, null)
        item.icon = drawable
    }

    private fun setSaveIcon(item: MenuItem): Unit {
        val drawable = ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_save, null)
        item.icon = drawable
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
                profileUtil.grantedRequiredPermissions(profile) -> {
                    scheduleAlarm()
                    setSuccessfulResult()
                }
                profileUtil.shouldRequestPhonePermission(profile) -> {
                    phonePermissionLauncher.launch(READ_PHONE_STATE)
                }
                else -> {
                    sendSystemPreferencesAccessNotification(this, profileUtil)
                }
            }
        } else {
            setSuccessfulResult()
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
        binding = CreateAlarmActivityBinding.inflate(layoutInflater)
        binding.viewModel = detailsViewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
    }

    private fun scheduleAlarm(): Unit {
        alarmUtil.scheduleAlarm(detailsViewModel.getAlarm(), detailsViewModel.getProfile(), repeating = false, showToast = true)
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
            val scheduledDays: ArrayList<Int>? = bundle.getSerializable(ScheduledDaysPickerDialog.EXTRA_SCHEDULED_DAYS) as? ArrayList<Int>
            if (scheduledDays != null) {
                detailsViewModel.scheduledDays.value = scheduledDays
            }
        }
    }

    private fun collectEventsFlow(): Unit {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    detailsViewModel.eventsFlow.collect {
                        when (it) {
                            is ShowDialogEvent -> {
                                if (it.dialogType == DialogType.DAYS_SELECTION) {
                                    showDaysPickerDialog()
                                } else if (it.dialogType == DialogType.TIME_SELECTION) {
                                    showTimePickerDialog()
                                }
                            }

                            QueryAvailableCalendarsEvent -> {
                                Log.i("EditAlarmActivity", "QueryAvailableCalendarsEvent")
                            }

                            RequestReadCalendarPermission -> {
                                calendarPermissionLauncher.launch(READ_CALENDAR)
                            }
                        }
                    }
                }
                launch {
                    detailsViewModel.profilesStateFlow.collect {
                        if (it.isNotEmpty()) {
                            detailsViewModel.setArgs(getAlarmRelation(), it)
                        }
                    }
                }
                launch {
                    detailsViewModel.shouldScheduleTimer.collect {
                        if (it && !runnableInQueue) {
                            postRunnable()
                        } else {
                            removeCallbacks()
                        }
                        handler.dump(LogPrinter(Log.DEBUG, "Handler"), "MessageQueue: ")
                    }
                }
            }
        }
    }

    private fun postRunnable(): Unit {
        handler.postDelayed(localTimeUpdateRunnable, AlarmUtil.getLocalTimeUpdateTaskDelay())
        runnableInQueue = true
    }

    private fun removeCallbacks(): Unit {
        handler.removeCallbacks(localTimeUpdateRunnable)
        runnableInQueue = false
    }

    private fun showDaysPickerDialog(): Unit {
        val fragment: ScheduledDaysPickerDialog = ScheduledDaysPickerDialog.newInstance(detailsViewModel.scheduledDays.value)
        fragment.show(supportFragmentManager, null)
    }

    private fun showTimePickerDialog(): Unit {
        val fragment: TimePickerFragment = TimePickerFragment.newInstance(detailsViewModel.localTime.value)
        fragment.show(supportFragmentManager, null)
    }

    private fun setSuccessfulResult(): Unit {
        val intent: Intent = Intent().apply {
            putExtra(EXTRA_ALARM, detailsViewModel.getAlarm())
            putExtra(EXTRA_UPDATE_FLAG, detailsViewModel.getAlarmId() != null)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun setCancelledResult(): Unit {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onBackPressed() {
        if (elapsedTime + ViewUtil.DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
            setCancelledResult()
        } else {
            Toast.makeText(this, "Press back button again to dismiss changes", Toast.LENGTH_SHORT).show()
        }
        elapsedTime = System.currentTimeMillis()
    }

    companion object {

        private const val CURSOR_LOADER_ID: Int = 1
        private const val LOG_TAG: String = "EditEventActivity"

        const val EXTRA_UPDATE_FLAG: String = "extra_update_flag"
        const val TIME_REQUEST_KEY: String = "time_request_key"
        const val SCHEDULED_DAYS_REQUEST_KEY: String = "scheduled_days_request_key"
        const val EXTRA_ALARM_PROFILE_RELATION: String = "extra_alarm_relation"
        const val EXTRA_ALARM: String = "extra_alarm"
    }
}