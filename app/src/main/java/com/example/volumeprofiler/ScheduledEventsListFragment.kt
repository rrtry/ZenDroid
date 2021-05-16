package com.example.volumeprofiler

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.AudioUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.StringBuilder
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*

class ScheduledEventsListFragment: Fragment(), AnimImplementation {

    private var requireDialog: Boolean = false
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private val model: ScheduledEventsViewModel by viewModels()
    private val sharedModel: SharedViewModel by activityViewModels()
    private val eventAdapter: EventAdapter = EventAdapter()
    private lateinit var alarmManager: AlarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.scheduled_events, container, false)
        floatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(context)) {
                    if (!requireDialog) {
                        val intent: Intent = Intent(requireContext(), EditEventActivity::class.java)
                        startActivity(intent)
                    }
                    else {
                        val fragment: NoProfilesDialog = NoProfilesDialog()
                        val fragmentManager = requireActivity().supportFragmentManager
                        fragment.show(fragmentManager, null)
                    }
                }
                else {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                    intent.data = Uri.parse("package:" + requireActivity().packageName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
            }
            else {
                if (!requireDialog) {
                    val intent: Intent = Intent(requireContext(), EditEventActivity::class.java)
                    startActivity(intent)
                }
                else {
                    val fragment: NoProfilesDialog = NoProfilesDialog()
                    val fragmentManager = requireActivity().supportFragmentManager
                    fragment.show(fragmentManager, null)
                }
            }
        }
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = eventAdapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.eventListLiveData.observe(viewLifecycleOwner,
                Observer<List<ProfileAndEvent>> { t ->
                    if (t != null) {
                        Log.i("ScheduledEventsFragment", "observing data: ${t.size}")
                        updateUI(t)
                    }
                })
        sharedModel.isProfileQueryEmpty.observe(viewLifecycleOwner,
            Observer<Boolean> { t ->
                if (t != null) {
                    Log.i("ScheduledEventsFragment", "sharedModel: $t")
                    requireDialog = t
                }
            })
    }

    private fun updateUI(events: List<ProfileAndEvent>) {
        if (events.isEmpty()) {
            view?.findViewById<TextView>(R.id.hint_scheduler)?.visibility = View.VISIBLE
            view?.findViewById<ImageView>(R.id.hint_icon_scheduler)?.visibility = View.VISIBLE
        }
        else {
            view?.findViewById<TextView>(R.id.hint_scheduler)?.visibility = View.GONE
            view?.findViewById<ImageView>(R.id.hint_icon_scheduler)?.visibility = View.GONE
        }
        eventAdapter.submitList(events)
    }

    private inner class EventHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {

        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        private val enableSwitch: Switch = itemView.findViewById(R.id.scheduleSwitch)
        private val daysTextView: TextView = itemView.findViewById(R.id.occurrencesTextView)
        private val profileTextView: TextView = itemView.findViewById(R.id.profileName)
        private lateinit var profileAndEvent: ProfileAndEvent
        private lateinit var event: Event
        private lateinit var profile: Profile

        init {
            view.setOnClickListener(this)
        }

        private fun setupCallbacks(): Unit {
            profileTextView.setOnClickListener {
                event.profileUUID = UUID.fromString("4eb9764c-af80-4f49-b352-4dd7d9ddc3f3")
                model.updateEvent(event)
            }
            enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                val profileAndEvent: ProfileAndEvent = eventAdapter.getEvent(absoluteAdapterPosition)
                val event: Event = profileAndEvent.event
                val eventOccurrences: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
                val volumeMapPair: Pair<Map<Int, Int>, Map<String, Int>> = AudioUtil.getVolumeSettingsMapPair(profile)
                val alarmUtil: AlarmUtil = AlarmUtil(requireContext().applicationContext)
                if (isChecked && enableSwitch.isPressed) {
                    event.isScheduled = 1
                    model.updateEvent(event)
                    Log.i("ScheduleListFragment", "is checked, scheduling alarm")
                    alarmUtil.setAlarm(volumeMapPair, eventOccurrences, event.localDateTime, event.eventId)
                }
                else if (!isChecked && enableSwitch.isPressed) {
                    event.isScheduled = 0
                    model.updateEvent(event)
                    Log.i("ScheduleListFragment", "isn't checked, cancelling alarm")
                    alarmUtil.cancelAlarm(volumeMapPair, eventOccurrences, event.localDateTime, event.eventId)
                }
            }
        }

        private fun updateTextViews(): Unit {
            val time = event.localDateTime.toLocalTime()
            val pattern = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
            timeTextView.text = time.format(pattern)
            daysTextView.text = setupScheduledDaysTextView()
            profileTextView.text = "${profile.title}"
        }

        private fun setupScheduledDaysTextView(): String {
            val stringBuilder: StringBuilder = StringBuilder()
            val workingsDays: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
            if (workingsDays.isNotEmpty()) {
                if (workingsDays.size == 1) {
                    return DayOfWeek.of(workingsDays[0]).getDisplayName(TextStyle.FULL, Locale.getDefault())
                }
                else if (workingsDays.size == 7) {
                    return "Every day"
                }
                for (i in workingsDays) {
                    stringBuilder.append(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()) + ", ")
                }
                for (i in 0..1) {
                    stringBuilder.deleteCharAt(stringBuilder.lastIndex)
                }
                return stringBuilder.toString()
            }
            else {
                return if (event.localDateTime.toLocalTime() > LocalTime.now()) {
                    "Today"
                } else {
                    "Tomorrow"
                }
            }
        }

        fun bindEvent(profileAndEvent: ProfileAndEvent, position: Int) {
            this.profileAndEvent = profileAndEvent
            this.event = profileAndEvent.event
            this.profile = profileAndEvent.profile
            enableSwitch.isChecked = event.isScheduled == 1
            setupCallbacks()
            updateTextViews()
        }

        override fun onClick(v: View?) {
            val eventId: Long = eventAdapter.getEvent(absoluteAdapterPosition).event.eventId
            val intent: Intent = Intent(requireContext(), EditEventActivity::class.java).apply {
                this.putExtra(EditEventActivity.EXTRA_ID, eventId)
            }
            startActivity(intent)
        }
    }

    private inner class EventAdapter : androidx.recyclerview.widget.ListAdapter<ProfileAndEvent, EventHolder>(object : DiffUtil.ItemCallback<ProfileAndEvent>() {

        override fun areItemsTheSame(oldItem: ProfileAndEvent, newItem: ProfileAndEvent): Boolean = oldItem == newItem

        override fun areContentsTheSame(oldItem: ProfileAndEvent, newItem: ProfileAndEvent): Boolean = oldItem == newItem

    }) {

        private var lastPosition: Int = -1

        fun getEvent(position: Int): ProfileAndEvent = getItem(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
            return EventHolder(layoutInflater.inflate(EVENT_LAYOUT, parent, false))
        }

        private fun startScaleUpAnimation(view: View) {
            val anim = ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f)
            anim.duration = 300
            view.startAnimation(anim)
        }

        override fun onBindViewHolder(holder: EventHolder, position: Int) {
            if (holder.absoluteAdapterPosition > lastPosition) {
                lastPosition = position
                startScaleUpAnimation(holder.itemView)
            }
            holder.bindEvent(getItem(position), position)
        }
    }

    companion object {
        private const val EVERY_DAY_OF_WEEK: Int = "Mon, Tue, Wed, Thu, Fri, Sat, Sun".length
        private const val EVENT_LAYOUT: Int = R.layout.item_view_event
    }
}