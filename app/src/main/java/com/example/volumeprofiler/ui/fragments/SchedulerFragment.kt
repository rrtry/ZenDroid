package com.example.volumeprofiler.ui.fragments

import android.content.*
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.os.*
import android.provider.Settings.System.TIME_12_24
import android.provider.Settings.System.getUriFor
import android.view.*
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.res.ResourcesCompat
import com.example.volumeprofiler.viewmodels.MainActivityViewModel.ViewEvent.*
import androidx.core.view.isVisible
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.AlarmAdapter
import com.example.volumeprofiler.ui.activities.AlarmDetailsActivity
import com.example.volumeprofiler.ui.activities.AlarmDetailsActivity.Companion.EXTRA_ALARM_PROFILE_RELATION
import com.example.volumeprofiler.ui.activities.MainActivity
import com.example.volumeprofiler.selection.BaseSelectionObserver
import com.example.volumeprofiler.selection.DetailsLookup
import com.example.volumeprofiler.selection.KeyProvider
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.databinding.AlarmItemViewBinding
import com.example.volumeprofiler.databinding.AlarmsFragmentBinding
import com.example.volumeprofiler.entities.*
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.interfaces.*
import com.example.volumeprofiler.ui.activities.MainActivity.Companion.SCHEDULER_FRAGMENT
import com.example.volumeprofiler.util.TimeFormatChangeObserver
import com.example.volumeprofiler.util.ViewUtil.Companion.isViewPartiallyVisible
import com.example.volumeprofiler.viewmodels.SchedulerViewModel
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.SchedulerViewModel.ViewEvent.*
import kotlinx.coroutines.flow.*

@AndroidEntryPoint
class SchedulerFragment: Fragment(),
    FabContainer,
    ActionModeProvider<Long>,
    FragmentSwipedListener,
    MainActivity.OptionsItemSelectedListener,
    ListItemInteractionListener<AlarmRelation, AlarmItemViewBinding> {

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var profileManager: ProfileManager

    private val viewModel: SchedulerViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private var showDialog: Boolean = false

    private var callback: FabContainerCallbacks? = null
    private var bindingImpl: AlarmsFragmentBinding? = null
    private val binding: AlarmsFragmentBinding get() = bindingImpl!!

    private lateinit var tracker: SelectionTracker<Long>
    private lateinit var alarmAdapter: AlarmAdapter

    private var timeFormatChangeObserver: TimeFormatChangeObserver? = null

    private val localeReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_LOCALE_CHANGED) {
                alarmAdapter.refresh()
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as FabContainerCallbacks
        registerTimeFormatObserver()
        registerLocaleChangeReceiver()
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
        unregisterTimeFormatObserver()
        unregisterLocaleChangeReceiver()
    }

    private fun registerLocaleChangeReceiver() {
        requireActivity().registerReceiver(
            localeReceiver,
            IntentFilter().apply {
                addAction(ACTION_LOCALE_CHANGED)
            }
        )
    }

    private fun registerTimeFormatObserver() {
        timeFormatChangeObserver = TimeFormatChangeObserver(Handler(Looper.getMainLooper())) {
            alarmAdapter.refresh()
        }
        requireContext().contentResolver.registerContentObserver(
            getUriFor(TIME_12_24),
            true,
            timeFormatChangeObserver!!
        )
    }

    private fun unregisterTimeFormatObserver() {
        timeFormatChangeObserver?.let {
            requireContext().contentResolver.unregisterContentObserver(it)
        }
        timeFormatChangeObserver = null
    }

    private fun unregisterLocaleChangeReceiver() {
        requireActivity().unregisterReceiver(localeReceiver)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        bindingImpl = AlarmsFragmentBinding.inflate(inflater, container, false)

        alarmAdapter = AlarmAdapter(requireContext(), WeakReference(this))
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
                    sharedViewModel.viewEvents
                        .collect {
                            when (it) {
                                is OnMenuOptionSelected -> onSelected(it.itemId)
                                is AnimateFloatingActionButton -> updateFloatingActionButton(it.fragment)
                                is OnSwiped -> onFragmentSwiped(it.fragment)
                                is OnFloatingActionButtonClick -> onFloatingActionButtonClick(it.fragment)
                            }
                        }
                }
                launch {
                    viewModel.viewEvents.collect {
                        when (it) {
                            is OnAlarmSet -> onAlarmSet(it.relation, it.scheduledAlarms)
                            is OnAlarmRemoved -> onAlarmRemoved(it.relation, it.scheduledAlarms)
                            is OnAlarmCancelled -> onAlarmCancelled(it.relation, it.scheduledAlarms)
                        }
                    }
                }
                launch {
                    viewModel.alarmsFlow
                        .map {
                            ScheduleManager.sortInstances(it)
                        }
                        .collect {
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

    private fun startAlarmDetailsActivity(
        alarmRelation: AlarmRelation? = null,
        activityOptions: ActivityOptionsCompat? = null
    ) {
        Intent(requireContext(), AlarmDetailsActivity::class.java).apply {
            alarmRelation?.let {
                putExtra(EXTRA_ALARM_PROFILE_RELATION, it)
            }
            startActivity(this, activityOptions?.toBundle())
        }
    }

    private fun updateAlarmAdapter(alarms: List<AlarmRelation>) {
        binding.hintScheduler.isVisible = alarms.isEmpty()
        alarmAdapter.submitList(alarms)
    }

    private fun createTransitionAnimationOptions(binding: AlarmItemViewBinding): ActivityOptionsCompat {
        return ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            androidx.core.util.Pair.create(binding.startTime, SHARED_TRANSITION_START_TIME),
            androidx.core.util.Pair.create(binding.endTime, SHARED_TRANSITION_END_TIME),
            androidx.core.util.Pair.create(binding.scheduleSwitch, SHARED_TRANSITION_SWITCH),
            androidx.core.util.Pair.create(binding.clockViewSeparator, SHARED_TRANSITION_SEPARATOR))
    }

    override fun onEdit(entity: AlarmRelation, alarmBinding: AlarmItemViewBinding) {
        if (binding.recyclerView.isViewPartiallyVisible(alarmBinding.root)) {
            binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    binding.recyclerView.clearOnScrollListeners()

                    Handler(Looper.getMainLooper()).postDelayed({
                        startAlarmDetailsActivity(entity, createTransitionAnimationOptions(alarmBinding))
                    }, 100)
                }
            })
            binding.recyclerView.smoothScrollToPosition(
                alarmAdapter.getItemPosition(entity.alarm.id)
            )
        } else {
            startAlarmDetailsActivity(
                entity,
                createTransitionAnimationOptions(alarmBinding)
            )
        }
    }

    override fun onEnable(entity: AlarmRelation) {
        viewModel.scheduleAlarm(entity)
    }

    override fun onDisable(entity: AlarmRelation) {
        viewModel.cancelAlarm(entity)
    }

    override fun onRemove(entity: AlarmRelation) {
        viewModel.removeAlarm(entity)
    }

    override fun isSelected(entity: AlarmRelation): Boolean {
        return tracker.isSelected(entity.alarm.id)
    }

    override fun onSelected(itemId: Int) {
        // Empty implementation
    }

    private fun onAlarmSet(relation: AlarmRelation, alarms: List<AlarmRelation>) {
        scheduleManager.scheduleAlarm(
            relation.alarm,
            relation.startProfile,
            relation.endProfile
        )
        profileManager.updateScheduledProfile(alarms)
        callback?.showSnackBar(
            scheduleManager.getNextOccurrenceFormatted(
                relation
            ), Snackbar.LENGTH_LONG, null)
    }

    private fun onAlarmCancelled(alarmRelation: AlarmRelation, alarms: List<AlarmRelation>) {
        scheduleManager.cancelAlarm(alarmRelation.alarm)
        profileManager.updateScheduledProfile(alarms)
    }

    private fun onAlarmRemoved(alarmRelation: AlarmRelation, alarms: List<AlarmRelation>) {
        scheduleManager.cancelAlarm(alarmRelation.alarm)
        profileManager.updateScheduledProfile(alarms)
    }

    private fun onFragmentSwiped(fragment: Int) {
        if (fragment == SCHEDULER_FRAGMENT) {
            onSwipe()
        }
    }

    private fun updateFloatingActionButton(fragment: Int) {
        if (fragment == SCHEDULER_FRAGMENT) {
            onAnimateFab(callback!!.getFloatingActionButton())
        }
    }

    private fun onFloatingActionButtonClick(fragment: Int) {
        if (fragment == SCHEDULER_FRAGMENT) {
            onFabClick(callback!!.getFloatingActionButton())
        }
    }

    override fun onFabClick(fab: FloatingActionButton) {
        if (sharedViewModel.showDialog.value) {
            WarningDialog().show(
                requireActivity().supportFragmentManager,
                null
            )
            return
        }
        startActivity(Intent(context, AlarmDetailsActivity::class.java))
    }

    override fun onUpdateFab(fab: FloatingActionButton) {
        fab.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources, R.drawable.ic_baseline_access_time_24, context?.theme
            )
        )
    }

    override fun onActionItemRemove() {
        tracker.selection.forEach { selection ->
            viewModel.removeAlarm(
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
        internal const val SHARED_TRANSITION_SEPARATOR: String = "shared_transition_separator"
        internal const val SHARED_TRANSITION_START_TIME: String = "shared_transition_start_time"
        internal const val SHARED_TRANSITION_END_TIME: String = "shared_transition_end_time"
        internal const val SHARED_TRANSITION_SWITCH: String = "shared_transition_switch"
        private const val SELECTION_ID: String = "alarm"

    }
}