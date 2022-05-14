package com.example.volumeprofiler.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
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
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.selection.SelectionTracker
import com.example.volumeprofiler.R
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.interfaces.FabContainer
import com.example.volumeprofiler.interfaces.FragmentSwipedListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.volumeprofiler.viewmodels.LocationsListViewModel.ViewEvent.*

@AndroidEntryPoint
class LocationsListFragment: Fragment(), FabContainer, FragmentSwipedListener {

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var profileManager: ProfileManager

    private val viewModel: LocationsListViewModel by viewModels()

    private val locationAdapter: LocationAdapter by lazy {
        LocationAdapter() }

    private var _binding: LocationsListFragmentBinding? = null
    private val binding: LocationsListFragmentBinding get() = _binding!!

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

    private fun startMapActivity(locationRelation: LocationRelation? = null) {
        startActivity(MapsActivity.newIntent(requireContext(), locationRelation))
    }

    private fun updateAdapterData(list: List<LocationRelation>) {
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
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.locationsFlow.collect {
                        updateAdapterData(it)
                    }
                }
                launch {
                    viewModel.viewEvents.collect {
                        when (it) {
                            is OnGeofenceRemoved -> removeGeofence(it.relation)
                            is OnGeofenceDisabled -> disableGeofence(it.relation)
                            is OnGeofenceEnabled -> enableGeofence(it.relation)
                        }
                    }
                }
            }
        }
    }

    @Suppress("MissingPermission")
    private fun removeGeofence(locationRelation: LocationRelation) {
        geofenceManager.removeGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        deleteThumbnail(requireContext(), locationRelation.location.previewImageId)
    }

    @Suppress("MissingPermission")
    private fun enableGeofence(locationRelation: LocationRelation) {
        geofenceManager.addGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        locationAdapter.updateGeofenceState(locationRelation, true)
    }

    @Suppress("MissingPermission")
    private fun disableGeofence(locationRelation: LocationRelation) {
        geofenceManager.removeGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        locationAdapter.updateGeofenceState(locationRelation, false)
    }

    private inner class LocationViewHolder(
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
                startMapActivity(locationRelation)
            }
            binding.removeGeofenceButton.setOnClickListener {
                viewModel.removeGeofence(locationRelation)
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
            locationAdapter.getItemAtPosition(bindingAdapterPosition).also {
                if (it.location.enabled) {
                    viewModel.disableGeofence(it)
                } else {
                    viewModel.enableGeofence(it)
                }
            }
        }
    }

    private inner class LocationAdapter : ListAdapter<LocationRelation, LocationViewHolder>(object : DiffUtil.ItemCallback<LocationRelation>() {

        override fun areItemsTheSame(oldItem: LocationRelation, newItem: LocationRelation): Boolean {
            return oldItem.location.id == newItem.location.id
        }

        override fun areContentsTheSame(oldItem: LocationRelation, newItem: LocationRelation): Boolean {
            return oldItem == newItem
        }

    }), ListAdapterItemProvider<String> {

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
                            holder.updateEnabledState(it.getBoolean(PAYLOAD_GEOFENCE_CHANGED))
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
                getEnabledStatePayload(enabled)
            )
        }

        fun getItemAtPosition(position: Int): LocationRelation {
            return getItem(position)
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

        private const val PAYLOAD_GEOFENCE_CHANGED: String = "geofence_changed"

        fun getEnabledStatePayload(enabled: Boolean): Bundle {
            return Bundle().apply {
                putBoolean(PAYLOAD_GEOFENCE_CHANGED, enabled)
            }
        }
    }
}