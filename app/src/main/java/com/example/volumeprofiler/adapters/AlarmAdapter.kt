package com.example.volumeprofiler.adapters

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
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
import com.example.volumeprofiler.ui.Animations
import com.example.volumeprofiler.ui.fragments.SchedulerFragment
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_END_TIME
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SEPARATOR
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_START_TIME
import com.example.volumeprofiler.ui.fragments.SchedulerFragment.Companion.SHARED_TRANSITION_SWITCH
import com.example.volumeprofiler.util.TextUtil
import com.example.volumeprofiler.util.ViewUtil.Companion.isViewPartiallyVisible
import java.lang.ref.WeakReference

class AlarmAdapter(
    private val activity: Activity,
    private val recyclerView: RecyclerView,
    listener: WeakReference<ListItemActionListener<AlarmRelation>>
) : ListAdapter<AlarmRelation, AlarmAdapter.AlarmViewHolder>(object : DiffUtil.ItemCallback<AlarmRelation>() {

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

}), ListAdapterItemProvider<AlarmRelation> {

    private val itemActionListener: ListItemActionListener<AlarmRelation> = listener.get()!!

    inner class AlarmViewHolder(private val binding: AlarmItemViewBinding):
        RecyclerView.ViewHolder(binding.root), ViewHolderItemDetailsProvider<AlarmRelation> {

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<AlarmRelation> {
            return ItemDetails(
                bindingAdapterPosition,
                getItemAtPosition(bindingAdapterPosition)
            )
        }

        fun bind(alarmRelation: AlarmRelation) {

            val alarm: Alarm = alarmRelation.alarm

            binding.scheduleSwitch.isChecked = alarm.isScheduled
            binding.startTime.text = TextUtil.formatLocalTime(recyclerView.context, alarm.startTime)
            binding.endTime.text = TextUtil.formatLocalTime(recyclerView.context, alarm.endTime)
            binding.occurrencesTextView.text = TextUtil.formatWeekDays(alarm.scheduledDays)
            binding.profileName.text = "${alarmRelation.startProfile} - ${alarmRelation.endProfile}"
            binding.eventTitle.text = alarm.title

            binding.scheduleSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    if (isChecked) {
                        onEnable(alarmRelation)
                    } else {
                        onDisable(alarmRelation)
                    }
                }
            }
            binding.deleteAlarmButton.setOnClickListener {
                onRemove(alarmRelation)
            }
            binding.editAlarmButton.setOnClickListener {
                onEdit(alarmRelation, binding)
            }
        }
    }

    private fun onDisable(alarmRelation: AlarmRelation) {
        itemActionListener.onDisable(alarmRelation)
    }

    private fun onEnable(alarmRelation: AlarmRelation) {
        itemActionListener.onEnable(alarmRelation)
    }

    private fun onRemove(alarmRelation: AlarmRelation) {
        itemActionListener.onRemove(alarmRelation)
    }

    private fun onEdit(alarmRelation: AlarmRelation, binding: AlarmItemViewBinding) {

        val onEdit = { delay: Long ->
            Handler(Looper.getMainLooper()).postDelayed({
                itemActionListener.onEdit(alarmRelation, createSceneTransitionAnimation(binding))
            }, delay)
        }

        if (recyclerView.isViewPartiallyVisible(binding.root)) {
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    recyclerView.clearOnScrollListeners()
                    onEdit(100)
                }
            })
            recyclerView.smoothScrollToPosition(
                getItemPosition(alarmRelation.alarm.id)
            )
        } else onEdit(100)
    }

    private fun createSceneTransitionAnimation(binding: AlarmItemViewBinding): Bundle? {
        return ActivityOptionsCompat.makeSceneTransitionAnimation(
            activity,
            androidx.core.util.Pair.create(binding.startTime, SHARED_TRANSITION_START_TIME),
            androidx.core.util.Pair.create(binding.endTime, SHARED_TRANSITION_END_TIME),
            androidx.core.util.Pair.create(binding.scheduleSwitch, SHARED_TRANSITION_SWITCH),
            androidx.core.util.Pair.create(binding.clockViewSeparator, SHARED_TRANSITION_SEPARATOR)).toBundle()
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
                    SelectionTracker.SELECTION_CHANGED_MARKER -> Animations.selected(holder.itemView, itemActionListener.isSelected(getItem(position)))
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
        holder.bind(getItem(position))
    }

    fun refresh() {
        currentList.also {
            submitList(null)
            submitList(it)
        }
    }

    private fun getItemPosition(id: Long): Int {
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