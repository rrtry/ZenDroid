package com.example.volumeprofiler.activities

import android.app.Activity
import android.content.Intent
import android.Manifest.permission.*
import android.os.Bundle
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
import com.example.volumeprofiler.viewmodels.EditAlarmViewModel.DialogType
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.EditAlarmViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class EditAlarmActivity: AppCompatActivity() {

    private val viewModel: EditAlarmViewModel by viewModels()
    private var elapsedTime: Long = 0

    private lateinit var binding: CreateAlarmActivityBinding
    private lateinit var phonePermissionLauncher: ActivityResultLauncher<String>

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            when {
                it -> {
                    saveAlarm()
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
        val alarm: AlarmRelation? = getAlarmRelation()
        setBinding()
        setActionBar()
        setFragmentResultListeners()
        collectEventsFlow()
        viewModel.profilesLiveData.observe(this, {
            viewModel.setArgs(alarm)
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        phonePermissionLauncher.unregister()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val item: MenuItem = menu.findItem(R.id.saveChangesButton)
            return if (!areExtrasEmpty()) {
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
                saveAlarm()
                true
            }
            else -> false
        }
    }

    private fun saveAlarm(): Unit {
        val alarm: Alarm = viewModel.getAlarm()
        val shouldUpdateAlarm: Boolean = viewModel.getAlarmId() != null
        val profile: Profile? = getAlarmRelation()?.profile
        if (shouldUpdateAlarm && alarm.isScheduled == 1) {
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
            menuInflater.inflate(R.menu.action_menu_save_alarm, menu)
            true
        }
        else {
            false
        }
    }

    private fun areExtrasEmpty(): Boolean {
        return intent.extras == null
    }

    private fun setBinding(): Unit {
        binding = CreateAlarmActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
    }

    private fun scheduleAlarm(): Unit {
        alarmUtil.scheduleAlarm(viewModel.getAlarm(), viewModel.getProfile(), repeating = false, showToast = true)
    }

    private fun setActionBar(): Unit {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (areExtrasEmpty()) {
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
                viewModel.localTime.value = localTime
            }
        }
        supportFragmentManager.setFragmentResultListener(SCHEDULED_DAYS_REQUEST_KEY, this) {_, bundle ->
            val scheduledDays: ArrayList<Int>? = bundle.getSerializable(ScheduledDaysPickerDialog.EXTRA_SCHEDULED_DAYS) as? ArrayList<Int>
            if (scheduledDays != null) {
                viewModel.scheduledDays.value = scheduledDays
            }
        }
    }

    private fun collectEventsFlow(): Unit {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.eventsFlow.collect {
                        when (it) {
                            is EditAlarmViewModel.Event.ShowDialogEvent -> {
                                if (it.dialogType == DialogType.DAYS_SELECTION) {
                                    showDaysPickerDialog()
                                } else if (it.dialogType == DialogType.TIME_SELECTION) {
                                    showTimePickerDialog()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDaysPickerDialog(): Unit {
        val fragment: ScheduledDaysPickerDialog = ScheduledDaysPickerDialog.newInstance(viewModel.scheduledDays.value)
        fragment.show(supportFragmentManager, null)
    }

    private fun showTimePickerDialog(): Unit {
        val fragment: TimePickerFragment = TimePickerFragment.newInstance(viewModel.localTime.value)
        fragment.show(supportFragmentManager, null)
    }

    private fun setSuccessfulResult(): Unit {
        val intent: Intent = Intent().apply {
            putExtra(EXTRA_ALARM, viewModel.getAlarm())
            putExtra(EXTRA_UPDATE_FLAG, viewModel.getAlarmId() != null)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private fun setCancelledResult(): Unit {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onBackPressed() {
        if (elapsedTime + TIME_INTERVAL > System.currentTimeMillis()) {
            setCancelledResult()
        } else {
            Toast.makeText(this, "Press back button again to dismiss changes", Toast.LENGTH_SHORT).show()
        }
        elapsedTime = System.currentTimeMillis()
    }

    companion object {

        private const val TIME_INTERVAL: Int = 2000
        private const val LOG_TAG: String = "EditEventActivity"
        const val EXTRA_UPDATE_FLAG: String = "extra_update_flag"
        const val TIME_REQUEST_KEY: String = "time_request_key"
        const val SCHEDULED_DAYS_REQUEST_KEY: String = "scheduled_days_request_key"
        const val EXTRA_ALARM_PROFILE_RELATION: String = "extra_alarm_relation"
        const val EXTRA_ALARM: String = "extra_alarm"
    }
}