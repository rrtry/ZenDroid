package ru.rrtry.silentdroid.adapters

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.RecyclerView
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.entities.ListItem
import ru.rrtry.silentdroid.entities.Location
import ru.rrtry.silentdroid.entities.LocationRelation
import ru.rrtry.silentdroid.selection.ItemDetails
import ru.rrtry.silentdroid.ui.Animations.selected
import ru.rrtry.silentdroid.ui.fragments.LocationsListFragment
import ru.rrtry.silentdroid.util.MapsUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import ru.rrtry.silentdroid.databinding.LocationItemViewBinding
import ru.rrtry.silentdroid.databinding.PowerSaveModeHintBinding
import ru.rrtry.silentdroid.interfaces.*
import java.lang.ref.WeakReference

class LocationAdapter(
    override var currentList: List<ListItem<Int>>,
    private val context: Context,
    listener: WeakReference<ListViewContract<LocationRelation>>
):  RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    ListAdapterItemProvider<LocationRelation>,
    AdapterDatasetProvider<ListItem<Int>>
{
    private val viewContract: ListViewContract<LocationRelation> = listener.get()!!

    inner class LocationViewHolder(override val binding: LocationItemViewBinding):
        RecyclerView.ViewHolder(binding.root),
        ViewHolderItemDetailsProvider<LocationRelation>,
        ViewHolder<LocationItemViewBinding>,
        OnMapReadyCallback,
        View.OnClickListener {

        private lateinit var map: GoogleMap
        private lateinit var circle: Circle
        private lateinit var marker: Marker
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
            return ItemDetails(bindingAdapterPosition, getItem(bindingAdapterPosition))
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
            getItem<LocationRelation>(bindingAdapterPosition).also { geofence ->
                if (geofence.location.enabled) {
                    viewContract.onDisable(geofence)
                } else {
                    viewContract.onEnable(geofence)
                }
            }
        }

        private fun setMapLocation() {
            if (!::map.isInitialized) return
            with(map) {
                if (::circle.isInitialized) circle.remove()
                if (::marker.isInitialized) marker.remove()
                mapType = GoogleMap.MAP_TYPE_NORMAL
                marker = addMarker(MarkerOptions().position(latLng))!!
                circle = addCircle(
                    CircleOptions()
                        .fillColor(R.color.teal_700)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when (viewType) {
            R.layout.location_item_view -> {
                return LocationViewHolder(
                    LocationItemViewBinding.inflate(
                        LayoutInflater.from(context),
                        parent,
                        false)
                )
            }
            R.layout.power_save_mode_hint -> {
                return HintViewHolder(
                    PowerSaveModeHintBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false),
                    this)
            }
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return currentList[position].viewType
    }

    override fun getItemId(position: Int): Long {
        return currentList[position].id.toLong()
    }

    @Suppress("unchecked_cast")
    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) return super.onBindViewHolder(holder, position, payloads)
        payloads.forEach {
            when (it) {
                is Bundle -> {
                    (holder as LocationViewHolder).updateEnabledState(it.getBoolean(
                        LocationsListFragment.PAYLOAD_GEOFENCE_CHANGED))
                }
                SelectionTracker.SELECTION_CHANGED_MARKER -> {
                    selected(holder.itemView, viewContract.isSelected(getItem(position)))
                }
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            R.layout.location_item_view -> (holder as LocationViewHolder).bind(getItem(position), false)
            R.layout.power_save_mode_hint -> (holder as HintViewHolder).bind(getItem(position))
        }
    }

    override fun getItemKey(position: Int): LocationRelation {
        return getItem(position)
    }

    override fun getPosition(key: LocationRelation): Int {
        return currentList.indexOfFirst { key.location.id == it.id }
    }

    fun updateGeofenceState(relation: LocationRelation, enabled: Boolean) {
        notifyItemChanged(
            currentList.indexOfFirst { relation.location.id == it.id },
            LocationsListFragment.getEnabledStatePayload(enabled)
        )
    }

    override fun getItemCount(): Int {
        return currentList.size
    }
}