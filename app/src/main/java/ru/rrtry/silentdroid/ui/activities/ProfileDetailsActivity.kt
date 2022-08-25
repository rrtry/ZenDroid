package ru.rrtry.silentdroid.ui.activities

import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import androidx.activity.viewModels
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.rrtry.silentdroid.core.*
import ru.rrtry.silentdroid.databinding.CreateProfileActivityBinding
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class ProfileDetailsActivity: DetailsTransitionActivity(),
    ProfileDetailsActivityCallback,
    ActivityCompat.OnRequestPermissionsResultCallback,
    DetailsViewContract<Profile>,
    FragmentManager.OnBackStackChangedListener,
    AppBarLayout.OnOffsetChangedListener {

    override val slideDirection: Int get() = Gravity.END

    private val viewModel by viewModels<ProfileDetailsViewModel>()
    private lateinit var binding: CreateProfileActivityBinding

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var appAudioManager: AppAudioManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var scheduleManager: ScheduleManager
    @Inject lateinit var geofenceManager: GeofenceManager
    @Inject lateinit var appNotificationManager: AppNotificationManager

    private var showFixedVolumeSnackbar: Boolean = true
    private var elapsedTime: Long = 0L
    private var verticalOffset: Int = 0
    private val withTransition: Boolean
    get() = intent.extras?.get(EXTRA_PROFILE) != null

    private var scheduledAlarms: List<AlarmRelation>? = null
    private var registeredGeofences: List<LocationRelation>? = null

    override fun onUpdate(profile: Profile) {
        if (profileManager.isProfileSet(profile)) {
            profileManager.setProfile(profile, true)
            appNotificationManager.updateNotification(
                profile,
                scheduleManager.getPreviousAndNextTriggers(scheduledAlarms))
        }

        lifecycleScope.launch {
            viewModel.updateProfile(profile)
            scheduledAlarms = viewModel.getScheduledAlarms()
            registeredGeofences = viewModel.getRegisteredGeofences()
        }.invokeOnCompletion {
            geofenceManager.updateGeofences(registeredGeofences, profile)
            scheduleManager.updateAlarms(scheduledAlarms, profile)
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

        savedInstanceState?.let {
            elapsedTime = it.getLong(EXTRA_ELAPSED_TIME, 0)
            showFixedVolumeSnackbar = it.getBoolean(EXTRA_SHOW_FIXED_VOLUME_HINT, false)
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
                    viewModel.geofencesFlow.collect { geofences ->
                        registeredGeofences = geofences
                    }
                }
                launch {
                    viewModel.alarmsFlow.collect { alarms ->
                        scheduledAlarms = alarms
                    }
                }
            }
        }
    }

    override fun onBackStackChanged() {
        (supportFragmentManager.backStackEntryCount > 0).let { hasBackStack ->
            if (hasBackStack) {
                binding.appBar.setExpanded(false, true)
            } else {
                binding.appBar.setExpanded(true, true)
            }
            Animations.scale(binding.toolbarEditTitleButton, !hasBackStack)
        }
    }

    private fun setEntity() {
        viewModel.setEntity(
            intent.getParcelableExtra<Profile>(EXTRA_PROFILE) ?: profileManager.getDefaultProfile(),
            intent.extras != null
        )
        viewModel.notificationStreamIndependent.value = appAudioManager.isNotificationStreamIndependent()
        viewModel.isVolumeFixed.value = appAudioManager.isVolumeFixed
        viewModel.isVoiceCapable.value = appAudioManager.isVoicePlatform
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_ELAPSED_TIME, elapsedTime)
        outState.putBoolean(EXTRA_SHOW_FIXED_VOLUME_HINT, showFixedVolumeSnackbar)
    }

    override fun onStart() {
        super.onStart()
        binding.toolbar.setNavigationOnClickListener { onBack() }
        binding.appBar.addOnOffsetChangedListener(this)
        supportFragmentManager.addOnBackStackChangedListener(this)
    }

    override fun onResume() {
        super.onResume()
        if (appAudioManager.isVolumeFixed &&
            showFixedVolumeSnackbar)
        {
            showSnackbar(
                binding.coordinatorLayout,
                resources.getString(R.string.fixed_volume_policy),
                Snackbar.LENGTH_INDEFINITE
            )
            showFixedVolumeSnackbar = false
        }
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
            .replace(R.id.fragmentContainer, InterruptionFilterFragment(), null)
            .addToBackStack(null)
            .commit()
    }

    private fun showDialog(type: DialogType) {
        if (type == PROFILE_TITLE) {
            ProfileNameInputDialog().show(supportFragmentManager, null)
        } else if (type == PROFILE_IMAGE) {
            ProfileImageSelectionDialog().show(supportFragmentManager, null)
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

    override fun onBack() {
        if (supportFragmentManager.backStackEntryCount >= 1) {
            supportFragmentManager.popBackStack()
            return
        }
        if (elapsedTime + DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
            onFinish(false)
        } else {
            showSnackbar(binding.root, resources.getString(R.string.confirm_change_dismissal), LENGTH_LONG)
        }
        elapsedTime = System.currentTimeMillis()
    }

    companion object {

        private const val EXTRA_ELAPSED_TIME: String = "key_elapsed_time"
        private const val EXTRA_SHOW_FIXED_VOLUME_HINT: String = "extra_show_fixed_volume_hint"
        private const val DELAY: Long = 700

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