package com.example.volumeprofiler.adapters

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.databinding.AlarmItemViewBinding
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.interfaces.ListItemActionListener
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
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
import com.example.volumeprofiler.interfaces.ViewHolder
import com.example.volumeprofiler.ui.Animations.selected
import java.time.LocalTime

class AlarmAdapter(
    var currentList: List<AlarmRelation>,
    private val recyclerView: RecyclerView,
    listener: WeakReference<ListItemActionListener<AlarmRelation>>
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>(), ListAdapterItemProvider<AlarmRelation> {

    private val itemActionListener: ListItemActionListener<AlarmRelation> = listener.get()!!

    inner class AlarmViewHolder(override val binding: AlarmItemViewBinding):
        RecyclerView.ViewHolder(binding.root),
        ViewHolderItemDetailsProvider<AlarmRelation>,
        ViewHolder<AlarmItemViewBinding> {

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<AlarmRelation> {
            return ItemDetails(
                bindingAdapterPosition,
                currentList[bindingAdapterPosition]
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
                        itemActionListener.onEnable(alarmRelation)
                    } else {
                        itemActionListener.onDisable(alarmRelation)
                    }
                }
            }
            binding.deleteAlarmButton.setOnClickListener {
                itemActionListener.onRemove(alarmRelation)
            }
            binding.editAlarmButton.setOnClickListener {
                itemActionListener.onEditWithTransition(
                    alarmRelation,
                    binding.root,
                    Pair.create(binding.startTime, SHARED_TRANSITION_START_TIME),
                    Pair.create(binding.endTime, SHARED_TRANSITION_END_TIME),
                    Pair.create(binding.scheduleSwitch, SHARED_TRANSITION_SWITCH),
                    Pair.create(binding.clockViewSeparator, SHARED_TRANSITION_SEPARATOR)
                )
            }
            binding.root.post {
                itemActionListener.onSharedViewReady()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        return AlarmViewHolder(
            AlarmItemViewBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
        )
    }

    override fun getItemId(position: Int): Long {
        return currentList[position].alarm.id
    }

    @Suppress("unchecked_cast")
    override fun onBindViewHolder(
        holder: AlarmViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            payloads.forEach { i ->
                when (i) {
                    is Bundle -> {
                        i.keySet().forEach { key ->
                            i.getSerializable(key)?.toString().let { timeFormatted ->
                                if (key == PAYLOAD_START_TIME_CHANGED) {
                                    holder.binding.startTime.text = timeFormatted
                                }
                                if (key == PAYLOAD_END_TIME_CHANGED) {
                                    holder.binding.endTime.text = timeFormatted
                                }
                            }
                        }
                    }
                    SELECTION_CHANGED_MARKER -> {
                        selected(holder.itemView, itemActionListener.isSelected(currentList[position]))
                    }
                }
            }
        } else super.onBindViewHolder(holder, position, payloads)
    }

    override fun getItemKey(position: Int): AlarmRelation {
        return currentList[position]
    }

    override fun getPosition(key: AlarmRelation): Int {
        return currentList.indexOfFirst { key.alarm.id == it.alarm.id }
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    private fun getItemPosition(id: Long): Int {
        return currentList.indexOfFirst {
            it.alarm.id == id
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

        private const val PAYLOAD_SCHEDULED_STATE_CHANGED: String = "scheduled_state"
        private const val PAYLOAD_START_TIME_CHANGED: String = "start_time"
        private const val PAYLOAD_END_TIME_CHANGED: String = "end_time"

    }
}