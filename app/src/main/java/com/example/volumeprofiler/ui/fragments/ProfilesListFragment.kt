package com.example.volumeprofiler.ui.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.example.volumeprofiler.viewmodels.ProfilesListViewModel.ViewEvent.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.*
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.ListAdapter
import androidx.transition.*
import com.example.volumeprofiler.ui.activities.ProfileDetailsActivity
import com.example.volumeprofiler.adapters.ItemDetails
import com.example.volumeprofiler.databinding.ProfileItemViewBinding
import com.example.volumeprofiler.databinding.ProfilesListFragmentBinding
import com.example.volumeprofiler.eventBus.EventBus
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.ui.Animations
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
import com.example.volumeprofiler.R
import com.example.volumeprofiler.ui.activities.ProfileDetailsActivity.Companion.EXTRA_PROFILE
import com.example.volumeprofiler.adapters.BaseSelectionObserver
import com.example.volumeprofiler.adapters.DetailsLookup
import com.example.volumeprofiler.adapters.KeyProvider
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.PreferencesManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.interfaces.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.lang.ref.WeakReference
import kotlin.NoSuchElementException

@AndroidEntryPoint
class ProfilesListFragment: Fragment(),
    ActionModeProvider<String>,
    FabContainer,
    FragmentSwipedListener {

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
                                    profileManager.setDefaultProfile()
                                    profileAdapter.setSelection(null)
                                } else {
                                    profileManager.setProfile(it.profile)
                                    profileAdapter.setSelection(it.profile)
                                }
                            }
                            is CancelAlarmsViewEvent -> {
                                scheduleManager.cancelAlarms(it.alarms)
                            }
                            is RemoveGeofencesViewEvent -> {
                                it.geofences.forEach { i ->
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
                                    profileAdapter.setSelection(profile)
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

    private inner class ProfileHolder(private val binding: ProfileItemViewBinding):
            RecyclerView.ViewHolder(binding.root),
            ViewHolderItemDetailsProvider<String>,
            View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
            binding.expandableView.visibility = View.GONE
        }

        override fun getItemDetails(): ItemDetailsLookup.ItemDetails<String> {
            return ItemDetails(bindingAdapterPosition, profileAdapter.getItemAtPosition(bindingAdapterPosition).id.toString())
        }

        private fun expand(animate: Boolean) {
            if (animate) {
                TransitionManager.beginDelayedTransition(this@ProfilesListFragment.binding.root, AutoTransition())
                binding.expandButton.animate().rotation(180.0f).start()
            } else {
                binding.expandButton.rotation = 180f
            }
            binding.itemSeparator.visibility = View.VISIBLE
            binding.expandableView.visibility = View.VISIBLE
        }

        private fun collapse() {
            TransitionManager.beginDelayedTransition(this@ProfilesListFragment.binding.root, AutoTransition())
            binding.itemSeparator.visibility = View.GONE
            binding.expandableView.visibility = View.GONE
            binding.expandButton.animate().rotation(0f).start()
        }

        private fun setViewScale(isSelected: Boolean) {
            val scale: Float = if (isSelected) 0.8f else 1.0f
            itemView.scaleX = scale
            itemView.scaleY = scale
        }

        fun bind(profile: Profile, isSelected: Boolean, animate: Boolean) {

            binding.checkBox.text = profile.title

            if (animate) {
                Animations.selected(itemView, isSelected)
            } else {
                setViewScale(isSelected)
            }
            preferencesManager.isProfileEnabled(profile).let {
                binding.checkBox.isChecked = it
                if (it) {
                    viewModel.lastSelected = profile.id
                }
            }
            binding.expandButton.setOnClickListener {
                if (binding.expandableView.isVisible) {
                    collapse()
                } else {
                    expand(true)
                }
            }
            binding.editProfileButton.setOnClickListener {
                startActivity(Intent(
                    requireContext(),
                    ProfileDetailsActivity::class.java
                ).apply {
                    putExtra(EXTRA_PROFILE, profile)
                })
            }
            binding.removeProfileButton.setOnClickListener {
                viewModel.removeProfile(profile)
            }
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

        fun setSelection(profile: Profile?) {
            profile?.id?.also {
                updatePreviousProfileView()
                updateCurrentProfileView(it)
                viewModel.lastSelected = it
                return
            }
            updatePreviousProfileView()
            viewModel.lastSelected = null
        }

        private fun updatePreviousProfileView() {
            viewModel.lastSelected?.let {
                notifyItemChanged(getPosition(it), true)
            }
        }

        private fun updateCurrentProfileView(uuid: UUID) {
            notifyItemChanged(getPosition(uuid), true)
        }

        private fun getPosition(id: UUID): Int {
            return currentList.indexOfFirst {
                it.id == id
            }
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
            holder.bind(
                getItem(position),
                isSelected(tracker, position),
                true)
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