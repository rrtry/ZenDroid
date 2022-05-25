package com.example.volumeprofiler.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.Window
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.PreferencesManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.databinding.CreateProfileActivityBinding
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.interfaces.DetailsViewContract
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.ui.Animations
import com.example.volumeprofiler.ui.fragments.InterruptionFilterFragment
import com.example.volumeprofiler.ui.fragments.ProfileDetailsFragment
import com.example.volumeprofiler.ui.fragments.ProfileImageSelectionDialog
import com.example.volumeprofiler.ui.fragments.ProfileNameInputDialog
import com.example.volumeprofiler.ui.fragments.ProfilesListFragment.Companion.SHARED_TRANSITION_PROFILE_IMAGE
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.ViewUtil.Companion.showSnackbar
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel.*
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel.DialogType.*
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel.ViewEvent.*
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class ProfileDetailsActivity: AppCompatActivity(),
    EditProfileActivityCallbacks,
    ActivityCompat.OnRequestPermissionsResultCallback,
    DetailsViewContract<Profile>,
    FragmentManager.OnBackStackChangedListener {

    private val viewModel by viewModels<ProfileDetailsViewModel>()
    private lateinit var binding: CreateProfileActivityBinding

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var geofenceManager: GeofenceManager

    private var showExplanationDialog: Boolean = true
    private var elapsedTime: Long = 0L
    private var verticalOffset: Int = 0

    private var scheduledAlarms: List<AlarmRelation>? = null
    private var registeredGeofences: List<LocationRelation>? = null

    override fun onUpdate(profile: Profile) {
        if (preferencesManager.isProfileEnabled(profile)) {
            profileManager.setProfile(profile)
        }
        geofenceManager.updateGeofenceProfile(registeredGeofences, profile)
        scheduleManager.updateAlarmProfile(scheduledAlarms, profile)

        lifecycleScope.launch {
            viewModel.updateProfile(profile)
        }.invokeOnCompletion {
            onCancel()
        }
    }

    override fun onInsert(profile: Profile) {
        lifecycleScope.launch {
            viewModel.addProfile(profile)
        }.invokeOnCompletion {
            onCancel()
        }
    }

    override fun onCancel() {

        val finish = { delay: Long ->
            Handler(Looper.getMainLooper()).postDelayed({
                clearLayoutParams()
                ActivityCompat.finishAfterTransition(this)
            }, delay)
        }

        val scroll: Boolean = isViewBelowToolbar(binding.profileImage)
        val delay: Long = if (scroll) 500 else 0

        if (scroll) {
            dispatchNestedScrollToTop()
        }
        finish(delay)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setEntity()

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        with(window) {
            sharedElementEnterTransition = TransitionSet().addTransition(ChangeBounds())
            enterTransition = TransitionSet().apply {

                ordering = TransitionSet.ORDERING_TOGETHER
                duration = 350
                addTransition(Fade())
                addTransition(Slide(Gravity.END))

                excludeTarget(android.R.id.statusBarBackground, true)
                excludeTarget(android.R.id.navigationBarBackground, true)
            }
            allowEnterTransitionOverlap = true
        }

        binding = CreateProfileActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        ViewCompat.setTransitionName(binding.profileImage, SHARED_TRANSITION_PROFILE_IMAGE)

        savedInstanceState?.let {
            elapsedTime = it.getLong(EXTRA_ELAPSED_TIME, 0)
            showExplanationDialog = it.getBoolean(EXTRA_SHOW_DIALOG, false)
        }

        openProfileDetailsFragment()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.activityEventsFlow.collect {
                        when (it) {
                            is ShowDialogFragment -> showDialog(it.dialogType)
                            is OnUpdateProfileEvent -> onUpdate(it.profile)
                            is OnInsertProfileEvent -> onInsert(it.profile)
                            is OnRemoveProfileEvent -> onCancel()
                            else -> Log.i("EditProfileActivity", "unknown event")
                        }
                    }
                }
                launch {
                    viewModel.geofencesFlow.collect {
                        registeredGeofences = it
                    }
                }
                launch {
                    viewModel.alarmsFlow.collect {
                        scheduledAlarms = it
                    }
                }
            }
        }
    }

    override fun onBackStackChanged() {
        (supportFragmentManager.backStackEntryCount > 0).let {
            if (it) {
                binding.appBar.setExpanded(false, true)
            } else {
                binding.appBar.setExpanded(true, true)
            }
            Animations.scale(binding.toolbarEditTitleButton, !it)
        }
    }

    private fun setEntity() {
        intent.extras?.getParcelable<Profile>(EXTRA_PROFILE)?.let {
            viewModel.setEntity(it, true)
            return
        }
        viewModel.setEntity(profileManager.getDefaultProfile(), false)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_ELAPSED_TIME, elapsedTime)
        outState.putBoolean(EXTRA_SHOW_DIALOG, showExplanationDialog)
    }

    override fun onStart() {
        super.onStart()
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    override fun onStop() {
        super.onStop()
        supportFragmentManager.removeOnBackStackChangedListener(this)
    }

    private fun openProfileDetailsFragment() {
        val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, ProfileDetailsFragment(), TAG_PROFILE_FRAGMENT)
                .commit()
        }
    }

    private fun openInterruptionsFilterFragment() {
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .replace(R.id.fragmentContainer, InterruptionFilterFragment(), null)
            .addToBackStack(null)
            .commit()
    }

    private fun showDialog(type: DialogType) {
        if (type == PROFILE_TITLE) {
            ProfileNameInputDialog()
                .show(supportFragmentManager, null)
        } else if (type == PROFILE_IMAGE) {
            ProfileImageSelectionDialog()
                .show(supportFragmentManager, null)
        }
    }

    override fun onFragmentReplace(fragment: Int) {
        if (fragment == INTERRUPTION_FILTER_FRAGMENT) {
            openInterruptionsFilterFragment()
        }
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        ViewCompat.setNestedScrollingEnabled(binding.nestedScrollView, enabled)
    }

    private fun isViewBelowToolbar(view: View): Boolean {

        val minHeight: Int = binding.appBar.minimumHeightForVisibleOverlappingContent
        val descendantRect: Rect = Rect()

        binding.toolbarLayout.offsetDescendantRectToMyCoords(view, descendantRect)
        return descendantRect.bottom >= minHeight
    }

    private fun dispatchNestedScrollToTop() {

        val toolbarHeight: Int = binding.toolbar.height

        binding.nestedScrollView.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL)
        binding.nestedScrollView.dispatchNestedPreScroll(0, toolbarHeight, null, null)
        binding.nestedScrollView.dispatchNestedScroll(0, 0, 0, 0, intArrayOf(0, -toolbarHeight))
        binding.nestedScrollView.smoothScrollTo(0, binding.nestedScrollView.top - toolbarHeight)
        binding.appBar.setExpanded(true, true)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount < 1) {
            if (elapsedTime + ViewUtil.DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
                onCancel()
            } else {
                showSnackbar(binding.root, "Press back button again to exit", LENGTH_LONG)
            }
            elapsedTime = System.currentTimeMillis()
        } else super.onBackPressed()
    }

    private fun clearLayoutParams() {
        (binding.saveChangesButton.layoutParams as CoordinatorLayout.LayoutParams).apply {
            behavior = null
        }
        binding.saveChangesButton.hide()
    }

    companion object {

        private const val EXTRA_ELAPSED_TIME: String = "key_elapsed_time"
        private const val EXTRA_SHOW_DIALOG: String = "extra_show_dialog"

        const val TAG_PROFILE_FRAGMENT: String = "tag_profile_fragment"
        const val EXTRA_PROFILE = "extra_profile"
        const val INTERRUPTION_FILTER_FRAGMENT: Int = 0
        const val NOTIFICATION_RESTRICTIONS_FRAGMENT: Int = 1

        fun newIntent(context: Context, profile: Profile?): Intent {
            return Intent(context, ProfileDetailsActivity::class.java).apply {
                profile?.let {
                    putExtra(EXTRA_PROFILE, it)
                }
            }
        }
    }
}