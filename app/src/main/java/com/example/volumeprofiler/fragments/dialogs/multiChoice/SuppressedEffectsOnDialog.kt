package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.annotation.TargetApi
import android.app.NotificationManager.Policy.*
import android.os.Build
import android.os.Bundle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.entities.Profile
import com.example.volumeprofiler.fragments.InterruptionFilterFragment

@TargetApi(Build.VERSION_CODES.P)
class SuppressedEffectsOnDialog : BasePolicyPreferencesDialog() {

    override val title: String = "When screen is on"
    override val arrayRes: Int = R.array.screenIsOn
    override val categories: List<Int> = listOf(
        SUPPRESSED_EFFECT_BADGE,
        SUPPRESSED_EFFECT_STATUS_BAR,
        SUPPRESSED_EFFECT_PEEK,
        SUPPRESSED_EFFECT_NOTIFICATION_LIST
    )

    override fun applyChanges(mask: Int) {
        parentFragmentManager.setFragmentResult(
            InterruptionFilterFragment.EFFECTS_REQUEST_KEY, Bundle().apply {
                putInt(EXTRA_CATEGORIES, mask)
                putInt(EXTRA_MODE, 1)
            })
    }

    companion object {

        fun newInstance(profile: Profile): SuppressedEffectsOnDialog {
            return SuppressedEffectsOnDialog().apply {
                arguments = Bundle().apply {
                    putInt(EXTRA_CATEGORIES, profile.screenOnVisualEffects)
                }
            }
        }
    }
}