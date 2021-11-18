package com.example.volumeprofiler.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.ListAdapter
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.activities.EditAlarmActivity
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.BaseSelectionObserver
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.DetailsLookup
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.ItemDetails
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.KeyProvider
import com.example.volumeprofiler.databinding.AlarmItemViewBinding
import com.example.volumeprofiler.databinding.AlarmsFragmentBinding
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.fragments.dialogs.WarningDialog
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.PermissionRequestCallback
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.AlarmsListViewModel
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class AlarmsListFragment: Fragment(), ActionModeProvider<Long> {

    private var showDialog: Boolean = false

    private lateinit var tracker: SelectionTracker<Long>

    private val viewModel: AlarmsListViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private val alarmAdapter: AlarmAdapter = AlarmAdapter()

    private var _binding: AlarmsFragmentBinding? = null
    private val binding: AlarmsFragmentBinding get() = _binding!!

    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    private var callback: PermissionRequestCallback? = null

    @Inject
    lateinit var alarmUtil: AlarmUtil

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var profileUtil: ProfileUtil

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as PermissionRequestCallback
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val alarm: Alarm = it.data?.getParcelableExtra(EditAlarmActivity.EXTRA_ALARM)!!
                val shouldUpdate: Boolean = it.data?.getBooleanExtra(EditAlarmActivity.EXTRA_UPDATE_FLAG, false)!!
                if (shouldUpdate) {
                    viewModel.updateAlarm(alarm)
                } else {
                    viewModel.addAlarm(alarm)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
        activityResultLauncher.unregister()
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        _binding = AlarmsFragmentBinding.inflate(inflater, container, false)
        val view: View = binding.root
        binding.createAlarmButton.setOnClickListener {
            if (!showDialog) {
                startDetailsActivity()
            }
            else {
                showWarningDialog()
            }
        }
        initRecyclerView()
        initSelectionTracker(savedInstanceState)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.alarmsFlow.map {
                        AlarmUtil.sortAlarms(it)
                    }.collect {
                        Log.i("AlarmsListFragment", it.map { it.alarm.localDateTime.toLocalTime() }.toString())
                        updateUI(it)
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
                                    putSerializable(EXTRA_DIFF_SCHEDULED_DAYS, it.alarm.scheduledDays)
                                    putSerializable(EXTRA_DIFF_LOCAL_TIME, it.alarm.localDateTime)
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (!requireActivity().isChangingConfigurations) {
            tracker.clearSelection()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initRecyclerView(): Unit {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = alarmAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()
    }

    private fun showWarningDialog(): Unit {
        val fragment: WarningDialog = WarningDialog()
        val fragmentManager = requireActivity().supportFragmentManager
        fragment.show(fragmentManager, null)
    }

    @Suppress("unchecked_cast")
    private fun initSelectionTracker(savedInstanceState: Bundle?): Unit {
        tracker = SelectionTracker.Builder(
            SELECTION_ID,
            binding.recyclerView,
            KeyProvider(alarmAdapter),
            DetailsLookup(binding.recyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        tracker.addObserver(BaseSelectionObserver(WeakReference(this)))
    }

    private fun startDetailsActivity(alarmRelation: AlarmRelation? = null): Unit {
        val intent: Intent = Intent(requireContext(), EditAlarmActivity::class.java)
        if (alarmRelation != null) {
            intent.putExtra(EditAlarmActivity.EXTRA_ALARM_PROFILE_RELATION, alarmRelation)
        }
        activityResultLauncher.launch(intent)
    }

    private fun updateAlarmState(position:Int, scheduled: Int): Unit {
        alarmAdapter.notifyItemChanged(position, Bundle().apply {
            putInt(EXTRA_DIFF_SCHEDULED, scheduled)
        })
    }

    private fun updateUI(alarms: List<AlarmRelation>) {
        if (alarms.isEmpty()) {
            binding.hintScheduler.visibility = View.VISIBLE
        }
        else {
            binding.hintScheduler.visibility = View.GONE
        }
        alarmAdapter.submitList(alarms.toMutableList())
    }

    private fun scheduleAlarm(alarmRelation: AlarmRelation, position: Int): Unit {
        alarmUtil.scheduleAlarm(alarmRelation.alarm, alarmRelation.profile, false, true)
        viewModel.scheduleAlarm(alarmRelation)
        updateAlarmState(position, 1)
    }

    private fun removeAlarm(alarmRelation: AlarmRelation): Unit {
        if (alarmRelation.alarm.isScheduled == 1) {
            alarmUtil.cancelAlarm(alarmRelation.alarm, alarmRelation.profile)
        }
        viewModel.removeAlarm(alarmRelation)
    }

    private fun cancelAlarm(alarmRelation: AlarmRelation): Unit {
        alarmUtil.cancelAlarm(alarmRelation.alarm, alarmRelation.profile)
        viewModel.cancelAlarm(alarmRelation)
        updateAlarmState(alarmAdapter.getItemPosition(alarmRelation.alarm.id)!!, 0)
    }

    private inner class AlarmViewHolder(private val binding: AlarmItemViewBinding) :
        RecyclerView.ViewHolder(binding.root),
        View.OnClickListener,
        ViewHolderItemDetailsProvider<Long> {

        init {
            binding.root.setOnClickListener(this)
        }

        fun bindLocalTimeTextView(payload: Bundle): Unit {
            binding.timeTextView.text = TextUtil.localizedTimeToString((payload.getSerializable(EXTRA_DIFF_LOCAL_TIME)!! as LocalTime))
        }

        fun bindProfileTextView(payload: Bundle): Unit {
            binding.profileName.text = payload.getString(EXTRA_DIFF_PROFILE_TITLE)
        }

        fun bindScheduledDaysTextView(payload: Bundle): Unit {
            binding.occurrencesTextView.text = TextUtil.weekDaysToString(
                    payload.getIntegerArrayList(EXTRA_DIFF_SCHEDULED_DAYS)!!,
                    payload.getSerializable(EXTRA_DIFF_LOCAL_TIME)!! as LocalTime)
        }

        fun bindSwitch(payload: Bundle): Unit {
            binding.scheduleSwitch.isChecked = payload.getInt(EXTRA_DIFF_SCHEDULED) == 1
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> {
            return ItemDetails(bindingAdapterPosition, alarmAdapter.getItemAtPosition(bindingAdapterPosition).alarm.id)
        }

        private fun setListeners(): Unit {
            binding.editAlarmButton.setOnClickListener {
                startDetailsActivity(alarmAdapter.getItemAtPosition(bindingAdapterPosition))
            }
            binding.deleteAlarmButton.setOnClickListener {
                removeAlarm(alarmAdapter.getItemAtPosition(bindingAdapterPosition))
            }
        }

        private fun updateTextViews(profile: Profile, alarm: Alarm): Unit {
            binding.timeTextView.text = TextUtil.localizedTimeToString(alarm.localDateTime.toLocalTime())
            binding.occurrencesTextView.text = TextUtil.weekDaysToString(alarm.scheduledDays, alarm.localDateTime.toLocalTime())
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
            val alarmRelation: AlarmRelation = alarmAdapter.getItemAtPosition(bindingAdapterPosition)
            val scheduled: Boolean = alarmRelation.alarm.isScheduled == 1
            if (!scheduled) {
                when {
                    profileUtil.grantedRequiredPermissions(alarmRelation.profile) -> {
                        scheduleAlarm(alarmRelation, bindingAdapterPosition)
                    }
                    profileUtil.shouldRequestPhonePermission(alarmRelation.profile) -> {
                        callback?.requestProfilePermissions(alarmRelation.profile)
                    }
                    else -> {
                        sendSystemPreferencesAccessNotification(requireContext(), profileUtil)
                    }
                }
            } else {
                cancelAlarm(alarmRelation)
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

            if (oldAlarm.localDateTime.toLocalTime() != newAlarm.localDateTime.toLocalTime()) {
                putTimePayload(diffBundle, newItem)
            }
            if (oldAlarm.profileUUID != oldAlarm.profileUUID) {
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
            diffBundle.putSerializable(EXTRA_DIFF_LOCAL_TIME, newItem.alarm.localDateTime.toLocalTime())
        }

        private fun putScheduledPayload(diffBundle: Bundle, newItem: AlarmRelation): Unit {
            diffBundle.putInt(EXTRA_DIFF_SCHEDULED, newItem.alarm.isScheduled)
        }

        private fun putProfileNamePayload(diffBundle: Bundle, newItem: AlarmRelation): Unit {
            diffBundle.putSerializable(EXTRA_DIFF_PROFILE_TITLE, newItem.profile.title)
        }

        private fun putScheduledDaysPayload(diffBundle: Bundle, newItem: AlarmRelation): Unit {
            diffBundle.putIntegerArrayList(EXTRA_DIFF_SCHEDULED_DAYS, newItem.alarm.scheduledDays)
            diffBundle.putSerializable(EXTRA_DIFF_LOCAL_TIME, newItem.alarm.localDateTime.toLocalTime())
        }

    }), ListAdapterItemProvider<Long> {

        private lateinit var binding: AlarmItemViewBinding

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
            binding = AlarmItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AlarmViewHolder(binding)
        }

        fun getItemPosition(id: Long): Int? {
            for ((index, i) in currentList.withIndex()) {
                if (i.alarm.id == id) {
                    return index
                }
            }
            return null
        }

        override fun onBindViewHolder(holder: AlarmViewHolder, position: Int, payloads: MutableList<Any>) {
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
                            AnimUtil.selectedItemAnimation(holder.itemView, tracker.isSelected(getItem(position).alarm.id))
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

    companion object {

        private const val SELECTION_ID: String = "ALARM"
        private const val EXTRA_DIFF_LOCAL_TIME: String = "extra_start_time"
        private const val EXTRA_DIFF_SCHEDULED_DAYS: String = "extra_scheduled_days"
        private const val EXTRA_DIFF_PROFILE_TITLE: String = "extra_profile_id"
        private const val EXTRA_DIFF_SCHEDULED: String = "extra_scheduled"
        private const val PAYLOAD_ADD_DISMISS_OPTION: String = "payload_add_dismiss_option"
        private const val EXTRA_SELECTED: String = "extra_selected"
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