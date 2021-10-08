package com.example.volumeprofiler.fragments

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import com.example.volumeprofiler.activities.MapsActivity
import com.example.volumeprofiler.databinding.LocationItemViewBinding
import com.example.volumeprofiler.databinding.LocationsListFragmentBinding
import com.example.volumeprofiler.interfaces.ActionModeProvider
import com.example.volumeprofiler.interfaces.ListAdapterItemProvider
import com.example.volumeprofiler.models.Location
import com.example.volumeprofiler.models.LocationRelation
import com.example.volumeprofiler.util.GeofenceUtil
import com.example.volumeprofiler.viewmodels.LocationsListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class LocationsListFragment: Fragment(), ActionModeProvider<String> {

    @Inject
    lateinit var geofenceUtil: GeofenceUtil

    private lateinit var tracker: SelectionTracker<String>
    private val locationAdapter: LocationAdapter = LocationAdapter()

    private var _binding: LocationsListFragmentBinding? = null
    private val binding: LocationsListFragmentBinding get() = _binding!!

    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    private val viewModel: LocationsListViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        locationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                startMapActivity()
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        locationPermissionLauncher.unregister()
    }

    private fun launchPermissionRequest(): Unit {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LocationsListFragmentBinding.inflate(inflater, container, false)
        binding.fab.setOnClickListener {

        }
        return binding.root
    }

    private fun startMapActivity(locationRelation: LocationRelation? = null): Unit {
        val intent: Intent = MapsActivity.newIntent(requireContext(), locationRelation)
        startActivity(intent)
    }

    private fun initRecyclerView(view: View) {
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = locationAdapter
    }

    /*
    private fun initSelectionTracker(): Unit {
        tracker = SelectionTracker.Builder(
                SELECTION_ID,
                binding.recyclerView,
                KeyProvider(locationAdapter),
                DetailsLookup(binding.recyclerView),
                StorageStrategy.createStringStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
                .build()
        tracker.addObserver(BaseSelectionObserver(WeakReference(this)))
    }
     */

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

    private fun removeGeofence(locationRelation: LocationRelation): Unit {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofenceUtil.removeGeofence(locationRelation)
            viewModel.removeLocation(locationRelation.location)
        } else {
            launchPermissionRequest()
        }
    }

    private fun enableGeofence(locationRelation: LocationRelation): Unit {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofenceUtil.addGeofence(locationRelation)
            viewModel.enableGeofence(locationRelation.location)
        } else {
            launchPermissionRequest()
        }
    }

    private fun disableGeofence(locationRelation: LocationRelation): Unit {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofenceUtil.removeGeofence(locationRelation)
            viewModel.disableGeofence(locationRelation.location)
        } else {
            launchPermissionRequest()
        }
    }

    private inner class LocationViewHolder(private val binding: LocationItemViewBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(locationRelation: LocationRelation, isSelected: Boolean): Unit {

            val location: Location = locationRelation.location

            binding.addressTextView.text = location.address
            binding.enabledGeofenceSwitch.isChecked = location.enabled == 1.toByte()
            binding.onEnterProfile.text = locationRelation.onEnterProfile.title
            binding.onExitProfile.text = locationRelation.onExitProfile.title

            binding.removeGeofenceButton.setOnClickListener {
                removeGeofence(locationAdapter.getItemAtPosition(bindingAdapterPosition))
            }
            binding.enabledGeofenceSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    val item: LocationRelation = locationAdapter.getItemAtPosition(bindingAdapterPosition)
                    if (isChecked) {
                        enableGeofence(item)
                    } else {
                        disableGeofence(item)
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