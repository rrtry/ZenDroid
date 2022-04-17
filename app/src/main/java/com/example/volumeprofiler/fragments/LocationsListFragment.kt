package com.example.volumeprofiler.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.activities.MapsActivity
import com.example.volumeprofiler.databinding.LocationItemViewBinding
import com.example.volumeprofiler.databinding.LocationsListFragmentBinding
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.LocationsListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Handler
import android.os.Looper
import androidx.core.content.res.ResourcesCompat
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.FabContainer
import com.example.volumeprofiler.interfaces.FragmentSwipedListener
import com.google.android.material.floatingactionbutton.FloatingActionButton

@AndroidEntryPoint
class LocationsListFragment: Fragment(), FabContainer, FragmentSwipedListener {

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var profileManager: ProfileManager

    private val locationAdapter: LocationAdapter by lazy {
        LocationAdapter()
    }

    private var _binding: LocationsListFragmentBinding? = null
    private val binding: LocationsListFragmentBinding get() = _binding!!

    private val viewModel: LocationsListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LocationsListFragmentBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = locationAdapter

        return binding.root
    }

    private fun startMapActivity(locationRelation: LocationRelation? = null): Unit {
        startActivity(MapsActivity.newIntent(requireContext(), locationRelation))
    }

    private fun updateAdapterData(list: List<LocationRelation>): Unit {
        if (list.isEmpty()) {
            binding.hintLocations.visibility = View.VISIBLE
        } else {
            binding.hintLocations.visibility = View.GONE
        }
        locationAdapter.submitList(list)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.locationsFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED).collect {
                updateAdapterData(it)
            }
        }
    }

    private fun removeGeofence(locationRelation: LocationRelation): Unit {
        geofenceManager.removeGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        deleteThumbnail(requireContext(), locationRelation.location.previewImageId)
        viewModel.removeLocation(locationRelation.location)
    }

    @Suppress("MissingPermission")
    private fun enableGeofence(locationRelation: LocationRelation, position: Int): Unit {
        geofenceManager.addGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        viewModel.enableGeofence(locationRelation.location)
        updateItem(position, 1)
    }

    private fun disableGeofence(locationRelation: LocationRelation, position: Int): Unit {
        geofenceManager.removeGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        viewModel.disableGeofence(locationRelation.location)
        updateItem(position, 0)
    }

    private fun updateItem(position: Int, enabled: Byte): Unit {
        locationAdapter.notifyItemChanged(position, Bundle().apply {
            putByte(PAYLOAD_GEOFENCE_ENABLED, enabled)
        })
    }

    private inner class LocationViewHolder(
        private val binding: LocationItemViewBinding
        ): RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(locationRelation: LocationRelation, isSelected: Boolean): Unit {

            val location: Location = locationRelation.location

            binding.geofenceTitle.text = location.title
            binding.enableGeofenceSwitch.isChecked = location.enabled == 1.toByte()
            binding.geofenceProfiles.text = "${locationRelation.onEnterProfile.title} - ${locationRelation.onExitProfile}"

            binding.editGeofenceButton.setOnClickListener {
                startMapActivity(locationAdapter.getItemAtPosition(bindingAdapterPosition))
            }
            binding.removeGeofenceButton.setOnClickListener {
                removeGeofence(locationAdapter.getItemAtPosition(bindingAdapterPosition))
            }
            binding.mapSnapshot.post {
                binding.mapSnapshot.setImageBitmap(
                    ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(resolvePath(
                        requireContext(), location.previewImageId
                    )), binding.mapSnapshot.width, binding.mapSnapshot.height)
                )
            }
        }

        override fun onClick(v: View?) {

        }
    }

    private inner class LocationAdapter : ListAdapter<LocationRelation, LocationViewHolder>(object : DiffUtil.ItemCallback<LocationRelation>() {

        override fun areItemsTheSame(oldItem: LocationRelation, newItem: LocationRelation): Boolean {
            return oldItem.location.id == newItem.location.id
        }

        override fun areContentsTheSame(oldItem: LocationRelation, newItem: LocationRelation): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: LocationRelation, newItem: LocationRelation): Any {
            super.getChangePayload(oldItem, newItem)

            val payloadBundle: Bundle = Bundle()

            if (oldItem.location.enabled != newItem.location.enabled) {
                payloadBundle.putByte(PAYLOAD_GEOFENCE_ENABLED, newItem.location.enabled)
            }
            return payloadBundle
        }

    }), ListAdapterItemProvider<String> {

        fun getItemAtPosition(position: Int): LocationRelation {
            return getItem(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
            return LocationViewHolder(
                LocationItemViewBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false)
            )
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
    }

    override fun onFabClick(fab: FloatingActionButton) {
        startMapActivity()
    }

    override fun onUpdateFab(fab: FloatingActionButton) {
        Handler(Looper.getMainLooper()).post {
            fab.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_baseline_location_on_24, context?.theme
                )
            )
        }
    }

    override fun onSwipe() {

    }

    companion object {
        private const val PAYLOAD_GEOFENCE_ENABLED: String = "payload_geofence_enabled"
    }
}