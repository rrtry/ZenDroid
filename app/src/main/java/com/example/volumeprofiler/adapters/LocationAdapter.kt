package com.example.volumeprofiler.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.databinding.LocationItemViewBinding
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.interfaces.ListItemInteractionListener
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.ui.fragments.LocationsListFragment
import com.example.volumeprofiler.util.resolvePath
import java.lang.ref.WeakReference

class LocationAdapter(
    private val context: Context,
    listener: WeakReference<ListItemInteractionListener<LocationRelation, LocationItemViewBinding>>
): ListAdapter<LocationRelation, LocationAdapter.LocationViewHolder>(object : DiffUtil.ItemCallback<LocationRelation>() {

    override fun areItemsTheSame(oldItem: LocationRelation, newItem: LocationRelation): Boolean {
        return oldItem.location.id == newItem.location.id
    }

    override fun areContentsTheSame(oldItem: LocationRelation, newItem: LocationRelation): Boolean {
        return oldItem == newItem
    }

}), ListAdapterItemProvider<String> {

    private val listener: ListItemInteractionListener<LocationRelation, LocationItemViewBinding> = listener.get()!!

    inner class LocationViewHolder(
        private val binding: LocationItemViewBinding
    ): RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        fun updateEnabledState(enabled: Boolean) {
            binding.enableGeofenceSwitch.isChecked = enabled
        }

        fun bind(locationRelation: LocationRelation, isSelected: Boolean): Unit {

            val location: Location = locationRelation.location

            binding.geofenceTitle.text = location.title
            binding.enableGeofenceSwitch.isChecked = location.enabled
            binding.geofenceProfiles.text = "${locationRelation.onEnterProfile.title} - ${locationRelation.onExitProfile}"

            binding.editGeofenceButton.setOnClickListener {
                listener.onEdit(locationRelation, binding)
            }
            binding.removeGeofenceButton.setOnClickListener {
                listener.onRemove(locationRelation)
            }
            binding.mapSnapshot.post {
                binding.mapSnapshot.setImageBitmap(
                    ThumbnailUtils.extractThumbnail(
                        BitmapFactory.decodeFile(
                            resolvePath(context, location.previewImageId
                    )), binding.mapSnapshot.width, binding.mapSnapshot.height)
                )
            }
        }

        override fun onClick(v: View?) {
            getItemAtPosition(bindingAdapterPosition).also {
                if (it.location.enabled) {
                    listener.onDisable(it)
                } else {
                    listener.onEnable(it)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
        return LocationViewHolder(
            LocationItemViewBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false)
        )
    }

    @Suppress("unchecked_cast")
    override fun onBindViewHolder(
        holder: LocationViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            payloads.forEach {
                when (it) {
                    is Bundle -> {
                        holder.updateEnabledState(it.getBoolean(LocationsListFragment.PAYLOAD_GEOFENCE_CHANGED))
                    }
                    SelectionTracker.SELECTION_CHANGED_MARKER -> {

                    }
                }
            }
        } else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(getItem(position), false)
    }

    override fun getItemKey(position: Int): String {
        return currentList[position].location.id.toString()
    }

    override fun getPosition(key: String): Int {
        return currentList.indexOfFirst { key == it.location.id.toString() }
    }

    fun updateGeofenceState(relation: LocationRelation, enabled: Boolean) {
        notifyItemChanged(
            currentList.indexOfFirst { relation.location.id == it.location.id },
            LocationsListFragment.getEnabledStatePayload(enabled)
        )
    }

    fun getItemAtPosition(position: Int): LocationRelation {
        return getItem(position)
    }
}