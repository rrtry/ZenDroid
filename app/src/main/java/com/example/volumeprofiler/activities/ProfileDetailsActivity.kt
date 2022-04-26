package com.example.volumeprofiler.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.CreateProfileActivityBinding
import com.example.volumeprofiler.fragments.InterruptionFilterFragment
import com.example.volumeprofiler.fragments.ProfileDetailsFragment
import com.example.volumeprofiler.fragments.ProfileNameInputDialog
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel.ViewEvent.*
import androidx.lifecycle.repeatOnLifecycle
import com.example.volumeprofiler.fragments.PermissionDenialDialog
import com.example.volumeprofiler.interfaces.DetailsViewContract
import com.example.volumeprofiler.util.ViewUtil.Companion.showSnackbar
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.example.volumeprofiler.util.ui.animations.AnimUtil.getFabCollapseAnimation
import com.example.volumeprofiler.util.ui.animations.AnimUtil.getFabExpandAnimation
import com.example.volumeprofiler.util.ui.animations.AnimUtil.getMenuOptionAnimation
import com.example.volumeprofiler.util.ui.animations.AnimUtil.getOverlayAnimation
import com.example.volumeprofiler.util.ui.animations.SimpleAnimationListener
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import kotlin.math.abs

@AndroidEntryPoint
class ProfileDetailsActivity: AppCompatActivity(),
    EditProfileActivityCallbacks,
    ActivityCompat.OnRequestPermissionsResultCallback,
    DetailsViewContract<Profile>,
    FragmentManager.OnBackStackChangedListener,
    AppBarLayout.OnOffsetChangedListener {

    private val viewModel by viewModels<ProfileDetailsViewModel>()
    private lateinit var binding: CreateProfileActivityBinding

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var scheduleManager: ScheduleManager

    private var showExplanationDialog: Boolean = true
    private var elapsedTime: Long = 0
    private var isOverlayVisible: Boolean = false
    private var isAnimationRunning: Boolean = false

    private var scheduledAlarms: List<AlarmRelation>? = null

    private fun toggleOverlayMenuState() { /*
        if (!isAnimationRunning) {

            isAnimationRunning = true

            binding.overlay.isClickable = !isOverlayVisible
            binding.overlay.isFocusable = !isOverlayVisible

            val views: List<View> = listOf(
                binding.overlay,
                binding.changeProfileNameButton,
                binding.changeProfileNameLabel,
                binding.changeProfileIconButton,
                binding.changeProfileIconLabel,
                binding.saveChangesButton,
                binding.saveChangesLabel,
            )
            if (isOverlayVisible) {
                getFabCollapseAnimation(binding.expandableFab)
                    .start()
            } else {
                getFabExpandAnimation(binding.expandableFab)
                    .start()
            }
            for ((index, view) in views.withIndex()) {
                if (view is ViewGroup) {
                    view.startAnimation(getOverlayAnimation(view))
                } else {
                    view.startAnimation(
                        getMenuOptionAnimation(binding.expandableFab, view).apply {
                            setAnimationListener(object : SimpleAnimationListener() {

                                override fun onAnimationEnd(animation: Animation?) {
                                    view.visibility = if (view.visibility == View.VISIBLE) {
                                        View.INVISIBLE
                                    } else {
                                        View.VISIBLE
                                    }
                                    if (index == views.size - 1) {
                                        isAnimationRunning = false
                                    }
                                }
                            })
                        })
                }
            }
            isOverlayVisible = !isOverlayVisible */
        }

    override fun onBackStackChanged() {
        (supportFragmentManager.backStackEntryCount > 0).let {
            if (it) {
                binding.expandableFab.hide()
                binding.appBar.setExpanded(false, true)
            } else {
                binding.expandableFab.show()
                binding.appBar.setExpanded(true, true)
            }
            AnimUtil.scale(binding.menuSaveChangesButton, !it)
        }
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
        appBarLayout?.let {
            when {
                abs(verticalOffset) == it.totalScrollRange -> {
                    // AnimUtil.scale(binding.menuEditNameButton, true)
                }
                verticalOffset == 0 -> {
                    // AnimUtil.scale(binding.menuEditNameButton, false)
                }
            }
        }
    }

    private fun showExplanationDialog() {
        showExplanationDialog = false
        PermissionDenialDialog().apply {
            show(supportFragmentManager, null)
        }
    }

    private fun setEntity() {
        intent.extras?.getParcelable<Profile>(EXTRA_PROFILE)?.let {
            viewModel.setEntity(it, true)
            return
        }
        viewModel.setEntity(profileManager.getDefaultProfile(), false)
    }

    override fun onUpdate(item: Profile) {
        if (shouldShowPermissionSuggestion(this, item) &&
                showExplanationDialog) {
            showExplanationDialog()
        } else {
            if (preferencesManager.isProfileEnabled(item)) {
                profileManager.setProfile(item)
            }
            scheduledAlarms?.let {
                for (i in it) {
                    scheduleManager.scheduleAlarm(i.alarm, item)
                }
            }
            lifecycleScope.launch {
                viewModel.updateProfile(item)
            }.invokeOnCompletion {
                finish()
            }
        }
    }

    override fun onInsert(item: Profile) {
        if (shouldShowPermissionSuggestion(this, item) &&
                showExplanationDialog) {
            showExplanationDialog()
        } else {
            lifecycleScope.launch {
                viewModel.addProfile(item)
            }.invokeOnCompletion {
                finish()
            }
        }
    }

    override fun onCancel() {
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_ELAPSED_TIME, elapsedTime)
        outState.putBoolean(EXTRA_SHOW_DIALOG, showExplanationDialog)
        outState.putBoolean(EXTRA_OVERLAY_VISIBILITY, isOverlayVisible)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = CreateProfileActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
        setEntity()

        savedInstanceState?.let {
            elapsedTime = it.getLong(EXTRA_ELAPSED_TIME, 0)
            showExplanationDialog = it.getBoolean(EXTRA_SHOW_DIALOG, preferencesManager.showPermissionExplanationDialog())
        }

        binding.expandableFab.setOnClickListener {
            toggleOverlayMenuState()
        }

        openProfileDetailsFragment()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.activityEventsFlow.collect {
                        when (it) {
                            is ShowDialogFragment -> {
                                if (it.dialogType == ProfileDetailsViewModel.DialogType.TITLE) {
                                    showTitleInputDialog()
                                }
                            }
                            is OnUpdateProfileEvent -> {
                                onUpdate(it.profile)
                            }
                            is OnInsertProfileEvent -> {
                                onInsert(it.profile)
                            }
                            is OnRemoveProfileEvent -> {
                                onCancel()
                            }
                            else -> Log.i("EditProfileActivity", "unknown event")
                        }
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

    override fun onStart() {
        super.onStart()
        setFragmentResultListener()
        setNavigationListener()
        supportFragmentManager.addOnBackStackChangedListener(this)
        binding.appBar.apply {
            addOnOffsetChangedListener(this@ProfileDetailsActivity)
        }
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
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.fragmentContainer, InterruptionFilterFragment(), null)
            .addToBackStack(null)
            .commit()
    }

    private fun setNavigationListener() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener(INPUT_TITLE_REQUEST_KEY, this) { _, bundle: Bundle ->
            bundle.getString(EXTRA_TITLE)?.let {
                viewModel.title.value = it
            }
        }
    }

    private fun showTitleInputDialog() {
        ProfileNameInputDialog.newInstance(viewModel.title.value).apply {
            show(supportFragmentManager, null)
        }
    }

    override fun onFragmentReplace(fragment: Int) {
        if (fragment == INTERRUPTION_FILTER_FRAGMENT) {
            openInterruptionsFilterFragment()
        } else {
            supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount < 1) {
            if (elapsedTime + ViewUtil.DISMISS_TIME_WINDOW > System.currentTimeMillis()) {
                finish()
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
        private const val EXTRA_OVERLAY_VISIBILITY: String = "extra_overlay_visibility"

        const val TAG_PROFILE_FRAGMENT: String = "tag_profile_fragment"
        const val EXTRA_PROFILE = "extra_profile"
        const val INPUT_TITLE_REQUEST_KEY: String = "input_title_request_key"
        const val EXTRA_TITLE: String = "extra_title"
        const val INTERRUPTION_FILTER_FRAGMENT: Int = 0

        fun newIntent(context: Context, profile: Profile?): Intent {
            return Intent(context, ProfileDetailsActivity::class.java).apply {
                profile?.let {
                    putExtra(EXTRA_PROFILE, it)
                }
            }
        }
    }
}