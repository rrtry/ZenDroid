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
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.AnimUtils
import com.example.volumeprofiler.viewmodels.AlarmsListViewModel
import com.example.volumeprofiler.viewmodels.ViewpagerSharedViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.StringBuilder
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.ArrayList

class AlarmsListFragment: Fragment() {

    private var showDialog: Boolean = false
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var tracker: SelectionTracker<Long>
    private val model: AlarmsListViewModel by viewModels()
    private val viewpagerSharedModel: ViewpagerSharedViewModel by activityViewModels()
    private val eventAdapter: EventAdapter = EventAdapter()
    private var actionMode: ActionMode? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alarms_fragment, container, false)
        floatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(context)) {
                    if (!showDialog) {
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
                if (!showDialog) {
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
                                        if (j.alarm.id == i) {
                                            removeAlarm(index)
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
                Observer<List<AlarmTrigger>> { t ->
                    if (t != null) {
                        Log.i("ScheduledEventsFragment", "observing data: ${t.size}")
                        updateUI(t)
                    }
                })
        viewpagerSharedModel.profileListLiveData.observe(viewLifecycleOwner,
            Observer<List<Profile>> { t ->
                if (t != null) {
                    showDialog = t.isEmpty()
                }
            })
    }

    private fun updateUI(events: List<AlarmTrigger>) {
        if (events.isEmpty()) {
            view?.findViewById<TextView>(R.id.hint_scheduler)?.visibility = View.VISIBLE
        }
        else {
            view?.findViewById<TextView>(R.id.hint_scheduler)?.visibility = View.GONE
        }
        eventAdapter.submitList(events)
    }

    private fun removeAlarm(adapterPosition: Int): Unit {
        val alarmTrigger: AlarmTrigger = eventAdapter.getAlarm(adapterPosition)
        val alarm: Alarm = alarmTrigger.alarm
        model.removeAlarm(alarm)
        if (alarm.isScheduled == 1) {
            val alarmUtil: AlarmUtil = AlarmUtil.getInstance()
            alarmUtil.cancelAlarm(alarmTrigger.alarm, alarmTrigger.profile)
        }
    }

    private inner class EventHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, ViewHolderItemDetailsProvider<Long> {

        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        private val enableSwitch: Switch = itemView.findViewById(R.id.scheduleSwitch)
        private val daysTextView: TextView = itemView.findViewById(R.id.occurrencesTextView)
        private val profileTextView: TextView = itemView.findViewById(R.id.profileName)
        private val deleteEventButton: Button = itemView.findViewById(R.id.deleteEventButton)
        private lateinit var alarm: Alarm
        private lateinit var profile: Profile

        init {
            view.setOnClickListener(this)
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> {
            return ItemDetails(absoluteAdapterPosition, eventAdapter.getAlarm(absoluteAdapterPosition).alarm.id)
        }

        private fun setCallbacks(): Unit {
            enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                val alarmTrigger: AlarmTrigger = eventAdapter.getAlarm(absoluteAdapterPosition)
                val alarmUtil: AlarmUtil = AlarmUtil.getInstance()

                if (isChecked && enableSwitch.isPressed) {
                    alarm.isScheduled = 1
                    model.updateAlarm(alarm)
                    alarmUtil.setAlarm(alarmTrigger.alarm, alarmTrigger.profile,false)
                }
                else if (!isChecked && enableSwitch.isPressed) {
                    alarm.isScheduled = 0
                    model.updateAlarm(alarm)
                    alarmUtil.cancelAlarm(alarmTrigger.alarm, alarmTrigger.profile)
                }
            }
            deleteEventButton.setOnClickListener {
                removeAlarm(absoluteAdapterPosition)
            }
        }

        private fun updateTextViews(): Unit {
            val time = alarm.localDateTime.toLocalTime()
            val pattern = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
            timeTextView.text = time.format(pattern)
            daysTextView.text = updateScheduledDaysTextView()
            profileTextView.text = "${profile.title}"
        }

        private fun updateScheduledDaysTextView(): String {
            val stringBuilder: StringBuilder = StringBuilder()
            val workingsDays: ArrayList<Int> = alarm.workingsDays
            if (workingsDays.isNotEmpty()) {
                if (workingsDays.size == 1) {
                    return DayOfWeek.of(workingsDays[0]).getDisplayName(TextStyle.FULL, Locale.getDefault())
                }
                else if (workingsDays.size == 7) {
                    return "Every day"
                }
                for ((index, i) in workingsDays.withIndex()) {
                    if (index < workingsDays.size - 1) {
                        stringBuilder.append(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()) + ", ")
                    }
                    else {
                        stringBuilder.append(DayOfWeek.of(i).getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                    }
                }
                return stringBuilder.toString()
            }
            else {
                return if (alarm.localDateTime.toLocalTime() > LocalTime.now()) {
                    "Today"
                } else {
                    "Tomorrow"
                }
            }
        }

        fun bindEvent(alarmTrigger: AlarmTrigger, isSelected: Boolean) {
            this.alarm = alarmTrigger.alarm
            this.profile = alarmTrigger.profile
            AnimUtils.selectedItemAnimation(itemView, isSelected)
            enableSwitch.isChecked = alarm.isScheduled == 1
            setCallbacks()
            updateTextViews()
        }

        override fun onClick(v: View?) {
            val alarmTrigger: AlarmTrigger = eventAdapter.getAlarm(absoluteAdapterPosition)
            val intent: Intent = Intent(requireContext(), EditEventActivity::class.java).apply {
                this.putExtra(EditEventActivity.EXTRA_TRIGGER, alarmTrigger)
            }
            startActivity(intent)
        }
    }

    private inner class EventAdapter : androidx.recyclerview.widget.ListAdapter<AlarmTrigger, EventHolder>(object : DiffUtil.ItemCallback<AlarmTrigger>() {

        override fun areItemsTheSame(oldItem: AlarmTrigger, newItem: AlarmTrigger): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: AlarmTrigger, newItem: AlarmTrigger): Boolean {
            return oldItem == newItem
        }

    }), ListAdapterItemProvider<Long> {

        private var lastPosition: Int = -1

        fun getAlarm(position: Int): AlarmTrigger = getItem(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder =
                EventHolder(layoutInflater.inflate(EVENT_LAYOUT, parent, false))

        override fun onBindViewHolder(holder: EventHolder, position: Int) {
            if (holder.absoluteAdapterPosition > lastPosition) {
                lastPosition = position
            }
            tracker.let {
                holder.bindEvent(getItem(position), it.isSelected(getItem(position).alarm.id))
            }
        }

        override fun getItemKey(position: Int): Long {
            return this.currentList[position].alarm.id
        }

        override fun getPosition(key: Long): Int {
            return this.currentList.indexOfFirst { key == it.alarm.id }
        }
    }

    companion object {

        private const val EVENT_LAYOUT: Int = R.layout.alarm_item_view
    }
}