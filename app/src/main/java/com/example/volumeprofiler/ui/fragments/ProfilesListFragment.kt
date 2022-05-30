package com.example.volumeprofiler.ui.fragments

import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.viewmodels.ProfilesListViewModel.ViewEvent.*
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.*
import com.example.volumeprofiler.ui.activities.ProfileDetailsActivity
import com.example.volumeprofiler.databinding.ProfilesListFragmentBinding
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.viewmodels.MainActivityViewModel.ViewEvent.*
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import com.example.volumeprofiler.viewmodels.ProfilesListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import androidx.fragment.app.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.adapters.ProfileAdapter
import com.example.volumeprofiler.core.*
import com.example.volumeprofiler.core.PreferencesManager.Companion.TRIGGER_TYPE_MANUAL
import com.example.volumeprofiler.ui.activities.ProfileDetailsActivity.Companion.EXTRA_PROFILE
import com.example.volumeprofiler.selection.BaseSelectionObserver
import com.example.volumeprofiler.selection.DetailsLookup
import com.example.volumeprofiler.selection.KeyProvider
import com.example.volumeprofiler.databinding.ProfileItemViewBinding
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.interfaces.*
import com.example.volumeprofiler.ui.activities.MainActivity.Companion.PROFILE_FRAGMENT
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
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
    @Inject lateinit var notificationDelegate: NotificationDelegate

    private lateinit var profileAdapter: ProfileAdapter
    private var selectedItems: ArrayList<String> = arrayListOf()
    private lateinit var tracker: SelectionTracker<String>

    private var bindingImpl: ProfilesListFragmentBinding? = null
    private val binding: ProfilesListFragmentBinding get() = bindingImpl!!

    private var callback: FabContainerCallbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = requireActivity() as FabContainerCallbacks
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
        profileAdapter = ProfileAdapter(binding.constraintLayout, WeakReference(this))
        binding.recyclerView.adapter = profileAdapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        tracker = SelectionTracker.Builder(
            SELECTION_ID,
            binding.recyclerView,
            KeyProvider(profileAdapter),
            DetailsLookup(binding.recyclerView),
            StorageStrategy.createStringStorage()
        ).withSelectionPredicate(SelectionPredicates.createSelectAnything()).build()
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
                    sharedViewModel.viewEvents
                        .onStart {
                            if (sharedViewModel.viewEvents.replayCache.isNotEmpty() &&
                                sharedViewModel.viewEvents.replayCache.last() is OnFloatingActionButtonClick
                            ) {
                                sharedViewModel.viewEvents.resetReplayCache()
                            }
                        }
                        .collect {
                            when (it) {
                                is UpdateFloatingActionButton -> updateFloatingActionButton(it.fragment)
                                is OnSwiped -> onFragmentSwiped(it.fragment)
                                is OnFloatingActionButtonClick -> onFloatingActionButtonClick(it.fragment)
                                else -> Log.i("ProfilesListFragment", "Unknown viewEvent: $it")
                            }
                        }
                }
                launch {
                    viewModel.viewEventFlow.onEach {
                        when (it) {
                            is ProfileSetViewEvent -> onProfileSet(it)
                            is RemoveGeofencesViewEvent -> removeGeofences(it.geofences)
                            is ProfileRemoveViewEvent -> onProfileRemove(it.profile)
                            is CancelAlarmsViewEvent -> cancelAlarms(it.alarms)
                        }
                    }.collect()
                }
                launch {
                    viewModel.profilesFlow.collect { list ->
                        updateProfileAdapter(list)
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
        if (!requireActivity().isChangingConfigurations) {
            tracker.clearSelection()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingImpl = null
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }

    override fun onEdit(entity: Profile, binding: ProfileItemViewBinding) {
        startActivity(Intent(
            requireContext(),
            ProfileDetailsActivity::class.java
        ).apply {
            putExtra(EXTRA_PROFILE, entity)
        }, createTransitionAnimationOptions(binding).toBundle())
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

    private fun updateProfileAdapter(profiles: List<Profile>) {
        profiles.isEmpty().let { isEmpty ->
            sharedViewModel.showDialog.value = isEmpty
            binding.placeHolderText.isVisible = isEmpty
        }
        profileAdapter.submitList(profiles)
    }

    private fun createTransitionAnimationOptions(binding: ProfileItemViewBinding): ActivityOptionsCompat {
        return ActivityOptionsCompat.makeSceneTransitionAnimation(
            requireActivity(),
            androidx.core.util.Pair.create(binding.profileIcon, SHARED_TRANSITION_PROFILE_IMAGE))
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

    private fun onProfileSet(event: ProfileSetViewEvent) {
        if (!preferencesManager.isProfileEnabled(event.profile)) {
            profileManager.setProfile(event.profile, TRIGGER_TYPE_MANUAL, null)
            profileAdapter.setSelection(event.profile, viewModel.lastSelected)
            notificationDelegate.updateNotification(
                event.profile, scheduleManager.getOngoingAlarm(event.alarms)
            )
        }
    }

    private fun updateFloatingActionButton(fragment: Int) {
        if (fragment == PROFILE_FRAGMENT) {
            onAnimateFab(callback!!.getFloatingActionButton())
        }
    }

    private fun onFragmentSwiped(fragment: Int) {
        if (fragment == PROFILE_FRAGMENT) {
            onSwipe()
        }
    }

    private fun onFloatingActionButtonClick(fragment: Int) {
        if (fragment == PROFILE_FRAGMENT) {
            onFabClick(callback!!.getFloatingActionButton())
        }
    }

    override fun onFabClick(fab: FloatingActionButton) {
        startActivity(Intent(context, ProfileDetailsActivity::class.java))
    }

    override fun onUpdateFab(fab: FloatingActionButton) {
        fab.setImageDrawable(
            ResourcesCompat.getDrawable(
                resources, R.drawable.ic_baseline_do_not_disturb_on_24, context?.theme
            )
        )
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

        const val SHARED_TRANSITION_PROFILE_IMAGE: String = "shared_transition_profile_image"
        private const val SELECTION_ID: String = "PROFILE"
        private const val EXTRA_SELECTION: String = "extra_selection"
        private const val EXTRA_RV_STATE: String = "abs_position"

    }
}