package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.os.Build
import android.os.Bundle
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.example.volumeprofiler.R
import android.app.NotificationManager.Policy.*
import androidx.annotation.ArrayRes
import com.example.volumeprofiler.fragments.InterruptionFilterFragment

class PriorityInterruptionsSelectionDialog: BaseMultiChoiceDialog<Int>() {

    @get:ArrayRes
    override val arrayRes: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
        R.array.priorityCategoriesApi23 else R.array.priorityCategoriesApi28

    override val optionsMap: ArrayMap<Int, Int> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) arrayMapOf(
        0 to PRIORITY_CATEGORY_ALARMS,
        1 to PRIORITY_CATEGORY_MEDIA,
        2 to PRIORITY_CATEGORY_SYSTEM,
        3 to PRIORITY_CATEGORY_REMINDERS,
        4 to PRIORITY_CATEGORY_EVENTS)
    else arrayMapOf(
        0 to PRIORITY_CATEGORY_REMINDERS,
        1 to PRIORITY_CATEGORY_EVENTS)

    override val title: String = TITLE

    @Suppress("unchecked_cast")
    override fun onApply(arrayList: ArrayList<Int>): Unit {
        val callsAndMessagesInterruptions: List<Int>? = (arguments?.getSerializable(ARG_SELECTED_ITEMS) as? ArrayList<Int>)?.filter { !optionsMap.values.contains(it) }
        val mergedPriorityInterruptions: List<Int> = (callsAndMessagesInterruptions as ArrayList<Int>) + arrayList
        val bundle: Bundle = Bundle().apply {
            this.putIntegerArrayList(InterruptionFilterFragment.PRIORITY_CATEGORIES_KEY, (mergedPriorityInterruptions as ArrayList<Int>))
        }
        parentFragmentManager.setFragmentResult(InterruptionFilterFragment.PRIORITY_REQUEST_KEY, bundle)
    }

    companion object {

        private const val TITLE: String = "Other interruptions"

        fun newInstance(priorityCategories: ArrayList<Int>): PriorityInterruptionsSelectionDialog {
            val arguments: Bundle = Bundle().apply {
                this.putSerializable(ARG_SELECTED_ITEMS,priorityCategories)
            }
            return PriorityInterruptionsSelectionDialog().apply {
                this.arguments = arguments
            }
        }
    }
}