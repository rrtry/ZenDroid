package com.example.volumeprofiler.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import android.media.RingtoneManager.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.CreateProfileActivityBinding
import com.example.volumeprofiler.fragments.InterruptionFilterFragment
import com.example.volumeprofiler.fragments.ProfileDetailsFragment
import com.example.volumeprofiler.fragments.dialogs.ProfileNameInputDialog
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel.Event.*
import android.Manifest.permission.*
import android.net.Uri
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class ProfileDetailsActivity: AppCompatActivity(), EditProfileActivityCallbacks, ActivityCompat.OnRequestPermissionsResultCallback {

    @Inject
    lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var alarmUtil: AlarmUtil

    private var elapsedTime: Long = 0
    private var scheduledAlarms: List<AlarmRelation>? = null

    private lateinit var permissionRequestLauncher: ActivityResultLauncher<Array<out String>>
    private lateinit var binding: CreateProfileActivityBinding

    private val viewModel by viewModels<ProfileDetailsViewModel>()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ELAPSED_TIME, elapsedTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        elapsedTime = savedInstanceState.getLong(KEY_ELAPSED_TIME, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setProfile()
        setBinding()
        openProfileDetailsFragment()
        collectEventsFlow()

        permissionRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val profile: Profile = viewModel.getProfile()
            when {
                !checkSelfPermission(this, READ_EXTERNAL_STORAGE) && !profileUtil.grantedRequiredPermissions(profile)-> {
                    Snackbar.make(binding.root, "Missing required permissions", Snackbar.LENGTH_LONG).show()
                }
                !checkSelfPermission(this, READ_EXTERNAL_STORAGE) -> {
                    ViewUtil.showStoragePermissionExplanation(supportFragmentManager)
                }
                profileUtil.shouldRequestPhonePermission(profile) -> {
                    ViewUtil.showPhoneStatePermissionExplanation(supportFragmentManager)
                }
                !profileUtil.grantedSystemPreferencesAccess() -> {
                    sendSystemPreferencesAccessNotification(this, profileUtil)
                }
                else -> {
                    updateMediaUris()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setFragmentResultListener()
        setNavigationListener()
    }

    private fun setBinding(): Unit {
        binding = CreateProfileActivityBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)
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

    private fun setProfile(): Unit {
        if (intent.extras != null) {
            val arg: Profile = intent.extras!!.getParcelable(EXTRA_PROFILE)!!
            viewModel.setArgs(arg, true)
        } else {
            viewModel.setArgs(Profile("New profile"), false)
        }
    }

    private fun collectEventsFlow(): Unit {
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
                            is ApplyChangesEvent -> {
                                applyChanges(it.profile, it.shouldUpdate)
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

    private fun requestPermissions(profile: Profile): Unit {
        var permissions: Array<String> = arrayOf(READ_EXTERNAL_STORAGE)
        if (profile.streamsUnlinked) {
            permissions += READ_PHONE_STATE
        }
        permissionRequestLauncher.launch(permissions)
    }

    private fun applyChanges(profile: Profile, update: Boolean, resolveMissingPermissions: Boolean = true): Unit {
        when {
            profileUtil.grantedRequiredPermissions(
                true,
                viewModel.usesUnlinkedStreams()
            ) -> {
                setProfile(profile)
                updateAlarms(profile)
                setSuccessfulResult(profile, update)
            }
            !checkSelfPermission(this, READ_EXTERNAL_STORAGE) || profileUtil.shouldRequestPhonePermission(profile) -> {
                if (resolveMissingPermissions) {
                    requestPermissions(profile)
                }
            }
            else -> {
                sendSystemPreferencesAccessNotification(this, profileUtil)
            }
        }
    }

    private fun setSuccessfulResult(profile: Profile, updateFlag: Boolean): Unit {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra(EXTRA_PROFILE, profile)
            putExtra(EXTRA_UPDATE, updateFlag)
        })
        finish()
    }

    private fun setCancelledResult(): Unit {
        setResult(Activity.RESULT_CANCELED)
        finish()
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
        when (fragment) {
            INTERRUPTION_FILTER_FRAGMENT -> {
                openInterruptionsFilterFragment()
            }
            else -> {
                supportFragmentManager.popBackStackImmediate()
            }
        }
    }

    override fun getBinding(): CreateProfileActivityBinding {
        return binding
    }

    override fun updateMediaUris() {
        viewModel.notificationSoundUri.value =
            if (viewModel.notificationUri != Uri.EMPTY) viewModel.notificationUri else getActualDefaultRingtoneUri(
                this,
                TYPE_NOTIFICATION
            )
        viewModel.phoneRingtoneUri.value =
            if (viewModel.ringtoneUri != Uri.EMPTY) viewModel.ringtoneUri else getActualDefaultRingtoneUri(
                this,
                TYPE_RINGTONE
            )
        viewModel.alarmSoundUri.value =
            if (viewModel.alarmUri != Uri.EMPTY) viewModel.alarmUri else getActualDefaultRingtoneUri(
                this,
                TYPE_ALARM
            )
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount < 1) {
            if (elapsedTime + TIME_INTERVAL > System.currentTimeMillis()) {
                setCancelledResult()
            } else {
                Toast.makeText(this, "Press back button again to exit", Toast.LENGTH_LONG).show()
            }
            elapsedTime = System.currentTimeMillis()
        }
        else {
            super.onBackPressed()
        }
    }

    private fun setAlarms(relations: List<AlarmRelation>, newProfile: Profile): Unit {
        for (i in relations) {
            alarmUtil.scheduleAlarm(i.alarm, newProfile)
        }
    }

    private fun updateAlarms(profile: Profile): Unit {
        scheduledAlarms.let {
            if (!it.isNullOrEmpty()) {
                setAlarms(it, profile)
            }
        }
    }

    private fun setProfile(profile: Profile): Unit {
        if (sharedPreferencesUtil.getEnabledProfileId() == profile.id.toString()) {
            profileUtil.setProfile(profile)
        }
    }

    companion object {


        private const val TIME_INTERVAL: Int = 2000
        private const val KEY_ELAPSED_TIME: String = "key_elapsed_time"

        const val TAG_PROFILE_FRAGMENT: String = "tag_profile_fragment"
        const val TAG_INTERRUPTIONS_FRAGMENT: String = "tag_interruptions_fragment"
        const val EXTRA_PROFILE = "extra_profile"
        const val EXTRA_UPDATE: String = "extra_update"
        const val INPUT_TITLE_REQUEST_KEY: String = "input_title_request_key"
        const val EXTRA_TITLE: String = "extra_title"
        const val INTERRUPTION_FILTER_FRAGMENT: Int = 0
        const val PROFILE_DETAILS_FRAGMENT: Int = 1

        fun newIntent(context: Context, profile: Profile?): Intent {
            val intent = Intent(context, ProfileDetailsActivity::class.java)
            if (profile != null) {
                intent.putExtra(EXTRA_PROFILE, profile)
            }
            return intent
        }
    }
}