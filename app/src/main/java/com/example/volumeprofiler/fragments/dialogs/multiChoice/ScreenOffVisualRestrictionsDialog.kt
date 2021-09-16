package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.example.volumeprofiler.R
import android.app.NotificationManager.Policy.*
import com.example.volumeprofiler.fragments.InterruptionFilterFragment

class ScreenOffVisualRestrictionsDialog : BaseMultiChoiceDialog<Int>() {

    override val title: String = TITLE
    override val optionsMap: ArrayMap<Int, Int> = arrayMapOf(
        0 to SUPPRESSED_EFFECT_LIGHTS,
        1 to SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
        2 to SUPPRESSED_EFFECT_AMBIENT
    )

    @get:ArrayRes
    override val arrayRes: Int = R.array.screenIsOff

    override fun onApply(arrayList: ArrayList<Int>) {
        val bundle: Bundle = Bundle().apply {
            this.putIntegerArrayList(InterruptionFilterFragment.EFFECTS_KEY, arrayList)
            this.putInt(InterruptionFilterFragment.EFFECTS_TYPE_KEY, 0)
        }
        parentFragmentManager.setFragmentResult(InterruptionFilterFragment.EFFECTS_REQUEST_KEY, bundle)
    }

    companion object {

        private const val TITLE: String = "When the screen is off"

        fun newInstance(screenOffVisualEffects: ArrayList<Int>): ScreenOffVisualRestrictionsDialog {
            val arguments: Bundle = Bundle().apply {
                this.putSerializable(ARG_SELECTED_ITEMS, screenOffVisualEffects)
            }
            return ScreenOffVisualRestrictionsDialog().apply {
                this.arguments = arguments
            }
        }
    }
}