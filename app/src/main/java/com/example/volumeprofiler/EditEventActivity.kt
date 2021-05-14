package com.example.volumeprofiler

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
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*

class EditEventActivity: AppCompatActivity(), AdapterView.OnItemSelectedListener, TimePickerFragment.Callbacks, WorkingDaysPickerDialog.Callbacks {

    private lateinit var profileSelectSpinner: Spinner
    private lateinit var startTimeSelectButton: Button
    private lateinit var workingDaysSelectButton: Button
    private lateinit var arrayAdapter: ArrayAdapter<String>
    private val profileMap: HashMap<String, Profile> = hashMapOf()
    private val viewModel: EditEventViewModel by viewModels()
    private var event: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_event)
        val eventId: Long? = intent?.extras?.get(EXTRA_ID) as? Long
        if (eventId != null) {
            viewModel.selectEvent(eventId)
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
            if (event != null) {
                val fragment: TimePickerFragment = TimePickerFragment.newInstance(event!!.localDateTime)
                fragment.show(supportFragmentManager, null)
            }
        }
        workingDaysSelectButton.setOnClickListener {
            if (event != null) {
                val fragment: WorkingDaysPickerDialog = WorkingDaysPickerDialog.newInstance(event!!)
                fragment.show(supportFragmentManager, null)
            }
        }
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item)
        profileSelectSpinner.adapter = arrayAdapter
        profileSelectSpinner.onItemSelectedListener = this
    }

    private fun setLiveDataObservers(): Unit {
        viewModel.profileListLiveData.observe(this, Observer<List<Profile>?> {
            if (it != null && it.isNotEmpty()) {
                if (event == null) {
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
                viewModel.selectMutableEvent(it)
            }
        })

        viewModel.mutableEvent.observe(this, Observer {
            if (it != null) {
                Log.i("EditEventActivity", "observing mutable object")
                event = it
                updateStartTimeText()
                updateScheduledDaysText()
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
        if (event.workingDays.isNotEmpty()) {
            var result: String
            result = if (event.workingDays.length == 7) {
                "Every day"
            } else {
                val stringBuilder: java.lang.StringBuilder = StringBuilder()
                val workingsDays: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
                for (i in workingsDays) {
                    stringBuilder.append(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()) + ", ")
                }
                if (stringBuilder.length > 2) {
                    for (i in 0..1) {
                        stringBuilder.deleteCharAt(stringBuilder.lastIndex)
                    }
                }
                stringBuilder.toString()
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

    override fun onDismiss() {
        Log.i("EditEventActivity", "Dialog was dismissed()")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i("EditEventActivity", (event as Event).workingDays)
    }

    companion object {

        private const val LOG_TAG: String = "EditEventActivity"
        const val EXTRA_ID: String = "extra_id"
    }
}