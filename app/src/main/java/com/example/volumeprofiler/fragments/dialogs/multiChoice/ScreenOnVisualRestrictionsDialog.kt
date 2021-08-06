package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.app.NotificationManager.Policy.*
import android.content.Context
import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.example.volumeprofiler.R
import com.example.volumeprofiler.interfaces.VisualRestrictionsCallback
import com.example.volumeprofiler.models.Profile
import java.util.*
import kotlin.collections.ArrayList

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

    override fun onApply(arrayList: ArrayList<Int>) {
        callbacks?.onEffectsSelected(arrayList, 1)
    }

    override fun getArrayList(): ArrayList<Int> {
        val arrayList: ArrayList<Int> = arrayListOf()
        for (i in selectedItems) {
            for (j in optionsMap[i]!!) {
                arrayList.add(j)
            }
        }
        return arrayList
    }

    override fun getKey(value: Int): Int? {
        var result: Int? = null
        for ((key, value1) in optionsMap.entries) {
            if (Objects.equals(value, value1[0])) {
                result = key
                break
            }
        }
        return result
    }

    companion object {

        fun newInstance(profile: Profile): ScreenOnVisualRestrictionsDialog {
            val arguments: Bundle = Bundle().apply {
                val arg: ArrayList<Int> = profile.screenOnVisualEffects
                this.putSerializable(ARG_SELECTED_ITEMS, arg)
            }
            return ScreenOnVisualRestrictionsDialog().apply {
                this.arguments = arguments
            }
        }
    }
}