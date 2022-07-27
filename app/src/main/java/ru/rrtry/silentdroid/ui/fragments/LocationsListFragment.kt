package ru.rrtry.silentdroid.ui.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.adapters.LocationAdapter
import ru.rrtry.silentdroid.core.FileManager
import ru.rrtry.silentdroid.core.GeofenceManager
import ru.rrtry.silentdroid.core.GeofenceManager.Companion.ACCESS_LOCATION
import ru.rrtry.silentdroid.entities.LocationRelation
import ru.rrtry.silentdroid.interfaces.FabContainer
import ru.rrtry.silentdroid.interfaces.ListViewContract
import ru.rrtry.silentdroid.ui.activities.MainActivity.Companion.LOCATIONS_FRAGMENT
import ru.rrtry.silentdroid.ui.activities.MapsActivity
import ru.rrtry.silentdroid.util.checkPermission
import ru.rrtry.silentdroid.viewmodels.LocationsListViewModel
import ru.rrtry.silentdroid.viewmodels.LocationsListViewModel.ViewEvent.*
import ru.rrtry.silentdroid.viewmodels.MainActivityViewModel
import ru.rrtry.silentdroid.viewmodels.MainActivityViewModel.ViewEvent.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.rrtry.silentdroid.databinding.LocationItemViewBinding
import ru.rrtry.silentdroid.databinding.LocationsListFragmentBinding
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class LocationsListFragment:
    ListFragment<LocationRelation, LocationsListFragmentBinding, LocationAdapter.LocationViewHolder, LocationItemViewBinding, LocationAdapter>(),
    FabContainer,
    ListViewContract<LocationRelation> {

    private lateinit var locationAdapter: LocationAdapter
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>

    override val selectionId: String = SELECTION_ID
    override val listItem: Class<LocationRelation> = LocationRelation::class.java
    override val hintRes: Int = R.string.geofencing_power_save_mode_hint

    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var fileManager: FileManager

    private val viewModel: LocationsListViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private val recycleListener = RecyclerView.RecyclerListener { viewHolder ->
        if (viewHolder is LocationAdapter.LocationViewHolder) {
            viewHolder.clearMapView()
        }
    }

    override fun onPermissionResult(permission: String, granted: Boolean) = Unit

    private fun startMapActivity(locationRelation: LocationRelation? = null) {
        startActivity(MapsActivity.newIntent(requireContext(), locationRelation))
    }

    private fun updateAdapterData(list: List<LocationRelation>) {
        viewBinding.hintLocations.isVisible = list.isEmpty()
        locationAdapter.currentList = list
        locationAdapter.notifyDataSetChanged()
        showPowerSaveModeHint(
            requireContext().getString(R.string.geofencing_power_save_mode_hint)
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationPermissionLauncher = registerForActivityResult(RequestMultiplePermissions()) { map ->
            if (!map.containsValue(false)) {
                geofenceManager.checkLocationServicesAvailability(requireActivity())
            } else if (shouldShowRequestPermissionRationale(ACCESS_LOCATION)) {
                callback?.showSnackBar("Geofencing feature requires all-time location access", length = Snackbar.LENGTH_INDEFINITE) {
                    requestLocationPermission()
                }
            } else {
                callback?.showSnackBar("Geofencing feature requires all-time location access", length = Snackbar.LENGTH_INDEFINITE) {
                    geofenceManager.openPackagePermissionSettings()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewBinding.recyclerView.addRecyclerListener(recycleListener)
        locationAdapter = LocationAdapter(
            listOf(),
            requireContext(),
            WeakReference(this))

        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.currentFragment.collect {
                        if (it == LOCATIONS_FRAGMENT) {
                            showPowerSaveModeHint(requireContext().getString(hintRes))
                        }
                    }
                }
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

    private suspend fun removeGeofence(locationRelation: LocationRelation) {
        geofenceManager.removeGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        fileManager.deleteThumbnail(locationRelation.location.previewImageId)
    }

    private fun enableGeofence(locationRelation: LocationRelation) {
        geofenceManager.addGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        locationAdapter.updateGeofenceState(locationRelation, true)
        geofenceManager.checkLocationServicesAvailability(requireActivity())
    }

    private fun disableGeofence(locationRelation: LocationRelation) {
        geofenceManager.removeGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        locationAdapter.updateGeofenceState(locationRelation, false)
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
        if (sharedViewModel.showDialog.value) {
            requireContext().resources.let { res ->
                PopupDialog.create(
                    "Feature unavailable",
                    "At least one existing profile required in order to create location triggers",
                    R.drawable.ic_baseline_not_listed_location_24
                ).show(
                    requireActivity().supportFragmentManager,
                    null
                )
            }
            return
        }
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
                (locationAdapter.currentList.first { item ->
                    item == selection
                } as LocationRelation).location
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

    override fun getAdapter(): LocationAdapter {
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