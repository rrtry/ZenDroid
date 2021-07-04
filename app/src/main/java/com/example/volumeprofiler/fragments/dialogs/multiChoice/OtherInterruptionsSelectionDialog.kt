package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.example.volumeprofiler.R
import com.example.volumeprofiler.models.Profile
import android.app.NotificationManager.Policy.*
import android.util.Log
import androidx.annotation.ArrayRes
import com.example.volumeprofiler.interfaces.OtherInterruptionsCallback
import java.lang.NumberFormatException
import java.util.*

class OtherInterruptionsSelectionDialog: BaseMultiChoiceDialog<Int>() {

    private var callbacks: OtherInterruptionsCallback? = null
    override val optionsMap: ArrayMap<Int, Int> = arrayMapOf(
            0 to PRIORITY_CATEGORY_ALARMS,
            1 to PRIORITY_CATEGORY_MEDIA,
            2 to PRIORITY_CATEGORY_SYSTEM,
            3 to PRIORITY_CATEGORY_REMINDERS,
            4 to PRIORITY_CATEGORY_EVENTS)

    @get:ArrayRes
    override val arrayRes: Int
    get() = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
        R.array.priorityCategoriesApi23 else R.array.priorityCategoriesApi28
    override val title: String = "Other interruptions"

    override fun constructString(): String {
        val stringBuilder: StringBuilder = StringBuilder()
        for (i in selectedItems) {
            stringBuilder.append("${optionsMap[i]},")
        }
        return stringBuilder.toString()
    }

    override fun onAttach(context: Context) {
        callbacks = targetFragment as OtherInterruptionsCallback
        super.onAttach(context)
    }

    override fun onDetach() {
        callbacks = null
        super.onDetach()
    }

    override fun onApply(string: String) {
        callbacks?.onPrioritySelected(string)
    }

    companion object {

        fun newInstance(profile: Profile): OtherInterruptionsSelectionDialog {
            val arguments: Bundle = Bundle().apply {
                val arg: String = profile.suppressedVisualEffects!!
                this.putString(ARG_SELECTED_ITEMS,arg)
            }
            return OtherInterruptionsSelectionDialog().apply {
                this.arguments = arguments
            }
        }
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
}