package ru.rrtry.silentdroid.ui.activities

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionSet
import android.util.Log
import android.view.Gravity
import android.view.Window
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_OPEN
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.entities.AlarmRelation
import ru.rrtry.silentdroid.entities.LocationRelation
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.interfaces.DetailsViewContract
import ru.rrtry.silentdroid.interfaces.ProfileDetailsActivityCallback
import ru.rrtry.silentdroid.ui.Animations
import ru.rrtry.silentdroid.ui.fragments.InterruptionFilterFragment
import ru.rrtry.silentdroid.ui.fragments.ProfileDetailsFragment
import ru.rrtry.silentdroid.ui.fragments.ProfileImageSelectionDialog
import ru.rrtry.silentdroid.ui.fragments.ProfileNameInputDialog
import ru.rrtry.silentdroid.util.ViewUtil.Companion.DISMISS_TIME_WINDOW
import ru.rrtry.silentdroid.util.ViewUtil.Companion.showSnackbar
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.*
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.DialogType.*
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel.ViewEvent.*
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.rrtry.silentdroid.core.*
import ru.rrtry.silentdroid.databinding.CreateProfileActivityBinding
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ProfileDetailsDetailsActivity: AppCompatActivity(),
    ProfileDetailsActivityCallback,
    ActivityCompat.OnRequestPermissionsResultCallback,
    DetailsViewContract<Profile>,
    FragmentManager.OnBackStackChangedListener,
    AppBarLayout.OnOffsetChangedListener {

    private val viewModel by viewModels<ProfileDetailsViewModel>()
    private lateinit var binding: CreateProfileActivityBinding

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var notificationHelper: NotificationHelper

    private var showExplanationDialog: Boolean = false
    private var elapsedTime: Long = 0L
    private var verticalOffset: Int = 0
    private val withTransition: Boolean
    get() = intent.extras?.get(EXTRA_PROFILE) != null

    private var scheduledAlarms: List<AlarmRelation>? = null
    private var registeredGeofences: List<LocationRelation>? = null

    override fun onUpdate(profile: Profile) {
        if (preferencesManager.isProfileEnabled(profile)) {
            profileManager.setProfile(profile, true)
            notificationHelper.updateNotification(
                profile,
                scheduleManager.getCurrentAlarmInstance(scheduledAlarms))
        }
        geofenceManager.updateGeofenceProfile(registeredGeofences, profile)
        scheduleManager.updateAlarmProfile(scheduledAlarms, profile)

        lifecycleScope.launch {
            viewModel.updateProfile(profile)
        }.invokeOnCompletion {
            onFinish(true)
        }
    }

    override fun onInsert(profile: Profile) {
        lifecycleScope.launch {
            viewModel.addProfile(profile)
        }.invokeOnCompletion {
            onFinish(false)
        }
    }

    override fun onFinish(result: Boolean) {
        val finish = { delay: Long ->
            Handler(Looper.getMainLooper()).postDelayed({
                detachFloatingActionButton()
                ActivityCompat.finishAfterTransition(this)
            }, delay)
        }
        val scroll: Boolean = isToolbarContentVisible() && withTransition
        val delay: Long = if (scroll) DELAY else 0

        if (scroll) {
            dispatchNestedScrollToTop()
        }
        finish(delay)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            elapsedTime = savedInstanceState.getLong(EXTRA_ELAPSED_TIME, 0)
            showExplanationDialog = savedInstanceState.getBoolean(EXTRA_SHOW_DIALOG, false)
        }

        window.requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS)
        with(window) {

            sharedElementEnterTransition = ChangeBounds()
            sharedElementExitTransition = ChangeBounds()

            TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                duration = 350
                addTransition(Fade())
                addTransition(Slide(Gravity.END))

                excludeTarget(android.R.id.statusBarBackground, true)
                excludeTarget(android.R.id.navigationBarBackground, true)

                enterTransition = this
                exitTransition = this
            }
            allowEnterTransitionOverlap = true
        }

        binding = CreateProfileActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setEntity()
        openProfileDetailsFragment()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.activityEventsFlow.collect {
                        when (it) {
                            is ShowDialogFragment -> showDialog(it.dialogType)
                            is OnUpdateProfileEvent -> onUpdate(it.profile)
                            is OnInsertProfileEvent -> onInsert(it.profile)
                            is OnRemoveProfileEvent -> onFinish(false)
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
        binding.appBar.addOnOffsetChangedListener(this)
        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    override fun onStop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !isFinishing) {
            Instrumentation().callActivityOnSaveInstanceState(
                this, Bundle()
            )
        }
        super.onStop()
        binding.appBar.removeOnOffsetChangedListener(this)
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
            .setTransition(TRANSIT_FRAGMENT_OPEN)
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

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        this.verticalOffset = verticalOffset
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        ViewCompat.setNestedScrollingEnabled(binding.nestedScrollView, enabled)
    }

    private fun detachFloatingActionButton() {
        (binding.saveChangesButton.layoutParams as CoordinatorLayout.LayoutParams).apply {
            behavior = null
        }
        binding.saveChangesButton.hide()
    }

    private fun isToolbarContentVisible(): Boolean {
        return binding.toolbarLayout.scrimVisibleHeightTrigger + abs(verticalOffset) > binding.toolbarLayout.height
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
            if (elapsedTime + DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
                onFinish(false)
            } else {
                showSnackbar(binding.root, "Press back button again to exit", LENGTH_LONG)
            }
            elapsedTime = System.currentTimeMillis()
        } else {
            super.onBackPressed()
        }
    }

    companion object {

        private const val EXTRA_ELAPSED_TIME: String = "key_elapsed_time"
        private const val EXTRA_SHOW_DIALOG: String = "extra_show_dialog"
        private const val EXTRA_SAVED_STATE: String = "extra_saved_state"
        private const val DELAY: Long = 700

        const val TAG_PROFILE_FRAGMENT: String = "tag_profile_fragment"
        const val EXTRA_PROFILE = "extra_profile"
        const val INTERRUPTION_FILTER_FRAGMENT: Int = 0
        const val NOTIFICATION_RESTRICTIONS_FRAGMENT: Int = 1

        fun newIntent(context: Context, profile: Profile?): Intent {
            return Intent(context, ProfileDetailsDetailsActivity::class.java).apply {
                profile?.let {
                    putExtra(EXTRA_PROFILE, it)
                }
            }
        }
    }
}