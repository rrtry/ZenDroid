package com.example.volumeprofiler.ui.fragments

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.LocationAdapter
import com.example.volumeprofiler.core.FileManager
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.GeofenceManager.Companion.ACCESS_LOCATION
import com.example.volumeprofiler.databinding.LocationItemViewBinding
import com.example.volumeprofiler.databinding.LocationsListFragmentBinding
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.interfaces.FabContainer
import com.example.volumeprofiler.interfaces.ListViewContract
import com.example.volumeprofiler.ui.activities.MainActivity.Companion.LOCATIONS_FRAGMENT
import com.example.volumeprofiler.ui.activities.MapsActivity
import com.example.volumeprofiler.util.ViewUtil.Companion.showSnackbar
import com.example.volumeprofiler.util.checkPermission
import com.example.volumeprofiler.viewmodels.LocationsListViewModel
import com.example.volumeprofiler.viewmodels.LocationsListViewModel.ViewEvent.*
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import com.example.volumeprofiler.viewmodels.MainActivityViewModel.ViewEvent.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class LocationsListFragment: ListFragment<LocationRelation, LocationsListFragmentBinding, LocationAdapter.LocationViewHolder, LocationItemViewBinding>(),
    FabContainer,
    ListViewContract<LocationRelation> {

    private lateinit var locationAdapter: LocationAdapter
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    override val selectionId: String = SELECTION_ID
    override val listItem: Class<LocationRelation> = LocationRelation::class.java

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var fileManager: FileManager

    private val viewModel: LocationsListViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private val recycleListener = RecyclerView.RecyclerListener { viewHolder ->
        (viewHolder as LocationAdapter.LocationViewHolder).clearMapView()
    }

    override fun onPermissionResult(permission: String, granted: Boolean) = Unit

    private fun startMapActivity(locationRelation: LocationRelation? = null) {
        startActivity(MapsActivity.newIntent(requireContext(), locationRelation))
    }

    private fun updateAdapterData(list: List<LocationRelation>) {
        viewBinding.hintLocations.isVisible = list.isEmpty()
        locationAdapter.currentList = list
        locationAdapter.notifyDataSetChanged()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationPermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { map ->
            if (!map.containsValue(false)) {
                geofenceManager.checkLocationServicesAvailability(requireActivity())
            } else if (shouldShowRequestPermissionRationale(ACCESS_LOCATION)) {
                callback?.showSnackBar("Geofencing feature requires all-time location access", Snackbar.LENGTH_INDEFINITE) {
                    requestLocationPermission()
                }
            } else {
                callback?.showSnackBar("Geofencing feature requires all-time location access", Snackbar.LENGTH_INDEFINITE) {
                    geofenceManager.openPackagePermissionSettings()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewBinding.recyclerView.addRecyclerListener(recycleListener)
        locationAdapter = LocationAdapter(listOf(), requireContext(), WeakReference(this))
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
                            is RequestLocationPermission -> requestLocationPermission()
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

    private fun requestLocationPermission() {
        geofenceManager.requestLocationPermission(locationPermissionLauncher)
    }

    @Suppress("MissingPermission")
    private suspend fun removeGeofence(locationRelation: LocationRelation) {
        geofenceManager.removeGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        fileManager.deleteThumbnail(locationRelation.location.previewImageId)
        callback?.showSnackBar("Geofence successfully removed", Snackbar.LENGTH_LONG, null)
    }

    @Suppress("MissingPermission")
    private fun enableGeofence(locationRelation: LocationRelation) {
        geofenceManager.addGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        locationAdapter.updateGeofenceState(locationRelation, true)
        geofenceManager.checkLocationServicesAvailability(requireActivity())
    }

    @Suppress("MissingPermission")
    private fun disableGeofence(locationRelation: LocationRelation) {
        geofenceManager.removeGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        locationAdapter.updateGeofenceState(locationRelation, false)
        callback?.showSnackBar("Geofence successfully unregistered", Snackbar.LENGTH_LONG, null)
    }

    override fun onEdit(entity: LocationRelation, options: Bundle?) {
        startMapActivity(entity)
    }

    override fun onEnable(entity: LocationRelation) {
        if (checkPermission(ACCESS_LOCATION)) {
            viewModel.enableGeofence(entity)
        } else {
            viewModel.requestLocationPermission()
        }
    }

    override fun onDisable(entity: LocationRelation) {
        viewModel.disableGeofence(entity)
    }

    override fun onRemove(entity: LocationRelation) {
        viewModel.removeGeofence(entity)
    }

    private fun updateFloatingActionButton(fragment: Int) {
        if (fragment == LOCATIONS_FRAGMENT) {
            onAnimateFab(callback!!.getFloatingActionButton())
        }
    }

    private fun onFragmentSwiped(fragment: Int) {
        if (fragment == LOCATIONS_FRAGMENT) onFragmentSwiped()
    }

    private fun onFloatingActionButtonClick(fragment: Int) {
        if (fragment == LOCATIONS_FRAGMENT) onFabClick(callback!!.getFloatingActionButton())
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

    override fun onActionItemRemove() {
        selectionTracker.selection.forEach { selection ->
            viewModel.removeLocation(
                locationAdapter.currentList.first {
                    it == selection
                }.location
            )
        }
    }

    override fun getBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LocationsListFragmentBinding {
        return LocationsListFragmentBinding.inflate(inflater, container, false)
    }

    override fun getRecyclerView(): RecyclerView {
        return viewBinding.recyclerView
    }

    override fun getAdapter(): RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {
        return locationAdapter
    }

    override fun onDestroyView() {
        getRecyclerView().removeRecyclerListener(recycleListener)
        super.onDestroyView()
    }

    companion object {

        private const val SELECTION_ID: String = "location"
        const val PAYLOAD_GEOFENCE_CHANGED: String = "geofence_changed"

        fun getEnabledStatePayload(enabled: Boolean): Bundle {
            return Bundle().apply {
                putBoolean(PAYLOAD_GEOFENCE_CHANGED, enabled)
            }
        }
    }
}