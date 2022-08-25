package ru.rrtry.silentdroid.ui.fragments

import android.content.Intent
import ru.rrtry.silentdroid.viewmodels.ProfilesListViewModel.ViewEvent.*
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.recyclerview.widget.*
import ru.rrtry.silentdroid.ui.activities.ProfileDetailsActivity
import ru.rrtry.silentdroid.event.EventBus
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.viewmodels.MainActivityViewModel.ViewEvent.*
import ru.rrtry.silentdroid.viewmodels.MainActivityViewModel
import ru.rrtry.silentdroid.viewmodels.ProfilesListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import androidx.fragment.app.*
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.adapters.ProfileAdapter
import ru.rrtry.silentdroid.core.PreferencesManager.Companion.TRIGGER_TYPE_MANUAL
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.entities.LocationRelation
import ru.rrtry.silentdroid.ui.activities.ViewPagerActivity.Companion.PROFILE_FRAGMENT
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import ru.rrtry.silentdroid.core.*
import ru.rrtry.silentdroid.databinding.ProfileItemViewBinding
import ru.rrtry.silentdroid.databinding.ProfilesListFragmentBinding
import ru.rrtry.silentdroid.interfaces.FabContainer
import ru.rrtry.silentdroid.interfaces.ProfileActionListener
import java.lang.ref.WeakReference
import kotlin.NoSuchElementException

@AndroidEntryPoint
class ProfilesListFragment:
    ListFragment<Profile, ProfilesListFragmentBinding, ProfileAdapter.ProfileHolder, ProfileItemViewBinding, ProfileAdapter>(),
    FabContainer,
    ProfileActionListener {

    private val viewModel: ProfilesListViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    private lateinit var profileAdapter: ProfileAdapter

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var eventBus: EventBus
    @Inject lateinit var fileManager: FileManager
    @Inject lateinit var appNotificationManager: AppNotificationManager

    override val listItem: Class<Profile> = Profile::class.java
    override val selectionId: String = SELECTION_ID

    override fun onPermissionResult(permission: String, granted: Boolean) {
        profileManager.getProfile()?.let {
            profileManager.setProfile(it, true)
        }
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): ProfilesListFragmentBinding {
        return ProfilesListFragmentBinding.inflate(inflater, container, false)
    }

    override fun getRecyclerView(): RecyclerView {
        return viewBinding.recyclerView
    }

    override fun getAdapter(): ProfileAdapter {
        return profileAdapter
    }

    @Suppress("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        profileAdapter = ProfileAdapter(
            listOf(),
            viewBinding.constraintLayout,
            WeakReference(this))
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.currentFragment.collect {
                        withContext(Dispatchers.Main) {
                            if (it == PROFILE_FRAGMENT) setSharedElementCallback()
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
                    viewModel.viewEventFlow.onEach {
                        when (it) {
                            is ProfileSetViewEvent -> onProfileSet(it)
                            is ProfileRemoveViewEvent -> onProfileRemove(it)
                            is RemoveGeofencesViewEvent -> removeGeofences(it.geofences)
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
                    eventBus.eventBus.collectLatest {
                        if (it is EventBus.Event.OnProfileChanged) {
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

    override fun onEdit(entity: Profile, options: Bundle?) {
        startActivity(Intent(
            requireContext(),
            ProfileDetailsActivity::class.java
        ).apply {
            putExtra(EXTRA_PROFILE, entity)
        }, options)
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
        return selectionTracker.isSelected(entity)
    }

    override fun isEnabled(entity: Profile): Boolean {
        return profileManager.isProfileSet(entity)
    }

    private fun cancelAlarms(alarms: List<AlarmRelation>?) {
        alarms?.let {
            scheduleManager.cancelAlarms(it)
        }
    }

    private fun updateProfileAdapter(profiles: List<Profile>) {
        profiles.isEmpty().let { isEmpty ->
            sharedViewModel.showDialog.value = isEmpty
            viewBinding.placeHolderText.isVisible = isEmpty
        }
        profileAdapter.currentList = profiles
        profileAdapter.notifyDataSetChanged()
    }

    private suspend fun removeGeofences(geofences: List<LocationRelation>) {
        geofences.forEach { i ->
            fileManager.deleteThumbnail(i.location.previewImageId)
            geofenceManager.removeGeofence(
                i.location,
                i.onEnterProfile,
                i.onExitProfile
            )
        }
    }

    private fun onProfileRemove(event: ProfileRemoveViewEvent) {

        val profile: Profile = event.profile
        val alarms: List<AlarmRelation> = event.alarms

        if (profile.id == viewModel.lastSelected) {
            viewModel.lastSelected = null
        }
        if (profileManager.isProfileSet(profile)) {
            preferencesManager.clearPreferences()
            appNotificationManager.cancelProfileNotification()
        }
        appNotificationManager.updateNotification(
            profileManager.getProfile(),
            scheduleManager.getPreviousAndNextTriggers(alarms)
        )
    }

    private fun onProfileSet(event: ProfileSetViewEvent) {

        callback?.removeSnackbar()

        val profile: Profile = event.profile
        val alarms: List<AlarmRelation> = event.alarms

        if (!profileManager.isProfileSet(profile)) {

            profileManager.setProfile(profile, TRIGGER_TYPE_MANUAL, null)
            profileAdapter.setSelection(profile, viewModel.lastSelected)
            appNotificationManager.updateNotification(profile, scheduleManager.getPreviousAndNextTriggers(alarms))
        }
    }

    override fun mapSharedElements(
        names: MutableList<String>?,
        sharedElements: MutableMap<String, View>?
    ): ProfileItemViewBinding? {
        val binding: ProfileItemViewBinding? = super.mapSharedElements(names, sharedElements)
        binding?.let {
            sharedElements?.put(SHARED_TRANSITION_PROFILE_IMAGE, binding.profileIcon)
        }
        return binding
    }

    private fun updateFloatingActionButton(fragment: Int) {
        if (fragment == PROFILE_FRAGMENT) {
            onAnimateFab(callback!!.getFloatingActionButton())
        }
    }

    private fun onFragmentSwiped(fragment: Int) {
        if (fragment == PROFILE_FRAGMENT) {
            onFragmentSwiped()
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
        selectionTracker.selection.forEach { selection ->
            viewModel.removeProfile(
                profileAdapter.currentList.first {
                    it == selection
                }
            )
        }
    }

    companion object {

        private const val SELECTION_ID: String = "PROFILE"
        private const val EXTRA_PROFILE: String = "extra_profile"
        private const val EXTRA_ACTION: String = "extra_action"

        const val SHARED_TRANSITION_PROFILE_IMAGE: String = "shared_transition_profile_image"

    }
}