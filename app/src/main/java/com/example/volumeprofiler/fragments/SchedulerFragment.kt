package com.example.volumeprofiler.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.AlarmDetailsActivity
import com.example.volumeprofiler.activities.AlarmDetailsActivity.Companion.EXTRA_ALARM_PROFILE_RELATION
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.BaseSelectionObserver
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.DetailsLookup
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.ItemDetails
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.KeyProvider
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
import java.time.LocalTime
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SchedulerFragment: Fragment(), FabContainer, ActionModeProvider<Long>, FragmentSwipedListener {

    private var showDialog: Boolean = false

    private val viewModel: SchedulerViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private var activity: FabContainerCallbacks? = null
    private var _binding: AlarmsFragmentBinding? = null
    private val binding: AlarmsFragmentBinding get() = _binding!!

    private lateinit var tracker: SelectionTracker<Long>

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var profileManager: ProfileManager

    private val alarmAdapter: AlarmAdapter by lazy {
        AlarmAdapter()
    }

    private val localeChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
                alarmAdapter.refresh()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as FabContainerCallbacks
        requireActivity().registerReceiver(
            localeChangeReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_LOCALE_CHANGED)
            }
        )
    }

    override fun onDetach() {
        super.onDetach()
        activity = null
        requireActivity().unregisterReceiver(localeChangeReceiver)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = AlarmsFragmentBinding.inflate(inflater, container, false)

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
                                scheduleManager.cancelAlarm(it.relation.alarm, it.relation.profile)
                            }
                            is SchedulerViewModel.ViewEvent.OnAlarmCancelled -> {
                                scheduleManager.cancelAlarm(it.relation.alarm, it.relation.profile)
                                alarmAdapter.updateAlarmState(it.relation.alarm, 0)
                            }
                            is SchedulerViewModel.ViewEvent.OnAlarmSet -> {
                                scheduleManager.scheduleAlarm(it.relation.alarm, it.relation.profile)
                                alarmAdapter.updateAlarmState(it.relation.alarm, 1)

                                activity?.showSnackBar(
                                    ScheduleManager.formatRemainingTimeUntilAlarm(
                                        it.relation.alarm
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
                            val id: Int? = alarmAdapter.getItemPosition(it.alarm.id)
                            if (id != null) {
                                alarmAdapter.notifyItemChanged(id, Bundle().apply {
                                    putInt(EXTRA_DIFF_SCHEDULED_DAYS, it.alarm.scheduledDays)
                                    putSerializable(EXTRA_DIFF_LOCAL_TIME, it.alarm.instanceStartTime)
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startAlarmDetailsActivity(alarmRelation: AlarmRelation? = null, activityOptions: ActivityOptionsCompat? = null): Unit {
        val intent: Intent = Intent(requireContext(), AlarmDetailsActivity::class.java)
        if (alarmRelation != null) {
            intent.putExtra(EXTRA_ALARM_PROFILE_RELATION, alarmRelation)
        }
        startActivity(intent, activityOptions?.toBundle())
    }

    private fun setPlaceholderVisibility(empty: Boolean): Unit {
        val visibility: Int = if (empty) View.VISIBLE else View.GONE
        binding.hintScheduler.visibility = visibility
    }

    private fun updateAlarmAdapter(alarms: List<AlarmRelation>) {
        setPlaceholderVisibility(alarms.isEmpty())
        alarmAdapter.submitList(alarms.toMutableList())
    }

    private inner class AlarmViewHolder(private val binding: AlarmItemViewBinding) :
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener,
        ViewHolderItemDetailsProvider<Long> {

        init {
            binding.root.setOnClickListener(this)
            binding.timeTextView.transitionName = SHARED_TRANSITION_CLOCK
            binding.scheduleSwitch.transitionName = SHARED_TRANSITION_SWITCH
        }

        fun bindLocalTimeTextView(payload: Bundle): Unit {
            binding.timeTextView.text = TextUtil.formatLocalTime(requireContext(), (payload.getSerializable(EXTRA_DIFF_LOCAL_TIME)!! as LocalTime))
        }

        fun bindProfileTextView(payload: Bundle): Unit {
            binding.profileName.text = payload.getString(EXTRA_DIFF_PROFILE_TITLE)
        }

        fun bindScheduledDaysTextView(payload: Bundle): Unit {
            binding.occurrencesTextView.text = TextUtil.weekDaysToString(
                payload.getInt(EXTRA_DIFF_SCHEDULED_DAYS, 0),
                payload.getSerializable(EXTRA_DIFF_LOCAL_TIME) as LocalTime
            )
        }

        fun bindSwitch(payload: Bundle): Unit {
            binding.scheduleSwitch.isChecked = payload.getInt(EXTRA_DIFF_SCHEDULED) == 1
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> {
            return ItemDetails(bindingAdapterPosition, alarmAdapter.getItemAtPosition(bindingAdapterPosition).alarm.id)
        }

        private fun createTransitionAnimationOptions(): ActivityOptionsCompat {
            return ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                androidx.core.util.Pair.create(binding.timeTextView, SHARED_TRANSITION_CLOCK),
                androidx.core.util.Pair.create(binding.scheduleSwitch, SHARED_TRANSITION_SWITCH))
        }

        private fun setListeners(): Unit {
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
        }

        private fun updateTextViews(profile: Profile, alarm: Alarm): Unit {
            binding.timeTextView.text = TextUtil.formatLocalTime(requireContext(), alarm.localStartTime)
            binding.occurrencesTextView.text = TextUtil.weekDaysToString(alarm.scheduledDays, alarm.localStartTime)
            binding.profileName.text = profile.title
        }

        fun bind(alarmRelation: AlarmRelation) {
            val alarm: Alarm = alarmRelation.alarm
            val profile: Profile = alarmRelation.profile
            binding.scheduleSwitch.isChecked = alarm.isScheduled == 1
            setListeners()
            updateTextViews(profile, alarm)
        }

        override fun onClick(v: View?) {
            alarmAdapter.getItemAtPosition(bindingAdapterPosition).also {
                if (!tracker.isSelected(it.alarm.id)) {
                    if (it.alarm.isScheduled == 1) {
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

        override fun getChangePayload(oldItem: AlarmRelation, newItem: AlarmRelation): Any {
            super.getChangePayload(oldItem, newItem)

            val diffBundle: Bundle = Bundle()

            val oldAlarm: Alarm = oldItem.alarm
            val newAlarm: Alarm = newItem.alarm

            if (oldAlarm.localStartTime != newAlarm.localStartTime) {
                putTimePayload(diffBundle, newItem)
            }
            if (oldAlarm.profileUUID != newAlarm.profileUUID) {
                putProfileNamePayload(diffBundle, newItem)
            }
            if (oldAlarm.scheduledDays != newAlarm.scheduledDays) {
                putScheduledDaysPayload(diffBundle, newItem)
            }
            if (oldAlarm.isScheduled != newAlarm.isScheduled) {
                putScheduledPayload(diffBundle, newItem)
            }
            return diffBundle
        }

        private fun putTimePayload(diffBundle: Bundle, newItem: AlarmRelation): Unit {
            diffBundle.putSerializable(EXTRA_DIFF_LOCAL_TIME, newItem.alarm.localStartTime)
        }

        private fun putScheduledPayload(diffBundle: Bundle, newItem: AlarmRelation): Unit {
            diffBundle.putInt(EXTRA_DIFF_SCHEDULED, newItem.alarm.isScheduled)
            putScheduledDaysPayload(diffBundle, newItem)
        }

        private fun putProfileNamePayload(diffBundle: Bundle, newItem: AlarmRelation): Unit {
            diffBundle.putSerializable(EXTRA_DIFF_PROFILE_TITLE, newItem.profile.title)
        }

        private fun putScheduledDaysPayload(diffBundle: Bundle, newItem: AlarmRelation): Unit {
            diffBundle.putInt(EXTRA_DIFF_SCHEDULED_DAYS, newItem.alarm.scheduledDays)
            diffBundle.putSerializable(EXTRA_DIFF_LOCAL_TIME, newItem.alarm.localStartTime)
        }

    }), ListAdapterItemProvider<Long> {

        fun updateAlarmState(alarm: Alarm, scheduled: Int): Unit {
            getItemPosition(alarm.id)?.let {
                notifyItemChanged(it, Bundle().apply {
                    putInt(EXTRA_DIFF_SCHEDULED, scheduled)
                })
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
            val binding =
                AlarmItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AlarmViewHolder(binding)
        }

        fun refresh(): Unit {
            val currentList = currentList
            submitList(null)
            submitList(currentList)
        }

        fun getItemPosition(id: Long): Int? {
            for ((index, i) in currentList.withIndex()) {
                if (i.alarm.id == id) {
                    return index
                }
            }
            return null
        }

        override fun onBindViewHolder(
            holder: AlarmViewHolder,
            position: Int,
            payloads: MutableList<Any>
        ) {
            if (payloads.isNotEmpty()) {
                when (payloads[0]) {
                    is Bundle -> {
                        val payload: Bundle = payloads[0] as Bundle
                        for (key in payload.keySet()) {
                            when (key) {
                                EXTRA_DIFF_PROFILE_TITLE -> {
                                    holder.bindProfileTextView(payload)
                                }
                                EXTRA_DIFF_SCHEDULED_DAYS -> {
                                    holder.bindScheduledDaysTextView(payload)
                                }
                                EXTRA_DIFF_LOCAL_TIME -> {
                                    holder.bindLocalTimeTextView(payload)
                                }
                                EXTRA_DIFF_SCHEDULED -> {
                                    holder.bindSwitch(payload)
                                }
                            }
                        }
                    }
                    SelectionTracker.SELECTION_CHANGED_MARKER -> {
                        tracker.let {
                            AnimUtil.selected(holder.itemView, tracker.isSelected(getItem(position).alarm.id))
                        }
                    }
                }
            } else {
                super.onBindViewHolder(holder, position, payloads)
            }
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
        for (i in tracker.selection) {
            for (a in alarmAdapter.currentList) {
                if (a.alarm.id == i) {
                    viewModel.sendRemoveAlarmEvent(a)
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

    override fun onSwipe() {
        tracker.clearSelection()
    }

    companion object {

        internal const val SHARED_TRANSITION_CLOCK: String = "ClockViewSharedTransition"
        internal const val SHARED_TRANSITION_SWITCH: String = "SwitchSharedTransition"
        private const val EXTRA_DIFF_LOCAL_TIME: String = "extra_start_time"
        private const val EXTRA_DIFF_SCHEDULED_DAYS: String = "extra_scheduled_days"
        private const val EXTRA_DIFF_PROFILE_TITLE: String = "extra_profile_id"
        private const val EXTRA_DIFF_SCHEDULED: String = "extra_scheduled"
        private const val SELECTION_ID: String = "alarm"

    }
}