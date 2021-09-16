package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.app.NotificationManager.Policy.*
import android.os.Bundle
import androidx.annotation.ArrayRes
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.example.volumeprofiler.R
import com.example.volumeprofiler.fragments.InterruptionFilterFragment
import java.util.*
import kotlin.collections.ArrayList

class ScreenOnVisualRestrictionsDialog: BaseMultiChoiceDialog<List<Int>>() {

    override val optionsMap: ArrayMap<Int, List<Int>> = arrayMapOf(
            0 to listOf(SUPPRESSED_EFFECT_BADGE),
            1 to listOf(SUPPRESSED_EFFECT_STATUS_BAR),
            2 to listOf(SUPPRESSED_EFFECT_PEEK, SUPPRESSED_EFFECT_SCREEN_ON),
            3 to listOf(SUPPRESSED_EFFECT_NOTIFICATION_LIST)
    )
    override val title: String = TITLE

    @get:ArrayRes
    override val arrayRes: Int = R.array.screenIsOn

    override fun onApply(arrayList: ArrayList<Int>) {
        val bundle: Bundle = Bundle().apply {
            this.putIntegerArrayList(InterruptionFilterFragment.EFFECTS_KEY, arrayList)
            this.putInt(InterruptionFilterFragment.EFFECTS_TYPE_KEY, 1)
        }
        parentFragmentManager.setFragmentResult(InterruptionFilterFragment.EFFECTS_REQUEST_KEY, bundle)
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

        private const val TITLE: String = "When the screen is on"

        fun newInstance(screenOnVisualEffects: ArrayList<Int>): ScreenOnVisualRestrictionsDialog {
            val arguments: Bundle = Bundle().apply {
                this.putSerializable(ARG_SELECTED_ITEMS, screenOnVisualEffects)
            }
            return ScreenOnVisualRestrictionsDialog().apply {
                this.arguments = arguments
            }
        }
    }
}