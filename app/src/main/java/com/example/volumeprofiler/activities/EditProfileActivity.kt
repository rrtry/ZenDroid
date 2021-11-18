package com.example.volumeprofiler.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.CreateProfileActivityBinding
import com.example.volumeprofiler.fragments.InterruptionFilterFragment
import com.example.volumeprofiler.fragments.EditProfileFragment
import com.example.volumeprofiler.fragments.dialogs.ProfileNameInputDialog
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.entities.AlarmRelation
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.util.*
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
import com.example.volumeprofiler.viewmodels.EditProfileViewModel.Event.*
import android.Manifest.permission.*
import android.os.Build
import com.google.android.material.snackbar.Snackbar

@AndroidEntryPoint
class EditProfileActivity: AppCompatActivity(), EditProfileActivityCallbacks, ActivityCompat.OnRequestPermissionsResultCallback {

    @Inject
    lateinit var sharedPreferencesUtil: SharedPreferencesUtil

    @Inject
    lateinit var profileUtil: ProfileUtil

    @Inject
    lateinit var alarmUtil: AlarmUtil

    private var elapsedTime: Long = 0

    private lateinit var binding: CreateProfileActivityBinding

    private val viewModel by viewModels<EditProfileViewModel>()

    private lateinit var permissionRequestLauncher: ActivityResultLauncher<Array<out String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val profile: Profile = viewModel.getProfile()
            if (checkSelfPermission(this, READ_EXTERNAL_STORAGE)) {
                viewModel.updateSoundUris()
            }
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
                    saveProfile(profile, viewModel.shouldUpdateProfile(), false)
                }
            }
        }
        setArgs()
        setBinding()
        setContentView(binding.root)
        addFragment()
        if (supportFragmentManager.fragments.isNotEmpty() && supportFragmentManager.fragments.last().tag != TAG_PROFILE_FRAGMENT) {
            hideToolbarItems()
        }
        collectEventsFlow()
    }

    private fun collectEventsFlow(): Unit {
        lifecycleScope.launch {
            viewModel.activityEventsFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).onEach {
                when (it) {
                    is ShowDialogFragment -> {
                        if (it.dialogType == EditProfileViewModel.DialogType.TITLE) {
                            showTitleInputDialog()
                        }
                    }
                    is SaveChangesEvent -> {
                        saveProfile(it.profile, it.shouldUpdate)
                    }
                    else -> Log.i("EditProfileActivity", "unknown event")
                }
            }.collect()
        }
    }

    private fun requestPermissions(profile: Profile): Unit {
        var permissions: Array<String> = arrayOf(READ_EXTERNAL_STORAGE)
        if (profile.streamsUnlinked) {
            permissions += READ_PHONE_STATE
        }
        permissionRequestLauncher.launch(permissions)
    }

    private fun saveProfile(profile: Profile, update: Boolean, resolveMissingPermissions: Boolean = true): Unit {
        when {
            profileUtil.grantedRequiredPermissions(
                true,
                viewModel.usesUnlinkedStreams()
            ) -> {
                applyProfileIfEnabled(profile)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ELAPSED_TIME, elapsedTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        elapsedTime = savedInstanceState.getLong(KEY_ELAPSED_TIME, 0)
    }

    private fun setSuccessfulResult(profile: Profile, updateFlag: Boolean): Unit {
        setResult(Activity.RESULT_OK, Intent().apply {
            this.putExtra(EXTRA_PROFILE, profile)
            this.putExtra(EXTRA_SHOULD_UPDATE, updateFlag)
        })
        finish()
    }

    private fun setCancelledResult(): Unit {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun onStart() {
        super.onStart()
        setFragmentResultListener()
        setOffsetListener()
        setNavigationListener()
    }

    private fun setNavigationListener(): Unit {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setOffsetListener(): Unit {
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (supportFragmentManager.fragments.last().tag == TAG_PROFILE_FRAGMENT) {
                if (binding.menuSaveChangesButton.visibility != View.VISIBLE) {
                    revealToolbarItems()
                }
                if (abs(verticalOffset) == appBarLayout.totalScrollRange &&
                    binding.menuEditNameButton.visibility == View.INVISIBLE) {
                    AnimUtil.scaleAnimation(binding.menuEditNameButton, true)
                }
                else if (verticalOffset == 0 && binding.menuEditNameButton.visibility == View.VISIBLE) {
                    AnimUtil.scaleAnimation(binding.menuEditNameButton, false)
                }
            }
        })
    }

    private fun setFragmentResultListener(): Unit {
        supportFragmentManager.setFragmentResultListener(INPUT_TITLE_REQUEST_KEY, this) { _, bundle: Bundle ->
            viewModel.title.value = bundle.getString(EXTRA_TITLE)
        }
    }

    private fun showTitleInputDialog(): Unit {
        val title: String = viewModel.title.value!!
        val dialog: DialogFragment = ProfileNameInputDialog.newInstance(title)
        dialog.show(supportFragmentManager, null)
    }

    private fun setBinding(): Unit {
        binding = CreateProfileActivityBinding.inflate(layoutInflater)
        binding.viewModel = this.viewModel
        binding.lifecycleOwner = this
    }

    private fun setArgs(): Unit {
        if (intent.extras != null) {
            val arg: Profile = intent.extras!!.getParcelable(EXTRA_PROFILE)!!
            viewModel.setArgs(arg, true)
        } else {
            viewModel.setArgs(Profile("New profile"), false)
        }
    }

    private fun popFromBackStack(): Unit {
        supportFragmentManager.popBackStackImmediate()
    }

    private fun replaceFragment(fragment: Fragment): Unit {
        supportFragmentManager
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragmentContainer, fragment, TAG_INTERRUPTIONS_FRAGMENT)
                .addToBackStack(null)
                .commit()
    }

    private fun addFragment(): Unit {
        val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment == null) {
            addRootFragment()
        }
    }

    private fun addRootFragment(): Unit {
        val extras: Profile? = intent.extras?.getParcelable(EXTRA_PROFILE) as? Profile
        val fragment: EditProfileFragment = EditProfileFragment.newInstance(extras)
        supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, fragment, TAG_PROFILE_FRAGMENT)
                .commit()
    }

    private fun hideToolbarItems(): Unit {
        if (binding.menuSaveChangesButton.visibility == View.VISIBLE) {
            AnimUtil.scaleAnimation(binding.menuSaveChangesButton, false)
        }
        if (binding.menuEditNameButton.visibility == View.VISIBLE) {
            AnimUtil.scaleAnimation(binding.menuEditNameButton, false)
        }
    }

    private fun revealToolbarItems(): Unit {
        if (binding.menuSaveChangesButton.visibility == View.INVISIBLE) {
            AnimUtil.scaleAnimation(binding.menuSaveChangesButton, true)
        }
        if (binding.menuEditNameButton.visibility == View.INVISIBLE) {
            AnimUtil.scaleAnimation(binding.menuEditNameButton, true)
        }
    }

    override fun onFragmentReplace(fragment: Int): Unit {
        when (fragment) {
            DND_PREFERENCES_FRAGMENT -> {
                replaceFragment(InterruptionFilterFragment())
                hideToolbarItems()
                changeFragmentTag(TAG_INTERRUPTIONS_FRAGMENT)
            }
            else -> {
                popFromBackStack()
                changeFragmentTag(TAG_PROFILE_FRAGMENT)
            }
        }
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
            changeFragmentTag(TAG_PROFILE_FRAGMENT)
            super.onBackPressed()
        }
    }

    private fun changeFragmentTag(tag: String): Unit {
        viewModel.currentFragmentTag.value = tag
    }

    override fun onPopBackStack() {
        popFromBackStack()
    }

    private fun setAlarms(relations: List<AlarmRelation>, newProfile: Profile): Unit {
        for (i in relations) {
            alarmUtil.scheduleAlarm(i.alarm, newProfile, false)
        }
    }

    private fun updateAlarms(profile: Profile): Unit {
        viewModel.getAlarms().let {
            if (!it.isNullOrEmpty()) {
                setAlarms(it, profile)
            }
        }
    }

    private fun applyProfileIfEnabled(profile: Profile): Unit {
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
        const val EXTRA_SHOULD_UPDATE: String = "extra_should_update"
        const val INPUT_TITLE_REQUEST_KEY: String = "input_title_request_key"
        const val EXTRA_TITLE: String = "extra_title"
        const val DND_PREFERENCES_FRAGMENT: Int = 0x00
        const val PROFILE_FRAGMENT: Int = 0x01

        fun newIntent(context: Context, profile: Profile?): Intent {
            val intent = Intent(context, EditProfileActivity::class.java)
            if (profile != null) {
                intent.putExtra(EXTRA_PROFILE, profile)
            }
            return intent
        }
    }
}