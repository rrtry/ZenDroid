package com.example.volumeprofiler.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.viewmodels.ProfilesListViewModel.ViewEvent.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.*
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.ListAdapter
import androidx.transition.*
import com.example.volumeprofiler.activities.ProfileDetailsActivity
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.ItemDetails
import com.example.volumeprofiler.databinding.ProfileItemViewBinding
import com.example.volumeprofiler.databinding.ProfilesListFragmentBinding
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.MainActivityViewModel
import com.example.volumeprofiler.viewmodels.ProfilesListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import androidx.fragment.app.*
import com.example.volumeprofiler.R
import com.example.volumeprofiler.activities.ProfileDetailsActivity.Companion.EXTRA_PROFILE
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.BaseSelectionObserver
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.DetailsLookup
import com.example.volumeprofiler.adapters.recyclerview.multiSelection.KeyProvider
import com.example.volumeprofiler.interfaces.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference

@AndroidEntryPoint
class ProfilesListFragment: Fragment(), ActionModeProvider<String>, FabContainer, FragmentSwipedListener {

    private val viewModel: ProfilesListViewModel by viewModels()
    private val sharedViewModel: MainActivityViewModel by activityViewModels()

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var eventBus: EventBus

    private val profileAdapter: ProfileAdapter by lazy {
        ProfileAdapter()
    }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.viewEventFlow.onEach {
                        when (it) {
                            is ProfileSetViewEvent -> {
                                if (preferencesManager.isProfileEnabled(it.profile)) {
                                    preferencesManager.clearPreferences()
                                    profileAdapter.setSelection(null)
                                } else {
                                    profileManager.setProfile(it.profile)
                                    profileAdapter.setSelection(it.profile)
                                    if (shouldShowPermissionSuggestion(requireContext(), it.profile)) {
                                        activity?.showSnackBar(
                                            "Insufficient permissions for profile",
                                            Snackbar.LENGTH_LONG)
                                        {
                                            sendPermissionsNotification(requireContext(), profileManager, it.profile)
                                            activity?.requestPermissions(
                                                getDeniedPermissionsForProfile(requireContext(), it.profile))
                                        }
                                    }
                                }
                            }
                            is CancelAlarmsViewEvent -> {
                                if (!it.alarms.isNullOrEmpty()) {
                                    scheduleManager.cancelAlarms(it.alarms)
                                }
                            }
                            is RemoveGeofencesViewEvent -> {
                                for (i in it.geofences) {
                                    deleteThumbnail(requireContext(), i.location.previewImageId)
                                    geofenceManager.removeGeofence(
                                        i.location,
                                        i.onEnterProfile,
                                        i.onExitProfile
                                    )
                                }
                            }
                            is ProfileRemoveViewEvent -> {
                                if (it.profile.id == viewModel.lastSelected) {
                                    viewModel.lastSelected = null
                                }
                            }
                        }
                    }.collect()
                }
                launch {
                    viewModel.profilesFlow.map { list ->
                        if (preferencesManager.getRecyclerViewPositionsMap() != null) {
                            restoreChangedPositions(list, preferencesManager.getRecyclerViewPositionsMap()!!)
                        } else {
                            list
                        }
                    }.onEach { list -> sharedViewModel.showDialog.value = list.isEmpty()
                        profileAdapter.submitList(list)
                    }.collect()
                }
                launch {
                    eventBus.sharedFlow.collectLatest {
                        if (it is EventBus.Event.ProfileApplied) {
                            for (item in profileAdapter.currentList) {
                                if (item.id == it.id) {
                                    profileAdapter.setSelection(item)
                                }
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

    private inner class ProfileHolder(private val adapterBinding: ProfileItemViewBinding):
            RecyclerView.ViewHolder(adapterBinding.root),
            ViewHolderItemDetailsProvider<String>,
            View.OnClickListener {

        init {
            adapterBinding.root.setOnClickListener(this)
            adapterBinding.expandableView.visibility = View.GONE
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> {
            return ItemDetails(bindingAdapterPosition, profileAdapter.getItemAtPosition(bindingAdapterPosition).id.toString())
        }

        private fun expandView(animate: Boolean): Unit {
            if (animate) {
                TransitionManager.beginDelayedTransition(binding.root, AutoTransition())
                adapterBinding.expandButton.animate().rotation(180.0f).start()
            }
            else {
                adapterBinding.expandButton.rotation = 180f
            }
            adapterBinding.itemSeparator.visibility = View.VISIBLE
            adapterBinding.expandableView.visibility = View.VISIBLE
        }

        private fun collapseView(): Unit {
            TransitionManager.beginDelayedTransition(binding.root, AutoTransition())
            adapterBinding.itemSeparator.visibility = View.GONE
            adapterBinding.expandableView.visibility = View.GONE
            adapterBinding.expandButton.animate().rotation(0f).start()
        }

        private fun setProfileTitle(profile: Profile): Unit {
            adapterBinding.checkBox.text = profile.title
        }

        private fun setViewScale(isSelected: Boolean): Unit {
            val scale: Float = if (isSelected) 0.8f else 1.0f
            itemView.scaleX = scale
            itemView.scaleY = scale
        }

        private fun setSelectedState(isSelected: Boolean, animate: Boolean): Unit {
            if (animate) {
                AnimUtil.selected(itemView, isSelected)
            } else {
                setViewScale(isSelected)
            }
        }

        private fun setEnabledState(profile: Profile): Unit {
            preferencesManager.isProfileEnabled(profile).let {
                adapterBinding.checkBox.isChecked = it
                if (it) {
                    viewModel.lastSelected = profileAdapter.getItemAtPosition(bindingAdapterPosition).id
                }
            }
        }

        private fun setListeners(profile: Profile): Unit {
            adapterBinding.expandButton.setOnClickListener {
                if (adapterBinding.expandableView.visibility == View.GONE) {
                    expandView(true)
                } else {
                    collapseView()
                }
            }
            adapterBinding.editProfileButton.setOnClickListener {
                startActivity(Intent(
                    requireContext(),
                    ProfileDetailsActivity::class.java
                ).apply {
                    putExtra(EXTRA_PROFILE, profileAdapter.getItemAtPosition(bindingAdapterPosition))
                })
            }
            adapterBinding.removeProfileButton.setOnClickListener {
                viewModel.removeProfile(profile)
            }
        }

        fun bind(profile: Profile, isSelected: Boolean, animate: Boolean): Unit {
            setProfileTitle(profile)
            setSelectedState(isSelected, animate)
            setEnabledState(profile)
            setListeners(profile)
        }

        override fun onClick(v: View?) {
            profileAdapter.getItemAtPosition(bindingAdapterPosition).also {
                if (!tracker.isSelected(it.id.toString())) {
                    viewModel.setProfile(it)
                }
            }
        }
    }

    private inner class ProfileAdapter : ListAdapter<Profile, ProfileHolder>(object : DiffUtil.ItemCallback<Profile>() {

        override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
            return oldItem == newItem
        }

    }), ListAdapterItemProvider<String> {

        fun setSelection(profile: Profile?): Unit {
            profile?.let {
                updatePreviousProfileView()
                updateCurrentProfileView(profile.id)
                viewModel.lastSelected = profile.id
                return
            }
            updatePreviousProfileView()
            viewModel.lastSelected = null
        }

        private fun updatePreviousProfileView(): Unit {
            viewModel.lastSelected?.let {
                notifyItemChanged(getPosition(it), true)
            }
        }

        private fun updateCurrentProfileView(uuid: UUID): Unit {
            notifyItemChanged(getPosition(uuid), true)
        }

        private fun getPosition(id: UUID): Int {
            for ((index, i) in currentList.withIndex()) {
                if (i.id == id) {
                    return index
                }
            }
            return -1
        }

        private fun isSelected(tracker: SelectionTracker<String>, position: Int): Boolean {
            return tracker.isSelected(getItemAtPosition(position).id.toString())
        }

        fun getItemAtPosition(position: Int): Profile {
            return getItem(position)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileHolder {
            return ProfileHolder(
                ProfileItemViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: ProfileHolder, @SuppressLint("RecyclerView") position: Int) {
            val profile = getItem(position)
            tracker.let {
                var animate: Boolean = true
                if (isSelected(tracker, position)) {
                    if (selectedItems.contains(profile.id.toString())) {
                        animate = false
                    } else {
                        selectedItems.add(profile.id.toString())
                    }
                } else {
                    selectedItems.remove(profile.id.toString())
                }
                holder.bind(profile, it.isSelected(getItemAtPosition(position).id.toString()), animate)
            }
        }

        override fun getItemKey(position: Int): String {
            return getItemAtPosition(position).id.toString()
        }

        override fun getPosition(key: String): Int {
            return currentList.indexOfFirst { key == it.id.toString() }
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
        for (i in tracker.selection) {
            val id: UUID = UUID.fromString(i)
            for ((index, j) in profileAdapter.currentList.withIndex()) {
                if (j.id == id) {
                    viewModel.removeProfile(j)
                }
            }
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