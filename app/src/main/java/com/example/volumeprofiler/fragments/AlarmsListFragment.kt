package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.ListAdapter
import android.view.*
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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
import com.example.volumeprofiler.activities.EditAlarmActivity
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.BaseSelectionObserver
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.DetailsLookup
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.ItemDetails
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.KeyProvider
import com.example.volumeprofiler.fragments.dialogs.WarningDialog
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.AlarmsListViewModel
import com.example.volumeprofiler.viewmodels.MainActivitySharedViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.ArrayList

class AlarmsListFragment: Fragment(), ActionModeProvider<Long> {

    private var showDialog: Boolean = false
    private lateinit var recyclerView: RecyclerView
    private lateinit var tracker: SelectionTracker<Long>
    private val viewModel: AlarmsListViewModel by viewModels()
    private val sharedViewModel: MainActivitySharedViewModel by activityViewModels()
    private val alarmAdapter: AlarmAdapter = AlarmAdapter()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.alarms_fragment, container, false)
        val floatingActionButton: FloatingActionButton = view.findViewById(R.id.fab)
        floatingActionButton.setOnClickListener {
            if (!showDialog) {
                val intent: Intent = Intent(requireContext(), EditAlarmActivity::class.java)
                startActivity(intent)
            }
            else {
                val fragment: WarningDialog = WarningDialog()
                val fragmentManager = requireActivity().supportFragmentManager
                fragment.show(fragmentManager, null)
            }
        }
        return view
    }

    private fun initRecyclerView(view: View): Unit {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = alarmAdapter
        recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun initSelectionTracker(): Unit {
        tracker = SelectionTracker.Builder(
                SELECTION_ID,
                recyclerView,
                KeyProvider(alarmAdapter),
                DetailsLookup(recyclerView),
                StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()
        tracker.addObserver(BaseSelectionObserver<Long>(WeakReference(this)))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView(view)
        initSelectionTracker()
        viewModel.eventListLiveData.observe(viewLifecycleOwner,
                Observer<List<AlarmTrigger>> { t ->
                    if (t != null) {
                        updateUI(t)
                    }
                })
        sharedViewModel.profileListLiveData.observe(viewLifecycleOwner,
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
        alarmAdapter.submitList(events)
    }

    private inner class EventHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, ViewHolderItemDetailsProvider<Long> {

        private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        private val enableSwitch: Switch = itemView.findViewById(R.id.scheduleSwitch)
        private val daysTextView: TextView = itemView.findViewById(R.id.occurrencesTextView)
        private val profileTextView: TextView = itemView.findViewById(R.id.profileName)
        private val removeAlarmButton: Button = itemView.findViewById(R.id.deleteGeofenceButton)
        private lateinit var alarm: Alarm
        private lateinit var profile: Profile

        init {
            view.setOnClickListener(this)
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> {
            return ItemDetails(absoluteAdapterPosition, alarmAdapter.getItemAtPosition(absoluteAdapterPosition).alarm.id)
        }

        private fun setCallbacks(): Unit {
            enableSwitch.setOnCheckedChangeListener { _, isChecked ->
                val alarmTrigger: AlarmTrigger = alarmAdapter.getItemAtPosition(absoluteAdapterPosition)
                if (isChecked && enableSwitch.isPressed) {
                    viewModel.scheduleAlarm(alarmTrigger)
                }
                else if (!isChecked && enableSwitch.isPressed) {
                    viewModel.unScheduleAlarm(alarmTrigger)
                }
            }
            removeAlarmButton.setOnClickListener {
                viewModel.removeAlarm(alarmAdapter.getItemAtPosition(absoluteAdapterPosition))
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

        fun bind(alarmTrigger: AlarmTrigger, isSelected: Boolean) {
            this.alarm = alarmTrigger.alarm
            this.profile = alarmTrigger.profile
            AnimUtil.selectedItemAnimation(itemView, isSelected)
            enableSwitch.isChecked = alarm.isScheduled == 1
            setCallbacks()
            updateTextViews()
        }

        override fun onClick(v: View?) {
            val alarmTrigger: AlarmTrigger = alarmAdapter.getItemAtPosition(absoluteAdapterPosition)
            val intent: Intent = Intent(requireContext(), EditAlarmActivity::class.java).apply {
                this.putExtra(EditAlarmActivity.EXTRA_TRIGGER, alarmTrigger)
            }
            startActivity(intent)
        }
    }

    private inner class AlarmAdapter : ListAdapter<AlarmTrigger, EventHolder>(object : DiffUtil.ItemCallback<AlarmTrigger>() {

        override fun areItemsTheSame(oldItem: AlarmTrigger, newItem: AlarmTrigger): Boolean {
            return oldItem.alarm.id == newItem.alarm.id
        }

        override fun areContentsTheSame(oldItem: AlarmTrigger, newItem: AlarmTrigger): Boolean {
            return oldItem == newItem
        }

    }), ListAdapterItemProvider<Long> {

        private var lastPosition: Int = -1

        fun getItemAtPosition(position: Int): AlarmTrigger = getItem(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
            return EventHolder(layoutInflater.inflate(EVENT_LAYOUT, parent, false))
        }

        override fun onBindViewHolder(holder: EventHolder, position: Int) {
            if (holder.absoluteAdapterPosition > lastPosition) {
                lastPosition = position
            }
            tracker.let {
                holder.bind(getItem(position), it.isSelected(getItem(position).alarm.id))
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
        private const val SELECTION_ID: String = "ALARM"
    }

    override fun onActionItemRemove() {
        for (i in tracker.selection) {
            for ((index, j) in alarmAdapter.currentList.withIndex()) {
                if (j.alarm.id == i) {
                    viewModel.removeAlarm(alarmAdapter.getItemAtPosition(index))
                }
            }
        }
    }

    override fun getTracker(): SelectionTracker<Long> {
        return tracker
    }

    override fun getFragmentActivity(): FragmentActivity {
        return requireActivity()
    }
}