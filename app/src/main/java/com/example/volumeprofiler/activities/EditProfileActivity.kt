package com.example.volumeprofiler.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.DnDPreferencesFragment
import com.example.volumeprofiler.fragments.EditProfileFragment
import com.example.volumeprofiler.interfaces.EditProfileActivityCallbacks
import java.util.*

class EditProfileActivity: AppCompatActivity(), EditProfileActivityCallbacks {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_profile_activity)
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
        val fragment: EditProfileFragment = EditProfileFragment.newInstance(
                intent.extras?.getSerializable(EXTRA_UUID) as? UUID)
        supportFragmentManager
                .beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .commit()
    }

    override fun onFragmentReplace(fragment: Int): Unit {
        if (fragment == DND_PREFERENCES_FRAGMENT) {
            replaceFragment(DnDPreferencesFragment())
        }
    }

    override fun onPopBackStack() {
        popFromBackStack()
    }

    companion object {

        const val EXTRA_UUID = "uuid"
        const val DND_PREFERENCES_FRAGMENT: Int = 0

        fun newIntent(context: Context, id: UUID?): Intent {
            val intent = Intent(context, EditProfileActivity::class.java)
            if (id != null) {
                intent.putExtra(EXTRA_UUID, id)
            }
            return intent
        }
    }
}