package com.example.volumeprofiler.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.volumeprofiler.ui.activities.MapsActivity
import com.example.volumeprofiler.databinding.LocationItemViewBinding
import com.example.volumeprofiler.databinding.LocationsListFragmentBinding
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.LocationsListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.os.Handler
import android.os.Looper
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.LocationAdapter
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.interfaces.FabContainer
import com.example.volumeprofiler.interfaces.FragmentSwipedListener
import com.example.volumeprofiler.interfaces.ListItemInteractionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.volumeprofiler.viewmodels.LocationsListViewModel.ViewEvent.*
import java.lang.ref.WeakReference

@AndroidEntryPoint
class LocationsListFragment: Fragment(),
    FabContainer,
    FragmentSwipedListener,
    ListItemInteractionListener<LocationRelation, LocationItemViewBinding> {

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var profileManager: ProfileManager

    private val viewModel: LocationsListViewModel by viewModels()

    private lateinit var locationAdapter: LocationAdapter

    private var _binding: LocationsListFragmentBinding? = null
    private val binding: LocationsListFragmentBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LocationsListFragmentBinding.inflate(inflater, container, false)

        locationAdapter = LocationAdapter(requireContext(), WeakReference(this))
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
                    viewModel.viewEvents.collect {
                        when (it) {
                            is OnGeofenceRemoved -> removeGeofence(it.relation)
                            is OnGeofenceDisabled -> disableGeofence(it.relation)
                            is OnGeofenceEnabled -> enableGeofence(it.relation)
                        }
                    }
                }
                launch {
                    viewModel.locationsFlow.collect {
                        updateAdapterData(it)
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

    override fun onEdit(entity: LocationRelation, binding: LocationItemViewBinding) {
        startMapActivity(entity)
    }

    override fun onEnable(entity: LocationRelation) {
        viewModel.enableGeofence(entity)
    }

    override fun onDisable(entity: LocationRelation) {
        viewModel.disableGeofence(entity)
    }

    override fun onRemove(entity: LocationRelation) {
        viewModel.removeGeofence(entity)
    }

    override fun isSelected(entity: LocationRelation): Boolean {
        return false
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

        const val PAYLOAD_GEOFENCE_CHANGED: String = "geofence_changed"

        fun getEnabledStatePayload(enabled: Boolean): Bundle {
            return Bundle().apply {
                putBoolean(PAYLOAD_GEOFENCE_CHANGED, enabled)
            }
        }
    }
}