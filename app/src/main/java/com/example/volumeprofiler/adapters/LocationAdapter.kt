package com.example.volumeprofiler.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.core.FileManager
import com.example.volumeprofiler.databinding.LocationItemViewBinding
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.interfaces.ListViewContract
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.interfaces.ViewHolder
import com.example.volumeprofiler.interfaces.ViewHolderItemDetailsProvider
import com.example.volumeprofiler.selection.ItemDetails
import com.example.volumeprofiler.ui.Animations.selected
import com.example.volumeprofiler.ui.fragments.LocationsListFragment
import java.lang.ref.WeakReference

class LocationAdapter(
    var currentList: List<LocationRelation>,
    private val context: Context,
    listener: WeakReference<ListViewContract<LocationRelation>>
): RecyclerView.Adapter<LocationAdapter.LocationViewHolder>(), ListAdapterItemProvider<LocationRelation> {

    private val viewContract: ListViewContract<LocationRelation> = listener.get()!!

    inner class LocationViewHolder(override val binding: LocationItemViewBinding):
        RecyclerView.ViewHolder(binding.root),
        ViewHolderItemDetailsProvider<LocationRelation>,
        ViewHolder<LocationItemViewBinding>,
        View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<LocationRelation> {
            return ItemDetails(bindingAdapterPosition, currentList[bindingAdapterPosition])
        }

        fun updateEnabledState(enabled: Boolean) {
            binding.enableGeofenceSwitch.isChecked = enabled
        }

        fun bind(locationRelation: LocationRelation, isSelected: Boolean) {

            if (isSelected) selected(binding.root, isSelected)

            val location: Location = locationRelation.location
            binding.geofenceTitle.text = location.title
            binding.enableGeofenceSwitch.isChecked = location.enabled
            binding.geofenceProfiles.text = "${locationRelation.onEnterProfile.title} - ${locationRelation.onExitProfile}"

            binding.editGeofenceButton.setOnClickListener {
                viewContract.onEdit(locationRelation, null)
            }
            binding.removeGeofenceButton.setOnClickListener {
                viewContract.onRemove(locationRelation)
            }
            binding.mapSnapshot.post {
                BitmapFactory.decodeFile(
                    FileManager.resolvePath(context, location.previewImageId)
                )?.let { bitmap ->
                    binding.mapSnapshot.apply {
                        setImageBitmap(ThumbnailUtils.extractThumbnail(bitmap, width, height))
                    }
                }
            }
        }

        override fun onClick(v: View?) {
            currentList[bindingAdapterPosition].also {
                if (it.location.enabled) {
                    viewContract.onDisable(it)
                } else {
                    viewContract.onEnable(it)
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

    override fun getItemId(position: Int): Long {
        return currentList[position].location.id.toLong()
    }

    @Suppress("unchecked_cast")
    override fun onBindViewHolder(
        holder: LocationViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty()) {
            payloads.forEach {
                if (it is Bundle) {
                    holder.updateEnabledState(it.getBoolean(LocationsListFragment.PAYLOAD_GEOFENCE_CHANGED))
                }
            }
        } else super.onBindViewHolder(holder, position, payloads)
    }

    override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
        holder.bind(currentList[position], false)
    }

    override fun getItemKey(position: Int): LocationRelation {
        return currentList[position]
    }

    override fun getPosition(key: LocationRelation): Int {
        return currentList.indexOfFirst { key.location.id == it.location.id }
    }

    fun updateGeofenceState(relation: LocationRelation, enabled: Boolean) {
        notifyItemChanged(
            currentList.indexOfFirst { relation.location.id == it.location.id },
            LocationsListFragment.getEnabledStatePayload(enabled)
        )
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}