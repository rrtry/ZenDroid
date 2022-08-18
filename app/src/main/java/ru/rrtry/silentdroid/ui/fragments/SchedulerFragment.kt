package ru.rrtry.silentdroid.ui.fragments

import android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
import android.content.*
import android.content.Intent.ACTION_LOCALE_CHANGED
import android.os.*
import android.provider.Settings.System.TIME_12_24
import android.provider.Settings.System.getUriFor
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.res.ResourcesCompat
import ru.rrtry.silentdroid.viewmodels.MainActivityViewModel.ViewEvent.*
import androidx.core.view.isVisible
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.*
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.adapters.AlarmAdapter
import ru.rrtry.silentdroid.core.PreferencesManager
import ru.rrtry.silentdroid.ui.activities.AlarmDetailsActivity
import ru.rrtry.silentdroid.ui.activities.AlarmDetailsActivity.Companion.EXTRA_ALARM_PROFILE_RELATION
import ru.rrtry.silentdroid.ui.activities.ViewPagerActivity
import ru.rrtry.silentdroid.core.ProfileManager
import ru.rrtry.silentdroid.core.ScheduleManager
import ru.rrtry.silentdroid.event.EventBus
import ru.rrtry.silentdroid.ui.activities.ViewPagerActivity.Companion.SCHEDULER_FRAGMENT
import ru.rrtry.silentdroid.util.TimeFormatChangeObserver
import ru.rrtry.silentdroid.viewmodels.SchedulerViewModel
import ru.rrtry.silentdroid.viewmodels.MainActivityViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject
import ru.rrtry.silentdroid.viewmodels.SchedulerViewModel.ViewEvent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import ru.rrtry.silentdroid.databinding.AlarmItemViewBinding
import ru.rrtry.silentdroid.databinding.AlarmsFragmentBinding
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.interfaces.FabContainer

@AndroidEntryPoint
class SchedulerFragment:
    ListFragment<AlarmRelation, AlarmsFragmentBinding, AlarmAdapter.AlarmViewHolder, AlarmItemViewBinding, AlarmAdapter>(),
    FabContainer,
    ViewPagerActivity.MenuItemSelectedListener
{

    override val listItem: Class<AlarmRelation> = AlarmRelation::class.java
    override val selectionId: String = SELECTION_ID
    override val hintRes: Int = R.string.scheduler_power_save_mode_hint

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var eventBus: EventBus

    private lateinit var alarmAdapter: AlarmAdapter
    private lateinit var exactAlarmPermissionLauncher: ActivityResultLauncher<Intent>

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

    private val exactAlarmPermissionStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
                viewModel.canScheduleExactAlarms = scheduleManager.canScheduleExactAlarms()
            }
        }
    }

    override fun onPermissionResult(permission: String, granted: Boolean) {
        preferencesManager.getProfile()?.let {
            profileManager.setProfile(it, true)
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

    override fun getAdapter(): AlarmAdapter {
        return alarmAdapter
    }

    override fun onStart() {
        super.onStart()
        viewModel.canScheduleExactAlarms = scheduleManager.canScheduleExactAlarms()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerTimeFormatObserver()
        registerLocaleChangeReceiver()
        registerAlarmPermissionLauncher()
        registerExactAlarmPermissionStateReceiver()
    }

    override fun onDetach() {
        super.onDetach()
        unregisterTimeFormatObserver()
        unregisterLocaleChangeReceiver()
        unregisterExactAlarmPermissionLauncher()
        unregisterExactAlarmPermissionStateReceiver()
    }

    private fun registerLocaleChangeReceiver() {
        requireActivity().registerReceiver(
            localeReceiver,
            IntentFilter().apply {
                addAction(ACTION_LOCALE_CHANGED)
            }
        )
    }

    private fun registerAlarmPermissionLauncher() {
        exactAlarmPermissionLauncher = registerForActivityResult(StartActivityForResult()) {
            if (!scheduleManager.canScheduleExactAlarms()) {
                callback?.showSnackBar(
                    "Alarm permission is required for scheduler to work properly",
                    "Open Settings",
                    Snackbar.LENGTH_INDEFINITE
                ) {
                    scheduleManager.requestExactAlarmPermission(exactAlarmPermissionLauncher)
                }
            }
        }
    }

    private fun registerExactAlarmPermissionStateReceiver() {
        requireActivity().registerReceiver(
            exactAlarmPermissionStateReceiver,
            IntentFilter(ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
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

    private fun unregisterExactAlarmPermissionLauncher() {
        exactAlarmPermissionLauncher.unregister()
    }

    private fun unregisterExactAlarmPermissionStateReceiver() {
        requireActivity().unregisterReceiver(exactAlarmPermissionStateReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        alarmAdapter = AlarmAdapter(
            listOf(),
            viewBinding.recyclerView,
            WeakReference(this))

        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.currentFragment.collect {
                        withContext(Dispatchers.Main) {
                            if (it == SCHEDULER_FRAGMENT) {
                                setSharedElementCallback()
                                showPowerSaveModeHint(requireContext().getString(hintRes))
                            }
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
                            else -> Log.i("SchedulerFragment", "Unknown viewEvent")
                        }
                    }
                }
                launch {
                    viewModel.viewEvents.collect { event ->
                        when (event) {
                            is OnAlarmSet -> onAlarmSet(event.relation, event.scheduledAlarms)
                            is OnAlarmRemoved -> onAlarmRemoved(event.relation, event.scheduledAlarms)
                            is OnAlarmCancelled -> onAlarmCancelled(event.relation, event.scheduledAlarms)
                            is OnRequestExactAlarmPermission -> scheduleManager.requestExactAlarmPermission(exactAlarmPermissionLauncher)
                        }
                    }
                }
                launch {
                    viewModel.alarmsFlow.collect { alarms ->
                        updateAlarmAdapter(alarms)
                        if (alarms.any { it.alarm.isScheduled } &&
                            !scheduleManager.canScheduleExactAlarms())
                        {
                            callback?.showSnackBar(
                                resources.getString(R.string.snackbar_alarm_permission_explanation),
                                resources.getString(R.string.open_settings),
                                Snackbar.LENGTH_INDEFINITE
                            ) {
                                scheduleManager.requestExactAlarmPermission(exactAlarmPermissionLauncher)
                            }
                        }
                    }
                }
                launch {
                    sharedViewModel.showDialog.collect {
                        showDialog = it
                    }
                }
                launch {
                    eventBus.eventBus.collectLatest { event ->
                        if (event is EventBus.Event.OnUpdateAlarmState) {
                            alarmAdapter.updateAlarmState(event.alarm)
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
        showPowerSaveModeHint(
            requireContext().getString(R.string.scheduler_power_save_mode_hint)
        )
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
            ), length = Snackbar.LENGTH_LONG, action = null
        )
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
            PopupDialog.create(
                resources.getString(R.string.cannot_create_time_trigger),
                resources.getString(R.string.cannot_create_time_trigger_description),
                R.drawable.ic_baseline_alarm_off_24
            ).show(
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
                (alarmAdapter.currentList.first { item ->
                    item == selection
                } as AlarmRelation)
            )
        }
    }

    override fun mapSharedElements(
        names: MutableList<String>?,
        sharedElements: MutableMap<String, View>?
    ): AlarmItemViewBinding? {
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