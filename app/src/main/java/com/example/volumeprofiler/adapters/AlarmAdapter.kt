package com.example.volumeprofiler.adapters

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.databinding.AlarmItemViewBinding
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.selection.ItemDetails
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_END_TIME
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SEPARATOR
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_START_TIME
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SWITCH
import com.example.volumeprofiler.util.TextUtil.Companion.formatLocalTime
import com.example.volumeprofiler.util.TextUtil.Companion.formatWeekDays
import java.lang.ref.WeakReference
import androidx.core.util.Pair
import androidx.recyclerview.selection.SelectionTracker.SELECTION_CHANGED_MARKER
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.PowerSaveModeHintBinding
import com.example.volumeprofiler.entities.Hint
import com.example.volumeprofiler.entities.ListItem
import com.example.volumeprofiler.interfaces.*
import com.example.volumeprofiler.ui.Animations.selected
import java.time.LocalTime

class AlarmAdapter(
    override var currentList: List<ListItem<Int>>,
    private val recyclerView: RecyclerView,
    listener: WeakReference<ListViewContract<AlarmRelation>>
):  RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    ListAdapterItemProvider<AlarmRelation>,
    AdapterDatasetProvider<ListItem<Int>>
{
    private val viewContract: ListViewContract<AlarmRelation> = listener.get()!!

    inner class AlarmViewHolder(override val binding: AlarmItemViewBinding):
        RecyclerView.ViewHolder(binding.root),
        ViewHolderItemDetailsProvider<AlarmRelation>,
        ViewHolder<AlarmItemViewBinding> {

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<AlarmRelation> {
            return ItemDetails(
                bindingAdapterPosition,
                getItem<AlarmRelation>(bindingAdapterPosition)
            )
        }

        @SuppressLint("SetTextI18n")
        fun bind(alarmRelation: AlarmRelation) {

            val alarm: Alarm = alarmRelation.alarm

            binding.scheduleSwitch.isChecked = alarm.isScheduled
            binding.startTime.text = formatLocalTime(recyclerView.context, alarm.startTime)
            binding.endTime.text = formatLocalTime(recyclerView.context, alarm.endTime)
            binding.occurrencesTextView.text = formatWeekDays(alarm.scheduledDays)
            binding.profileName.text = "${alarmRelation.startProfile} - ${alarmRelation.endProfile}"
            binding.eventTitle.text = alarm.title

            binding.scheduleSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    if (isChecked) {
                        viewContract.onEnable(alarmRelation)
                    } else {
                        viewContract.onDisable(alarmRelation)
                    }
                }
            }
            binding.deleteAlarmButton.setOnClickListener {
                viewContract.onRemove(alarmRelation)
            }
            binding.editAlarmButton.setOnClickListener {
                viewContract.onEditWithTransition(
                    alarmRelation,
                    binding.root,
                    Pair.create(binding.startTime, SHARED_TRANSITION_START_TIME),
                    Pair.create(binding.endTime, SHARED_TRANSITION_END_TIME),
                    Pair.create(binding.scheduleSwitch, SHARED_TRANSITION_SWITCH),
                    Pair.create(binding.clockViewSeparator, SHARED_TRANSITION_SEPARATOR)
                )
            }
            binding.root.post {
                viewContract.onSharedViewReady()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return currentList[position].viewType
    }

    override fun getItemId(position: Int): Long {
        return currentList[position].id.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            R.layout.alarm_item_view -> {
                return AlarmViewHolder(
                    AlarmItemViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false))
            }
            R.layout.power_save_mode_hint -> {
                return HintViewHolder(
                    PowerSaveModeHintBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    ), this)
            }
            else -> throw IllegalArgumentException("Unknown viewType $viewType")
        }
    }

    @Suppress("unchecked_cast")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) return super.onBindViewHolder(holder, position, payloads)
        payloads.forEach { i ->
            when (i) {
                is Bundle -> {
                    i.keySet().forEach { key ->
                        i.getSerializable(key)?.toString().let { timeFormatted ->
                            if (key == PAYLOAD_START_TIME_CHANGED) {
                                (holder as ViewHolder<AlarmItemViewBinding>).binding.startTime.text = timeFormatted
                            }
                            if (key == PAYLOAD_END_TIME_CHANGED) {
                                (holder as ViewHolder<AlarmItemViewBinding>).binding.endTime.text = timeFormatted
                            }
                        }
                    }
                }
                SELECTION_CHANGED_MARKER -> {
                    selected(holder.itemView, viewContract.isSelected(getItem(position)))
                }
            }
        }
    }

    override fun getItemKey(position: Int): AlarmRelation {
        return getItem(position)
    }

    override fun getPosition(key: AlarmRelation): Int {
        return currentList.indexOfFirst { item -> key.alarm.id == item.id }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.alarm_item_view -> (holder as AlarmViewHolder).bind(getItem(position))
            R.layout.power_save_mode_hint -> (holder as HintViewHolder).bind(getItem(position))
        }
    }

    private fun getItemPosition(id: Int): Int {
        return currentList.indexOfFirst { item ->
            item.id == id
        }
    }

    fun updateAlarmState(alarm: Alarm) {
        notifyItemChanged(
            getItemPosition(alarm.id),
            addScheduledStatePayload(Bundle(), alarm.isScheduled)
        )
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    companion object {

        private const val PAYLOAD_SCHEDULED_STATE_CHANGED: String = "scheduled_state"
        private const val PAYLOAD_START_TIME_CHANGED: String = "start_time"
        private const val PAYLOAD_END_TIME_CHANGED: String = "end_time"

        private fun addStartTimeChangedPayload(payloadsBundle: Bundle, startTime: LocalTime): Bundle {
            return payloadsBundle.apply {
                putSerializable(PAYLOAD_START_TIME_CHANGED, startTime)
            }
        }

        private fun addEndTimeChangedPayload(payloadsBundle: Bundle, endTime: LocalTime): Bundle {
            return payloadsBundle.apply {
                putSerializable(PAYLOAD_END_TIME_CHANGED, endTime)
            }
        }

        private fun addScheduledStatePayload(payloadsBundle: Bundle, scheduled: Boolean): Bundle {
            return payloadsBundle.apply {
                putBoolean(PAYLOAD_SCHEDULED_STATE_CHANGED, scheduled)
            }
        }
    }
}