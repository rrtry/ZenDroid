package com.example.volumeprofiler.fragments

import android.annotation.TargetApi
import android.app.NotificationManager.Policy.*
import android.os.Build
import android.os.Bundle
import com.example.volumeprofiler.R
import com.example.volumeprofiler.entities.Profile

@TargetApi(Build.VERSION_CODES.P)
class SuppressedEffectsOffDialog : BaseDialog() {

    override val title: String = "When screen is off"
    override val arrayRes: Int = R.array.screenIsOff
    override val categories: List<Int> = listOf(
        SUPPRESSED_EFFECT_LIGHTS,
        SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
        SUPPRESSED_EFFECT_AMBIENT
    )

    override fun applyChanges(mask: Int) {
        parentFragmentManager.setFragmentResult(
            InterruptionFilterFragment.EFFECTS_REQUEST_KEY, Bundle().apply {
                putInt(EXTRA_MASK, mask)
            })
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