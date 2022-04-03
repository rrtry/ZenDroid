package com.example.volumeprofiler.fragments

import android.annotation.TargetApi
import android.app.NotificationManager.Policy.*
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.activityViewModels
import com.example.volumeprofiler.R
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.viewmodels.ProfileDetailsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@TargetApi(Build.VERSION_CODES.P)
class SuppressedEffectsOffDialog : BaseDialog() {

    private val viewModel: ProfileDetailsViewModel by activityViewModels()

    override val title: String = "When screen is off"
    override val arrayRes: Int = R.array.screenIsOff
    override val categories: List<Int> = listOf(
        SUPPRESSED_EFFECT_LIGHTS,
        SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
        SUPPRESSED_EFFECT_AMBIENT
    )

    override fun applyChanges(mask: Int) {
        viewModel.suppressedVisualEffects.value = mask
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