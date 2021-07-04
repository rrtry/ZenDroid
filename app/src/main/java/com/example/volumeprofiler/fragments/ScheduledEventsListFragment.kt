package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.*
import com.example.volumeprofiler.activities.EditEventActivity
import com.example.volumeprofiler.adapters.*
import com.example.volumeprofiler.fragments.dialogs.WarningDialog
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.models.Event
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.ProfileAndEvent
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.AnimationUtils
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.viewmodels.ScheduledEventsViewModel
import com.example.volumeprofiler.viewmodels.SharedViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.StringBuilder
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*

class ScheduledEventsListFragment: Fragment() {

    private var requireDialog: Boolean = false
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var tracker: SelectionTracker<Long>
    private val model: ScheduledEventsViewModel by viewModels()
    private val sharedModel: SharedViewModel by activityViewModels()
    private val eventAdapter: EventAdapter = EventAdapter()
    private var actionMode: ActionMode? = null

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
                        val fragment: WarningDialog = WarningDialog()
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
                    val fragment: WarningDialog = WarningDialog()
                    val fragmentManager = requireActivity().supportFragmentManager
                    fragment.show(fragmentManager, null)
                }
            }
        }
        return view
    }

    private fun initRecyclerView(view: View): Unit {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = eventAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun initSelectionTracker(): Unit {
        tracker = SelectionTracker.Builder<Long>(
                "SelectionId2",
                recyclerView,
                KeyProvider(eventAdapter),
                DetailsLookup(recyclerView),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()
        tracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                super.onSelectionChanged()
                if (tracker.hasSelection() && actionMode == null) {
                    actionMode = requireActivity().startActionMode(object : ActionMode.Callback {

                        override fun onActionItemClicked(
                                mode: ActionMode?,
                                item: MenuItem?
                        ): Boolean {
                            if (item?.itemId == R.id.deleteProfileButton) {
                                for (i in tracker.selection) {
                                    for ((index, j) in eventAdapter.currentList.withIndex()) {
                                        if (j.event.id == i) {
                                            removeEvent(index)
                                        }
                                    }
                                }
                                mode?.finish()
                                return true
                            }
                            return false
                        }

                        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                            mode?.menuInflater?.inflate(R.menu.action_menu_selected, menu)
                            return true
                        }

                        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = true

                        override fun onDestroyActionMode(mode: ActionMode?) {
                            tracker.clearSelection()
                        }
                    })
                    setSelectedTitle(tracker.selection.size())
                } else if (!tracker.hasSelection()) {
                    actionMode?.finish()
                    actionMode = null
                } else {
                    setSelectedTitle(tracker.selection.size())
                }
            }
        })
    }

    private fun setSelectedTitle(selected: Int) {
        actionMode?.title = "Selected: $selected"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView(view)
        initSelectionTracker()
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
        }
        else {
            view?.findViewById<TextView>(R.id.hint_scheduler)?.visibility = View.GONE
        }
        eventAdapter.submitList(events)
    }

    private fun removeEvent(adapterPosition: Int): Unit {
        val profileAndEvent: ProfileAndEvent = eventAdapter.getEventAndProfile(adapterPosition)
        val profile: Profile = profileAndEvent.profile
        val event: Event = profileAndEvent.event

        model.removeEvent(event)
        if (event.isScheduled == 1) {
            val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
            val eventOccurrences: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
            val volumeMapPair: Pair<Map<Int, Int>, Map<String, Int>> = ProfileUtil.getVolumeSettingsMapPair(profile)
            alarmUtil.cancelAlarm(volumeMapPair, eventOccurrences,
                    event.localDateTime, event.id, profile.id, profile.title)
        }
    }

    private inner class EventHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, ViewHolderItemDetailsProvider<Long> {

        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        private val enableSwitch: Switch = itemView.findViewById(R.id.scheduleSwitch)
        private val daysTextView: TextView = itemView.findViewById(R.id.occurrencesTextView)
        private val profileTextView: TextView = itemView.findViewById(R.id.profileName)
        private val deleteEventButton: Button = itemView.findViewById(R.id.deleteEventButton)
        private lateinit var event: Event
        private lateinit var profile: Profile

        init {
            view.setOnClickListener(this)
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> {
            return ItemDetails(absoluteAdapterPosition, eventAdapter.getEventAndProfile(absoluteAdapterPosition).event.id)
        }

        private fun setCallbacks(): Unit {
            enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                val profileAndEvent: ProfileAndEvent = eventAdapter.getEventAndProfile(absoluteAdapterPosition)
                val event: Event = profileAndEvent.event
                val profile: Profile = profileAndEvent.profile
                val eventOccurrences: Array<Int> = event.workingDays.split("").slice(1..event.workingDays.length).map { it.toInt() }.toTypedArray()
                val volumeMapPair: Pair<Map<Int, Int>, Map<String, Int>> = ProfileUtil.getVolumeSettingsMapPair(profile)
                val alarmUtil: AlarmUtil = AlarmUtil.getInstance()

                if (isChecked && enableSwitch.isPressed) {
                    event.isScheduled = 1
                    model.updateEvent(event)
                    alarmUtil.setAlarm(volumeMapPair, eventOccurrences, event.localDateTime,
                            event.id, false, profile.id, profile.title)
                }
                else if (!isChecked && enableSwitch.isPressed) {
                    event.isScheduled = 0
                    model.updateEvent(event)
                    alarmUtil.cancelAlarm(volumeMapPair, eventOccurrences, event.localDateTime,
                            event.id, profile.id, profile.title)
                }
            }
            deleteEventButton.setOnClickListener {
                removeEvent(absoluteAdapterPosition)
            }
        }

        private fun updateTextViews(): Unit {
            val time = event.localDateTime.toLocalTime()
            val pattern = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
            timeTextView.text = time.format(pattern)
            daysTextView.text = updateScheduledDaysTextView()
            profileTextView.text = "${profile.title}"
        }

        private fun updateScheduledDaysTextView(): String {
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

        fun bindEvent(profileAndEvent: ProfileAndEvent, position: Int, isSelected: Boolean) {
            this.event = profileAndEvent.event
            this.profile = profileAndEvent.profile
            AnimationUtils.selectedItemAnimation(itemView, isSelected)
            enableSwitch.isChecked = event.isScheduled == 1
            setCallbacks()
            updateTextViews()
        }

        override fun onClick(v: View?) {
            val eventId: Long = eventAdapter.getEventAndProfile(absoluteAdapterPosition).event.id
            val intent: Intent = Intent(requireContext(), EditEventActivity::class.java).apply {
                this.putExtra(EditEventActivity.EXTRA_ID, eventId)
            }
            startActivity(intent)
        }
    }

    private inner class EventAdapter : androidx.recyclerview.widget.ListAdapter<ProfileAndEvent, EventHolder>(object : DiffUtil.ItemCallback<ProfileAndEvent>() {

        override fun areItemsTheSame(oldItem: ProfileAndEvent, newItem: ProfileAndEvent): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ProfileAndEvent, newItem: ProfileAndEvent): Boolean {
            return oldItem == newItem
        }

    }), ListAdapterItemProvider<Long> {

        private var lastPosition: Int = -1

        fun getEventAndProfile(position: Int): ProfileAndEvent = getItem(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder =
                EventHolder(layoutInflater.inflate(EVENT_LAYOUT, parent, false))

        override fun onBindViewHolder(holder: EventHolder, position: Int) {
            if (holder.absoluteAdapterPosition > lastPosition) {
                lastPosition = position
            }
            tracker.let {
                holder.bindEvent(getItem(position), position, it.isSelected(getItem(position).event.id))
            }
        }

        override fun getItemKey(position: Int): Long {
            return this.currentList[position].event.id
        }

        override fun getPosition(key: Long): Int {
            return this.currentList.indexOfFirst { key == it.event.id }
        }
    }

    companion object {

        private const val EVENT_LAYOUT: Int = R.layout.item_view_event
    }
}