package com.example.volumeprofiler.adapters

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.databinding.AlarmItemViewBinding
import com.example.volumeprofiler.entities.Alarm
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.interfaces.ListItemInteractionListener
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.selection.ItemDetails
import com.example.volumeprofiler.ui.Animations
import com.example.volumeprofiler.ui.fragments.SchedulerFragment
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_END_TIME
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SEPARATOR
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_START_TIME
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SWITCH
import com.example.volumeprofiler.util.TextUtil
import java.lang.ref.WeakReference

class AlarmAdapter(
    private val context: Context,
    listener: WeakReference<ListItemInteractionListener<AlarmRelation, AlarmItemViewBinding>>
    ): ListAdapter<AlarmRelation, AlarmAdapter.AlarmViewHolder>(object : DiffUtil.ItemCallback<AlarmRelation>() {

    override fun areItemsTheSame(oldItem: AlarmRelation, newItem: AlarmRelation): Boolean {
        return oldItem.alarm.id == newItem.alarm.id
    }

    override fun areContentsTheSame(oldItem: AlarmRelation, newItem: AlarmRelation): Boolean {
        return oldItem == newItem
    }

    override fun getChangePayload(oldItem: AlarmRelation, newItem: AlarmRelation): Any? {
        super.getChangePayload(oldItem, newItem)

        if (oldItem.alarm.isScheduled != newItem.alarm.isScheduled) {
            return SchedulerFragment.getScheduledStatePayload(newItem.alarm.isScheduled)
        }
        return null
    }

}), ListAdapterItemProvider<Long> {

    private val listener: ListItemInteractionListener<AlarmRelation, AlarmItemViewBinding> = listener.get()!!

    inner class AlarmViewHolder(
        private val binding: AlarmItemViewBinding
        ): RecyclerView.ViewHolder(binding.root), ViewHolderItemDetailsProvider<Long> {

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<Long> {
            return ItemDetails(
                bindingAdapterPosition,
                getItemAtPosition(bindingAdapterPosition).alarm.id
            )
        }

        fun bind(alarmRelation: AlarmRelation) {

            val alarm: Alarm = alarmRelation.alarm

            binding.scheduleSwitch.isChecked = alarm.isScheduled
            binding.startTime.text = TextUtil.formatLocalTime(context, alarm.startTime)
            binding.endTime.text = TextUtil.formatLocalTime(context, alarm.endTime)
            binding.occurrencesTextView.text = TextUtil.formatWeekDays(alarm.scheduledDays)
            binding.profileName.text = "${alarmRelation.startProfile} - ${alarmRelation.endProfile}"
            binding.eventTitle.text = alarm.title

            binding.scheduleSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    if (isChecked) {
                        listener.onEnable(alarmRelation)
                    } else {
                        listener.onDisable(alarmRelation)
                    }
                }
            }
            binding.deleteAlarmButton.setOnClickListener {
                listener.onRemove(alarmRelation)
            }
            binding.editAlarmButton.setOnClickListener {
                listener.onEdit(alarmRelation, binding)
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

    @Suppress("unchecked_cast")
    override fun onBindViewHolder(
        holder: AlarmViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            payloads.forEach { i ->
                when (i) {
                    is Bundle -> holder.bind(getItem(position))
                    SelectionTracker.SELECTION_CHANGED_MARKER -> Animations.selected(holder.itemView, listener.isSelected(getItem(position)))
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

    fun updateAlarmState(alarm: Alarm) {
        notifyItemChanged(
            getItemPosition(alarm.id),
            SchedulerFragment.getScheduledStatePayload(alarm.isScheduled)
        )
    }

    fun getItemAtPosition(position: Int): AlarmRelation {
        return getItem(position)
    }
}