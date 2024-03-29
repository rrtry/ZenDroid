package ru.rrtry.silentdroid.ui.fragments

import android.annotation.TargetApi
import android.app.NotificationManager.Policy.*
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import ru.rrtry.silentdroid.R
import ru.rrtry.silentdroid.entities.Profile
import ru.rrtry.silentdroid.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@TargetApi(Build.VERSION_CODES.P)
class SuppressedEffectsOffDialog : BaseDialog() {

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    override val titleRes: Int = R.string.notification_restrictions_when_screen_is_off_title
    override val arrayRes: Int = R.array.notification_restrictions_when_screen_is_off
    override val values: List<Int> = listOf(
        SUPPRESSED_EFFECT_LIGHTS,
        SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
        SUPPRESSED_EFFECT_AMBIENT
    )

    override fun applyChanges(mask: Int) {
        viewModel.suppressedVisualEffects.value = mask
    }

    override fun onValueAdded(position: Int, value: Int) {

    }

    override fun onValueRemoved(position: Int, value: Int) {

    }

    companion object {

        fun newInstance(profile: Profile): SuppressedEffectsOffDialog {
            return SuppressedEffectsOffDialog().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_MASK, profile.suppressedVisualEffects)
                }
            }
        }
    }
}