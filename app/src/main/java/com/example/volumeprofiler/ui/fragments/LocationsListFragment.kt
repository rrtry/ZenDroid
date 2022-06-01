package com.example.volumeprofiler.ui.fragments

import android.content.Context
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
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.LocationAdapter
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.interfaces.FabContainer
import com.example.volumeprofiler.interfaces.FabContainerCallbacks
import com.example.volumeprofiler.interfaces.FragmentSwipedListener
import com.example.volumeprofiler.interfaces.ListItemInteractionListener
import com.example.volumeprofiler.ui.activities.MainActivity.Companion.LOCATIONS_FRAGMENT
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.volumeprofiler.viewmodels.LocationsListViewModel.ViewEvent.*
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import java.lang.ref.WeakReference
import com.example.volumeprofiler.viewmodels.MainActivityViewModel.ViewEvent.*

@AndroidEntryPoint
class LocationsListFragment: Fragment(),
    FabContainer,
    FragmentSwipedListener,
    ListItemInteractionListener<LocationRelation, LocationItemViewBinding> {

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var profileManager: ProfileManager

    private val viewModel: LocationsListViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private lateinit var locationAdapter: LocationAdapter
    private var callback: FabContainerCallbacks? = null

    private var _binding: LocationsListFragmentBinding? = null
    private val binding: LocationsListFragmentBinding get() = _binding!!

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as FabContainerCallbacks
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

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
                    sharedViewModel.viewEvents
                        .collect {
                            when (it) {
                                is AnimateFloatingActionButton -> updateFloatingActionButton(it.fragment)
                                is OnSwiped -> onFragmentSwiped(it.fragment)
                                is OnFloatingActionButtonClick -> onFloatingActionButtonClick(it.fragment)
                                else -> Log.i("ProfilesListFragment", "Unknown viewEvent: $it")
                            }
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

    private fun updateFloatingActionButton(fragment: Int) {
        if (fragment == LOCATIONS_FRAGMENT) {
            onAnimateFab(callback!!.getFloatingActionButton())
        }
    }

    private fun onFragmentSwiped(fragment: Int) {
        if (fragment == LOCATIONS_FRAGMENT) {
            onSwipe()
        }
    }

    private fun onFloatingActionButtonClick(fragment: Int) {
        if (fragment == LOCATIONS_FRAGMENT) {
            onFabClick(callback!!.getFloatingActionButton())
        }
    }

    override fun onFabClick(fab: FloatingActionButton) {
        startMapActivity()
    }

    override fun onUpdateFab(fab: FloatingActionButton) {
        fab.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources, R.drawable.ic_baseline_location_on_24, context?.theme
            )
        )
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