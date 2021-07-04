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
import android.util.Log
import java.lang.NumberFormatException
import java.util.*

class ScreenOffVisualRestrictions : BaseMultiChoiceDialog<Int>() {

    override val title: String = "When the screen is off"
    override val optionsMap: ArrayMap<Int, Int> = arrayMapOf(
            0 to SUPPRESSED_EFFECT_FULL_SCREEN_INTENT,
            1 to SUPPRESSED_EFFECT_AMBIENT
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
        callbacks?.onPolicySelected(string)
    }

    override fun constructString(): String {
        val stringBuilder: StringBuilder = StringBuilder()
        for (i in selectedItems) {
            stringBuilder.append("${optionsMap[i]},")
        }
        return stringBuilder.toString()
    }

    override fun getKey(value: String): Int? {
        var result: Int? = null
        try {
            val num: Int = value.toInt()
            for ((key, value1) in optionsMap.entries) {
                if (Objects.equals(num, value1)) {
                    result = key
                    break
                }
            }
        }
        catch (e: NumberFormatException) {
            Log.d("Dialog", "NumberFormatException", e)
        }
        return result
    }

    companion object {

        fun newInstance(profile: Profile): ScreenOffVisualRestrictions {
            val arguments: Bundle = Bundle().apply {
                val arg: String = profile.suppressedVisualEffects!!
                this.putString(ARG_SELECTED_ITEMS, arg)
            }
            return ScreenOffVisualRestrictions().apply {
                this.arguments = arguments
            }
        }
    }
}