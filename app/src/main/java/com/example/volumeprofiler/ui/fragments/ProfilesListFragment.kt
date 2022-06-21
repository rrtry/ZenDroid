package com.example.volumeprofiler.ui.fragments

import android.Manifest.permission.READ_PHONE_STATE
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.volumeprofiler.viewmodels.ProfilesListViewModel.ViewEvent.*
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import android.util.Log
import android.view.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.recyclerview.widget.*
import com.example.volumeprofiler.ui.activities.ProfileDetailsDetailsActivity
import com.example.volumeprofiler.databinding.ProfilesListFragmentBinding
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.viewmodels.MainActivityViewModel.ViewEvent.*
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
import com.example.volumeprofiler.databinding.ProfileItemViewBinding
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.interfaces.*
import com.example.volumeprofiler.ui.activities.MainActivity.Companion.PROFILE_FRAGMENT
import com.example.volumeprofiler.util.ParcelableUtil
import com.example.volumeprofiler.util.canWriteSettings
import com.example.volumeprofiler.util.checkPermission
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.NoSuchElementException

@AndroidEntryPoint
class ProfilesListFragment: ListFragment<Profile, ProfilesListFragmentBinding, ProfileAdapter.ProfileHolder, ProfileItemViewBinding>(),
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
    @Inject lateinit var notificationDelegate: NotificationDelegate

    override val listItem: Class<Profile> = Profile::class.java
    override val selectionId: String = SELECTION_ID

    override fun onPermissionResult(permission: String, granted: Boolean) {
        preferencesManager.getProfile()?.let {
            showDeniedPermissionHint(it)
            profileManager.setProfile(it, true)
        }
    }

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): ProfilesListFragmentBinding {
        return ProfilesListFragmentBinding.inflate(inflater, container, false)
    }

    override fun getRecyclerView(): RecyclerView {
        return viewBinding.recyclerView
    }

    override fun getAdapter(): RecyclerView.Adapter<ProfileAdapter.ProfileHolder> {
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

    override fun onEdit(entity: Profile, options: Bundle?) {
        startActivity(Intent(
            requireContext(),
            ProfileDetailsDetailsActivity::class.java
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
            viewBinding.placeHolderText.isVisible = isEmpty
        }
        profileAdapter.currentList = profiles
        profileAdapter.notifyDataSetChanged()
    }

    @Suppress("MissingPermission")
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
        if (profile.id == preferencesManager.getProfile()?.id) {
            preferencesManager.clearPreferences()
        }
        notificationDelegate.updateNotification(
            profile, scheduleManager.getOngoingAlarm(alarms)
        )
    }

    private fun onProfileSet(event: ProfileSetViewEvent) {

        callback?.removeSnackbar()

        val profile: Profile = event.profile
        val alarms: List<AlarmRelation> = event.alarms

        if (!preferencesManager.isProfileEnabled(profile)) {

            profileManager.setProfile(profile, TRIGGER_TYPE_MANUAL, null)
            profileAdapter.setSelection(profile, viewModel.lastSelected)
            notificationDelegate.updateNotification(profile, scheduleManager.getOngoingAlarm(alarms))

            showDeniedPermissionHint(profile)
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
        startActivity(Intent(context, ProfileDetailsDetailsActivity::class.java))
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