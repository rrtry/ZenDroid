package com.example.volumeprofiler.activities

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.*
import com.example.volumeprofiler.databinding.CreateAlarmActivityBinding
import com.example.volumeprofiler.fragments.TimePickerFragment
import com.example.volumeprofiler.fragments.dialogs.multiChoice.WorkingDaysPickerDialog
import com.example.volumeprofiler.viewmodels.EditAlarmViewModel.DialogType
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.viewmodels.EditAlarmViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class EditAlarmActivity: AppCompatActivity() {

    private val viewModel: EditAlarmViewModel by viewModels()

    private var elapsedTime: Long = 0

    private lateinit var binding: CreateAlarmActivityBinding
    @Inject lateinit var alarmUtil: AlarmUtil

    private var job: Job? = null

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val drawable: Drawable?
            val item: MenuItem = menu.findItem(R.id.saveChangesButton)
            return if (hasArgs) {
                drawable = ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_save, null)
                item.icon = drawable
                true
            } else {
                drawable = ResourcesCompat.getDrawable(resources, android.R.drawable.ic_menu_add, null)
                item.icon = drawable
                true
            }
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if (item.itemId == R.id.saveChangesButton) {
            setSuccessfulResult()
            return true
        }
        return false
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

    private fun setBinding(): Unit {
        binding = CreateAlarmActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    private fun setArgs(): Unit {
        if (intent.extras != null) {
            val alarmTrigger: AlarmTrigger = intent.getParcelableExtra(EXTRA_TRIGGER)!!
        } else {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBinding()
        collectFlows()
        viewModel.profilesLiveData.observe(this, androidx.lifecycle.Observer {
            val profile: Profile = getProfile(it)
        })
        /*
        val alarm: AlarmTrigger? = intent?.extras?.getParcelable(EXTRA_TRIGGER)
        if (alarm != null) {
            passedExtras = true
            loadArgs(alarm)
            supportActionBar?.title = "Edit alarm"
        }
        else {
            passedExtras = false
            supportActionBar?.title = "Create alarm"
        }
        setLiveDataObservers()
        setupCallbacks()
         */
    }

    private fun collectFlows(): Unit {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventsFlow.onEach {
                    when (it) {
                        is EditAlarmViewModel.Event.ShowDialogEvent -> {
                            if (it.dialogType == DialogType.DAYS_SELECTION) {
                                showDaysPickerDialog()
                            } else if (it.dialogType == DialogType.TIME_SELECTION) {
                                showTimePickerDialog()
                            }
                        }
                        else -> Log.i("EditAlarmActivity", "Unknown event")
                    }
                }.collect()
            }
        }
    }

    private fun getProfile(list: List<Profile>): Profile? {
        for (i in list) {
            if (profileUUID == i.id) {
                return i
            }
        }
        return null
    }

    private fun showDaysPickerDialog(): Unit {
        val fragment: WorkingDaysPickerDialog = WorkingDaysPickerDialog.newInstance(viewModel.scheduledDays.value!!)
        fragment.show(supportFragmentManager, null)
    }

    private fun showTimePickerDialog(): Unit {
        val fragment: TimePickerFragment = TimePickerFragment.newInstance(viewModel.startTime.value!!)
        fragment.show(supportFragmentManager, null)
    }

    /*
    private fun loadArgs(alarm: AlarmTrigger): Unit {
        if (viewModel.mutableAlarm == null) {
            viewModel.mutableProfile = alarm.profile
            viewModel.mutableAlarm = alarm.alarm
        }
    }

    private fun setupCallbacks(): Unit {
        val onClickListener = View.OnClickListener {
            when (it.id) {
                R.id.startTimeSelectButton -> {
                    val fragment: TimePickerFragment = TimePickerFragment.newInstance(viewModel.mutableAlarm!!.localDateTime)
                    fragment.show(supportFragmentManager, null)
                }
                R.id.workingDaysSelectButton -> {
                    val fragment: WorkingDaysPickerDialog = WorkingDaysPickerDialog.newInstance(viewModel.mutableAlarm!!)
                    fragment.show(supportFragmentManager, null)
                }
            }
        }
        binding.startTimeSelectButton.setOnClickListener(onClickListener)
        binding.workingDaysSelectButton.setOnClickListener(onClickListener)
        binding.profileSpinner.onItemSelectedListener = this
    }


    private fun getInitialAdapterPosition(items: List<Profile>): Int {
        var result by Delegates.notNull<Int>()
        for ((index, i) in items.withIndex()) {
            if (i.id == viewModel.mutableProfile?.id) {
                result = index
                break
            }
        }
        return result
    }

    private fun setLiveDataObservers(): Unit {
         val profileObserver = Observer<List<Profile>?> {
             if (it != null && it.isNotEmpty()) {

                 arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, it)
                 binding.profileSpinner.adapter = arrayAdapter

                 if (!passedExtras) {
                     viewModel.selectedItem = 0
                     val alarm: Alarm = Alarm(profileUUID = it[viewModel.selectedItem].id)
                     viewModel.mutableAlarm = alarm
                     binding.profileSpinner.setSelection(viewModel.selectedItem)
                 }
                 else if (viewModel.selectedItem == -1) {
                     val position: Int = getInitialAdapterPosition(it)
                     binding.profileSpinner.setSelection(position)
                 }
                 else {
                     binding.profileSpinner.setSelection(viewModel.selectedItem)
                 }
             }
             updateStartTimeView()
             updateDaysView()
        }
        viewModel.profileListLiveData.observe(this, profileObserver)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(LOG_TAG, "onNothingSelected")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.selectedItem = position
        val uuid: UUID? = arrayAdapter.getItem(position)?.id
        if (uuid != null) {
            viewModel.mutableAlarm!!.profileUUID = uuid
        }
    }

    private fun updateStartTimeView(): Unit {
        binding.startTimeSelectButton.text = viewModel.mutableAlarm!!.localDateTime.
        format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
    }


    private fun updateDaysView(): Unit {
        val alarm: Alarm = viewModel.mutableAlarm!!
        val result: String
        val workingDays: ArrayList<Int> = alarm.workingsDays
        if (workingDays.isNotEmpty()) {
            if (workingDays.size == 1) {
                result = DayOfWeek.of(workingDays[0]).getDisplayName(TextStyle.FULL, Locale.getDefault())
            }
            else if (workingDays.size == 7) {
                result = "Every day"
            }
            else {
                val stringBuilder: java.lang.StringBuilder = StringBuilder()
                for ((index, i) in workingDays.withIndex()) {
                    if (index < workingDays.size - 1) {
                        stringBuilder.append(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()) + ", ")
                    }
                    else {
                        stringBuilder.append(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                    }
                }
                result = stringBuilder.toString()
            }
            binding.workingDaysSelectButton.text = result
        }
        else {
            if (alarm.localDateTime.toLocalTime() > LocalTime.now()) {
                binding.workingDaysSelectButton.text = "Today"
            } else {
                binding.workingDaysSelectButton.text = "Tomorrow"
            }
        }
    }

    override fun onTimeSelected(date: LocalDateTime): Unit {
        viewModel.mutableAlarm!!.localDateTime = date
        updateStartTimeView()
        updateDaysView()
    }

    override fun onDaysSelected(arrayList: ArrayList<Int>): Unit {
        viewModel.mutableAlarm!!.workingsDays = arrayList.map { it + 1} as ArrayList<Int>
        updateDaysView()
    }

    private fun setSuccessfulResult(): Unit {
        if (viewModel.mutableProfile != null) {
            setAlarm()
        }
        if (!passedExtras) {
            viewModel.addAlarm(viewModel.mutableAlarm!!)
        }
        else {
            viewModel.updateAlarm(viewModel.mutableAlarm!!)
        }
        this.finish()
    }

    private fun setAlarm(): Unit {
        alarmUtil.setAlarm(viewModel.mutableAlarm!!, viewModel.mutableProfile!!, false)
    }
     */

    private fun setSuccessfulResult(): Unit {
        val intent: Intent = Intent().apply {
            this.putExtra(EXTRA_ALARM, viewModel.getAlarm())
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
            super.onBackPressed()
        } else {
            Toast.makeText(this, "Press back button again to exit", Toast.LENGTH_SHORT).show()
        }
        elapsedTime = System.currentTimeMillis()
    }

    companion object {

        private const val TIME_INTERVAL: Int = 2000
        private const val LOG_TAG: String = "EditEventActivity"
        const val EXTRA_TRIGGER: String = "extra_trigger"
        const val EXTRA_ALARM: String = "extra_alarm"
    }
}