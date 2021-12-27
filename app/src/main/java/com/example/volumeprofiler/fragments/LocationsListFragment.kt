package com.example.volumeprofiler.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest.permission.*
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.volumeprofiler.activities.MapsActivity
import com.example.volumeprofiler.databinding.LocationItemViewBinding
import com.example.volumeprofiler.databinding.LocationsListFragmentBinding
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.entities.Location
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.LocationsListViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject
import com.example.volumeprofiler.interfaces.PermissionRequestCallback

@AndroidEntryPoint
class LocationsListFragment: Fragment(), ActionModeProvider<String> {

    @Inject
    lateinit var geofenceUtil: GeofenceUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    private lateinit var tracker: SelectionTracker<String>
    private val locationAdapter: LocationAdapter = LocationAdapter()

    private var _binding: LocationsListFragmentBinding? = null
    private val binding: LocationsListFragmentBinding get() = _binding!!

    private lateinit var mapActivityLauncher: ActivityResultLauncher<Intent>

    private val viewModel: LocationsListViewModel by viewModels()

    private var callback: PermissionRequestCallback? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as PermissionRequestCallback
        mapActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val geofence: Location = it.data!!.getParcelableExtra(MapsActivity.EXTRA_LOCATION)!!
                val updateExisting: Boolean = it.data!!.getBooleanExtra(MapsActivity.FLAG_UPDATE_EXISTING, false)
                if (updateExisting) {
                    Log.i("LocationsListFragment", "updating existing location")
                    viewModel.updateLocation(geofence)
                } else {
                    Log.i("LocationsListFragment", "adding new location")
                    viewModel.addLocation(geofence)
                }
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
        mapActivityLauncher.unregister()
    }

    private fun requestLocationPermission(): Unit {
        /*
        var permissions: Array<String> = arrayOf(ACCESS_FINE_LOCATION)
        if (Build.VERSION_CODES.Q <= Build.VERSION.SDK_INT) {
            permissions += ACCESS_BACKGROUND_LOCATION
        }
         */
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LocationsListFragmentBinding.inflate(inflater, container, false)
        binding.createGeofenceButton.setOnClickListener {
            startMapActivity(null)
        }
        return binding.root
    }

    private fun startMapActivity(locationRelation: LocationRelation? = null): Unit {
        val intent: Intent = MapsActivity.newIntent(requireContext(), locationRelation)
        mapActivityLauncher.launch(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeofenceUtil.REQUEST_ENABLE_LOCATION_SERVICES &&
            resultCode != Activity.RESULT_OK) {
            val snackBar: Snackbar = Snackbar.make(binding.root, "Enabling location services is required", Snackbar.LENGTH_LONG)
            snackBar.setAction("Enable location") {
                geofenceUtil.checkLocationServicesAvailability(requireActivity(),null)
            }
            snackBar.show()
        }
    }

    private fun initRecyclerView(view: View) {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = locationAdapter
    }

    private fun updateUI(list: List<LocationRelation>): Unit {
        if (list.isEmpty()) {
            binding.hintLocations.visibility = View.VISIBLE
        } else {
            binding.hintLocations.visibility = View.GONE
        }
        locationAdapter.submitList(list)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView(view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.locationsFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle, Lifecycle.State.STARTED).collect {
                updateUI(it)
            }
        }
    }

    @Suppress("MissingPermission")
    private fun removeGeofence(locationRelation: LocationRelation): Unit {
        if (geofenceUtil.locationAccessGranted()) {
            geofenceUtil.removeGeofence(
                locationRelation.location,
                locationRelation.onEnterProfile,
                locationRelation.onExitProfile
            )
            viewModel.removeLocation(locationRelation.location)
        } else {
            requestLocationPermission()
        }
    }

    @Suppress("MissingPermission")
    private fun enableGeofence(locationRelation: LocationRelation, position: Int): Unit {
        geofenceUtil.addGeofence(
            locationRelation.location,
            locationRelation.onEnterProfile,
            locationRelation.onExitProfile
        )
        viewModel.enableGeofence(locationRelation.location)
        updateItem(position, 1)
    }

    private fun updateItem(position: Int, enabled: Byte): Unit {
        locationAdapter.notifyItemChanged(position, Bundle().apply {
            putByte(PAYLOAD_GEOFENCE_ENABLED, enabled)
        })
    }

    @Suppress("MissingPermission")
    private fun disableGeofence(locationRelation: LocationRelation, position: Int): Unit {
        if (geofenceUtil.locationAccessGranted()) {
            geofenceUtil.removeGeofence(
                locationRelation.location,
                locationRelation.onEnterProfile,
                locationRelation.onExitProfile
            )
            viewModel.disableGeofence(locationRelation.location)
            updateItem(position, 0)
        } else {
            requestLocationPermission()
        }
    }

    private inner class LocationViewHolder(private val binding: LocationItemViewBinding): RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(locationRelation: LocationRelation, isSelected: Boolean): Unit {

            val location: Location = locationRelation.location

            binding.title.text = location.title
            binding.addressTextView.text = TextUtil.formatAddress(location.address)
            binding.enabledGeofenceSwitch.isChecked = location.enabled == 1.toByte()
            binding.profiles.text = "${locationRelation.onEnterProfile.title} - ${locationRelation.onExitProfile}"

            binding.editGeofenceButton.setOnClickListener {
                startMapActivity(locationAdapter.getItemAtPosition(bindingAdapterPosition))
            }

            binding.removeGeofenceButton.setOnClickListener {
                removeGeofence(locationAdapter.getItemAtPosition(bindingAdapterPosition))
            }
        }

        override fun onClick(v: View?) {
            val locationRelation: LocationRelation = locationAdapter.getItemAtPosition(bindingAdapterPosition)
            if (locationRelation.location.enabled == 1.toByte()) {
                disableGeofence(locationRelation, bindingAdapterPosition)
            } else {
                when {
                    profileUtil.grantedRequiredPermissions(locationRelation) && geofenceUtil.locationAccessGranted() -> {
                        val weakReference: WeakReference<LocationsListFragment> = WeakReference(this@LocationsListFragment)
                        val adapterPosition: Int = bindingAdapterPosition
                        geofenceUtil.checkLocationServicesAvailability(requireActivity()) {
                            weakReference.get()?.enableGeofence(locationRelation, adapterPosition)
                        }
                    }
                    !geofenceUtil.locationAccessGranted() || profileUtil.shouldRequestPhonePermission(locationRelation) -> {
                        callback?.requestLocationPermissions(locationRelation)
                    }
                    else -> {
                        sendSystemPreferencesAccessNotification(requireContext(), profileUtil)
                    }
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

        override fun getChangePayload(oldItem: LocationRelation, newItem: LocationRelation): Any {
            super.getChangePayload(oldItem, newItem)

            val payloadBundle: Bundle = Bundle()

            if (oldItem.location.enabled != newItem.location.enabled) {
                payloadBundle.putByte(PAYLOAD_GEOFENCE_ENABLED, newItem.location.enabled)
            }
            return payloadBundle
        }

    }), ListAdapterItemProvider<String> {

        private lateinit var binding: LocationItemViewBinding

        fun getItemAtPosition(position: Int): LocationRelation {
            return getItem(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationViewHolder {
            binding = LocationItemViewBinding.inflate(LayoutInflater.from(context), parent, false)
            return LocationViewHolder(binding)
        }

        override fun onBindViewHolder(holder: LocationViewHolder, position: Int) {
            val locationRelation: LocationRelation = getItem(position)
            holder.bind(locationRelation, false)
            /*
            tracker.let {
                holder.bind(locationTrigger, it.isSelected(locationTrigger.location.id.toString()))
            }
             */
        }

        override fun getItemKey(position: Int): String {
            return currentList[position].location.id.toString()
        }

        override fun getPosition(key: String): Int {
            return currentList.indexOfFirst { key == it.location.id.toString() }
        }
    }

    companion object {

        private const val SELECTION_ID: String = "LOCATION"
        private const val REQUEST_TURN_DEVICE_LOCATION_ON: Int = 2
        private const val PAYLOAD_GEOFENCE_ENABLED: String = "payload_geofence_enabled"
        private const val ACTION_ENABLE_GEOFENCE: Int = 0x03
        private const val ACTION_DISABLE_GEOFENCE: Int = 0x04
        private const val EXTRA_ACTION: String = "extra_action"
    }

    override fun onActionItemRemove() {

    }

    override fun getTracker(): SelectionTracker<String> {
        return tracker
    }

    override fun getFragmentActivity(): FragmentActivity {
        return requireActivity()
    }
}