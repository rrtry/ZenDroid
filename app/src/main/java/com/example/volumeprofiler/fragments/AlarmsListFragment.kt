package com.example.volumeprofiler.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.ListAdapter
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.volumeprofiler.databinding.AlarmItemViewBinding
import com.example.volumeprofiler.databinding.AlarmsFragmentBinding
import com.example.volumeprofiler.fragments.dialogs.WarningDialog
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.models.Alarm
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.models.AlarmTrigger
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.AlarmsListViewModel
import com.example.volumeprofiler.viewmodels.MainActivitySharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.lang.StringBuilder
import java.lang.ref.WeakReference
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@AndroidEntryPoint
class AlarmsListFragment: Fragment(), ActionModeProvider<Long> {

    private var showDialog: Boolean = false

    private lateinit var tracker: SelectionTracker<Long>

    private val viewModel: AlarmsListViewModel by viewModels()
    private val sharedViewModel: MainActivitySharedViewModel by activityViewModels()

    private val alarmAdapter: AlarmAdapter = AlarmAdapter()

    private var _binding: AlarmsFragmentBinding? = null
    private val binding: AlarmsFragmentBinding get() = _binding!!

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    @Inject lateinit var alarmUtil: AlarmUtil

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // TODO handle activity result
        }
    }

    override fun onDetach() {
        super.onDetach()
        activityResultLauncher.unregister()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = AlarmsFragmentBinding.inflate(inflater, container, false)
        binding.fab.setOnClickListener {
            if (!showDialog) {
                startDetailsActivity()
            }
            else {
                showWarningDialog()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView(view)
        initSelectionTracker()
        viewModel.alarmListLiveData.observe(viewLifecycleOwner,
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

    override fun onPause() {
        super.onPause()
        tracker.clearSelection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initRecyclerView(view: View): Unit {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = alarmAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun showWarningDialog(): Unit {
        val fragment: WarningDialog = WarningDialog()
        val fragmentManager = requireActivity().supportFragmentManager
        fragment.show(fragmentManager, null)
    }

    private fun initSelectionTracker(): Unit {
        tracker = SelectionTracker.Builder(
            SELECTION_ID,
            binding.recyclerView,
            KeyProvider(alarmAdapter),
            DetailsLookup(binding.recyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        tracker.addObserver(BaseSelectionObserver<Long>(WeakReference(this)))
    }

    private fun startDetailsActivity(alarmTrigger: AlarmTrigger? = null): Unit {
        val intent: Intent = Intent(requireContext(), EditAlarmActivity::class.java)
        if (alarmTrigger != null) {
            intent.putExtra(EditAlarmActivity.EXTRA_TRIGGER, alarmTrigger)
        }
        activityResultLauncher.launch(intent)
    }

    private fun updateUI(events: List<AlarmTrigger>) {
        if (events.isEmpty()) {
            binding.hintScheduler.visibility = View.VISIBLE
        }
        else {
            binding.hintScheduler.visibility = View.GONE
        }
        alarmAdapter.submitList(events)
    }

    private fun cancelAlarm(alarmTrigger: AlarmTrigger): Unit {
        alarmUtil.cancelAlarm(alarmTrigger.alarm, alarmTrigger.profile)
        viewModel.cancelAlarm(alarmTrigger)
    }

    private fun scheduleAlarm(alarmTrigger: AlarmTrigger): Unit {
        alarmUtil.setAlarm(alarmTrigger.alarm, alarmTrigger.profile, false)
        viewModel.scheduleAlarm(alarmTrigger)
    }

    private fun removeAlarm(alarmTrigger: AlarmTrigger): Unit {
        if (alarmTrigger.alarm.isScheduled == 1) {
            alarmUtil.cancelAlarm(alarmTrigger.alarm, alarmTrigger.profile)
        }
        viewModel.removeAlarm(alarmTrigger)
    }

    private inner class AlarmViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener, ViewHolderItemDetailsProvider<Long> {

        private lateinit var alarm: Alarm
        private lateinit var profile: Profile

        init {
            view.setOnClickListener(this)
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> {
            return ItemDetails(absoluteAdapterPosition, alarmAdapter.getItemAtPosition(absoluteAdapterPosition).alarm.id)
        }

        private fun setCallbacks(): Unit {
            alarmAdapter.binding.scheduleSwitch.setOnCheckedChangeListener { _, isChecked ->
                val alarmTrigger: AlarmTrigger = alarmAdapter.getItemAtPosition(absoluteAdapterPosition)
                if (isChecked && alarmAdapter.binding.scheduleSwitch.isPressed) {
                    scheduleAlarm(alarmTrigger)
                }
                else if (!isChecked && alarmAdapter.binding.scheduleSwitch.isPressed) {
                    cancelAlarm(alarmTrigger)
                }
            }
            alarmAdapter.binding.deleteAlarmButton.setOnClickListener {
                removeAlarm(alarmAdapter.getItemAtPosition(absoluteAdapterPosition))
            }
        }

        private fun updateTextViews(): Unit {
            val time = alarm.localDateTime.toLocalTime()
            val pattern = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
            alarmAdapter.binding.timeTextView.text = time.format(pattern)
            alarmAdapter.binding.occurrencesTextView.text = updateScheduledDaysTextView()
            alarmAdapter.binding.profileName.text = "${profile.title}"
        }

        private fun updateScheduledDaysTextView(): String {
            val stringBuilder: StringBuilder = StringBuilder()
            val workingsDays: ArrayList<Int> = alarm.scheduledDays
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
            alarmAdapter.binding.scheduleSwitch.isChecked = alarm.isScheduled == 1
            setCallbacks()
            updateTextViews()
        }

        override fun onClick(v: View?) {
            startDetailsActivity(alarmAdapter.getItemAtPosition(absoluteAdapterPosition))
        }
    }

    private inner class AlarmAdapter : ListAdapter<AlarmTrigger, AlarmViewHolder>(object : DiffUtil.ItemCallback<AlarmTrigger>() {

        override fun areItemsTheSame(oldItem: AlarmTrigger, newItem: AlarmTrigger): Boolean {
            return oldItem.alarm.id == newItem.alarm.id
        }

        override fun areContentsTheSame(oldItem: AlarmTrigger, newItem: AlarmTrigger): Boolean {
            return oldItem == newItem
        }

    }), ListAdapterItemProvider<Long> {

        private var lastPosition: Int = -1
        lateinit var binding: AlarmItemViewBinding

        fun getItemAtPosition(position: Int): AlarmTrigger = getItem(position)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
            binding = AlarmItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AlarmViewHolder(binding.root)
        }

        override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
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
                    removeAlarm(j)
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