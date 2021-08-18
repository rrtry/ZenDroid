package com.example.volumeprofiler.activities

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import com.example.volumeprofiler.*
import com.example.volumeprofiler.fragments.TimePickerFragment
import com.example.volumeprofiler.fragments.dialogs.multiChoice.WorkingDaysPickerDialog
import com.example.volumeprofiler.interfaces.DaysPickerDialogCallback
import com.example.volumeprofiler.interfaces.TimePickerFragmentCallback
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.viewmodels.EditAlarmViewModel
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

class EditAlarmActivity: AppCompatActivity(), AdapterView.OnItemSelectedListener, TimePickerFragmentCallback, DaysPickerDialogCallback {

    private lateinit var profileSelectSpinner: Spinner
    private lateinit var startTimeSelectButton: Button
    private lateinit var workingDaysSelectButton: Button
    private lateinit var arrayAdapter: ArrayAdapter<Profile>
    private val viewModel: EditAlarmViewModel by viewModels()
    private var alarmExists: Boolean = false
    private var elapsedTime: Long = 0

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val drawable: Drawable?
            val item: MenuItem = menu.findItem(R.id.saveChangesButton)
            return if (alarmExists) {
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
            commitChanges()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_alarm_activity)
        val alarmTrigger: AlarmTrigger? = intent?.extras?.getParcelable(EXTRA_TRIGGER)
        if (alarmTrigger != null) {
            alarmExists = true
            supportActionBar?.title = "Edit alarm"
            if (viewModel.mutableAlarm == null) {
                viewModel.profile = alarmTrigger.profile
                viewModel.mutableAlarm = alarmTrigger.alarm
            }
        }
        else {
            alarmExists = false
            supportActionBar?.title = "Create alarm"
        }
        initViews()
        setLiveDataObservers()
        setupCallbacks()
    }

    private fun initViews(): Unit {
        profileSelectSpinner = findViewById(R.id.profileSpinner)
        startTimeSelectButton = findViewById(R.id.startTimeSelectButton)
        workingDaysSelectButton = findViewById(R.id.workingDaysSelectButton)
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
        startTimeSelectButton.setOnClickListener(onClickListener)
        workingDaysSelectButton.setOnClickListener(onClickListener)
        profileSelectSpinner.onItemSelectedListener = this
    }

    private fun getInitalPosition(items: List<Profile>): Int {
        var result by Delegates.notNull<Int>()
        for ((index, i) in items.withIndex()) {
            if (i.id == viewModel.profile?.id) {
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
                 profileSelectSpinner.adapter = arrayAdapter

                 if (!alarmExists) {
                     viewModel.selectedItem = 0
                     val alarm: Alarm = Alarm(profileUUID = it[viewModel.selectedItem].id)
                     viewModel.mutableAlarm = alarm
                     profileSelectSpinner.setSelection(viewModel.selectedItem)
                 }
                 else if (viewModel.selectedItem == -1) {
                     val position: Int = getInitalPosition(it)
                     profileSelectSpinner.setSelection(position)
                 }
                 else {
                     profileSelectSpinner.setSelection(viewModel.selectedItem)
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
        startTimeSelectButton.text = viewModel.mutableAlarm!!.localDateTime.
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
            workingDaysSelectButton.text = result
        }
        else {
            if (alarm.localDateTime.toLocalTime() > LocalTime.now()) {
                workingDaysSelectButton.text = "Today"
            } else {
                workingDaysSelectButton.text = "Tomorrow"
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

    private fun commitChanges(): Unit {
        if (viewModel.profile != null) {
            setAlarm()
        }
        if (!alarmExists) {
            viewModel.addAlarm(viewModel.mutableAlarm!!)
        }
        else {
            viewModel.updateAlarm(viewModel.mutableAlarm!!)
        }
        this.finish()
    }

    private fun setAlarm(): Unit {
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        alarmUtil.setAlarm(viewModel.mutableAlarm!!, viewModel.profile!!, false)
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
    }
}