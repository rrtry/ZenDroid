package com.example.volumeprofiler.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
import android.util.Log
import android.view.View
import android.widget.Toast
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
import com.example.volumeprofiler.models.AlarmRelation
import com.example.volumeprofiler.models.Profile
import com.example.volumeprofiler.util.AlarmUtil
import com.example.volumeprofiler.util.ProfileUtil
import com.example.volumeprofiler.util.SharedPreferencesUtil
import com.example.volumeprofiler.util.animations.AnimUtil
import com.example.volumeprofiler.viewmodels.EditProfileViewModel
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class EditProfileActivity: AppCompatActivity(), EditProfileActivityCallbacks, ActivityCompat.OnRequestPermissionsResultCallback {

    @Inject lateinit var sharedPreferencesUtil: SharedPreferencesUtil
    @Inject lateinit var profileUtil: ProfileUtil
    @Inject lateinit var alarmUtil: AlarmUtil

    private var elapsedTime: Long = 0

    private lateinit var binding: CreateProfileActivityBinding

    private val viewModel by viewModels<EditProfileViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setArgs()
        setNotificationPolicyProperty()
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
                    is EditProfileViewModel.Event.ShowDialogFragment -> {
                        if (it.dialogType == EditProfileViewModel.DialogType.TITLE) {
                            showTitleInputDialog()
                        }
                    }
                    is EditProfileViewModel.Event.SaveChangesEvent -> {
                        var granted: Boolean = true

                        if (!profileUtil.arePermissionsGranted()) {
                            granted = false
                            requestPermissions()
                        }
                        if (!profileUtil.isNotificationPolicyAccessGranted() && granted) {
                            granted = false
                            startNotificationPolicyActivity()
                        }
                        if (!profileUtil.canWriteSettings() && granted) {
                            granted = false
                            startWriteSettingsActivity()
                        }

                        if (granted) {
                            updateAlarms(it.profile)
                            applyProfileIfEnabled(it.profile)
                            setSuccessfulResult(it.profile, it.shouldUpdate)
                        }
                    }
                    else -> Log.i("EditProfileActivity", "unknown event")
                }
            }.collect()
        }
    }

    private fun requestPermissions(): Unit {
        ActivityCompat.requestPermissions(this@EditProfileActivity, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_PHONE_STATE
        ), 0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == 1) {
            viewModel.storagePermissionGranted.value = true
        }
    }

    private fun startNotificationPolicyActivity(): Unit {
        startActivity(Intent(ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    }

    private fun startWriteSettingsActivity(): Unit {
        startActivity(Intent(ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ELAPSED_TIME, elapsedTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        elapsedTime = savedInstanceState.getLong(KEY_ELAPSED_TIME, 0)
    }

    private fun setNotificationPolicyProperty(): Unit {
        viewModel.notificationPolicyAccessGranted.value = profileUtil.isNotificationPolicyAccessGranted()
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
                if (abs(verticalOffset) == appBarLayout.totalScrollRange) {
                    if (binding.menuEditNameButton.visibility == View.GONE || binding.menuEditNameButton.visibility == View.INVISIBLE) {
                        AnimUtil.scaleAnimation(binding.menuEditNameButton, true)
                    }
                } else if (verticalOffset == 0) {
                    if (binding.menuEditNameButton.visibility == View.VISIBLE) {
                        AnimUtil.scaleAnimation(binding.menuEditNameButton, false)
                    }
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
        if (fragment == DND_PREFERENCES_FRAGMENT) {
            replaceFragment(InterruptionFilterFragment())
            hideToolbarItems()
            changeFragmentTag(TAG_INTERRUPTIONS_FRAGMENT)
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount < 1) {
            if (elapsedTime + TIME_INTERVAL > System.currentTimeMillis()) {
                setCancelledResult()
            } else {
                Toast.makeText(this, "Press back button again to exit", Toast.LENGTH_SHORT).show()
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
        const val DND_PREFERENCES_FRAGMENT: Int = 0

        fun newIntent(context: Context, profile: Profile?): Intent {
            val intent = Intent(context, EditProfileActivity::class.java)
            if (profile != null) {
                intent.putExtra(EXTRA_PROFILE, profile)
            }
            return intent
        }
    }
}