package com.example.volumeprofiler.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.volumeprofiler.*
import com.example.volumeprofiler.databinding.CreateAlarmActivityBinding
import com.example.volumeprofiler.fragments.TimePickerFragment
import com.example.volumeprofiler.fragments.TimePickerFragment.Companion.EXTRA_LOCAL_DATE_TIME
import com.example.volumeprofiler.fragments.dialogs.multiChoice.ScheduledDaysPickerDialog
import com.example.volumeprofiler.viewmodels.EditAlarmViewModel.DialogType
import com.example.volumeprofiler.models.AlarmRelation
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.viewmodels.EditAlarmViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@AndroidEntryPoint
class EditAlarmActivity: AppCompatActivity() {

    private val viewModel: EditAlarmViewModel by viewModels()

    private var elapsedTime: Long = 0

    private lateinit var binding: CreateAlarmActivityBinding

    @Inject lateinit var alarmUtil: AlarmUtil

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
                scheduleAlarm()
                setSuccessfulResult()
                true
            }
            else -> false
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
        if (viewModel.getAlarmId() != null) {
            alarmUtil.scheduleAlarm(viewModel.getAlarm(), viewModel.getProfile(), false)
        }
    }

    private fun setActionBar(): Unit {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        if (areExtrasEmpty()) {
            supportActionBar?.title = "Create alarm"
        } else {
            supportActionBar?.title = "Edit alarm"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alarm: AlarmRelation? = intent.getParcelableExtra(EXTRA_ALARM_PROFILE_RELATION) as? AlarmRelation
        setBinding()
        setActionBar()
        setFragmentResultListeners()
        collectEventsFlow()
        viewModel.profilesLiveData.observe(this, {
            viewModel.setArgs(alarm)
        })
    }

    private fun setFragmentResultListeners(): Unit {
        supportFragmentManager.setFragmentResultListener(TIME_REQUEST_KEY, this) { _, bundle ->
            val localDateTime: LocalDateTime? = bundle.getSerializable(EXTRA_LOCAL_DATE_TIME) as? LocalDateTime
            if (localDateTime != null) {
                viewModel.startTime.value = localDateTime
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
            viewModel.eventsFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).onEach {
                when (it) {
                    is EditAlarmViewModel.Event.ShowDialogEvent -> {
                        if (it.dialogType == DialogType.DAYS_SELECTION) {
                            showDaysPickerDialog()
                        } else if (it.dialogType == DialogType.TIME_SELECTION) {
                            showTimePickerDialog()
                        }
                    }
                }
            }.collect()
        }
    }

    private fun showDaysPickerDialog(): Unit {
        val fragment: ScheduledDaysPickerDialog = ScheduledDaysPickerDialog.newInstance(viewModel.scheduledDays.value!!)
        fragment.show(supportFragmentManager, null)
    }

    private fun showTimePickerDialog(): Unit {
        val fragment: TimePickerFragment = TimePickerFragment.newInstance(viewModel.startTime.value!!)
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
        const val EXTRA_ALARM_PROFILE_RELATION: String = "extra_trigger"
        const val EXTRA_ALARM: String = "extra_alarm"
    }
}