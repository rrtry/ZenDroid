package com.example.volumeprofiler.fragments.dialogs.multiChoice

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.collection.ArrayMap
import androidx.collection.arrayMapOf
import com.example.volumeprofiler.R
import com.example.volumeprofiler.models.Profile
import android.app.NotificationManager.Policy.*
import androidx.annotation.ArrayRes
import com.example.volumeprofiler.interfaces.OtherInterruptionsCallback

class OtherInterruptionsSelectionDialog: BaseMultiChoiceDialog<Int>() {

    private var callbacks: OtherInterruptionsCallback? = null

    @get:ArrayRes
    override val arrayRes: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
        R.array.priorityCategoriesApi23 else R.array.priorityCategoriesApi28

    override val optionsMap: ArrayMap<Int, Int> = if (arrayRes == R.array.priorityCategoriesApi28) arrayMapOf(
        0 to PRIORITY_CATEGORY_ALARMS,
        1 to PRIORITY_CATEGORY_MEDIA,
        2 to PRIORITY_CATEGORY_SYSTEM,
        3 to PRIORITY_CATEGORY_REMINDERS,
        4 to PRIORITY_CATEGORY_EVENTS) else arrayMapOf(
        0 to PRIORITY_CATEGORY_REMINDERS,
        1 to PRIORITY_CATEGORY_EVENTS)

    override val title: String = "Other interruptions"

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
                val arg: String = profile.priorityCategories
                this.putString(ARG_SELECTED_ITEMS,arg)
            }
            return OtherInterruptionsSelectionDialog().apply {
                this.arguments = arguments
            }
        }
    }
}