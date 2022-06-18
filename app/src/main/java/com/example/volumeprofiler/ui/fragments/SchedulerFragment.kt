package com.example.volumeprofiler.ui.fragments

import android.content.*
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.os.*
import android.provider.Settings.System.TIME_12_24
import android.provider.Settings.System.getUriFor
import android.util.Log
import android.view.*
import androidx.core.content.res.ResourcesCompat
import com.example.volumeprofiler.viewmodels.MainActivityViewModel.ViewEvent.*
import androidx.core.view.isVisible
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.AlarmAdapter
import com.example.volumeprofiler.ui.activities.AlarmDetailsActivity
import com.example.volumeprofiler.ui.activities.AlarmDetailsActivity.Companion.EXTRA_ALARM_PROFILE_RELATION
import com.example.volumeprofiler.ui.activities.MainActivity
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.databinding.AlarmItemViewBinding
import com.example.volumeprofiler.databinding.AlarmsFragmentBinding
import com.example.volumeprofiler.entities.*
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.interfaces.*
import com.example.volumeprofiler.ui.activities.MainActivity.Companion.SCHEDULER_FRAGMENT
import com.example.volumeprofiler.util.TimeFormatChangeObserver
import com.example.volumeprofiler.viewmodels.SchedulerViewModel
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.SchedulerViewModel.ViewEvent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class SchedulerFragment: ListFragment<AlarmRelation, AlarmsFragmentBinding, AlarmAdapter.AlarmViewHolder, AlarmItemViewBinding>(),
    FabContainer,
    MainActivity.MenuItemSelectedListener {

    override val listItem: Class<AlarmRelation> = AlarmRelation::class.java
    override val selectionId: String = SELECTION_ID

    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var profileManager: ProfileManager
    private lateinit var alarmAdapter: AlarmAdapter

    private val viewModel: SchedulerViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private var showDialog: Boolean = false

    private var timeFormatChangeObserver: TimeFormatChangeObserver? = null
    private val localeReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_LOCALE_CHANGED) {
                alarmAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun getBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): AlarmsFragmentBinding {
        return AlarmsFragmentBinding.inflate(inflater, container, false)
    }

    override fun getRecyclerView(): RecyclerView {
        return viewBinding.recyclerView
    }

    override fun getAdapter(): RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
        return alarmAdapter
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerTimeFormatObserver()
        registerLocaleChangeReceiver()
    }

    override fun onDetach() {
        super.onDetach()
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
            alarmAdapter.notifyDataSetChanged()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        alarmAdapter = AlarmAdapter(listOf(),
            viewBinding.recyclerView,
            WeakReference(this))
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.currentFragment.collect {
                        withContext(Dispatchers.Main) {
                            if (it == SCHEDULER_FRAGMENT) setSharedElementCallback()
                        }
                    }
                }
                launch {
                    sharedViewModel.viewEvents.collect {
                        when (it) {
                            is OnMenuOptionSelected -> onMenuOptionSelected(it.itemId)
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
                    viewModel.alarmsFlow.collect {
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

    private fun startAlarmDetailsActivity(
        alarmRelation: AlarmRelation? = null,
        activityOptions: Bundle? = null
    ) {
        Intent(requireContext(), AlarmDetailsActivity::class.java).apply {
            alarmRelation?.let {
                putExtra(EXTRA_ALARM_PROFILE_RELATION, it)
            }
            startActivity(this, activityOptions)
        }
    }

    private fun updateAlarmAdapter(alarms: List<AlarmRelation>) {
        viewBinding.hintScheduler.isVisible = alarms.isEmpty()
        alarmAdapter.currentList = alarms
        alarmAdapter.notifyDataSetChanged()
    }

    override fun onEdit(entity: AlarmRelation, options: Bundle?) {
        startAlarmDetailsActivity(entity, options)
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
        return selectionTracker.isSelected(entity)
    }

    override fun onMenuOptionSelected(itemId: Int) {

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
            onFragmentSwiped()
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
        selectionTracker.selection.forEach { selection ->
            viewModel.removeAlarm(
                alarmAdapter.currentList.first {
                    it.alarm.id == selection.alarm.id
                }
            )
        }
    }

    override fun mapSharedElements(
        names: MutableList<String>?,
        sharedElements: MutableMap<String, View>?
    ): AlarmItemViewBinding? {
        Log.i("Transition", "SchedulerFragment")
        val binding: AlarmItemViewBinding? = super.mapSharedElements(names, sharedElements)
        binding?.let {
            sharedElements?.put(SHARED_TRANSITION_START_TIME, binding.startTime)
            sharedElements?.put(SHARED_TRANSITION_END_TIME, binding.endTime)
            sharedElements?.put(SHARED_TRANSITION_SWITCH, binding.scheduleSwitch)
            sharedElements?.put(SHARED_TRANSITION_SEPARATOR, binding.clockViewSeparator)
        }
        return binding
    }

    companion object {

        internal const val SHARED_TRANSITION_SEPARATOR: String = "shared_transition_separator"
        internal const val SHARED_TRANSITION_START_TIME: String = "shared_transition_start_time"
        internal const val SHARED_TRANSITION_END_TIME: String = "shared_transition_end_time"
        internal const val SHARED_TRANSITION_SWITCH: String = "shared_transition_switch"
        private const val SELECTION_ID: String = "alarm"

    }
}