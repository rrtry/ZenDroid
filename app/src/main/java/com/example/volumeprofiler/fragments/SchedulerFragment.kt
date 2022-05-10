package com.example.volumeprofiler.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.SelectionTracker.SELECTION_CHANGED_MARKER
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.AlarmDetailsActivity
import com.example.volumeprofiler.activities.AlarmDetailsActivity.Companion.EXTRA_ALARM_PROFILE_RELATION
import com.example.volumeprofiler.activities.MainActivity
import com.example.volumeprofiler.adapters.BaseSelectionObserver
import com.example.volumeprofiler.adapters.DetailsLookup
import com.example.volumeprofiler.adapters.ItemDetails
import com.example.volumeprofiler.adapters.KeyProvider
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.databinding.AlarmItemViewBinding
import com.example.volumeprofiler.databinding.AlarmsFragmentBinding
import com.example.volumeprofiler.entities.*
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.interfaces.*
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.SchedulerViewModel
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class SchedulerFragment: Fragment(),
    FabContainer,
    ActionModeProvider<Long>,
    FragmentSwipedListener,
    MainActivity.OptionsItemSelectedListener {

    private var showDialog: Boolean = false

    private val viewModel: SchedulerViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private var activity: FabContainerCallbacks? = null
    private var bindingImpl: AlarmsFragmentBinding? = null
    private val binding: AlarmsFragmentBinding get() = bindingImpl!!

    private lateinit var tracker: SelectionTracker<Long>

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var profileManager: ProfileManager

    private val alarmAdapter: AlarmAdapter by lazy {
        AlarmAdapter()
    }

    private val localeReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_LOCALE_CHANGED) {
                alarmAdapter.refresh()
            }
        }
    }

    override fun onSelected(itemId: Int) {
        when (itemId) {
            R.id.action_show_scheduled_for_today -> {

            }
            else -> {

            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as FabContainerCallbacks
        requireActivity().registerReceiver(
            localeReceiver,
            IntentFilter().apply {
                addAction(ACTION_LOCALE_CHANGED)
            }
        )
    }

    override fun onDetach() {
        super.onDetach()
        activity = null
        requireActivity().unregisterReceiver(localeReceiver)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        bindingImpl = AlarmsFragmentBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = alarmAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        tracker = SelectionTracker.Builder(
            SELECTION_ID,
            binding.recyclerView,
            KeyProvider(alarmAdapter),
            DetailsLookup(binding.recyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        tracker.addObserver(BaseSelectionObserver(WeakReference(this)))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.viewEvents.collect {
                        when (it) {
                            is SchedulerViewModel.ViewEvent.OnAlarmRemoved -> {
                                scheduleManager.cancelAlarm(it.relation.alarm)
                                profileManager.setScheduledProfile(it.scheduledAlarms)
                            }
                            is SchedulerViewModel.ViewEvent.OnAlarmCancelled -> {
                                scheduleManager.cancelAlarm(it.relation.alarm)
                                alarmAdapter.updateAlarmState(it.relation.alarm)
                                profileManager.setScheduledProfile(it.scheduledAlarms)
                            }
                            is SchedulerViewModel.ViewEvent.OnAlarmSet -> {

                                scheduleManager.scheduleAlarm(
                                    it.relation.alarm,
                                    it.relation.startProfile,
                                    it.relation.endProfile
                                )

                                profileManager.setScheduledProfile(it.scheduledAlarms)
                                alarmAdapter.updateAlarmState(it.relation.alarm)

                                activity?.showSnackBar(
                                    scheduleManager.getNextOccurrenceFormatted(
                                        it.relation
                                    ), Snackbar.LENGTH_LONG, null)
                            }
                        }
                    }
                }
                launch {
                    viewModel.alarmsFlow.map {
                        ScheduleManager.sortInstances(it)
                    }.collect {
                        updateAlarmAdapter(it)
                    }
                }
                launch {
                    sharedViewModel.showDialog.collect {
                        showDialog = it
                    }
                }
                launch {
                    eventBus.sharedFlow.collectLatest {
                        if (it is EventBus.Event.UpdateAlarmState) {
                            alarmAdapter.updateAlarmState(it.alarm)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingImpl = null
    }

    private fun startAlarmDetailsActivity(alarmRelation: AlarmRelation? = null, activityOptions: ActivityOptionsCompat? = null) {
        Intent(requireContext(), AlarmDetailsActivity::class.java).apply {
            alarmRelation?.let {
                putExtra(EXTRA_ALARM_PROFILE_RELATION, it)
            }
            startActivity(this, activityOptions?.toBundle())
        }
    }

    private fun setPlaceholderVisibility(empty: Boolean) {
        binding.hintScheduler.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun updateAlarmAdapter(alarms: List<AlarmRelation>) {
        setPlaceholderVisibility(alarms.isEmpty())
        alarmAdapter.submitList(alarms)
    }

    private inner class AlarmViewHolder(private val binding: AlarmItemViewBinding) :
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener,
        ViewHolderItemDetailsProvider<Long> {

        init {
            binding.root.setOnClickListener(this)
            ViewCompat.setTransitionName(binding.startTime, SHARED_TRANSITION_START_TIME)
            ViewCompat.setTransitionName(binding.endTime, SHARED_TRANSITION_END_TIME)
            ViewCompat.setTransitionName(binding.scheduleSwitch, SHARED_TRANSITION_SWITCH)
            ViewCompat.setTransitionName(binding.clockViewSeparator, SHARED_TRANSITION_SEPARATOR)
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> {
            return ItemDetails(bindingAdapterPosition,
                alarmAdapter.getItemAtPosition(bindingAdapterPosition).alarm.id
            )
        }

        private fun createTransitionAnimationOptions(): ActivityOptionsCompat {
            return ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                androidx.core.util.Pair.create(binding.startTime, SHARED_TRANSITION_START_TIME),
                androidx.core.util.Pair.create(binding.endTime, SHARED_TRANSITION_END_TIME),
                androidx.core.util.Pair.create(binding.scheduleSwitch, SHARED_TRANSITION_SWITCH),
                androidx.core.util.Pair.create(binding.clockViewSeparator, SHARED_TRANSITION_SEPARATOR))
        }

        fun updateScheduledState(scheduled: Boolean) {
            binding.scheduleSwitch.isChecked = scheduled
        }

        fun bind(alarmRelation: AlarmRelation) {

            val alarm: Alarm = alarmRelation.alarm
            val startProfile: Profile = alarmRelation.startProfile
            val endProfile: Profile = alarmRelation.endProfile

            binding.eventTitle.text = alarm.title
            binding.editAlarmButton.setOnClickListener {
                startAlarmDetailsActivity(
                    alarmAdapter.getItemAtPosition(bindingAdapterPosition),
                    createTransitionAnimationOptions())
            }
            binding.deleteAlarmButton.setOnClickListener {
                viewModel.sendRemoveAlarmEvent(
                    alarmAdapter.getItemAtPosition(bindingAdapterPosition)
                )
            }
            binding.startTime.text = TextUtil.formatLocalTime(requireContext(), alarm.startTime)
            binding.endTime.text = TextUtil.formatLocalTime(requireContext(), alarm.endTime)
            binding.occurrencesTextView.text = TextUtil.formatWeekDays(
                alarm.scheduledDays, alarm.startTime, alarm.endTime
            )
            binding.profileName.text = "${startProfile.title} - ${endProfile.title}"
            binding.scheduleSwitch.isChecked = alarm.isScheduled
        }

        override fun onClick(v: View?) {
            alarmAdapter.getItemAtPosition(bindingAdapterPosition).also {
                if (!tracker.isSelected(it.alarm.id)) {
                    if (it.alarm.isScheduled) {
                        viewModel.sendCancelAlarmEvent(it)
                    } else {
                        viewModel.sendScheduleAlarmEvent(it)
                    }
                }
            }
        }
    }

    private inner class AlarmAdapter : ListAdapter<AlarmRelation, AlarmViewHolder>(object : DiffUtil.ItemCallback<AlarmRelation>() {

        override fun areItemsTheSame(oldItem: AlarmRelation, newItem: AlarmRelation): Boolean {
            return oldItem.alarm.id == newItem.alarm.id
        }

        override fun areContentsTheSame(oldItem: AlarmRelation, newItem: AlarmRelation): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: AlarmRelation, newItem: AlarmRelation): Any? {
            super.getChangePayload(oldItem, newItem)
            if (oldItem.alarm.isScheduled != newItem.alarm.isScheduled) {
                return getScheduledStatePayload(newItem.alarm.isScheduled)
            }
            return null
        }

    }), ListAdapterItemProvider<Long> {

        fun updateAlarmState(alarm: Alarm) {
            notifyItemChanged(
                getItemPosition(alarm.id),
                getScheduledStatePayload(alarm.isScheduled)
            )
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
            return AlarmViewHolder(
                AlarmItemViewBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false)
            )
        }

        fun refresh() {
            currentList.also {
                submitList(null)
                submitList(it)
            }
        }

        fun getItemPosition(id: Long): Int {
            return currentList.indexOfFirst {
                it.alarm.id == id
            }
        }

        @Suppress("unchecked_cast")
        override fun onBindViewHolder(
            holder: AlarmViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isNotEmpty()) {
                (payloads as MutableList<Bundle>).forEach { bundle ->
                    bundle.keySet().forEach {
                        when (it) {
                            SELECTION_CHANGED_MARKER -> {
                                AnimUtil.selected(holder.itemView, tracker.isSelected(getItem(position).alarm.id))
                            }
                            SCHEDULED_STATE_CHANGED -> holder.updateScheduledState(bundle.getBoolean(it))
                        }
                    }
                }
            } else super.onBindViewHolder(holder, position, payloads)
        }

        override fun getItemKey(position: Int): Long {
            return currentList[position].alarm.id
        }

        override fun getPosition(key: Long): Int {
            return currentList.indexOfFirst { key == it.alarm.id }
        }

        override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        fun getItemAtPosition(position: Int): AlarmRelation {
            return getItem(position)
        }
    }

    override fun onFabClick(fab: FloatingActionButton) {
        startActivity(Intent(context, AlarmDetailsActivity::class.java))
    }

    override fun onUpdateFab(fab: FloatingActionButton) {
        Handler(Looper.getMainLooper()).post {
            fab.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_baseline_access_time_24, context?.theme
                )
            )
        }
    }

    override fun onActionItemRemove() {
        tracker.selection.forEach { selection ->
            viewModel.sendRemoveAlarmEvent(
                alarmAdapter.currentList.first {
                    it.alarm.id == selection
                }
            )
        }
    }

    override fun getTracker(): SelectionTracker<Long> {
        return tracker
    }

    override fun getFragmentActivity(): FragmentActivity {
        return requireActivity()
    }

    override fun onSwipe() {
        tracker.clearSelection()
    }

    companion object {

        fun getScheduledStatePayload(scheduled: Boolean): Bundle {
            return Bundle().apply {
                putBoolean(SCHEDULED_STATE_CHANGED, scheduled)
            }
        }

        private const val SCHEDULED_STATE_CHANGED: String = "scheduled"
        internal const val SHARED_TRANSITION_SEPARATOR: String = "separator"
        internal const val SHARED_TRANSITION_START_TIME: String = "start_time"
        internal const val SHARED_TRANSITION_END_TIME: String = "end_time"
        internal const val SHARED_TRANSITION_SWITCH: String = "SwitchSharedTransition"
        private const val SELECTION_ID: String = "alarm"

    }
}