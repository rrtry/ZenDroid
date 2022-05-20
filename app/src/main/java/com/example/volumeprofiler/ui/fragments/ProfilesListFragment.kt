package com.example.volumeprofiler.ui.fragments

import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.viewmodels.ProfilesListViewModel.ViewEvent.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.*
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.*
import com.example.volumeprofiler.ui.activities.ProfileDetailsActivity
import com.example.volumeprofiler.databinding.ProfilesListFragmentBinding
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import com.example.volumeprofiler.viewmodels.ProfilesListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import androidx.fragment.app.*
import androidx.viewbinding.ViewBinding
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.ProfileAdapter
import com.example.volumeprofiler.ui.activities.ProfileDetailsActivity.Companion.EXTRA_PROFILE
import com.example.volumeprofiler.selection.BaseSelectionObserver
import com.example.volumeprofiler.selection.DetailsLookup
import com.example.volumeprofiler.selection.KeyProvider
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.PreferencesManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.interfaces.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.ref.WeakReference
import kotlin.NoSuchElementException

@AndroidEntryPoint
class ProfilesListFragment: Fragment(),
    ActionModeProvider<String>,
    FabContainer,
    FragmentSwipedListener,
    SelectableListItemInteractionListener<Profile, UUID> {

    private val viewModel: ProfilesListViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var eventBus: EventBus

    private lateinit var profileAdapter: ProfileAdapter
    private var selectedItems: ArrayList<String> = arrayListOf()
    private lateinit var tracker: SelectionTracker<String>

    private var bindingImpl: ProfilesListFragmentBinding? = null
    private val binding: ProfilesListFragmentBinding get() = bindingImpl!!

    private var activity: FabContainerCallbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity = requireActivity() as FabContainerCallbacks
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(EXTRA_SELECTION, selectedItems)
        outState.putParcelable(EXTRA_RV_STATE, binding.recyclerView.layoutManager?.onSaveInstanceState())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        bindingImpl = ProfilesListFragmentBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        profileAdapter = ProfileAdapter(binding.recyclerView, WeakReference(this))
        binding.recyclerView.adapter = profileAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        tracker = SelectionTracker.Builder(
            SELECTION_ID,
            binding.recyclerView,
            KeyProvider(profileAdapter),
            DetailsLookup(binding.recyclerView),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything())
            .build()
        tracker.addObserver(BaseSelectionObserver(WeakReference(this)))

        savedInstanceState?.let {
            selectedItems = it.getStringArrayList(EXTRA_SELECTION) as ArrayList<String>
            tracker.setItemsSelected(selectedItems, true)
        }
        return binding.root
    }

    @Suppress("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.viewEventFlow.onEach {
                        when (it) {
                            is ProfileSetViewEvent -> onProfileSet(it.profile)
                            is RemoveGeofencesViewEvent -> removeGeofences(it.geofences)
                            is ProfileRemoveViewEvent -> onProfileRemove(it.profile)
                            is CancelAlarmsViewEvent -> cancelAlarms(it.alarms)
                        }
                    }.collect()
                }
                launch {
                    viewModel.profilesFlow.collect { list ->
                        sharedViewModel.showDialog.value = list.isEmpty()
                        profileAdapter.submitList(list)
                    }
                }
                launch {
                    eventBus.sharedFlow.collectLatest {
                        if (it is EventBus.Event.ProfileChanged) {
                            try {
                                profileAdapter.currentList.first { profile ->
                                    profile.id == it.id
                                }.also { profile ->
                                    profileAdapter.setSelection(profile, viewModel.lastSelected)
                                }
                            } catch (e: NoSuchElementException) {
                                Log.e("ProfilesListFragment", "Invalid profile: ${it.id}")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        tracker.clearSelection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingImpl = null
    }

    override fun onDetach() {
        super.onDetach()
        activity = null
    }

    override fun onEdit(entity: Profile, binding: ViewBinding) {
        startActivity(Intent(
            requireContext(),
            ProfileDetailsActivity::class.java
        ).apply {
            putExtra(EXTRA_PROFILE, entity)
        })
    }

    override fun setSelection(id: UUID?) {
        viewModel.lastSelected = id
    }

    override fun onEnable(entity: Profile) {
        viewModel.setProfile(entity)
    }

    override fun onDisable(entity: Profile) {
        viewModel.setProfile(entity)
    }

    override fun onRemove(entity: Profile) {
        viewModel.removeProfile(entity)
    }

    override fun isSelected(entity: Profile): Boolean {
        return tracker.isSelected(entity.id.toString())
    }

    override fun isEnabled(entity: Profile): Boolean {
        return preferencesManager.isProfileEnabled(entity)
    }

    private fun cancelAlarms(alarms: List<AlarmRelation>?) {
        alarms?.let {
            scheduleManager.cancelAlarms(it)
        }
    }

    @Suppress("MissingPermission")
    private fun removeGeofences(geofences: List<LocationRelation>) {
        geofences.forEach { i ->
            deleteThumbnail(requireContext(), i.location.previewImageId)
            geofenceManager.removeGeofence(
                i.location,
                i.onEnterProfile,
                i.onExitProfile
            )
        }
    }

    private fun onProfileRemove(profile: Profile) {
        if (profile.id == viewModel.lastSelected) {
            viewModel.lastSelected = null
        }
    }

    private fun onProfileSet(profile: Profile) {
        if (preferencesManager.isProfileEnabled(profile)) {
            profileManager.setDefaultProfile()
            profileAdapter.setSelection(null, viewModel.lastSelected)
        } else {
            profileManager.setProfile(profile)
            profileAdapter.setSelection(profile, viewModel.lastSelected)
        }
    }

    override fun onFabClick(fab: FloatingActionButton) {
        startActivity(Intent(context, ProfileDetailsActivity::class.java))
    }

    override fun onUpdateFab(fab: FloatingActionButton) {
        Handler(Looper.getMainLooper()).post {
            fab.setImageDrawable(
                ResourcesCompat.getDrawable(
                    resources, R.drawable.ic_baseline_do_not_disturb_on_24, context?.theme
                )
            )
        }
    }

    override fun onActionItemRemove() {
        tracker.selection.forEach { selection ->
            viewModel.removeProfile(
                profileAdapter.currentList.first {
                    it.id.toString() == selection
                }
            )
        }
    }

    override fun getTracker(): SelectionTracker<String> {
        return tracker
    }

    override fun getFragmentActivity(): FragmentActivity {
        return requireActivity()
    }

    override fun onSwipe() {
        tracker.clearSelection()
    }

    companion object {

        private const val SELECTION_ID: String = "PROFILE"
        private const val EXTRA_SELECTION: String = "extra_selection"
        private const val EXTRA_RV_STATE: String = "abs_position"

    }
}