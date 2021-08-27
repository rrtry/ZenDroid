package com.example.volumeprofiler.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.volumeprofiler.R
import com.example.volumeprofiler.databinding.CreateProfileActivityBinding
import com.example.volumeprofiler.fragments.ZenModePreferencesFragment
import com.example.volumeprofiler.fragments.EditProfileFragment
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import com.example.volumeprofiler.models.Profile

class EditProfileActivity: AppCompatActivity(), EditProfileActivityCallbacks {

    private var elapsedTime: Long = 0
    private lateinit var binding: CreateProfileActivityBinding

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_ELAPSED_TIME, elapsedTime)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        elapsedTime = savedInstanceState.getLong(KEY_ELAPSED_TIME, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = CreateProfileActivityBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val currentFragment: Fragment? = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment == null) {
            addRootFragment()
        }
    }

    private fun popFromBackStack(): Unit {
        supportFragmentManager.popBackStackImmediate()
    }

    private fun replaceFragment(fragment: Fragment): Unit {
        supportFragmentManager
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
    }

    private fun addRootFragment(): Unit {
        val extras: Profile? = intent.extras?.getParcelable(EXTRA_PROFILE) as? Profile
        val fragment: EditProfileFragment = EditProfileFragment.newInstance(extras)
        supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .commit()
    }

    override fun onFragmentReplace(fragment: Int): Unit {
        if (fragment == DND_PREFERENCES_FRAGMENT) {
            replaceFragment(ZenModePreferencesFragment())
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount < 1) {
            if (elapsedTime + TIME_INTERVAL > System.currentTimeMillis()) {
                super.onBackPressed()
            } else {
                Toast.makeText(this, "Press back button again to exit", Toast.LENGTH_SHORT).show()
            }
            elapsedTime = System.currentTimeMillis()
        }
        else {
            super.onBackPressed()
        }
    }

    override fun onPopBackStack() {
        popFromBackStack()
    }

    override fun getBinding(): CreateProfileActivityBinding {
        return binding
    }

    companion object {

        private const val KEY_ELAPSED_TIME: String = "key_elapsed_time"
        const val EXTRA_PROFILE = "extra_profile"
        const val DND_PREFERENCES_FRAGMENT: Int = 0
        private const val TIME_INTERVAL: Int = 2000

        fun newIntent(context: Context, profile: Profile?): Intent {
            val intent = Intent(context, EditProfileActivity::class.java)
            if (profile != null) {
                intent.putExtra(EXTRA_PROFILE, profile)
            }
            return intent
        }
    }
}