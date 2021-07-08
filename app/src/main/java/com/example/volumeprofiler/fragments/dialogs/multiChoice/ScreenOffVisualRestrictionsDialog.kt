package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.content.Context
import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.VisualRestrictionsCallback
import com.example.volumeprofiler.models.Profile
import android.app.NotificationManager.Policy.*

class ScreenOffVisualRestrictionsDialog : BaseMultiChoiceDialog<Int>() {

    override val title: String = "When the screen is off"
    override val optionsMap: ArrayMap<Int, Int> = arrayMapOf(
        0 to SUPPRESSED_EFFECT_LIGHTS,
        1 to SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
        2 to SUPPRESSED_EFFECT_AMBIENT
    )
    private var callbacks: VisualRestrictionsCallback? = null

    @get:ArrayRes
    override val arrayRes: Int = R.array.screenIsOff

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = targetFragment as VisualRestrictionsCallback
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onApply(string: String) {
        callbacks?.onEffectsSelected(string, 0)
    }

    companion object {

        fun newInstance(profile: Profile): ScreenOffVisualRestrictionsDialog {
            val arguments: Bundle = Bundle().apply {
                val arg: String = profile.screenOffVisualEffects
                this.putString(ARG_SELECTED_ITEMS, arg)
            }
            return ScreenOffVisualRestrictionsDialog().apply {
                this.arguments = arguments
            }
        }
    }
}