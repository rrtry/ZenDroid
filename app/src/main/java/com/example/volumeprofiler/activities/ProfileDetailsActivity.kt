package com.example.volumeprofiler.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
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
import com.example.volumeprofiler.util.ui.animations.AnimUtil
import com.google.android.material.appbar.AppBarLayout
import kotlin.math.abs

@AndroidEntryPoint
class ProfileDetailsActivity: AppCompatActivity(),
    EditProfileActivityCallbacks,
    ActivityCompat.OnRequestPermissionsResultCallback,
    DetailsViewContract<Profile>,
    FragmentManager.OnBackStackChangedListener,
    AppBarLayout.OnOffsetChangedListener,
    AppBarLayout.LiftOnScrollListener {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var profileManager: ProfileManager
    @Inject lateinit var scheduleManager: ScheduleManager

    private var showExplanationDialog: Boolean = true
    private var elapsedTime: Long = 0
    private var scheduledAlarms: List<AlarmRelation>? = null

    private lateinit var binding: CreateProfileActivityBinding

    private val viewModel by viewModels<ProfileDetailsViewModel>()

    override fun onBackStackChanged() {
        (supportFragmentManager.backStackEntryCount > 0).let {
            if (it) {
                binding.appBar.setExpanded(false, true)
                binding.appBar.removeOnOffsetChangedListener(this)
            } else {
                binding.appBar.setExpanded(true, true)
                binding.appBar.addOnOffsetChangedListener(this)
            }
            AnimUtil.scale(binding.menuSaveChangesButton, !it)
            AnimUtil.scale(binding.editNameButton, !it)
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

    override fun onUpdate(elevation: Float, backgroundColor: Int) {
        Log.i("DetailsActivity", "elevation: $elevation")
    }

    private fun showExplanationDialog(): Unit {
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
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setEntity()
        showExplanationDialog = preferencesManager.showPermissionExplanationDialog()

        savedInstanceState?.let {
            elapsedTime = it.getLong(EXTRA_ELAPSED_TIME, 0)
            showExplanationDialog = it.getBoolean(EXTRA_SHOW_DIALOG, false)
        }

        binding = CreateProfileActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

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
            addLiftOnScrollListener(this@ProfileDetailsActivity)
            addOnOffsetChangedListener(this@ProfileDetailsActivity)
        }
    }

    override fun onStop() {
        super.onStop()
        supportFragmentManager.removeOnBackStackChangedListener(this)
    }

    private fun openProfileDetailsFragment(): Unit {
        val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, ProfileDetailsFragment(), TAG_PROFILE_FRAGMENT)
                .commit()
        }
    }

    private fun openInterruptionsFilterFragment(): Unit {
        supportFragmentManager
            .beginTransaction()
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
            .replace(R.id.fragmentContainer, InterruptionFilterFragment(), null)
            .addToBackStack(null)
            .commit()
    }

    private fun setNavigationListener(): Unit {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setFragmentResultListener(): Unit {
        supportFragmentManager.setFragmentResultListener(INPUT_TITLE_REQUEST_KEY, this) { _, bundle: Bundle ->
            viewModel.title.value = bundle.getString(EXTRA_TITLE)!!
        }
    }

    private fun showTitleInputDialog(): Unit {
        val title: String = viewModel.title.value
        val dialog: DialogFragment = ProfileNameInputDialog.newInstance(title)
        dialog.show(supportFragmentManager, null)
    }

    override fun onFragmentReplace(fragment: Int): Unit {
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
                Toast.makeText(this, "Press back button again to exit", Toast.LENGTH_LONG).show()
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
        const val TAG_INTERRUPTIONS_FRAGMENT: String = "tag_interruptions_fragment"
        const val EXTRA_PROFILE = "extra_profile"
        const val INPUT_TITLE_REQUEST_KEY: String = "input_title_request_key"
        const val EXTRA_TITLE: String = "extra_title"
        const val INTERRUPTION_FILTER_FRAGMENT: Int = 0

        fun newIntent(context: Context, profile: Profile?): Intent {
            val intent = Intent(context, ProfileDetailsActivity::class.java)
            if (profile != null) {
                intent.putExtra(EXTRA_PROFILE, profile)
            }
            return intent
        }
    }
}