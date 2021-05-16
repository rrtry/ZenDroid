package com.example.volumeprofiler.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.volumeprofiler.*
import com.example.volumeprofiler.fragments.TimePickerFragment
import com.example.volumeprofiler.fragments.WorkingDaysPickerDialog
import com.example.volumeprofiler.interfaces.DaysPickerDialogCallbacks
import com.example.volumeprofiler.interfaces.TimePickerFragmentCallbacks
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.AudioUtil
import com.example.volumeprofiler.viewmodels.EditEventViewModel
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*

class EditEventActivity: AppCompatActivity(), AdapterView.OnItemSelectedListener, TimePickerFragmentCallbacks, DaysPickerDialogCallbacks {

    private lateinit var profileSelectSpinner: Spinner
    private lateinit var startTimeSelectButton: Button
    private lateinit var workingDaysSelectButton: Button
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private val profileMap: HashMap<String, Profile> = hashMapOf()
    private val viewModel: EditEventViewModel by viewModels()
    private var event: Event? = null
    private var eventId: Long? = null
    private var profileAndEvent: ProfileAndEvent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_event)
        eventId = intent?.extras?.get(EXTRA_ID) as? Long
        if (eventId != null) {
            viewModel.selectEvent(eventId!!)
        }
        supportActionBar?.title = if (eventId != null) "Edit event" else "Create event"
        instantiateViews()
        setLiveDataObservers()
        setupViews()
    }

    private fun instantiateViews(): Unit {
        profileSelectSpinner = this.findViewById(R.id.profileSpinner) as Spinner
        startTimeSelectButton = this.findViewById(R.id.startTimeSelectButton) as Button
        workingDaysSelectButton = this.findViewById(R.id.workingDaysSelectButton) as Button
    }

    private fun setupViews(): Unit {
        startTimeSelectButton.setOnClickListener {
            val fragment: TimePickerFragment = TimePickerFragment.newInstance(event!!.localDateTime)
            fragment.show(supportFragmentManager, null)
        }
        workingDaysSelectButton.setOnClickListener {
            val fragment: WorkingDaysPickerDialog = WorkingDaysPickerDialog.newInstance(event!!)
            fragment.show(supportFragmentManager, null)
        }
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item)
        profileSelectSpinner.adapter = arrayAdapter
        profileSelectSpinner.onItemSelectedListener = this
    }

    private fun setLiveDataObservers(): Unit {
        viewModel.profileListLiveData.observe(this, Observer<List<Profile>?> {
            if (it != null && it.isNotEmpty()) {
                if (event == null) {
                    Log.i(LOG_TAG, "creating and settings mutable event object")
                    event = Event(profileUUID = it[0].id, workingDays = LocalDateTime.now().dayOfWeek.value.toString())
                    viewModel.selectMutableEvent(event as Event)
                }
                for (i in it) {
                    profileMap[i.title] = i
                    arrayAdapter.add(i.title)
                }
                arrayAdapter.notifyDataSetChanged()
            }
            profileSelectSpinner.setSelection(viewModel.selectedProfile)
        })

        viewModel.eventLiveData.observe(this, Observer<Event?> {
            if (it != null) {
                Log.i(LOG_TAG, "observing livedata and setting mutable event object")
                viewModel.selectMutableEvent(it)
            }
        })

        viewModel.mutableEvent.observe(this, Observer {
            if (it != null) {
                Log.i("EditEventActivity", "observing mutable event object and updating ui")
                event = it
                updateStartTimeText()
                updateScheduledDaysText()
            }
        })
        viewModel.profileAndEventLiveData.observe(this, Observer {
            if (it != null) {
                profileAndEvent = it
            }
        })
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
        startTimeSelectButton.text = (event as Event).localDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
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
        (event as Event).localDateTime = date
        updateStartTimeText()
        updateScheduledDaysText()
    }

    override fun onDaysSelected(arrayList: ArrayList<Int>): Unit {
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
                resetAlarm(event!!, profile)
            }
        }
        if (eventId == null) {
            Log.i(LOG_TAG, "adding new event: ${event?.localDateTime}")
            viewModel.addEvent(event!!)
        }
        else {
            Log.i(LOG_TAG, "updating existing event")
            viewModel.updateEvent(event!!)
        }
    }

    private fun resetAlarm(event: Event, profile: Profile): Unit {
        val eventOccurrences: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
        val volumeSettingsMap: Pair<Map<Int, Int>, Map<String, Int>> = AudioUtil.getVolumeSettingsMapPair(profile)
        val alarmUtil: AlarmUtil = AlarmUtil(this.applicationContext)
        alarmUtil.setAlarm(volumeSettingsMap, eventOccurrences, event.localDateTime, event.eventId)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        saveChanges()
    }

    companion object {

        private const val LOG_TAG: String = "EditEventActivity"
        const val EXTRA_ID: String = "extra_id"
    }
}