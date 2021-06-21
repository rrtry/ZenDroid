package com.example.volumeprofiler.activities

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Observer
import com.example.volumeprofiler.*
import com.example.volumeprofiler.fragments.ApplyChangesDialog
import com.example.volumeprofiler.fragments.TimePickerFragment
import com.example.volumeprofiler.fragments.WorkingDaysPickerDialog
import com.example.volumeprofiler.interfaces.ApplyChangesDialogCallbacks
import com.example.volumeprofiler.interfaces.DaysPickerDialogCallbacks
import com.example.volumeprofiler.interfaces.TimePickerFragmentCallbacks
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.viewmodels.EditEventViewModel
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*

class EditEventActivity: AppCompatActivity(), AdapterView.OnItemSelectedListener, TimePickerFragmentCallbacks, DaysPickerDialogCallbacks, ApplyChangesDialogCallbacks {

    private lateinit var profileSelectSpinner: Spinner
    private lateinit var startTimeSelectButton: Button
    private lateinit var workingDaysSelectButton: Button
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private val profileMap: HashMap<String, Profile> = hashMapOf()
    private val viewModel: EditEventViewModel by viewModels()
    private var event: Event? = null
    private var eventId: Long? = null
    private var profileAndEvent: ProfileAndEvent? = null

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        super.onPrepareOptionsMenu(menu)
        if (menu != null) {
            val drawable: Drawable?
            val item: MenuItem = menu.findItem(R.id.saveChangesButton)
            return if (eventId != null) {
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
            onApply()
            return true
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        return if (menu != null) {
            menuInflater.inflate(R.menu.action_menu_save_changes, menu)
            true
        }
        else {
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_event)
        eventId = intent?.extras?.get(EXTRA_ID) as? Long
        supportActionBar?.title = if (eventId != null) "Edit event" else "Create event"
        instantiateViews()
        setLiveDataObservers()
        setupCallbacks()
    }

    private fun instantiateViews(): Unit {
        profileSelectSpinner = this.findViewById(R.id.profileSpinner) as Spinner
        startTimeSelectButton = this.findViewById(R.id.startTimeSelectButton) as Button
        workingDaysSelectButton = this.findViewById(R.id.workingDaysSelectButton) as Button
    }

    private fun setupCallbacks(): Unit {
        startTimeSelectButton.setOnClickListener {
            Log.i(LOG_TAG, "startTimeSelectButton")
            val fragment: TimePickerFragment = TimePickerFragment.newInstance(event!!.localDateTime)
            fragment.show(supportFragmentManager, null)
        }
        workingDaysSelectButton.setOnClickListener {
            Log.i(LOG_TAG, "workingDaysSelectedButton")
            val fragment: WorkingDaysPickerDialog = WorkingDaysPickerDialog.newInstance(event!!)
            fragment.show(supportFragmentManager, null)
        }
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item)
        profileSelectSpinner.adapter = arrayAdapter
        profileSelectSpinner.onItemSelectedListener = this
    }

    private fun setLiveDataObservers(): Unit {
         val profileObserver = Observer<List<Profile>?> {
             if (it != null && it.isNotEmpty()) {
                 for (i in it) {
                     profileMap[i.title] = i
                     arrayAdapter.add(i.title)
                 }
                 arrayAdapter.notifyDataSetChanged()

                 val mutableEvent: Event? = viewModel.mutableEvent.value
                 if (mutableEvent == null) {
                     Log.i(LOG_TAG, "mutableEvent is null")
                     if (eventId != null) {
                         Log.i(LOG_TAG, "eventId was passed to the intent")
                         viewModel.setEvent(eventId!!)
                     }
                     else {
                         Log.i(LOG_TAG, "eventId was not passed to the intent")
                         viewModel.changesMade = true
                         viewModel.setMutableEvent(Event(profileUUID = it[0].id))
                     }
                 }
             }
             profileSelectSpinner.setSelection(viewModel.selectedProfile)
        }
        viewModel.profileListLiveData.observe(this, profileObserver)

        val immutableEventObserver = Observer<Event?> {
            if (it != null) {
                Log.i(LOG_TAG, "observing immutable livedata")
                viewModel.setMutableEvent(it)
            }
        }
        viewModel.eventLiveData.observe(this, immutableEventObserver)

        val mutableEventObserver = Observer<Event> {
            if (it != null) {
                Log.i(LOG_TAG, "observing mutable event object")
                event = it
                updateStartTimeText()
                updateScheduledDaysText()
            }
        }
        viewModel.mutableEvent.observe(this, mutableEventObserver)

        val profileAndEventObserver = Observer<ProfileAndEvent?> {
            if (it != null) {
                profileAndEvent = it
            }
        }
        viewModel.profileAndEventLiveData.observe(this, profileAndEventObserver)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        Log.i(LOG_TAG, "onNothingSelected")
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        viewModel.selectedProfile = position
        val uuid: UUID? = profileMap[arrayAdapter.getItem(position)]?.id
        if (uuid != null) {
            (event as Event).profileUUID = uuid
        }
    }

    private fun updateStartTimeText(): Unit {
        startTimeSelectButton.text = (event as Event).localDateTime.
        format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).
        withLocale(Locale.getDefault()))
    }

    private fun updateScheduledDaysText(): Unit {
        val event: Event = event as Event
        val result: String
        val workingDays: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
        if (workingDays.isNotEmpty()) {
            if (workingDays.size == 1) {
                result = DayOfWeek.of(workingDays[0]).getDisplayName(TextStyle.FULL, Locale.getDefault())
            }
            else if (workingDays.size == 7) {
                result = "Every day"
            }
            else {
                val stringBuilder: java.lang.StringBuilder = StringBuilder()
                for (i in workingDays) {
                    stringBuilder.append(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()) + ", ")
                }
                for (i in 0..1) {
                    stringBuilder.deleteCharAt(stringBuilder.lastIndex)
                }
                result = stringBuilder.toString()
            }
            workingDaysSelectButton.text = result
        }
        else {
            if (event.localDateTime.toLocalTime() > LocalTime.now()) {
                workingDaysSelectButton.text = "Today"
            } else {
                workingDaysSelectButton.text = "Tomorrow"
            }
        }
    }

    override fun onTimeSelected(date: LocalDateTime): Unit {
        viewModel.changesMade = true
        (event as Event).localDateTime = date
        updateStartTimeText()
        updateScheduledDaysText()
    }

    override fun onDaysSelected(arrayList: ArrayList<Int>): Unit {
        viewModel.changesMade = true
        val event: Event = event as Event
        val stringBuilder = StringBuilder()
        for (i in arrayList.sorted()) {
            stringBuilder.append(i + 1)
        }
        val workingDays: String = stringBuilder.toString()
        event.workingDays = workingDays
        updateScheduledDaysText()
    }

    private fun saveChanges(): Unit {
        if (profileAndEvent != null) {
            val profile: Profile? = profileAndEvent?.profile
            if (profile != null) {
                setAlarm(event!!, profile)
            }
        }
        if (eventId == null) {
            viewModel.addEvent(event!!)
        }
        else {
            viewModel.updateEvent(event!!)
        }
    }

    private fun setAlarm(event: Event, profile: Profile): Unit {
        val eventOccurrences: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
        val volumeSettingsMap: Pair<Map<Int, Int>, Map<String, Int>> = ProfileUtil.getVolumeSettingsMapPair(profile)
        val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
        alarmUtil.setAlarm(volumeSettingsMap, eventOccurrences, event.localDateTime,
                event.eventId, false, profile.id, profile.title)
    }

    override fun onBackPressed() {
        if (viewModel.changesMade) {
            val fragment: ApplyChangesDialog = ApplyChangesDialog()
            fragment.show(supportFragmentManager, null)
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onApply() {
        saveChanges()
        super.onBackPressed()
    }

    override fun onDismiss() {
        super.onBackPressed()
    }

    companion object {

        private const val LOG_TAG: String = "EditEventActivity"
        const val EXTRA_ID: String = "extra_id"
    }
}