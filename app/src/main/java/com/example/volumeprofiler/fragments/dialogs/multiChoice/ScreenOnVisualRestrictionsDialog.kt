package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.app.NotificationManager.Policy.*
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.ArrayRes
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.VisualRestrictionsCallback
import com.example.volumeprofiler.models.Profile
import java.lang.NumberFormatException
import java.util.*

class ScreenOnVisualRestrictionsDialog: BaseMultiChoiceDialog<List<Int>>() {

    override val optionsMap: ArrayMap<Int, List<Int>> = arrayMapOf(
            0 to listOf(SUPPRESSED_EFFECT_BADGE),
            1 to listOf(SUPPRESSED_EFFECT_STATUS_BAR),
            2 to listOf(SUPPRESSED_EFFECT_PEEK, SUPPRESSED_EFFECT_SCREEN_ON),
            3 to listOf(SUPPRESSED_EFFECT_NOTIFICATION_LIST)
    )
    override val title: String = "When the screen is on"
    private var callbacks: VisualRestrictionsCallback? = null

    @get:ArrayRes
    override val arrayRes: Int = R.array.screenIsOn

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = targetFragment as VisualRestrictionsCallback
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onApply(string: String) {
        callbacks?.onEffectsSelected(string, 1)
    }

    override fun constructString(): String {
        val stringBuilder: StringBuilder = StringBuilder()
        for (i in selectedItems) {
            for (j in optionsMap[i]!!) {
                stringBuilder.append("$j,")
            }
        }
        return stringBuilder.toString()
    }

    override fun getKey(value: String): Int? {
        var result: Int? = null
        try {
            val num: Int = value.toInt()
            for ((key, value1) in optionsMap.entries) {
                if (Objects.equals(num, value1[0])) {
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

        fun newInstance(profile: Profile): ScreenOnVisualRestrictionsDialog {
            val arguments: Bundle = Bundle().apply {
                val arg: String = profile.screenOnVisualEffects
                this.putString(ARG_SELECTED_ITEMS, arg)
            }
            return ScreenOnVisualRestrictionsDialog().apply {
                this.arguments = arguments
            }
        }
    }
}