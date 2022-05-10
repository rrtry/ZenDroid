package com.example.volumeprofiler.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.volumeprofiler.core.GeofenceManager
import com.example.volumeprofiler.core.PreferencesManager
import com.example.volumeprofiler.core.ProfileManager
import com.example.volumeprofiler.core.ScheduleManager
import com.example.volumeprofiler.entities.LocationRelation
import com.example.volumeprofiler.interfaces.DetailsViewContract
import com.example.volumeprofiler.util.ViewUtil.Companion.showSnackbar
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG

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
    private var elapsedTime: Long = 0

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
        }.invokeOnCompletion { finish() }
    }

    override fun onInsert(profile: Profile) {
        lifecycleScope.launch {
            viewModel.addProfile(profile)
        }.invokeOnCompletion { finish() }
    }

    override fun onCancel() {
        finish()
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
            showExplanationDialog = it.getBoolean(EXTRA_SHOW_DIALOG, false)
        }

        openProfileDetailsFragment()
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.activityEventsFlow.collect {
                        when (it) {
                            is ShowDialogFragment -> showDialog()
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
            AnimUtil.scale(binding.menuSaveChangesButton, !it)
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
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.fragmentContainer, InterruptionFilterFragment(), null)
            .addToBackStack(null)
            .commit()
    }

    private fun showDialog() {
        ProfileNameInputDialog()
            .show(supportFragmentManager, null)
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