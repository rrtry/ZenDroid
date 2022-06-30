package com.example.volumeprofiler.adapters

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.R
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
import com.example.volumeprofiler.util.MapsUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
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
        OnMapReadyCallback,
        View.OnClickListener {

        private lateinit var map: GoogleMap
        private lateinit var circle: Circle
        private lateinit var latLng: LatLng
        private var radius: Double = 100.0

        init {
            binding.root.setOnClickListener(this)
            with(binding.mapView) {
                onCreate(null)
                getMapAsync(this@LocationViewHolder)
            }
        }

        override fun onMapReady(googleMap: GoogleMap) {
            MapsInitializer.initialize(context)
            map = googleMap ?: return
            setMapLocation()
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<LocationRelation> {
            return ItemDetails(bindingAdapterPosition, currentList[bindingAdapterPosition])
        }

        fun updateEnabledState(enabled: Boolean) {
            binding.enableGeofenceSwitch.isChecked = enabled
        }

        fun bind(locationRelation: LocationRelation, isSelected: Boolean) {

            val location: Location = locationRelation.location
            latLng = LatLng(location.latitude, location.longitude)
            radius = location.radius.toDouble()

            binding.geofenceTitle.text = location.title
            binding.enableGeofenceSwitch.isChecked = location.enabled
            binding.geofenceProfiles.text = "${locationRelation.onEnterProfile.title} - ${locationRelation.onExitProfile}"
            binding.editGeofenceButton.setOnClickListener { viewContract.onEdit(locationRelation, null) }
            binding.removeGeofenceButton.setOnClickListener { viewContract.onRemove(locationRelation) }

            selected(binding.root, isSelected)
            setMapLocation()
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

        private fun setMapLocation() {
            if (!::map.isInitialized) return
            with(map) {
                mapType = GoogleMap.MAP_TYPE_NORMAL
                circle = addCircle(
                    CircleOptions()
                        .fillColor(Color.TRANSPARENT)
                        .strokeColor(Color.TRANSPARENT)
                        .center(latLng)
                        .radius(radius))
                moveCamera(CameraUpdateFactory.newLatLngBounds(
                    MapsUtil.getLatLngBoundsFromCircle(circle), 0
                ))
            }
        }

        fun clearMapView() {
            with(map) {
                mapType = GoogleMap.MAP_TYPE_NONE
                clear()
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
        if (payloads.isEmpty()) return super.onBindViewHolder(holder, position, payloads)
         payloads.forEach {
            when (it) {
                is Bundle -> {
                    holder.updateEnabledState(it.getBoolean(LocationsListFragment.PAYLOAD_GEOFENCE_CHANGED))
                }
                SelectionTracker.SELECTION_CHANGED_MARKER -> selected(holder.itemView, viewContract.isSelected(currentList[position]))
            }
        }
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